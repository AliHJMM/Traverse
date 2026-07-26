-- Part 2: the Part-1 "USER" role is renamed to "TRAVELER". Existing rows
-- created under Part 1 carry role='USER'; migrate them so they line up with
-- the new Role enum (ADMIN / TRAVEL_MANAGER / TRAVELER). ADMIN rows are
-- untouched.
UPDATE auth.users SET role = 'TRAVELER' WHERE role = 'USER';
