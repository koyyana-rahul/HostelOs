package com.rahul.HostelOs.identity;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

	@EntityGraph(attributePaths = "roles")
	Optional<UserAccount> findByLoginIdentifier(String loginIdentifier);

	boolean existsByLoginIdentifier(String loginIdentifier);

	boolean existsByIdAndStatus(Long id, AccountStatus status);
}
