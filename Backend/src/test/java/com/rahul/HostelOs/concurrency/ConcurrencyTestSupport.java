package com.rahul.HostelOs.concurrency;

import com.rahul.HostelOs.identity.HostelOsPrincipal;
import com.rahul.HostelOs.identity.UserAccount;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.mockito.stubbing.Answer;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public final class ConcurrencyTestSupport {

	public static final String MYSQL_TEST_CONDITION =
			"com.rahul.HostelOs.concurrency.ConcurrencyTestSupport#isMysqlTestSelected";

	static final String PROFILE_PROPERTY = "hostelos.test.profile";
	static final String PROFILE_ENVIRONMENT_VARIABLE = "HOSTELOS_TEST_PROFILE";
	private static final String MYSQL_TEST_PROFILE = "mysql-test";
	private static final long RESULT_TIMEOUT_SECONDS = 10L;
	private static final long BLOCKED_CHECK_MILLIS = 500L;
	private static final AtomicInteger WORKER_COUNTER = new AtomicInteger();

	private final JdbcTemplate jdbcTemplate;
	private final Set<Long> organizationIds = new LinkedHashSet<>();
	private final Set<Long> membershipIds = new LinkedHashSet<>();
	private final Set<Long> propertyIds = new LinkedHashSet<>();
	private final Set<Long> userIds = new LinkedHashSet<>();

	public ConcurrencyTestSupport(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate must not be null");
	}

	public static boolean isMysqlTestSelected() {
		String requested = System.getProperty(PROFILE_PROPERTY);
		if (requested == null) {
			requested = System.getenv(PROFILE_ENVIRONMENT_VARIABLE);
		}
		return requested != null && MYSQL_TEST_PROFILE.equals(requested.trim().toLowerCase(Locale.ROOT));
	}

	public static void assumeMysqlTestProfile(Environment environment) {
		Assumptions.assumeTrue(
				environment.acceptsProfiles(Profiles.of(MYSQL_TEST_PROFILE)),
				"Phase 2F concurrency tests require the mysql-test profile");
	}

	public static void requireNoActiveTransaction() {
		assertThat(TransactionSynchronizationManager.isActualTransactionActive())
				.as("concurrency workers must not inherit a test-managed transaction")
				.isFalse();
	}

	public static void installAuthentication(UserAccount user) {
		HostelOsPrincipal principal = HostelOsPrincipal.from(user);
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				principal, null, principal.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	public static void clearAuthentication() {
		SecurityContextHolder.clearContext();
	}

	public static <T> T callAs(UserAccount user, Callable<T> action) throws Exception {
		installAuthentication(user);
		try {
			return action.call();
		} finally {
			clearAuthentication();
		}
	}

	public static <T> Callable<T> authenticated(UserAccount user, Callable<T> action) {
		return () -> {
			requireNoActiveTransaction();
			return callAs(user, action);
		};
	}

	public static ExecutorService newExecutor() {
		return Executors.newFixedThreadPool(2, runnable -> {
			Thread thread = new Thread(runnable, "p2f-worker-" + WORKER_COUNTER.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		});
	}

	public static void shutdown(ExecutorService executor) {
		executor.shutdownNow();
		try {
			executor.awaitTermination(5L, TimeUnit.SECONDS);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	public static <T> List<Outcome<T>> runTwo(ExecutorService executor, List<Callable<T>> tasks) throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		List<Future<T>> futures = new ArrayList<>();
		for (Callable<T> task : tasks) {
			futures.add(executor.submit(() -> {
				start.await();
				return task.call();
			}));
		}
		start.countDown();
		return awaitAll(futures);
	}

	public static <T> List<Outcome<T>> awaitAll(List<Future<T>> futures) throws Exception {
		List<Outcome<T>> outcomes = new ArrayList<>();
		for (Future<T> future : futures) {
			try {
				outcomes.add(Outcome.success(future.get(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)));
			} catch (ExecutionException ex) {
				Throwable cause = ex.getCause() == null ? ex : ex.getCause();
				outcomes.add(Outcome.failure(cause));
			} catch (TimeoutException ex) {
				futures.forEach(pending -> pending.cancel(true));
				throw ex;
			}
		}
		return outcomes;
	}

	public static void assertBlocked(Future<?> future) {
		assertThatThrownBy(() -> future.get(BLOCKED_CHECK_MILLIS, TimeUnit.MILLISECONDS))
				.as("second worker must block on the real database lock while the first transaction holds it")
				.isInstanceOf(TimeoutException.class);
	}

	public static <T> void assertExactlyOneSuccessAndOneFailure(
			List<Outcome<T>> outcomes,
			Class<? extends Throwable> expectedFailureType) {
		assertThat(outcomes).hasSize(2);
		List<Outcome<T>> successes = outcomes.stream().filter(Outcome::succeeded).toList();
		List<Outcome<T>> failures = outcomes.stream().filter(outcome -> !outcome.succeeded()).toList();
		assertThat(successes).as("successful outcomes").hasSize(1);
		assertThat(failures).as("failed outcomes").hasSize(1);
		Throwable failure = failures.get(0).failure();
		assertThat(failure)
				.as("failure type %s; category=%s", failure.getClass().getName(), describeFailure(failure))
				.isInstanceOf(expectedFailureType);
	}

	public static String describeFailure(Throwable failure) {
		if (failure == null) {
			return "none";
		}
		if (failure instanceof DeadlockLoserDataAccessException) {
			return "deadlock";
		}
		if (failure instanceof CannotAcquireLockException) {
			return "lock wait timeout or lock acquisition failure";
		}
		if (failure instanceof CannotGetJdbcConnectionException) {
			return "connection or pool failure";
		}
		if (failure instanceof DataAccessResourceFailureException) {
			return "database resource failure";
		}
		if (failure instanceof PessimisticLockingFailureException) {
			return "pessimistic locking failure";
		}
		return failure.getClass().getName();
	}

	public void trackOrganization(Long organizationId) {
		if (organizationId != null) {
			organizationIds.add(organizationId);
		}
	}

	public void trackMembership(Long membershipId) {
		if (membershipId != null) {
			membershipIds.add(membershipId);
		}
	}

	public void trackProperty(Long propertyId) {
		if (propertyId != null) {
			propertyIds.add(propertyId);
		}
	}

	public void trackUser(Long userId) {
		if (userId != null) {
			userIds.add(userId);
		}
	}

	public long count(String sql, Object... args) {
		Long result = jdbcTemplate.queryForObject(sql, Long.class, args);
		return result == null ? 0L : result;
	}

	public DatabaseSnapshot snapshot() {
		return new DatabaseSnapshot(
				count("SELECT COUNT(*) FROM user_accounts"),
				count("SELECT COUNT(*) FROM user_roles"),
				count("SELECT COUNT(*) FROM organizations"),
				count("SELECT COUNT(*) FROM organization_memberships"),
				count("SELECT COUNT(*) FROM properties"),
				count("SELECT COUNT(*) FROM roles"),
				count("SELECT COUNT(*) FROM flyway_schema_history"));
	}

	public void cleanup() {
		deleteByIds("properties", "id", propertyIds);
		deleteMemberships();
		deleteByIds("organizations", "id", organizationIds);
		deleteByIds("user_roles", "user_id", userIds);
		deleteByIds("user_accounts", "id", userIds);
		organizationIds.clear();
		membershipIds.clear();
		propertyIds.clear();
		userIds.clear();
	}

	private void deleteMemberships() {
		if (organizationIds.isEmpty() && membershipIds.isEmpty()) {
			return;
		}
		List<String> predicates = new ArrayList<>();
		List<Object> arguments = new ArrayList<>();
		if (!organizationIds.isEmpty()) {
			predicates.add("organization_id IN (" + placeholders(organizationIds.size()) + ")");
			arguments.addAll(organizationIds);
		}
		if (!membershipIds.isEmpty()) {
			predicates.add("id IN (" + placeholders(membershipIds.size()) + ")");
			arguments.addAll(membershipIds);
		}
		jdbcTemplate.update(
				"DELETE FROM organization_memberships WHERE " + String.join(" OR ", predicates),
				arguments.toArray());
	}

	private void deleteByIds(String table, String idColumn, Set<Long> ids) {
		if (ids.isEmpty()) {
			return;
		}
		jdbcTemplate.update(
				"DELETE FROM " + table + " WHERE " + idColumn + " IN (" + placeholders(ids.size()) + ")",
				ids.toArray());
	}

	private static String placeholders(int count) {
		return String.join(", ", Collections.nCopies(count, "?"));
	}

	public record DatabaseSnapshot(
			long userAccounts,
			long userRoles,
			long organizations,
			long organizationMemberships,
			long properties,
			long roles,
			long flywaySchemaHistory) {
	}

	public record Outcome<T>(T value, Throwable failure) {

		public static <T> Outcome<T> success(T value) {
			return new Outcome<>(value, null);
		}

		public static <T> Outcome<T> failure(Throwable failure) {
			return new Outcome<>(null, failure);
		}

		public boolean succeeded() {
			return failure == null;
		}
	}

	public static final class PrecheckGate {

		private final CountDownLatch bothObservedFalse = new CountDownLatch(2);
		private final CountDownLatch release = new CountDownLatch(1);

		public Answer<Object> answer() {
			return invocation -> {
				Object result = invocation.callRealMethod();
				if (Boolean.FALSE.equals(result)) {
					bothObservedFalse.countDown();
					awaitRelease(release, "pre-check release");
				}
				return result;
			};
		}

		public void awaitBothFalse() {
			await(bothObservedFalse, "both pre-check calls to observe false");
		}

		public void releasePrechecks() {
			release.countDown();
		}
	}

	public static final class OrganizationLockGate {

		private final CountDownLatch firstLocked = new CountDownLatch(1);
		private final CountDownLatch secondAttempted = new CountDownLatch(1);
		private final CountDownLatch releaseFirst = new CountDownLatch(1);
		private final AtomicInteger invocations = new AtomicInteger();

		public Answer<Object> answer() {
			return invocation -> {
				int invocationNumber = invocations.incrementAndGet();
				if (invocationNumber == 1) {
					Object result = invocation.callRealMethod();
					firstLocked.countDown();
					awaitRelease(releaseFirst, "first organization lock release");
					return result;
				}
				if (invocationNumber == 2) {
					secondAttempted.countDown();
				}
				return invocation.callRealMethod();
			};
		}

		public void awaitFirstLocked() {
			await(firstLocked, "first organization lock acquisition");
		}

		public void awaitSecondAttempted() {
			await(secondAttempted, "second organization lock attempt");
		}

		public void releaseFirst() {
			releaseFirst.countDown();
		}
	}

	private static void await(CountDownLatch latch, String description) {
		try {
			if (!latch.await(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				throw new AssertionError("Timed out waiting for " + description);
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupted while waiting for " + description, ex);
		}
	}

	private static void awaitRelease(CountDownLatch latch, String description) {
		try {
			if (!latch.await(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Timed out waiting for " + description);
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for " + description, ex);
		}
	}

	public static final class DualCaptureGate {

		private final CyclicBarrier barrier = new CyclicBarrier(2, () -> {});

		private final AtomicInteger callCounter = new AtomicInteger(0);

		private final boolean[] results = { false, false };

		public Answer<Object> answer() {
			return invocation -> {
				try {
					barrier.await(10, TimeUnit.SECONDS);
				} catch (BrokenBarrierException | InterruptedException e) {
					throw new RuntimeException(
							"DualCaptureGate barrier timed out or was interrupted", e);
				}
				// Both workers have passed the barrier; capture the real result
				boolean dbResult = (Boolean) invocation.callRealMethod();
				int idx = callCounter.getAndIncrement(); // 0 then 1
				results[idx] = dbResult;
				return dbResult;
			};
		}

		public void assertBothFalse() {
			assertThat(!results[0])
					.as("first existsBySlug must return false")
					.isTrue();
			assertThat(!results[1])
					.as("second existsBySlug must return false")
					.isTrue();
		}
	}
}
