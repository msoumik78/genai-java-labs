package com.javagenai.lab1.services;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class RefundLedger {

    private final AtomicInteger refunds = new AtomicInteger();

    public int refund(String orderId) {
        return refunds.incrementAndGet();
    }

    public int count() {
        return refunds.get();
    }

    public void reset() {
        refunds.set(0);
    }
}
