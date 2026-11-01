package com.rahul.HostelOs.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.rahul.HostelOs.enquiry.Enquiry;
import com.rahul.HostelOs.enquiry.EnquiryRepository;
import com.rahul.HostelOs.enquiry.EnquiryService;
import com.rahul.HostelOs.enquiry.EnquiryStatus;
import com.rahul.HostelOs.enquiry.dto.CustomerEnquiryResponse;
import com.rahul.HostelOs.enquiry.exception.EnquiryNotFoundException;
import com.rahul.HostelOs.identity.HostelOsPrincipal;
import com.rahul.HostelOs.listing.Listing;
import com.rahul.HostelOs.listing.ListingRepository;
import com.rahul.HostelOs.organization.OrganizationAccessPolicy;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class CustomerSecurityServiceTests {

	@Mock
	private EnquiryRepository enquiries;

	@Mock
	private ListingRepository listings;

	@Mock
	private OrganizationAccessPolicy accessPolicy;

	@Mock
	private HostelOsPrincipal principal;

	@Mock
	private Enquiry enquiry;

	@Mock
	private Listing listing;

	@Mock
	private com.rahul.HostelOs.organization.Organization organization;

	private EnquiryService service;

	@BeforeEach
	void setUp() {
		service = new EnquiryService(enquiries, listings, accessPolicy);
		lenient().when(accessPolicy.currentPrincipal()).thenReturn(principal);
		lenient().when(principal.getUserId()).thenReturn(10L);
		lenient().when(enquiry.getId()).thenReturn(100L);
		lenient().when(enquiry.getListing()).thenReturn(listing);
		lenient().when(enquiry.getOrganization()).thenReturn(organization);
		lenient().when(listing.getId()).thenReturn(200L);
		lenient().when(listing.getTitle()).thenReturn("Safe listing");
		lenient().when(organization.getName()).thenReturn("Safe organization");
		lenient().when(enquiry.getMessage()).thenReturn("I am interested");
		lenient().when(enquiry.getStatus()).thenReturn(EnquiryStatus.NEW);
	}

	@Tes
	void customerCanRetrieveOnlyTheirOwnEnquiries() {
		when(enquiries.findByCustomerUserIdOrderByCreatedAtDesc(
				org.mockito.ArgumentMatchers.eq(10L),
				org.mockito.ArgumentMatchers.any()))
				.thenReturn(new PageImpl<>(List.of(enquiry)));

		var result = service.listCustomerEnquiries(null, PageRequest.of(0, 20));

		assertThat(result.getContent()).singleElement()
				.extracting(CustomerEnquiryResponse::listingTitle)
				.isEqualTo("Safe listing");
		verify(enquiries).findByCustomerUserIdOrderByCreatedAtDesc(
				org.mockito.ArgumentMatchers.eq(10L),
				org.mockito.ArgumentMatchers.any());
	}

	@Tes
	void customerCannotRetrieveAnotherCustomersEnquiry() {
		when(enquiries.findByCustomerUserIdAndId(10L, 100L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getCustomerEnquiry(100L))
				.isInstanceOf(EnquiryNotFoundException.class);
		verify(enquiries).findByCustomerUserIdAndId(10L, 100L);
	}

	@Tes
	void unauthenticatedCustomerAccessIsRejectedByTheSharedPolicy() {
		when(accessPolicy.currentPrincipal())
				.thenThrow(new com.rahul.HostelOs.organization.exception.OrganizationAccessDeniedException(
						"Authentication is required"));

		assertThatThrownBy(() -> service.listCustomerEnquiries(null, PageRequest.of(0, 20)))
				.isInstanceOf(com.rahul.HostelOs.organization.exception.OrganizationAccessDeniedException.class);
	}

	@Tes
	void organizationEnquiryAccessRemainsScopedToTheOrganization() {
		when(accessPolicy.currentUserId()).thenReturn(10L);
		when(enquiries.findByOrganizationIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(enquiry));

		service.listEnquiries(7L, null);

		verify(accessPolicy).requireMemberAccess(7L, 10L);
		verify(enquiries).findByOrganizationIdOrderByCreatedAtDesc(7L);
	}
}
