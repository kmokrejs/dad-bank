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
class SavingsFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private JsonNode register(String name) throws Exception {
        String json = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + name + "@example.com\",\"username\":\"" + name + "\",\"password\":\"password123\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return om.readTree(json);
    }

    private String adminToken() throws Exception {
        String json = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"testadmin\",\"password\":\"testadmin123\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return om.readTree(json).get("token").asText();
    }

    @Test
    void openSavingsMoveMoneyAndSeeItInHistory() throws Exception {
        JsonNode reg = register("fay");
        String fay = reg.get("token").asText();
        long checkingId = reg.at("/user/account/id").asLong();
        String admin = adminToken();

        // before opening: /me has no savings, savings deposit -> 404
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + fay))
            .andExpect(jsonPath("$.account.type").value("CHECKING"))
            .andExpect(jsonPath("$.savings").isEmpty());
        mvc.perform(post("/api/savings/deposit").header("Authorization", "Bearer " + fay)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amountCents\":100}"))
            .andExpect(status().isNotFound());

        // open, then open again -> 409
        mvc.perform(post("/api/savings").header("Authorization", "Bearer " + fay))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("SAVINGS"))
            .andExpect(jsonPath("$.accountNumber", matchesPattern("DB-\\d{4}-\\d{4}-\\d{4}")))
            .andExpect(jsonPath("$.balanceCents").value(0));
        mvc.perform(post("/api/savings").header("Authorization", "Bearer " + fay))
            .andExpect(status().isConflict());

        // fund the main account, then move 60 of 100 to savings
        mvc.perform(post("/api/admin/accounts/" + checkingId + "/deposit").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amountCents\":10000}"))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/savings/deposit").header("Authorization", "Bearer " + fay)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amountCents\":6000,\"note\":\"for a bike\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.checkingBalanceCents").value(4000))
            .andExpect(jsonPath("$.savingsBalanceCents").value(6000))
            .andExpect(jsonPath("$.transaction.direction").value("IN"))
            .andExpect(jsonPath("$.transaction.counterparty.accountType").value("CHECKING"));

        // more than the savings balance back -> 422; 25 back -> ok
        mvc.perform(post("/api/savings/withdraw").header("Authorization", "Bearer " + fay)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amountCents\":999999}"))
            .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/savings/withdraw").header("Authorization", "Bearer " + fay)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amountCents\":2500}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.checkingBalanceCents").value(6500))
            .andExpect(jsonPath("$.savingsBalanceCents").value(3500));

        // /me reflects both; histories are per account
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + fay))
            .andExpect(jsonPath("$.account.balanceCents").value(6500))
            .andExpect(jsonPath("$.savings.type").value("SAVINGS"))
            .andExpect(jsonPath("$.savings.balanceCents").value(3500));
        mvc.perform(get("/api/transactions?account=SAVINGS").header("Authorization", "Bearer " + fay))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].direction").value("OUT"))
            .andExpect(jsonPath("$.items[0].counterparty.accountType").value("CHECKING"))
            .andExpect(jsonPath("$.items[1].note").value("for a bike"));
        mvc.perform(get("/api/transactions").header("Authorization", "Bearer " + fay))
            .andExpect(jsonPath("$.totalItems").value(3)); // deposit + 2 moves

        // admin list shows both accounts of fay with their type
        mvc.perform(get("/api/admin/accounts").header("Authorization", "Bearer " + admin))
            .andExpect(jsonPath("$[?(@.username=='fay' && @.accountType=='SAVINGS')].balanceCents").value(3500))
            .andExpect(jsonPath("$[?(@.username=='fay' && @.accountType=='CHECKING')].balanceCents").value(6500));
    }

    @Test
    void adminSetsInterestRateAndHistoryIsKept() throws Exception {
        String kid = register("gus").get("token").asText();
        String admin = adminToken();

        // default 0 until set; kid can read but not set
        mvc.perform(get("/api/interest").header("Authorization", "Bearer " + kid))
            .andExpect(status().isOk()).andExpect(jsonPath("$.rateBps").value(0));
        mvc.perform(post("/api/admin/interest").header("Authorization", "Bearer " + kid)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rateBps\":250}"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/interest").header("Authorization", "Bearer " + kid))
            .andExpect(status().isForbidden());

        // validation: negative / over 100 %
        mvc.perform(post("/api/admin/interest").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rateBps\":-1}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.rateBps").exists());
        mvc.perform(post("/api/admin/interest").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rateBps\":10001}"))
            .andExpect(status().isBadRequest());

        // two changes -> current is the latest, history newest first with author
        mvc.perform(post("/api/admin/interest").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rateBps\":250,\"note\":\"start\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.rateBps").value(250)).andExpect(jsonPath("$.setBy").value("testadmin"));
        mvc.perform(post("/api/admin/interest").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rateBps\":300}"))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/interest").header("Authorization", "Bearer " + kid))
            .andExpect(jsonPath("$.rateBps").value(300));
        mvc.perform(get("/api/admin/interest").header("Authorization", "Bearer " + admin))
            .andExpect(jsonPath("$[0].rateBps").value(300))
            .andExpect(jsonPath("$[1].rateBps").value(250))
            .andExpect(jsonPath("$[1].note").value("start"))
            .andExpect(jsonPath("$[0].createdAt").isString());
    }
}
