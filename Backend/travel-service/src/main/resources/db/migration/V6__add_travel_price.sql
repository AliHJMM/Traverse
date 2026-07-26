-- Booking price per traveler for a travel; income is aggregated as
-- price x active subscriptions. Defaults to 0 for pre-existing travels.
ALTER TABLE travel.travels ADD COLUMN price NUMERIC(10, 2) NOT NULL DEFAULT 0;
