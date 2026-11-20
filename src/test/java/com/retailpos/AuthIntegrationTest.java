
package com.retailpos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRegisterWithEmailAndAllowEmailLogin() throws Exception {
        String email = "test-" + UUID.randomUUID() + "@example.com";
        String password = "StrongPassword123!";

        String registrationRequest = """
                {
                    "name": "John Doe",
                    "email": "%s",
                    "password": "%s",
                    "shopName": "Integration Test Shop"
                }
                """.formatted(email, password);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.phoneNumber").isEmpty())
                .andExpect(jsonPath("$.memberships.length()").value(1))
                .andExpect(jsonPath("$.memberships[0].role").value("OWNER"));

        String loginRequest = """
                {
                    "identifier": "%s",
                    "password": "%s"
                }
                """.formatted(email, password);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.memberships.length()").value(1));
    }

    @Test
    void shouldRegisterWithPhoneNumberAndAllowPhoneLogin() throws Exception {
        String phoneNumber = "+95" + String.format(
                "%09d",
                Math.floorMod(UUID.randomUUID().hashCode(), 1_000_000_000)
        );

        String password = "StrongPassword123!";

        String registrationRequest = """
                {
                    "name": "Jane Doe",
                    "phoneNumber": "%s",
                    "password": "%s",
                    "shopName": "Phone Registration Shop"
                }
                """.formatted(phoneNumber, password);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.email").isEmpty())
                .andExpect(jsonPath("$.phoneNumber").value(phoneNumber))
                .andExpect(jsonPath("$.memberships[0].role").value("OWNER"));

        String loginRequest = """
                {
                    "identifier": "%s",
                    "password": "%s"
                }
                """.formatted(phoneNumber, password);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value(phoneNumber));
    }

    @Test
    void shouldRejectRegistrationWhenBothIdentifiersAreMissing() throws Exception {
        String registrationRequest = """
                {
                    "name": "John Doe",
                    "password": "StrongPassword123!",
                    "shopName": "Invalid Registration Shop"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDuplicateEmailRegistration() throws Exception {
        String email = "duplicate-" + UUID.randomUUID() + "@example.com";

        String registrationRequest = """
                {
                    "name": "John Doe",
                    "email": "%s",
                    "password": "StrongPassword123!",
                    "shopName": "First Shop"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest))
                .andExpect(status().isCreated());

        String duplicateRequest = """
                {
                    "name": "Jane Doe",
                    "email": "%s",
                    "password": "StrongPassword123!",
                    "shopName": "Second Shop"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateRequest))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectDuplicatePhoneNumberRegistration() throws Exception {
        String phoneNumber = "+95" + String.format(
                "%09d",
                Math.floorMod(UUID.randomUUID().hashCode(), 1_000_000_000)
        );

        String registrationRequest = """
                {
                    "name": "John Doe",
                    "phoneNumber": "%s",
                    "password": "StrongPassword123!",
                    "shopName": "First Phone Shop"
                }
                """.formatted(phoneNumber);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest))
                .andExpect(status().isCreated());

        String duplicateRequest = """
                {
                    "name": "Jane Doe",
                    "phoneNumber": "%s",
                    "password": "StrongPassword123!",
                    "shopName": "Second Phone Shop"
                }
                """.formatted(phoneNumber);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateRequest))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectLoginWithIncorrectPassword() throws Exception {
        String loginRequest = """
                {
                    "identifier": "nonexistent@example.com",
                    "password": "WrongPassword123!"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isUnauthorized());
    }
}
