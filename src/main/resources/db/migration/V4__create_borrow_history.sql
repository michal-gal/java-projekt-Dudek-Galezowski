CREATE TABLE borrow_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    reservation_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'BORROWED',
    borrowed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_at TIMESTAMP NOT NULL,
    returned_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_borrow_history_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_borrow_history_book FOREIGN KEY (book_id) REFERENCES books (id),
    CONSTRAINT fk_borrow_history_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT chk_borrow_history_status CHECK (status IN ('BORROWED', 'RETURNED', 'OVERDUE', 'LOST')),
    CONSTRAINT chk_borrow_history_due_at CHECK (due_at > borrowed_at),
    CONSTRAINT chk_borrow_history_returned_at CHECK (status <> 'RETURNED' OR returned_at IS NOT NULL),
    CONSTRAINT chk_borrow_history_return_order CHECK (returned_at IS NULL OR returned_at >= borrowed_at)
);

CREATE INDEX idx_borrow_history_user_id ON borrow_history (user_id);
CREATE INDEX idx_borrow_history_book_id ON borrow_history (book_id);
CREATE INDEX idx_borrow_history_reservation_id ON borrow_history (reservation_id);
CREATE INDEX idx_borrow_history_status ON borrow_history (status);
CREATE INDEX idx_borrow_history_due_at ON borrow_history (due_at);
