package com.example.bonsai_shop.moderator;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ModeratorOrderDetailTemplateTest {

    @Test
    void orderDetailUsesVietnameseLedgerLabelsAndClientSideRefundGuard() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/moderator/order_detail.html"));

        assertThat(template).contains("ledger.ledgerTypeLabel");
        assertThat(template).contains("ledger.directionLabel");
        assertThat(template).contains("ledger.faultPartyLabel");
        assertThat(template).contains("ledger.ledgerStatusLabel");
        assertThat(template).contains("ph.paymentTypeLabel");
        assertThat(template).contains("ph.statusLabel");
        assertThat(template).contains("const REFUNDABLE_CASH");
        assertThat(template).contains("openFaultRefundConfirmation");
        assertThat(template).doesNotContain("forfeited deposit income");
    }
}
