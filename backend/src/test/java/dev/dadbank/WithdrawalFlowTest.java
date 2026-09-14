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
class WithdrawalFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    record Session(String token, long accountId) {}

    private Session register(String name) throws Exception {
        String json = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + name + "@example.com\",\"username\":\"" + name + "\",\"password\":\"password123\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode n = om.readTree(json);
        return new Session(n.get("token").asText(), n.at("/user/account/id").asLong());
    }

    private String adminToken() throws Exception {
        String json = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"testadmin\",\"password\":\"testadmin123\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return om.readTree(json).get("token").asText();
    }

    private void fund(String admin, long accountId, long cents) throws Exception {
        mvc.perform(post("/api/admin/accounts/" + accountId + "/deposit").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amountCents\":" + cents + "}"))
            .andExpect(status().isCreated());
    }

    private long request(String token, String body) throws Exception {
        String json = mvc.perform(post("/api/withdrawals").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn().getResponse().getContentAsString();
        return om.readTree(json).get("id").asLong();
    }

    private long balanceOf(String token) throws Exception {
        String json = mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return om.readTree(json).at("/account/balanceCents").asLong();
    }

    @Test
    void requestRejectThenRequestApprove() throws Exception {
        Session hana = register("hana");
        String admin = adminToken();
        fund(admin, hana.accountId(), 10000);

        // nothing yet; more than the balance is refused up front
        mvc.perform(get("/api/withdrawals").header("Authorization", "Bearer " + hana.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(post("/api/withdrawals").header("Authorization", "Bearer " + hana.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"amountCents\":99999}"))
            .andExpect(status().isUnprocessableEntity());

        long first = request(hana.token(), "{\"amountCents\":2500,\"note\":\"school trip\"}");

        // kid sees it PENDING; admin sees it in the queue with the kid's name; balance untouched
        mvc.perform(get("/api/withdrawals").header("Authorization", "Bearer " + hana.token()))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].status").value("PENDING"))
            .andExpect(jsonPath("$.items[0].note").value("school trip"));
        mvc.perform(get("/api/admin/withdrawals/pending").header("Authorization", "Bearer " + admin))
            .andExpect(jsonPath("$.items[?(@.id==" + first + ")].username").value("hana"));
        org.junit.jupiter.api.Assertions.assertEquals(10000, balanceOf(hana.token()));

        // reject needs a reason
        mvc.perform(post("/api/admin/withdrawals/" + first + "/reject").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"  \"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/withdrawals/" + first + "/reject").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Too much for a trip\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.withdrawal.status").value("REJECTED"))
            .andExpect(jsonPath("$.withdrawal.rejectionReason").value("Too much for a trip"))
            .andExpect(jsonPath("$.withdrawal.decidedBy").value("testadmin"))
            .andExpect(jsonPath("$.balanceCents").value(10000));

        // deciding twice -> 409; gone from the queue; kid sees the reason
        mvc.perform(post("/api/admin/withdrawals/" + first + "/approve").header("Authorization", "Bearer " + admin))
            .andExpect(status().isConflict());
        mvc.perform(get("/api/admin/withdrawals/pending").header("Authorization", "Bearer " + admin))
            .andExpect(jsonPath("$.items[?(@.id==" + first + ")]").isEmpty());
        mvc.perform(get("/api/withdrawals").header("Authorization", "Bearer " + hana.token()))
            .andExpect(jsonPath("$.items[0].status").value("REJECTED"))
            .andExpect(jsonPath("$.items[0].rejectionReason").value("Too much for a trip"));

        // second request, approved: balance drops and a WITHDRAWAL row lands in the ledger
        long second = request(hana.token(), "{\"amountCents\":1500,\"note\":\"comic\"}");
        mvc.perform(post("/api/admin/withdrawals/" + second + "/approve").header("Authorization", "Bearer " + admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.withdrawal.status").value("APPROVED"))
            .andExpect(jsonPath("$.withdrawal.decidedAt").isString())
            .andExpect(jsonPath("$.balanceCents").value(8500));
        org.junit.jupiter.api.Assertions.assertEquals(8500, balanceOf(hana.token()));
        mvc.perform(get("/api/transactions").header("Authorization", "Bearer " + hana.token()))
            .andExpect(jsonPath("$.items[0].type").value("WITHDRAWAL"))
            .andExpect(jsonPath("$.items[0].amountCents").value(1500))
            .andExpect(jsonPath("$.items[0].note").value("comic"));

        // newest first, both statuses visible to the kid
        mvc.perform(get("/api/withdrawals").header("Authorization", "Bearer " + hana.token()))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].status").value("APPROVED"))
            .andExpect(jsonPath("$.items[1].status").value("REJECTED"));
    }

    @Test
    void approvalFailsWhenMoneyIsGoneMeanwhile() throws Exception {
        Session ivan = register("ivan");
        String admin = adminToken();
        fund(admin, ivan.accountId(), 3000);
        long id = request(ivan.token(), "{\"amountCents\":3000}");

        // admin takes money out directly before deciding
        mvc.perform(post("/api/admin/accounts/" + ivan.accountId() + "/withdraw").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amountCents\":1000}"))
            .andExpect(status().isCreated());

        mvc.perform(post("/api/admin/withdrawals/" + id + "/approve").header("Authorization", "Bearer " + admin))
            .andExpect(status().isUnprocessableEntity());
        // still pending, balance unchanged
        mvc.perform(get("/api/withdrawals").header("Authorization", "Bearer " + ivan.token()))
            .andExpect(jsonPath("$.items[0].status").value("PENDING"));
        org.junit.jupiter.api.Assertions.assertEquals(2000, balanceOf(ivan.token()));
    }

    @Test
    void eachKidOnlySeesTheirOwnRequests() throws Exception {
        Session jane = register("jane");
        Session kip = register("kip");
        String admin = adminToken();
        fund(admin, jane.accountId(), 5000);
        fund(admin, kip.accountId(), 5000);
        request(jane.token(), "{\"amountCents\":1000,\"note\":\"jane wants a comic\"}");
        request(kip.token(), "{\"amountCents\":4000,\"note\":\"kip wants headphones\"}");

        mvc.perform(get("/api/withdrawals").header("Authorization", "Bearer " + jane.token()))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].note").value("jane wants a comic"));
        mvc.perform(get("/api/admin/withdrawals/pending").header("Authorization", "Bearer " + admin))
            .andExpect(jsonPath("$.items[?(@.note=='jane wants a comic')].amountCents").value(1000))
            .andExpect(jsonPath("$.items[?(@.note=='kip wants headphones')].amountCents").value(4000));
    }

    @Test
    void permissionsAndValidation() throws Exception {
        Session lena = register("lena");
        String admin = adminToken();
        fund(admin, lena.accountId(), 1000);

        mvc.perform(get("/api/admin/withdrawals/pending").header("Authorization", "Bearer " + lena.token()))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/withdrawals/1/approve").header("Authorization", "Bearer " + lena.token()))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/withdrawals")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/withdrawals/999999/approve").header("Authorization", "Bearer " + admin))
            .andExpect(status().isNotFound());

        mvc.perform(post("/api/withdrawals").header("Authorization", "Bearer " + lena.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"amountCents\":0}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.amountCents").exists());
        mvc.perform(post("/api/withdrawals").header("Authorization", "Bearer " + lena.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"amountCents\":100,\"note\":\"" + "x".repeat(141) + "\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.note").exists());
        mvc.perform(get("/api/withdrawals").header("Authorization", "Bearer " + lena.token()))
            .andExpect(jsonPath("$.totalItems").value(0));
    }
}
