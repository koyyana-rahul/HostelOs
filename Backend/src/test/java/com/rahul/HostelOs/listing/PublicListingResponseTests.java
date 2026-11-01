package com.rahul.HostelOs.listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rahul.HostelOs.inventory.Property;
import com.rahul.HostelOs.organization.Organization;
import com.rahul.HostelOs.propertymedia.PropertyMedia;
import com.rahul.HostelOs.propertymedia.PropertyMediaStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublicListingResponseTests {

	@Test
	void exposesOnlyReadyMediaFromTheListingPropertyAndOrganization() {
		Organization organization = mock(Organization.class);
		Property property = mock(Property.class);
		Property otherProperty = mock(Property.class);
		when(organization.getId()).thenReturn(10L);
		when(property.getId()).thenReturn(20L);
		when(otherProperty.getId()).thenReturn(21L);
		Listing listing = mock(Listing.class);
		when(listing.getOrganization()).thenReturn(organization);
		when(listing.getProperty()).thenReturn(property);
		when(listing.getTitle()).thenReturn("Sunny Hostel");

		PropertyMedia ready = media(organization, property, PropertyMediaStatus.READY, true, 0, "https://cdn/one");
		PropertyMedia pending = media(organization, property, PropertyMediaStatus.PENDING, false, 1, "https://cdn/two");
		PropertyMedia other = media(organization, otherProperty, PropertyMediaStatus.READY, false, 2, "https://cdn/three");

		var response = com.rahul.HostelOs.listing.dto.PublicListingResponse.from(
				listing, List.of(ready, pending, other));

		assertThat(response.imageCount()).isEqualTo(1);
		assertThat(response.images()).extracting("url").containsExactly("https://cdn/one");
		assertThat(response.primaryImage().url()).isEqualTo("https://cdn/one");
	}

	@Test
	void listingWithoutMediaReturnsEmptyGallery() {
		Listing listing = mock(Listing.class);
		when(listing.getOrganization()).thenReturn(mock(Organization.class));

		var response = com.rahul.HostelOs.listing.dto.PublicListingResponse.from(listing, List.of());

		assertThat(response.primaryImage()).isNull();
		assertThat(response.images()).isEmpty();
		assertThat(response.imageCount()).isZero();
	}

	private static PropertyMedia media(
			Organization organization,
			Property property,
			PropertyMediaStatus status,
			boolean primary,
			int position,
			String url) {
		PropertyMedia media = mock(PropertyMedia.class);
		when(media.getOrganization()).thenReturn(organization);
		when(media.getProperty()).thenReturn(property);
		when(media.getStatus()).thenReturn(status);
		when(media.isPrimaryImage()).thenReturn(primary);
		when(media.getPosition()).thenReturn(position);
		when(media.getSecureUrl()).thenReturn(url);
		return media;
	}
}
