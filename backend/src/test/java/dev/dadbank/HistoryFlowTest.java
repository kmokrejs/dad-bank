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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HistoryFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    record Session(String token, long accountId, String accountNumber) {}

    private Session register(String name) throws Exception {
        String json = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + name + "@example.com\",\"username\":\"" + name + "\",\"password\":\"password123\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode n = om.readTree(json);
        return new Session(n.get("token").asText(), n.at("/user/account/id").asLong(), n.at("/user/account/accountNumber").asText());
    }

    private String adminToken() throws Exception {
        String json = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"testadmin\",\"password\":\"testadmin123\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return om.readTree(json).get("token").asText();
    }

    @Test
    void historyShowsBothSidesNewestFirstAndPaginates() throws Exception {
        Session carol = register("carol");
        Session dave = register("dave");
        String admin = adminToken();

        // empty history
        mvc.perform(get("/api/transactions").header("Authorization", "Bearer " + carol.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isEmpty())
            .andExpect(jsonPath("$.totalItems").value(0));

        // deposit 100 to carol, carol -> dave 30, admin withdraws 10 from carol
        mvc.perform(post("/api/admin/accounts/" + carol.accountId() + "/deposit").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amountCents\":10000,\"note\":\"allowance\"}"))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/transfers").header("Authorization", "Bearer " + carol.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"toAccountNumber\":\"" + dave.accountNumber() + "\",\"amountCents\":3000,\"note\":\"lunch\"}"))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/admin/accounts/" + carol.accountId() + "/withdraw").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amountCents\":1000,\"note\":\"candy\"}"))
            .andExpect(status().isCreated());

        // carol sees 3 rows, newest first, from her point of view
        mvc.perform(get("/api/transactions").header("Authorization", "Bearer " + carol.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].type").value("WITHDRAWAL"))
            .andExpect(jsonPath("$.items[0].direction").value("OUT"))
            .andExpect(jsonPath("$.items[0].amountCents").value(1000))
            .andExpect(jsonPath("$.items[0].counterparty").isEmpty())
            .andExpect(jsonPath("$.items[1].type").value("TRANSFER"))
            .andExpect(jsonPath("$.items[1].direction").value("OUT"))
            .andExpect(jsonPath("$.items[1].counterparty.username").value("dave"))
            .andExpect(jsonPath("$.items[1].counterparty.accountNumber").value(dave.accountNumber()))
            .andExpect(jsonPath("$.items[1].note").value("lunch"))
            .andExpect(jsonPath("$.items[2].type").value("DEPOSIT"))
            .andExpect(jsonPath("$.items[2].direction").value("IN"))
            .andExpect(jsonPath("$.items[2].note").value("allowance"))
            .andExpect(jsonPath("$.items[2].createdAt").isString());

        // dave sees the same transfer as IN with carol as counterparty
        mvc.perform(get("/api/transactions").header("Authorization", "Bearer " + dave.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].direction").value("IN"))
            .andExpect(jsonPath("$.items[0].counterparty.username").value("carol"));

        // pagination: size 2 -> 2 pages
        mvc.perform(get("/api/transactions?page=0&size=2").header("Authorization", "Bearer " + carol.token()))
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.page").value(0));
        mvc.perform(get("/api/transactions?page=1&size=2").header("Authorization", "Bearer " + carol.token()))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].type").value("DEPOSIT"));

        // admin can read carol's history; kid cannot read via admin route; unknown account 404
        mvc.perform(get("/api/admin/accounts/" + carol.accountId() + "/transactions").header("Authorization", "Bearer " + admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3));
        mvc.perform(get("/api/admin/accounts/" + carol.accountId() + "/transactions").header("Authorization", "Bearer " + dave.token()))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/accounts/999999/transactions").header("Authorization", "Bearer " + admin))
            .andExpect(status().isNotFound());
    }
}
