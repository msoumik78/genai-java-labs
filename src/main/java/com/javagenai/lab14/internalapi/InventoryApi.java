package com.javagenai.lab14.internalapi;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/inventory")
public class InventoryApi {

    private static final Map<String, Stock> STOCK = Map.of(
            "SKU-1", new Stock("SKU-1", "Notebook", 12),
            "SKU-2", new Stock("SKU-2", "Pen", 0),
            "SKU-3", new Stock("SKU-3", "Desk lamp", 4));

    @GetMapping("/{sku}")
    public Stock get(@PathVariable String sku) {
        Stock stock = STOCK.get(sku);
        if (stock == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown sku");
        }
        return stock;
    }

    public record Stock(String sku, String name, int onHand) {
    }
}
