-- Part 2: travelers subscribe to travels. One active subscription per
-- (travel, traveler) via a partial unique index; cancelling flips status to
-- CANCELLED (kept, not deleted, so traveler stats can count cancellations).
CREATE TABLE travel.subscriptions (
    id           BIGSERIAL PRIMARY KEY,
    travel_id    BIGINT      NOT NULL REFERENCES travel.travels (id) ON DELETE CASCADE,
    traveler_id  BIGINT      NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'SUBSCRIBED',
    created_at   TIMESTAMP   NOT NULL DEFAULT now(),
    cancelled_at TIMESTAMP
);

CREATE UNIQUE INDEX uq_active_subscription
    ON travel.subscriptions (travel_id, traveler_id)
    WHERE status = 'SUBSCRIBED';

CREATE INDEX idx_subscriptions_traveler ON travel.subscriptions (traveler_id);
CREATE INDEX idx_subscriptions_travel ON travel.subscriptions (travel_id);
