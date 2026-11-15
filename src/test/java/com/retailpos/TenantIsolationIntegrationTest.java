package com.retailpos;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TenantIsolationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowUserToAccessTheirOwnShop() throws Exception {
        RegisteredShop shop = registerShopOwner();

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + shop.accessToken())
                        .header("X-Shop-Id", shop.shopId()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectAccessToAnotherUsersShop() throws Exception {
        RegisteredShop firstShop = registerShopOwner();
        RegisteredShop secondShop = registerShopOwner();

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + firstShop.accessToken())
                        .header("X-Shop-Id", secondShop.shopId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectRequestWithoutShopId() throws Exception {
        RegisteredShop shop = registerShopOwner();

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + shop.accessToken()))
                .andExpect(status().isBadRequest());
    }

    private RegisteredShop registerShopOwner() throws Exception {
        String email = "tenant-" + UUID.randomUUID() + "@example.com";

        String requestBody = """
                {
                    "email": "%s",
                    "password": "StrongPassword123!",
                    "shopName": "Tenant Test Shop"
                }
                """.formatted(email);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        String accessToken = JsonPath.read(response, "$.accessToken");
        Number shopId = JsonPath.read(response, "$.memberships[0].shopId");

        return new RegisteredShop(accessToken, shopId.longValue());
    }

    private record RegisteredShop(String accessToken, long shopId) {
    }
}