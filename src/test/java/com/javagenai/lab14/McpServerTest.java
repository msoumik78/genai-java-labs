package com.javagenai.lab14;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.javagenai.lab14.client.McpSelfClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class McpServerTest {

    @Autowired
    McpSelfClient client;

    @Test
    void clientListsTheThreeTools() {
        assertThat(client.listToolNames())
                .contains("findOrdersByStatus", "getCustomer", "checkInventory");
    }

    @Test
    void clientCallsEachToolThroughMcp() {
        assertThat(client.call("findOrdersByStatus", Map.of("status", "PAID"))).contains("1001", "1003");
        assertThat(client.call("getCustomer", Map.of("customerId", "C1"))).contains("Ada Lovelace");
        assertThat(client.call("checkInventory", Map.of("sku", "SKU-1"))).contains("12");
    }

    @Test
    void unknownStatusIsRejected() {
        assertThatThrownBy(() -> client.call("findOrdersByStatus", Map.of("status", "DROP TABLE")))
                .isInstanceOf(IllegalStateException.class);
    }
}
