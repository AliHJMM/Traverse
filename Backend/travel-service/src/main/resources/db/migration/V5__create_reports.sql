-- Part 2: travelers can report a manager or another traveler. Admins review.
CREATE TABLE travel.reports (
    id           BIGSERIAL PRIMARY KEY,
    reporter_id  BIGINT       NOT NULL,
    subject_type VARCHAR(20)  NOT NULL,   -- MANAGER | TRAVELER
    subject_id   BIGINT       NOT NULL,
    travel_id    BIGINT,                  -- optional: the travel it relates to
    reason       VARCHAR(2000) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'OPEN',  -- OPEN | REVIEWED
    created_at   TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_reports_subject ON travel.reports (subject_type, subject_id);
CREATE INDEX idx_reports_reporter ON travel.reports (reporter_id);
