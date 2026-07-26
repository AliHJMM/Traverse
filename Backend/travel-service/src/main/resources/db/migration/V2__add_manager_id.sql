-- Part 2: a travel is now owned by the Travel Manager who created it. Admins
-- can still manage any travel; managers only their own. Nullable so the
-- Part-1 travels created before this column existed remain valid.
ALTER TABLE travel.travels ADD COLUMN manager_id BIGINT;
CREATE INDEX idx_travels_manager_id ON travel.travels (manager_id);
