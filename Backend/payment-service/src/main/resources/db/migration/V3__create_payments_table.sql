-- Records an actual payment/charge made by a traveler for a travel booking.
-- Payment *methods* (saved cards / PayPal accounts) live in payment_methods;
-- this table is the transaction ledger referenced by dashboards and the
-- traveler's payment history.
CREATE TABLE payment.payments (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT        NOT NULL,
    travel_id          BIGINT        NOT NULL,
    payment_method_id  BIGINT,
    provider           VARCHAR(20)   NOT NULL,
    amount             NUMERIC(10, 2) NOT NULL,
    currency           VARCHAR(3)    NOT NULL DEFAULT 'USD',
    status             VARCHAR(20)   NOT NULL,
    external_charge_id VARCHAR(255),
    created_at         TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_user_id ON payment.payments (user_id);
CREATE INDEX idx_payments_travel_id ON payment.payments (travel_id);
