CREATE SCHEMA IF NOT EXISTS travel;

CREATE TABLE travel.travels (
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(255) NOT NULL,
    start_date DATE         NOT NULL,
    end_date   DATE         NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE travel.destinations (
    id              BIGSERIAL PRIMARY KEY,
    travel_id       BIGINT       NOT NULL REFERENCES travel.travels (id) ON DELETE CASCADE,
    city            VARCHAR(255) NOT NULL,
    country         VARCHAR(255) NOT NULL,
    arrival_date    DATE,
    departure_date  DATE
);

CREATE TABLE travel.activities (
    id               BIGSERIAL PRIMARY KEY,
    travel_id        BIGINT        NOT NULL REFERENCES travel.travels (id) ON DELETE CASCADE,
    name             VARCHAR(255)  NOT NULL,
    description      VARCHAR(1000),
    destination_city VARCHAR(255),
    date             DATE,
    cost             NUMERIC(10, 2)
);

CREATE TABLE travel.accommodations (
    id         BIGSERIAL PRIMARY KEY,
    travel_id  BIGINT       NOT NULL REFERENCES travel.travels (id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    type       VARCHAR(100) NOT NULL,
    address    VARCHAR(500),
    check_in   DATE,
    check_out  DATE
);

CREATE TABLE travel.transportations (
    id              BIGSERIAL PRIMARY KEY,
    travel_id       BIGINT       NOT NULL REFERENCES travel.travels (id) ON DELETE CASCADE,
    type            VARCHAR(100) NOT NULL,
    provider        VARCHAR(255),
    from_location   VARCHAR(255) NOT NULL,
    to_location     VARCHAR(255) NOT NULL,
    departure_time  TIMESTAMP,
    arrival_time    TIMESTAMP
);
