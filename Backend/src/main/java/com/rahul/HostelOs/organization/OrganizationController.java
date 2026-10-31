package com.rahul.HostelOs.organization;

import com.rahul.HostelOs.identity.HostelOsPrincipal;
import com.rahul.HostelOs.organization.dto.AddMemberRequest;
import com.rahul.HostelOs.organization.dto.ChangeMemberRoleRequest;
import com.rahul.HostelOs.organization.dto.CreateOrganizationRequest;
import com.rahul.HostelOs.organization.dto.MembershipResponse;
import com.rahul.HostelOs.organization.dto.OrganizationResponse;
import com.rahul.HostelOs.organization.dto.ReactivateMemberRequest;
import com.rahul.HostelOs.organization.exception.OrganizationAccessDeniedException;
import com.rahul.HostelOs.organization.exception.OrganizationNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations", description = "Organization provisioning and membership access")
@SecurityRequirement(name = "sessionAuth")
public class OrganizationController {

	private final OrganizationProvisioningService provisioningService;
	private final OrganizationRepository organizationRepository;
	private final OrganizationMembershipRepository membershipRepository;
	private final MembershipManagementService membershipManagementService;

	public OrganizationController(
			OrganizationProvisioningService provisioningService,
			OrganizationRepository organizationRepository,
			OrganizationMembershipRepository membershipRepository,
			MembershipManagementService membershipManagementService) {
		this.provisioningService = provisioningService;
		this.organizationRepository = organizationRepository;
		this.membershipRepository = membershipRepository;
		this.membershipManagementService = membershipManagementService;
	}

	@Operation(summary = "Create a new organization")
	@ApiResponse(responseCode = "201", description = "Organization created")
	@ApiResponse(responseCode = "400", description = "Invalid request")
	@ApiResponse(responseCode = "401", description = "Authentication required")
	@ApiResponse(responseCode = "409", description = "Duplicate organization slug")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrganizationResponse createOrganization(
			@Valid @RequestBody CreateOrganizationRequest request,
			@AuthenticationPrincipal HostelOsPrincipal principal) {
		return provisioningService.provisionOrganization(request, principal.getUserId());
	}

	@Operation(summary = "List accessible organizations for the current user")
	@ApiResponse(responseCode = "200", description = "Organizations returned")
	@ApiResponse(responseCode = "401", description = "Authentication required")
	@GetMapping
	public List<OrganizationResponse> listOrganizations(@AuthenticationPrincipal HostelOsPrincipal principal) {
		List<OrganizationMembership> memberships = membershipRepository.findByUserIdAndStatus(principal.getUserId(), MembershipStatus.ACTIVE);
		return memberships.stream()
				.map(m -> OrganizationResponse.from(m.getOrganization()))
				.toList();
	}

