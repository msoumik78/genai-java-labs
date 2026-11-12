package com.javagenai.lab14.tools;

import java.util.List;
import java.util.Set;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.javagenai.lab14.internalapi.CustomerApi.Customer;
import com.javagenai.lab14.internalapi.InventoryApi.Stock;

@Service
public class EnterpriseTools {

    private static final Set<String> STATUSES = Set.of("PAID", "PENDING", "SHIPPED");

    private final JdbcClient jdbc;
    private final ServletWebServerApplicationContext server;
    private final int maxRows;

    public EnterpriseTools(JdbcClient jdbc, ServletWebServerApplicationContext server,
            @Value("${lab.max-rows}") int maxRows) {
        this.jdbc = jdbc;
        this.server = server;
        this.maxRows = maxRows;
    }

    @Tool(description = "List orders in one status. Status must be PAID, PENDING, or SHIPPED. Returns at most the configured row cap.")
    public List<Order> findOrdersByStatus(@ToolParam(description = "Order status: PAID, PENDING, or SHIPPED") String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("status must be PAID, PENDING, or SHIPPED");
        }
        return jdbc.sql("""
                select id, status, customer_id, total_cents
                from orders
                where status = :status
                order by id
                fetch first :limit rows only
                """)
                .param("status", normalized)
                .param("limit", maxRows)
                .query(Order.class)
                .list();
    }

    @Tool(description = "Load one customer from the customer REST API by customer id, such as C1.")
    public Customer getCustomer(@ToolParam(description = "Customer id, such as C1") String customerId) {
        try {
            return http().get()
                    .uri("/internal/customers/{id}", customerId)
                    .retrieve()
                    .body(Customer.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new IllegalArgumentException("Unknown customer " + customerId);
        }
    }

    @Tool(description = "Check on-hand quantity for one SKU from the inventory service, such as SKU-1.")
    public Stock checkInventory(@ToolParam(description = "SKU, such as SKU-1") String sku) {
        try {
            return http().get()
                    .uri("/internal/inventory/{sku}", sku)
                    .retrieve()
                    .body(Stock.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new IllegalArgumentException("Unknown sku " + sku);
        }
    }

    private RestClient http() {
        int port = server.getWebServer().getPort();
        return RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
    }

    public record Order(String id, String status, String customerId, int totalCents) {
    }
}
