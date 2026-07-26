-- Part 2: travelers leave one rating+comment per travel they participated in.
CREATE TABLE travel.feedback (
    id          BIGSERIAL PRIMARY KEY,
    travel_id   BIGINT       NOT NULL REFERENCES travel.travels (id) ON DELETE CASCADE,
    traveler_id BIGINT       NOT NULL,
    rating      INTEGER      NOT NULL,
    comment     VARCHAR(2000),
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_feedback_travel_traveler UNIQUE (travel_id, traveler_id)
);

CREATE INDEX idx_feedback_travel ON travel.feedback (travel_id);
CREATE INDEX idx_feedback_traveler ON travel.feedback (traveler_id);
