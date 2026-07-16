CREATE SCHEMA IF NOT EXISTS payment;

CREATE TABLE payment.payment_methods (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    provider      VARCHAR(20)  NOT NULL,
    external_id   VARCHAR(255) NOT NULL,
    brand         VARCHAR(50),
    last4         VARCHAR(4),
    expiry_month  INTEGER,
    expiry_year   INTEGER,
    payer_email   VARCHAR(255),
    is_default    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_methods_user_id ON payment.payment_methods (user_id);
