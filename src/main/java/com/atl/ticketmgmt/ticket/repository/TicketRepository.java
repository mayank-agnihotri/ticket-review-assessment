package com.atl.ticketmgmt.ticket.repository;

import com.atl.ticketmgmt.ticket.domain.Ticket;
import com.atl.ticketmgmt.ticket.domain.TicketStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByTicketId(String ticketId);

    boolean existsByTicketId(String ticketId);

    @Query("""
            SELECT t FROM Ticket t
            WHERE (:status IS NULL OR t.status = :status)
              AND (
                :q IS NULL
                OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(t.description) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(t.ticketId) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<Ticket> findForList(
            @Param("q") String q, @Param("status") TicketStatus status, Pageable pageable);
}
