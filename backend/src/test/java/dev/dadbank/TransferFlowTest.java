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
class TransferFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    record Session(String token, long accountId, String accountNumber) {}

    private Session register(String name) throws Exception {
        String json = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + name + "@example.com\",\"username\":\"" + name + "\",\"password\":\"password123\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        JsonNode n = om.readTree(json);
        return new Session(n.get("token").asText(), n.at("/user/account/id").asLong(), n.at("/user/account/accountNumber").asText());
    }

    private String adminToken() throws Exception {
        String json = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"testadmin\",\"password\":\"testadmin123\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return om.readTree(json).get("token").asText();
    }

    private long balanceOf(String token) throws Exception {
        String json = mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return om.readTree(json).at("/account/balanceCents").asLong();
    }

    @Test
    void adminDepositThenKidTransfers() throws Exception {
        Session alice = register("alice");
        Session bob = register("bob");
        String admin = adminToken();

        // kid cannot deposit
        mvc.perform(post("/api/admin/accounts/" + alice.accountId() + "/deposit")
                .header("Authorization", "Bearer " + alice.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"amountCents\":10000,\"note\":\"hack\"}"))
            .andExpect(status().isForbidden());

        // admin deposits 100.00 to alice
        mvc.perform(post("/api/admin/accounts/" + alice.accountId() + "/deposit")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amountCents\":10000,\"note\":\"pocket money\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.balanceCents").value(10000))
            .andExpect(jsonPath("$.transaction.type").value("DEPOSIT"))
            .andExpect(jsonPath("$.transaction.direction").value("IN"))
            .andExpect(jsonPath("$.transaction.counterparty").isEmpty());

        // deposit with zero amount -> 400 with field error
        mvc.perform(post("/api/admin/accounts/" + alice.accountId() + "/deposit")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amountCents\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.amountCents").exists());

        // unknown account -> 404
        mvc.perform(post("/api/admin/accounts/999999/deposit")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amountCents\":100}"))
            .andExpect(status().isNotFound());

        // alice sends 25.50 to bob (account number lower-cased with spaces to check normalisation)
        String toNumber = " " + bob.accountNumber().toLowerCase() + " ";
        mvc.perform(post("/api/transfers")
                .header("Authorization", "Bearer " + alice.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"toAccountNumber\":\"" + toNumber + "\",\"amountCents\":2550,\"note\":\"for the game\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.balanceCents").value(7450))
            .andExpect(jsonPath("$.transaction.type").value("TRANSFER"))
            .andExpect(jsonPath("$.transaction.direction").value("OUT"))
            .andExpect(jsonPath("$.transaction.counterparty.username").value("bob"))
            .andExpect(jsonPath("$.transaction.note").value("for the game"));

        org.junit.jupiter.api.Assertions.assertEquals(7450, balanceOf(alice.token()));
        org.junit.jupiter.api.Assertions.assertEquals(2550, balanceOf(bob.token()));

        // insufficient funds -> 422, balances unchanged
        mvc.perform(post("/api/transfers")
                .header("Authorization", "Bearer " + bob.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"toAccountNumber\":\"" + alice.accountNumber() + "\",\"amountCents\":999999}"))
            .andExpect(status().isUnprocessableEntity());
        org.junit.jupiter.api.Assertions.assertEquals(2550, balanceOf(bob.token()));

        // self transfer -> 400
        mvc.perform(post("/api/transfers")
                .header("Authorization", "Bearer " + bob.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"toAccountNumber\":\"" + bob.accountNumber() + "\",\"amountCents\":100}"))
            .andExpect(status().isBadRequest());

        // unknown recipient -> 404
        mvc.perform(post("/api/transfers")
                .header("Authorization", "Bearer " + bob.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"toAccountNumber\":\"DB-0000-0000-0000\",\"amountCents\":100}"))
            .andExpect(status().isNotFound());

        // admin withdraws 5.00 from bob
        mvc.perform(post("/api/admin/accounts/" + bob.accountId() + "/withdraw")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amountCents\":500,\"note\":\"ice cream\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.balanceCents").value(2050))
            .andExpect(jsonPath("$.transaction.type").value("WITHDRAWAL"))
            .andExpect(jsonPath("$.transaction.direction").value("OUT"));

        // admin list reflects balances and exposes accountId
        mvc.perform(get("/api/admin/accounts").header("Authorization", "Bearer " + admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.username=='alice')].balanceCents").value(7450))
            .andExpect(jsonPath("$[?(@.username=='bob')].accountId").value((int) bob.accountId()));
    }
}
