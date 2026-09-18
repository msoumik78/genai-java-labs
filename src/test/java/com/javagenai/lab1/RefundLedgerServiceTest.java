package com.javagenai.lab1;

import static org.assertj.core.api.Assertions.assertThat;

import com.javagenai.lab1.services.RefundLedgerService;
import org.junit.jupiter.api.Test;

class RefundLedgerServiceTest {

    @Test
    void countsEachPost() {
        RefundLedgerService ledger = new RefundLedgerService();
        ledger.refund("A");
        ledger.refund("A");
        assertThat(ledger.count()).isEqualTo(2);
        ledger.reset();
        assertThat(ledger.count()).isEqualTo(0);
    }
}
