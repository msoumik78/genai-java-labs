package com.javagenai.lab1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.javagenai.lab1.services.ModelCallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LabControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    ModelCallService model;

    @BeforeEach
    void stubModel() {
        when(model.complete(anyString())).thenReturn("pong");
    }

    @Test
    void ordersStaysFast() throws Exception {
        mvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.ms").value(1));
    }

    @Test
    void statsReadsNestedYaml() throws Exception {
        mvc.perform(get("/lab/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extraHoldMs").value(8000))
                .andExpect(jsonPath("$.fenceSize").value(2));
    }

    @Test
    void badRetryPostsThreeRefunds() throws Exception {
        mvc.perform(post("/lab/reset-ledger")).andExpect(status().isOk());
        mvc.perform(get("/refund/bad-retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundsPosted").value(3));
    }

    @Test
    void idempotentRefundPostsOnce() throws Exception {
        mvc.perform(post("/lab/reset-ledger")).andExpect(status().isOk());
        mvc.perform(get("/refund/idempotent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundsPosted").value(1));
    }

    @Test
    void naiveChatUsesModel() throws Exception {
        mvc.perform(get("/chat/naive").param("q", "ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("naive"))
                .andExpect(jsonPath("$.answer").value("pong"));
    }
}
