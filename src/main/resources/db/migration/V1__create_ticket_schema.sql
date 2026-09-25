CREATE SEQUENCE IF NOT EXISTS ticket_public_id_seq START WITH 1000 INCREMENT BY 1;

CREATE TABLE tickets (
    id              UUID PRIMARY KEY,
    ticket_id       VARCHAR(32)  NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     VARCHAR(5000) NOT NULL,
    priority        VARCHAR(16)  NOT NULL,
    assignee        VARCHAR(200),
    status          VARCHAR(32)  NOT NULL,
    category        VARCHAR(100),
    resolution_notes TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_tickets_ticket_id UNIQUE (ticket_id),
    CONSTRAINT chk_tickets_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_tickets_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED'))
);

CREATE INDEX idx_tickets_status ON tickets (status);
CREATE INDEX idx_tickets_updated_at ON tickets (updated_at DESC);

CREATE TABLE comments (
    id          UUID PRIMARY KEY,
    ticket_id   UUID          NOT NULL,
    body        VARCHAR(5000) NOT NULL,
    author      VARCHAR(200)  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_comments_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE
);

CREATE INDEX idx_comments_ticket_id ON comments (ticket_id);
