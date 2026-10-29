package com.rahul.HostelOs.identity;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

	private static final int MAX_ATTEMPTS = 5;
	private static final int MAX_TRACKED_KEYS = 10_000;
	private static final Duration WINDOW = Duration.ofMinutes(15);

	private final Clock clock;
	private final ConcurrentHashMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

	public LoginAttemptService(Clock clock) {
		this.clock = clock;
	}

	public boolean isBlocked(String rawLoginIdentifier, String remoteAddress) {
		pruneExpired();
		AttemptWindow window = attempts.get(key(rawLoginIdentifier, remoteAddress));
		return window != null && window.failures() >= MAX_ATTEMPTS && clock.instant().isBefore(window.expiresAt());
	}

	public void recordFailure(String rawLoginIdentifier, String remoteAddress) {
		Instant now = clock.instant();
		attempts.compute(key(rawLoginIdentifier, remoteAddress), (ignored, current) -> {
			if (current == null || !now.isBefore(current.expiresAt())) {
				return new AttemptWindow(1, now.plus(WINDOW));
			}
			return new AttemptWindow(current.failures() + 1, current.expiresAt());
		});
		if (attempts.size() > MAX_TRACKED_KEYS) {
			pruneExpired();
			if (attempts.size() > MAX_TRACKED_KEYS) {
				evictOldest(attempts.size() - MAX_TRACKED_KEYS);
			}
		}
	}

	public void recordSuccess(String rawLoginIdentifier, String remoteAddress) {
		attempts.remove(key(rawLoginIdentifier, remoteAddress));
	}

	private void pruneExpired() {
		Instant now = clock.instant();
		attempts.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
	}

	private void evictOldest(int count) {
		Iterator<String> iterator = attempts.keySet().iterator();
		while (iterator.hasNext() && count > 0) {
			iterator.remove();
			count--;
		}
	}

	private String key(String rawLoginIdentifier, String remoteAddress) {
		String normalized;
		try {
			normalized = LoginIdentifier.normalize(rawLoginIdentifier);
		} catch (RuntimeException exception) {
			normalized = rawLoginIdentifier == null ? "" : rawLoginIdentifier.trim().toLowerCase(java.util.Locale.ROOT);
		}
		return normalized + "\u0000" + (remoteAddress == null ? "unknown" : remoteAddress);
	}

	private record AttemptWindow(int failures, Instant expiresAt) {
	}
}
