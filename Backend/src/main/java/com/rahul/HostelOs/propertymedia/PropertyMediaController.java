package com.rahul.HostelOs.propertymedia;

import com.rahul.HostelOs.organization.OrgRole;
import com.rahul.HostelOs.propertymedia.dto.CompleteUploadRequest;
import com.rahul.HostelOs.propertymedia.dto.CreateUploadIntentRequest;
import com.rahul.HostelOs.propertymedia.dto.PropertyMediaResponse;
import com.rahul.HostelOs.propertymedia.dto.ReorderMediaRequest;
import com.rahul.HostelOs.propertymedia.dto.UploadIntentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/properties/{propertyId}/media")
@Tag(name = "Property Media", description = "Owner-only property image management")
public class PropertyMediaController {

	private final PropertyMediaService mediaService;

	public PropertyMediaController(PropertyMediaService mediaService) {
		this.mediaService = mediaService;
	}

	@PostMapping("/uploads/signature")
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORGANIZATION_ADMIN)")
	@Operation(summary = "Create a signed image upload")
	public ResponseEntity<UploadIntentResponse> createUploadIntent(
			@PathVariable Long organizationId,
			@PathVariable Long propertyId,
			@Valid @RequestBody CreateUploadIntentRequest request) {
		PropertyMediaService.UploadIntent intent = mediaService.createUploadIntent(
				organizationId,
				propertyId,
				new PropertyMediaService.CreateUploadIntentRequest(
						request.mimeType(),
						request.byteSize(),
						request.checksumSha256()));
		return ResponseEntity.status(HttpStatus.CREATED).body(
				UploadIntentResponse.from(intent.mediaId(), intent.providerType(), intent.instructions()));
	}

	@PostMapping("/{mediaId}/complete")
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORGANIZATION_ADMIN)")
	@Operation(summary = "Verify and complete an image upload")
	public PropertyMediaResponse completeUpload(
			@PathVariable Long organizationId,
			@PathVariable Long propertyId,
			@PathVariable Long mediaId,
			@Valid @RequestBody CompleteUploadRequest request) {
		PropertyMedia media = mediaService.completeUpload(
				organizationId,
				propertyId,
				mediaId,
				new PropertyMediaService.CompleteUploadRequest(
						request.providerAssetId(),
						request.providerAssetUrl(),
						request.checksumSha256()));
		return PropertyMediaResponse.from(media);
	}

	@GetMapping
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORGANIZATION_ADMIN)")
	@Operation(summary = "List property images")
	public List<PropertyMediaResponse> listMedia(
			@PathVariable Long organizationId,
			@PathVariable Long propertyId) {
		return mediaService.listReadyMedia(organizationId, propertyId).stream()
				.map(PropertyMediaResponse::from)
				.toList();
	}

	@PostMapping("/{mediaId}/primary")
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORGANIZATION_ADMIN)")
	@Operation(summary = "Select the primary property image")
	public PropertyMediaResponse selectPrimary(
			@PathVariable Long organizationId,
			@PathVariable Long propertyId,
			@PathVariable Long mediaId) {
		return PropertyMediaResponse.from(mediaService.selectPrimary(organizationId, propertyId, mediaId));
	}

	@PutMapping("/order")
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORGANIZATION_ADMIN)")
	@Operation(summary = "Reorder property images")
	public List<PropertyMediaResponse> reorder(
			@PathVariable Long organizationId,
			@PathVariable Long propertyId,
			@Valid @RequestBody ReorderMediaRequest request) {
		return mediaService.reorder(organizationId, propertyId, request.mediaIds()).stream()
				.map(PropertyMediaResponse::from)
				.toList();
	}

	@DeleteMapping("/{mediaId}")
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORGANIZATION_ADMIN)")
	@Operation(summary = "Delete a property image")
	public ResponseEntity<Void> delete(
			@PathVariable Long organizationId,
			@PathVariable Long propertyId,
			@PathVariable Long mediaId) {
		mediaService.delete(organizationId, propertyId, mediaId);
		return ResponseEntity.noContent().build();
	}
}
