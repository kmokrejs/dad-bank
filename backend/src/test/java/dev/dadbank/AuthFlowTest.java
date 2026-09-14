package dev.dadbank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Test
    void registerLoginAndMe() throws Exception {
        String body = """
            {"email":"kid@example.com","username":"kiddo","password":"password123"}
            """;

        String registerJson = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.user.username").value("kiddo"))
            .andExpect(jsonPath("$.user.role").value("USER"))
            .andExpect(jsonPath("$.user.account.accountNumber", matchesPattern("DB-\\d{4}-\\d{4}-\\d{4}")))
            .andExpect(jsonPath("$.user.account.balanceCents").value(0))
            .andReturn().getResponse().getContentAsString();

        // duplicate email -> 409
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict());

        // login by email
        String loginJson = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"KID@example.com\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String token = om.readTree(loginJson).get("token").asText();

        // wrong password -> 401
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"kiddo\",\"password\":\"nope\"}"))
            .andExpect(status().isUnauthorized());

        // /me with token
        JsonNode reg = om.readTree(registerJson);
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.account.accountNumber").value(reg.at("/user/account/accountNumber").asText()));

        // /me without token -> 401, admin endpoint as kid -> 403
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/accounts").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void seededAdminCanListAllAccounts() throws Exception {
        String loginJson = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"testadmin\",\"password\":\"testadmin123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.role").value("ADMIN"))
            .andReturn().getResponse().getContentAsString();
        String token = om.readTree(loginJson).get("token").asText();

        mvc.perform(get("/api/admin/accounts").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].accountNumber").isString());
    }

    @Test
    void validationErrors() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\",\"username\":\"a b\",\"password\":\"short\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.email").exists())
            .andExpect(jsonPath("$.errors.username").exists())
            .andExpect(jsonPath("$.errors.password").exists());
    }
}
