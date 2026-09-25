package com.atl.ticketmgmt.ticket.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PriorityTest {

    @Test
    void containsDesignDecisionValues() {
        assertThat(Priority.values())
                .containsExactly(Priority.LOW, Priority.MEDIUM, Priority.HIGH, Priority.CRITICAL);
    }
}
