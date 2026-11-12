package com.javagenai.lab14.internalapi;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/customers")
public class CustomerApi {

    private static final Map<String, Customer> CUSTOMERS = Map.of(
            "C1", new Customer("C1", "Ada Lovelace", "ada@example.com"),
            "C2", new Customer("C2", "Grace Hopper", "grace@example.com"),
            "C3", new Customer("C3", "Alan Turing", "alan@example.com"));

    @GetMapping("/{id}")
    public Customer get(@PathVariable String id) {
        Customer customer = CUSTOMERS.get(id);
        if (customer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown customer");
        }
        return customer;
    }

    public record Customer(String id, String name, String email) {
    }
}
