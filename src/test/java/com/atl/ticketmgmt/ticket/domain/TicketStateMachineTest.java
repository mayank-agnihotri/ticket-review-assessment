package com.atl.ticketmgmt.ticket.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.atl.ticketmgmt.common.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;

class TicketStateMachineTest {

    @Test
    void allowsAllDefinedTransitions() {
        assertThat(TicketStateMachine.canTransition(TicketStatus.OPEN, TicketStatus.IN_PROGRESS))
                .isTrue();
        assertThat(TicketStateMachine.canTransition(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED))
                .isTrue();
        assertThat(TicketStateMachine.canTransition(TicketStatus.RESOLVED, TicketStatus.CLOSED))
                .isTrue();
        assertThat(TicketStateMachine.canTransition(TicketStatus.OPEN, TicketStatus.CANCELLED))
                .isTrue();
        assertThat(TicketStateMachine.canTransition(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED))
                .isTrue();
    }

    @Test
    void rejectsAssessmentInvalidExamples() {
        assertInvalid(TicketStatus.CLOSED, TicketStatus.OPEN);
        assertInvalid(TicketStatus.RESOLVED, TicketStatus.OPEN);
        assertInvalid(TicketStatus.CANCELLED, TicketStatus.OPEN);
    }

    @Test
    void rejectsOtherInvalidTransitions() {
        assertInvalid(TicketStatus.OPEN, TicketStatus.RESOLVED);
        assertInvalid(TicketStatus.OPEN, TicketStatus.CLOSED);
        assertInvalid(TicketStatus.IN_PROGRESS, TicketStatus.OPEN);
        assertInvalid(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED);
        assertInvalid(TicketStatus.RESOLVED, TicketStatus.IN_PROGRESS);
        assertInvalid(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS);
        assertInvalid(TicketStatus.CANCELLED, TicketStatus.IN_PROGRESS);
    }

    private static void assertInvalid(TicketStatus from, TicketStatus to) {
        assertThat(TicketStateMachine.canTransition(from, to)).isFalse();
        assertThatThrownBy(() -> TicketStateMachine.assertTransition(from, to))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining(from.name())
                .hasMessageContaining(to.name());
    }
}
