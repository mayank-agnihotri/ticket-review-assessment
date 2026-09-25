package com.atl.ticketmgmt.ticket.repository;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class TicketPublicIdSequenceRepository {

    private final EntityManager entityManager;

    public TicketPublicIdSequenceRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long nextValue() {
        Number value = (Number) entityManager
                .createNativeQuery("SELECT nextval('ticket_public_id_seq')")
                .getSingleResult();
        return value.longValue();
    }
}
