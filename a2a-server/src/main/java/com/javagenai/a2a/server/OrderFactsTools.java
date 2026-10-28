package com.javagenai.a2a.server;

import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class OrderFactsTools {

    private static final Map<String, String> ORDERS = Map.of(
            "A-100", "order A-100 status=SHIPPED city=Pune amount=1499",
            "B-200", "order B-200 status=PACKED city=Bengaluru amount=899",
            "C-300", "order C-300 status=CANCELLED city=Delhi amount=0");

    @Tool(description = "Look up a known order by id. Returns status, city, and amount.")
    public String lookupOrder(@ToolParam(description = "Order id, for example A-100") String orderId) {
        String key = orderId == null ? "" : orderId.trim().toUpperCase();
        return ORDERS.getOrDefault(key, "unknown order id " + orderId);
    }
}
