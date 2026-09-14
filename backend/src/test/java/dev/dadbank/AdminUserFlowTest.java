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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private String login(String login, String password, int expectedStatus) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().is(expectedStatus))
            .andReturn().getResponse().getContentAsString();
    }

    @Test
    void adminResetsKidPassword() throws Exception {
        String reg = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"erin@example.com\",\"username\":\"erin\",\"password\":\"oldpassword1\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode erin = om.readTree(reg);
        long erinId = erin.at("/user/id").asLong();
        String erinToken = erin.get("token").asText();
        String admin = om.readTree(login("testadmin", "testadmin123", 200)).get("token").asText();

        // kid cannot reset (even their own) via admin route
        mvc.perform(post("/api/admin/users/" + erinId + "/password").header("Authorization", "Bearer " + erinToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"newPassword\":\"hackhackhack\"}"))
            .andExpect(status().isForbidden());

        // too short -> 400 with field error
        mvc.perform(post("/api/admin/users/" + erinId + "/password").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"newPassword\":\"short\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.newPassword").exists());

        // unknown user -> 404
        mvc.perform(post("/api/admin/users/999999/password").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"newPassword\":\"newpassword1\"}"))
            .andExpect(status().isNotFound());

        // reset works: old password rejected, new one accepted
        mvc.perform(post("/api/admin/users/" + erinId + "/password").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"newPassword\":\"newpassword1\"}"))
            .andExpect(status().isNoContent());
        login("erin", "oldpassword1", 401);
        login("erin", "newpassword1", 200);
    }
}
