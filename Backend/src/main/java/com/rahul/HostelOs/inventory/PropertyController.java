package com.rahul.HostelOs.inventory;

import com.rahul.HostelOs.inventory.dto.ChangePropertyStatusRequest;
import com.rahul.HostelOs.inventory.dto.CreatePropertyRequest;
import com.rahul.HostelOs.inventory.dto.PropertyResponse;
import com.rahul.HostelOs.inventory.dto.UpdatePropertyRequest;
import com.rahul.HostelOs.organization.OrgRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/properties")
@Tag(name = "Property Management", description = "Endpoints for managing hostel properties within organizations.")
public class PropertyController {

	private final PropertyManagementService propertyManagementService;

	public PropertyController(PropertyManagementService propertyManagementService) {
		this.propertyManagementService = Objects.requireNonNull(propertyManagementService, "PropertyManagementService must not be null");
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORGANIZATION_ADMIN)")
	@Operation(summary = "Create property", description = "Creates a new property under the specified organization. Requires ORGANIZATION_ADMIN.")
	public ResponseEntity<PropertyResponse> createProperty(
			@PathVariable("organizationId") Long organizationId,
			@Valid @RequestBody CreatePropertyRequest request) {
		PropertyResponse response = propertyManagementService.createProperty(organizationId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORG_STAFF)")
	@Operation(summary = "List properties", description = "Lists properties within the organization, optionally filtered by status. Requires ORG_STAFF or ORGANIZATION_ADMIN.")
	public ResponseEntity<List<PropertyResponse>> listProperties(
			@PathVariable("organizationId") Long organizationId,
			@RequestParam(name = "status", required = false) PropertyStatus status) {
		List<PropertyResponse> response = propertyManagementService.listProperties(organizationId, status);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{propertyId}")
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORG_STAFF)")
	@Operation(summary = "Get property", description = "Retrieves details of a specific property. Requires ORG_STAFF or ORGANIZATION_ADMIN.")
	public ResponseEntity<PropertyResponse> getProperty(
			@PathVariable("organizationId") Long organizationId,
			@PathVariable("propertyId") Long propertyId) {
		PropertyResponse response = propertyManagementService.getProperty(organizationId, propertyId);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{propertyId}")
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORGANIZATION_ADMIN)")
	@Operation(summary = "Update property", description = "Replaces mutable configuration details of an existing property. Requires ORGANIZATION_ADMIN.")
	public ResponseEntity<PropertyResponse> updateProperty(
			@PathVariable("organizationId") Long organizationId,
			@PathVariable("propertyId") Long propertyId,
			@Valid @RequestBody UpdatePropertyRequest request) {
		PropertyResponse response = propertyManagementService.updateProperty(organizationId, propertyId, request);
		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{propertyId}/status")
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORGANIZATION_ADMIN)")
	@Operation(summary = "Change property status", description = "Transitions the operational status of an existing property. Requires ORGANIZATION_ADMIN.")
	public ResponseEntity<PropertyResponse> changeStatus(
			@PathVariable("organizationId") Long organizationId,
			@PathVariable("propertyId") Long propertyId,
			@Valid @RequestBody ChangePropertyStatusRequest request) {
		PropertyResponse response = propertyManagementService.changeStatus(organizationId, propertyId, request);
		return ResponseEntity.ok(response);
	}
}
