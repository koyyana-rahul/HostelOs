package com.rahul.HostelOs.organization;

import com.rahul.HostelOs.identity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "organization_memberships")
public class OrganizationMembership {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "organization_id", nullable = false, updatable = false)
	private Organization organization;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, updatable = false)
	private UserAccount user;

	@Enumerated(EnumType.STRING)
	@Column(name = "org_role", nullable = false, length = 30)
	private OrgRole orgRole;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private MembershipStatus status;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected OrganizationMembership() {
	}

	public OrganizationMembership(Organization organization, UserAccount user, OrgRole orgRole, MembershipStatus status) {
		this.organization = Objects.requireNonNull(organization, "Organization must not be null");
		this.user = Objects.requireNonNull(user, "User account must not be null");
		this.orgRole = Objects.requireNonNull(orgRole, "Org role must not be null");
		this.status = Objects.requireNonNull(status, "Membership status must not be null");
	}

	public Long getId() {
		return id;
	}

	public Organization getOrganization() {
		return organization;
	}

	public UserAccount getUser() {
		return user;
	}

	public OrgRole getOrgRole() {
		return orgRole;
	}

	public MembershipStatus getStatus() {
		return status;
	}

	public long getVersion() {
		return version;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void suspend() {
		if (this.status == MembershipStatus.REVOKED) {
			throw new IllegalStateException("Cannot suspend a revoked membership");
		}
		this.status = MembershipStatus.SUSPENDED;
	}

	public void revoke() {
		this.status = MembershipStatus.REVOKED;
	}

	public void reactivate(OrgRole newRole) {
		this.status = MembershipStatus.ACTIVE;
		if (newRole != null) {
			this.orgRole = newRole;
		}
	}

	public void changeRole(OrgRole newRole) {
		this.orgRole = Objects.requireNonNull(newRole, "Org role must not be null");
	}
}