	@Operation(summary = "Get organization details")
	@ApiResponse(responseCode = "200", description = "Organization returned")
	@ApiResponse(responseCode = "401", description = "Authentication required")
	@ApiResponse(responseCode = "403", description = "Access denied")
	@ApiResponse(responseCode = "404", description = "Organization not found")
	@GetMapping("/{organizationId}")
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORG_STAFF)")
	public OrganizationResponse getOrganization(@PathVariable Long organizationId) {
		Organization organization = organizationRepository.findById(organizationId)
				.orElseThrow(() -> new OrganizationNotFoundException("Organization not found: " + organizationId));
		if (organization.getStatus() != OrganizationStatus.ACTIVE) {
			throw new OrganizationAccessDeniedException("Organization is not active: " + organizationId);
		}
		return OrganizationResponse.from(organization);
	}

	@Operation(summary = "List memberships for an organization")
	@ApiResponse(responseCode = "200", description = "Memberships returned")
	@ApiResponse(responseCode = "401", description = "Authentication required")
	@ApiResponse(responseCode = "403", description = "Access denied")
	@ApiResponse(responseCode = "404", description = "Organization not found")
	@GetMapping("/{organizationId}/memberships")
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORG_STAFF)")
	public List<MembershipResponse> listMemberships(@PathVariable Long organizationId) {
		Organization organization = organizationRepository.findById(organizationId)
				.orElseThrow(() -> new OrganizationNotFoundException("Organization not found: " + organizationId));
		if (organization.getStatus() != OrganizationStatus.ACTIVE) {
			throw new OrganizationAccessDeniedException("Organization is not active: " + organizationId);
		}
		List<OrganizationMembership> memberships = membershipRepository.findByOrganizationId(organizationId);
		return memberships.stream().map(MembershipResponse::from).toList();
	}

	@Operation(summary = "Add a member to an organization")
	@ApiResponse(responseCode = "201", description = "Member added")
	@ApiResponse(responseCode = "400", description = "Invalid request")
	@ApiResponse(responseCode = "401", description = "Authentication required")
	@ApiResponse(responseCode = "403", description = "Access denied")
	@ApiResponse(responseCode = "404", description = "Organization or user not found")
	@ApiResponse(responseCode = "409", description = "Membership already exists or invalid transition")
	@PostMapping("/{organizationId}/members")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("isAuthenticated()")
	public MembershipResponse addMember(
			@PathVariable Long organizationId,
			@Valid @RequestBody AddMemberRequest request) {
		return membershipManagementService.addMember(organizationId, request);
	}

	@Operation(summary = "Change member role")
	@ApiResponse(responseCode = "200", description = "Role changed")
	@ApiResponse(responseCode = "400", description = "Invalid request")
	@ApiResponse(responseCode = "401", description = "Authentication required")
	@ApiResponse(responseCode = "403", description = "Access denied")
	@ApiResponse(responseCode = "404", description = "Organization or membership not found")
	@ApiResponse(responseCode = "409", description = "Last admin protection or invalid transition")
	@PutMapping("/{organizationId}/members/{membershipId}/role")
	@PreAuthorize("isAuthenticated()")
	public MembershipResponse changeRole(
			@PathVariable Long organizationId,
			@PathVariable Long membershipId,
			@Valid @RequestBody ChangeMemberRoleRequest request) {
		return membershipManagementService.changeRole(organizationId, membershipId, request);
	}

	@Operation(summary = "Suspend a member")
	@ApiResponse(responseCode = "200", description = "Membership suspended")
	@ApiResponse(responseCode = "401", description = "Authentication required")
	@ApiResponse(responseCode = "403", description = "Access denied")
	@ApiResponse(responseCode = "404", description = "Organization or membership not found")
	@ApiResponse(responseCode = "409", description = "Last admin protection or invalid transition")
	@PostMapping("/{organizationId}/members/{membershipId}/suspend")
	@PreAuthorize("isAuthenticated()")
	public MembershipResponse suspendMember(
			@PathVariable Long organizationId,
			@PathVariable Long membershipId) {
		return membershipManagementService.suspendMembership(organizationId, membershipId);
	}

	@Operation(summary = "Revoke a member")
	@ApiResponse(responseCode = "200", description = "Membership revoked")
	@ApiResponse(responseCode = "401", description = "Authentication required")
	@ApiResponse(responseCode = "403", description = "Access denied")
	@ApiResponse(responseCode = "404", description = "Organization or membership not found")
	@ApiResponse(responseCode = "409", description = "Last admin protection or invalid transition")
	@DeleteMapping("/{organizationId}/members/{membershipId}")
	@PreAuthorize("isAuthenticated()")
	public MembershipResponse revokeMember(
			@PathVariable Long organizationId,
			@PathVariable Long membershipId) {
		return membershipManagementService.revokeMembership(organizationId, membershipId);
	}

	@Operation(summary = "Reactivate a member")
	@ApiResponse(responseCode = "200", description = "Membership reactivated")
	@ApiResponse(responseCode = "400", description = "Invalid request")
	@ApiResponse(responseCode = "401", description = "Authentication required")
	@ApiResponse(responseCode = "403", description = "Access denied")
	@ApiResponse(responseCode = "404", description = "Organization or membership not found")
	@ApiResponse(responseCode = "409", description = "Invalid transition")
	@PostMapping("/{organizationId}/members/{membershipId}/reactivate")
	@PreAuthorize("isAuthenticated()")
	public MembershipResponse reactivateMember(
			@PathVariable Long organizationId,
			@PathVariable Long membershipId,
			@Valid @RequestBody ReactivateMemberRequest request) {
		return membershipManagementService.reactivateMembership(organizationId, membershipId, request);
	}
}
