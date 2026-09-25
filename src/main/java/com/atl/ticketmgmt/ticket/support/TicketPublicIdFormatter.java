package com.atl.ticketmgmt.ticket.support;

/**
 * Design Decision — public ticket id format TKT-{sequence} per spec/data-model.md.
 */
public final class TicketPublicIdFormatter {

    private TicketPublicIdFormatter() {
    }

    public static String format(long sequence) {
        return "TKT-" + sequence;
    }
}
