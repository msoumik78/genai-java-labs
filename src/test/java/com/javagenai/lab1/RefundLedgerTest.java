package com.javagenai.lab1;

import static org.assertj.core.api.Assertions.assertThat;

import com.javagenai.lab1.services.RefundLedger;
import org.junit.jupiter.api.Test;

class RefundLedgerTest {

    @Test
    void countsEachPost() {
        RefundLedger ledger = new RefundLedger();
        ledger.refund("A");
        ledger.refund("A");
        assertThat(ledger.count()).isEqualTo(2);
    }
}
