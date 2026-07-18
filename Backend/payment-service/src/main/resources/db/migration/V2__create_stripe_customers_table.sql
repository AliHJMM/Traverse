CREATE TABLE payment.stripe_customers (
    user_id     BIGINT PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL
);
