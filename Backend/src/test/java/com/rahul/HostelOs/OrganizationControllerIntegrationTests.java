package com.rahul.HostelOs;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class OrganizationControllerIntegrationTests extends AuthIntegrationTests {

	private String registerPayload(String loginIdentifier, String displayName, String password) {
		return "{\"loginIdentifier\":\"" + escapeJson(loginIdentifier) + "\",\"displayName\":\"" + escapeJson(displayName) + "\",\"password\":\"" + escapeJson(password) + "\"}";
	}

	private String organizationPayload(String name, String slug) {
		return "{\"name\":\"" + escapeJson(name) + "\",\"slug\":\"" + escapeJson(slug) + "\"}";
	}

	private String addMemberPayload(String login) {
		return "{\"userLoginIdentifier\":\"" + escapeJson(login) + "\",\"orgRole\":\"ORG_STAFF\"}";
	}

	private String addMemberPayload(String login, String role) {
		return "{\"userLoginIdentifier\":\"" + escapeJson(login) + "\",\"orgRole\":\"" + escapeJson(role) + "\"}";
	}

	private String changeRolePayload(String role) {
		return "{\"newRole\":\"" + escapeJson(role) + "\"}";
	}

	private String reactivatePayload(String role) {
		return "{\"orgRole\":\"" + escapeJson(role) + "\"}";
	}

	private String orgIdFromBody(String body) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(body);
		if (m.find()) {
			return m.group(1);
		}
		throw new AssertionError("Could not extract org id from: " + body);
	}

	private String membershipIdFromBody(String body) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(body);
		if (m.find()) {
			return m.group(1);
		}
		throw new AssertionError("Could not extract membership id from: " + body);
	}

	@Test
	void unauthenticatedCreateOrganizationRejected() throws Exception {
		Browser browser = new Browser();
		browser.bootstrapCsrf();
		HttpResponse<String> response = browser.postJson("/api/v1/organizations", organizationPayload("Test Org", "test-org-" + System.nanoTime()), browser.getCsrfToken());
		assertThat(response.statusCode()).isIn(401, 403);
	}

	@Test
	void createOrganizationWithoutCsrfRejected() throws Exception {
		Browser browser = new Browser();
		String login = "nocrsf-" + System.nanoTime() + "@example.com";
		browser.bootstrapCsrf();
		browser.postJson("/api/v1/auth/register", registerPayload(login, "NoCsrf", PASSWORD), browser.getCsrfToken());
		Browser authed = new Browser();
		authed.bootstrapCsrf();
		authed.login(login, PASSWORD);
		// Try without CSRF token
		HttpResponse<String> create = authed.postJson("/api/v1/organizations", organizationPayload("NoCsrf Org", "nocrsf-" + System.nanoTime()), null);
		assertThat(create.statusCode()).isIn(403, 401);
	}

	@Test
	void authenticatedOrganizationCreation() throws Exception {
		Browser browser = new Browser();
		browser.bootstrapCsrf();
		String login = "orgcreator-" + System.nanoTime() + "@example.com";
		HttpResponse<String> register = browser.postJson("/api/v1/auth/register", registerPayload(login, "Org Creator", PASSWORD), browser.getCsrfToken());
		assertThat(register.statusCode()).isEqualTo(201);
		Browser authed = new Browser();
		authed.bootstrapCsrf();
		assertThat(authed.login(login, PASSWORD).statusCode()).isEqualTo(200);
		String slug = "org-" + System.nanoTime();
		HttpResponse<String> create = authed.postJson("/api/v1/organizations", organizationPayload("Test Organization", slug), authed.csrfToken);
		assertThat(create.statusCode()).isEqualTo(201);
		assertThat(create.body()).contains("\"name\":\"Test Organization\"");
		assertThat(create.body()).contains("\"slug\":\"" + slug + "\"");
		assertThat(create.body()).contains("\"status\":\"ACTIVE\"");
	}

	@Test
	void listOrganizationsOnlyAccessibleToUser() throws Exception {
		String slug = "listorg-" + System.nanoTime();
		Browser browser = new Browser();
		browser.bootstrapCsrf();
		String login = "listuser-" + System.nanoTime() + "@example.com";
		browser.postJson("/api/v1/auth/register", registerPayload(login, "List User", PASSWORD), browser.csrfToken);
		Browser authed = new Browser();
		authed.bootstrapCsrf();
		authed.login(login, PASSWORD);
		HttpResponse<String> create = authed.postJson("/api/v1/organizations", organizationPayload("List Org", slug), authed.csrfToken);
		assertThat(create.statusCode()).isEqualTo(201);

		HttpResponse<String> list = authed.get("/api/v1/organizations");
		assertThat(list.statusCode()).isEqualTo(200);
		assertThat(list.body()).contains("\"slug\":\"" + slug + "\"");
	}

	@Test
	void organizationDetailRequiresActiveMembership() throws Exception {
		Browser owner = new Browser();
		owner.bootstrapCsrf();
		String ownerLogin = "owner-" + System.nanoTime() + "@example.com";
		owner.postJson("/api/v1/auth/register", registerPayload(ownerLogin, "Owner", PASSWORD), owner.csrfToken);
		Browser ownerAuthed = new Browser();
		ownerAuthed.bootstrapCsrf();
		ownerAuthed.login(ownerLogin, PASSWORD);
		String slug = "detail-" + System.nanoTime();
		HttpResponse<String> create = ownerAuthed.postJson("/api/v1/organizations", organizationPayload("Detail Org", slug), ownerAuthed.csrfToken);
		assertThat(create.statusCode()).isEqualTo(201);
		String orgId = orgIdFromBody(create.body());

		HttpResponse<String> detail = ownerAuthed.get("/api/v1/organizations/" + orgId);
		assertThat(detail.statusCode()).isEqualTo(200);
		assertThat(detail.body()).contains("\"slug\":\"" + slug + "\"");
	}

	@Test
	void crossOrganizationAccessDenied() throws Exception {
		Browser owner = new Browser();
		owner.bootstrapCsrf();
		String ownerLogin = "ownerx-" + System.nanoTime() + "@example.com";
		owner.postJson("/api/v1/auth/register", registerPayload(ownerLogin, "OwnerX", PASSWORD), owner.csrfToken);
		Browser ownerAuthed = new Browser();
		ownerAuthed.bootstrapCsrf();
		ownerAuthed.login(ownerLogin, PASSWORD);
		String slug = "cross-" + System.nanoTime();
		HttpResponse<String> create = ownerAuthed.postJson("/api/v1/organizations", organizationPayload("Cross Org", slug), ownerAuthed.csrfToken);
		String orgId = orgIdFromBody(create.body());

		Browser other = new Browser();
		other.bootstrapCsrf();
		String otherLogin = "other-" + System.nanoTime() + "@example.com";
		other.postJson("/api/v1/auth/register", registerPayload(otherLogin, "Other", PASSWORD), other.csrfToken);
		Browser otherAuthed = new Browser();
		otherAuthed.bootstrapCsrf();
		otherAuthed.login(otherLogin, PASSWORD);
		HttpResponse<String> getOrg = otherAuthed.get("/api/v1/organizations/" + orgId);
		assertThat(getOrg.statusCode()).isIn(403, 404);
	}

	@Test
	void listMembershipsAsAdminOrStaff() throws Exception {
		Browser owner = new Browser();
		owner.bootstrapCsrf();
		String ownerLogin = "owner2-" + System.nanoTime() + "@example.com";
		owner.postJson("/api/v1/auth/register", registerPayload(ownerLogin, "Owner2", PASSWORD), owner.csrfToken);
		Browser ownerAuthed = new Browser();
		ownerAuthed.bootstrapCsrf();
		ownerAuthed.login(ownerLogin, PASSWORD);
		String slug = "mem-" + System.nanoTime();
		HttpResponse<String> create = ownerAuthed.postJson("/api/v1/organizations", organizationPayload("Mem Org", slug), ownerAuthed.csrfToken);
		String orgId = orgIdFromBody(create.body());
		HttpResponse<String> members = ownerAuthed.get("/api/v1/organizations/" + orgId + "/memberships");
		assertThat(members.statusCode()).isEqualTo(200);
	}

	@Test
	void addMemberAsOrgAdmin() throws Exception {
		Browser owner = new Browser();
		owner.bootstrapCsrf();
		String ownerLogin = "owner3-" + System.nanoTime() + "@example.com";
		owner.postJson("/api/v1/auth/register", registerPayload(ownerLogin, "Owner3", PASSWORD), owner.csrfToken);
		Browser ownerAuthed = new Browser();
		ownerAuthed.bootstrapCsrf();
		ownerAuthed.login(ownerLogin, PASSWORD);
		String slug = "add-" + System.nanoTime();
		HttpResponse<String> create = ownerAuthed.postJson("/api/v1/organizations", organizationPayload("Add Org", slug), ownerAuthed.csrfToken);
		String orgId = orgIdFromBody(create.body());

		String newUser = "member-" + System.nanoTime() + "@example.com";
		Browser tmp = new Browser();
		tmp.bootstrapCsrf();
		tmp.postJson("/api/v1/auth/register", registerPayload(newUser, "Member", PASSWORD), tmp.csrfToken);

		HttpResponse<String> add = ownerAuthed.postJson("/api/v1/organizations/" + orgId + "/members", addMemberPayload(newUser, "ORG_STAFF"), ownerAuthed.csrfToken);
		assertThat(add.statusCode()).isEqualTo(201);
		assertThat(add.body()).contains(newUser);
	}

	@Test
	void duplicateMembershipRejected() throws Exception {
		Browser owner = new Browser();
		owner.bootstrapCsrf();
		String ownerLogin = "owner4-" + System.nanoTime() + "@example.com";
		owner.postJson("/api/v1/auth/register", registerPayload(ownerLogin, "Owner4", PASSWORD), owner.csrfToken);
		Browser ownerAuthed = new Browser();
		ownerAuthed.bootstrapCsrf();
		ownerAuthed.login(ownerLogin, PASSWORD);
		String slug = "dup-" + System.nanoTime();
		HttpResponse<String> create = ownerAuthed.postJson("/api/v1/organizations", organizationPayload("Dup Org", slug), ownerAuthed.csrfToken);
		String orgId = orgIdFromBody(create.body());

		String newUser = "dup-" + System.nanoTime() + "@example.com";
		Browser tmp = new Browser();
		tmp.bootstrapCsrf();
		tmp.postJson("/api/v1/auth/register", registerPayload(newUser, "Dup", PASSWORD), tmp.csrfToken);

		HttpResponse<String> add1 = ownerAuthed.postJson("/api/v1/organizations/" + orgId + "/members", addMemberPayload(newUser, "ORG_STAFF"), ownerAuthed.csrfToken);
		assertThat(add1.statusCode()).isEqualTo(201);
		HttpResponse<String> add2 = ownerAuthed.postJson("/api/v1/organizations/" + orgId + "/members", addMemberPayload(newUser, "ORG_STAFF"), ownerAuthed.csrfToken);
		assertThat(add2.statusCode()).isIn(409, 400);
	}

	@Test
	void changeRoleByOrgAdmin() throws Exception {
		Browser owner = new Browser();
		owner.bootstrapCsrf();
		String ownerLogin = "owner5-" + System.nanoTime() + "@example.com";
		owner.postJson("/api/v1/auth/register", registerPayload(ownerLogin, "Owner5", PASSWORD), owner.csrfToken);
		Browser ownerAuthed = new Browser();
		ownerAuthed.bootstrapCsrf();
		ownerAuthed.login(ownerLogin, PASSWORD);
		String slug = "role-" + System.nanoTime();
		HttpResponse<String> create = ownerAuthed.postJson("/api/v1/organizations", organizationPayload("Role Org", slug), ownerAuthed.csrfToken);
		String orgId = orgIdFromBody(create.body());

		String newUser = "roleuser-" + System.nanoTime() + "@example.com";
		Browser tmp = new Browser();
		tmp.bootstrapCsrf();
		tmp.postJson("/api/v1/auth/register", registerPayload(newUser, "RoleUser", PASSWORD), tmp.csrfToken);
		HttpResponse<String> add = ownerAuthed.postJson("/api/v1/organizations/" + orgId + "/members", addMemberPayload(newUser, "ORG_STAFF"), ownerAuthed.csrfToken);
		String mid = membershipIdFromBody(add.body());

		HttpResponse<String> change = ownerAuthed.putJson("/api/v1/organizations/" + orgId + "/members/" + mid + "/role", changeRolePayload("ORG_ADMIN"), ownerAuthed.csrfToken);
		assertThat(change.statusCode()).isEqualTo(200);
		assertThat(change.body()).contains("\"orgRole\":\"ORG_ADMIN\"");
	}

	@Test
	void suspendAndReactivateMembership() throws Exception {
		Browser owner = new Browser();
		owner.bootstrapCsrf();
		String ownerLogin = "owner6-" + System.nanoTime() + "@example.com";
		owner.postJson("/api/v1/auth/register", registerPayload(ownerLogin, "Owner6", PASSWORD), owner.csrfToken);
		Browser ownerAuthed = new Browser();
		ownerAuthed.bootstrapCsrf();
		ownerAuthed.login(ownerLogin, PASSWORD);
		String slug = "susp-" + System.nanoTime();
		HttpResponse<String> create = ownerAuthed.postJson("/api/v1/organizations", organizationPayload("Susp Org", slug), ownerAuthed.csrfToken);
		String orgId = orgIdFromBody(create.body());

		String newUser = "suspuser-" + System.nanoTime() + "@example.com";
		Browser tmp = new Browser();
		tmp.bootstrapCsrf();
		tmp.postJson("/api/v1/auth/register", registerPayload(newUser, "SuspUser", PASSWORD), tmp.csrfToken);
		HttpResponse<String> add = ownerAuthed.postJson("/api/v1/organizations/" + orgId + "/members", addMemberPayload(newUser, "ORG_STAFF"), ownerAuthed.csrfToken);
		String mid = membershipIdFromBody(add.body());

		HttpResponse<String> susp = ownerAuthed.postJson("/api/v1/organizations/" + orgId + "/members/" + mid + "/suspend", "{}", ownerAuthed.csrfToken);
		assertThat(susp.statusCode()).isEqualTo(200);
		assertThat(susp.body()).contains("\"status\":\"SUSPENDED\"");

		HttpResponse<String> react = ownerAuthed.postJson("/api/v1/organizations/" + orgId + "/members/" + mid + "/reactivate", reactivatePayload("ORG_STAFF"), ownerAuthed.csrfToken);
		assertThat(react.statusCode()).isEqualTo(200);
		assertThat(react.body()).contains("\"status\":\"ACTIVE\"");
	}

	@Test
	void revokeMembership() throws Exception {
		Browser owner = new Browser();
		owner.bootstrapCsrf();
		String ownerLogin = "owner7-" + System.nanoTime() + "@example.com";
		owner.postJson("/api/v1/auth/register", registerPayload(ownerLogin, "Owner7", PASSWORD), owner.csrfToken);
		Browser ownerAuthed = new Browser();
		ownerAuthed.bootstrapCsrf();
		ownerAuthed.login(ownerLogin, PASSWORD);
		String slug = "revoke-" + System.nanoTime();
		HttpResponse<String> create = ownerAuthed.postJson("/api/v1/organizations", organizationPayload("Revoke Org", slug), ownerAuthed.csrfToken);
		String orgId = orgIdFromBody(create.body());

		String newUser = "revuser-" + System.nanoTime() + "@example.com";
		Browser tmp = new Browser();
		tmp.bootstrapCsrf();
		tmp.postJson("/api/v1/auth/register", registerPayload(newUser, "RevUser", PASSWORD), tmp.csrfToken);
		HttpResponse<String> add = ownerAuthed.postJson("/api/v1/organizations/" + orgId + "/members", addMemberPayload(newUser, "ORG_STAFF"), ownerAuthed.csrfToken);
		String mid = membershipIdFromBody(add.body());

		HttpResponse<String> revoke = ownerAuthed.delete("/api/v1/organizations/" + orgId + "/members/" + mid, ownerAuthed.csrfToken);
		assertThat(revoke.statusCode()).isEqualTo(200);
		assertThat(revoke.body()).contains("\"status\":\"REVOKED\"");
	}

	@Test
	void customerCannotGainOrgAdminPrivileges() throws Exception {
		Browser owner = new Browser();
		owner.bootstrapCsrf();
		String ownerLogin = "owner8-" + System.nanoTime() + "@example.com";
		owner.postJson("/api/v1/auth/register", registerPayload(ownerLogin, "Owner8", PASSWORD), owner.csrfToken);
		Browser ownerAuthed = new Browser();
		ownerAuthed.bootstrapCsrf();
		ownerAuthed.login(ownerLogin, PASSWORD);
		String slug = "cust-" + System.nanoTime();
		HttpResponse<String> create = ownerAuthed.postJson("/api/v1/organizations", organizationPayload("Cust Org", slug), ownerAuthed.csrfToken);
		String orgId = orgIdFromBody(create.body());

		String cust = "custuser-" + System.nanoTime() + "@example.com";
		Browser tmp = new Browser();
		tmp.bootstrapCsrf();
		tmp.postJson("/api/v1/auth/register", registerPayload(cust, "Cust", PASSWORD), tmp.csrfToken);

		HttpResponse<String> add = ownerAuthed.postJson("/api/v1/organizations/" + orgId + "/members", addMemberPayload(cust, "ORG_STAFF"), ownerAuthed.csrfToken);
		assertThat(add.statusCode()).isEqualTo(201);
	}

	@Test
	void platformAdminWithoutMembershipCannotAccessOrg() throws Exception {
		Browser browser = new Browser();
		browser.bootstrapCsrf();
		HttpResponse<String> login = browser.login(PLATFORM_ADMIN_LOGIN, PASSWORD);
		assertThat(login.statusCode()).isEqualTo(200);
		HttpResponse<String> getOrg = browser.get("/api/v1/organizations/99999");
		assertThat(getOrg.statusCode()).isIn(403, 404);
	}

	@Test
	void nonAdminCannotManageMembers() throws Exception {
		Browser owner = new Browser();
		owner.bootstrapCsrf();
		String ownerLogin = "owner9-" + System.nanoTime() + "@example.com";
		owner.postJson("/api/v1/auth/register", registerPayload(ownerLogin, "Owner9", PASSWORD), owner.csrfToken);
		Browser ownerAuthed = new Browser();
		ownerAuthed.bootstrapCsrf();
		ownerAuthed.login(ownerLogin, PASSWORD);
		String slug = "staffm-" + System.nanoTime();
		HttpResponse<String> create = ownerAuthed.postJson("/api/v1/organizations", organizationPayload("StaffM Org", slug), ownerAuthed.csrfToken);
		String orgId = orgIdFromBody(create.body());

		String staff = "staffm-" + System.nanoTime() + "@example.com";
		Browser tmp = new Browser();
		tmp.bootstrapCsrf();
		tmp.postJson("/api/v1/auth/register", registerPayload(staff, "StaffM", PASSWORD), tmp.csrfToken);
		HttpResponse<String> add = ownerAuthed.postJson("/api/v1/organizations/" + orgId + "/members", addMemberPayload(staff, "ORG_STAFF"), ownerAuthed.csrfToken);
		String mid = membershipIdFromBody(add.body());

		Browser staffAuthed = new Browser();
		staffAuthed.bootstrapCsrf();
		staffAuthed.login(staff, PASSWORD);
		HttpResponse<String> tryAdd = staffAuthed.postJson("/api/v1/organizations/" + orgId + "/members", addMemberPayload("x@y.com", "ORG_STAFF"), staffAuthed.csrfToken);
		assertThat(tryAdd.statusCode()).isIn(403, 401);
	}

	@Test
	void lastAdminProtection() throws Exception {
		Browser owner = new Browser();
		owner.bootstrapCsrf();
		String ownerLogin = "owner10-" + System.nanoTime() + "@example.com";
		owner.postJson("/api/v1/auth/register", registerPayload(ownerLogin, "Owner10", PASSWORD), owner.csrfToken);
		Browser ownerAuthed = new Browser();
		ownerAuthed.bootstrapCsrf();
		ownerAuthed.login(ownerLogin, PASSWORD);
		String slug = "lastadmin-" + System.nanoTime();
		HttpResponse<String> create = ownerAuthed.postJson("/api/v1/organizations", organizationPayload("LastAdmin Org", slug), ownerAuthed.csrfToken);
		String orgId = orgIdFromBody(create.body());

		HttpResponse<String> members = ownerAuthed.get("/api/v1/organizations/" + orgId + "/memberships");
		String mid = membershipIdFromBody(members.body());
		HttpResponse<String> revoke = ownerAuthed.delete("/api/v1/organizations/" + orgId + "/members/" + mid, ownerAuthed.csrfToken);
		assertThat(revoke.statusCode()).isIn(409, 403);
	}
}
