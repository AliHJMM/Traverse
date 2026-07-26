-- Part 2: mirror auth-service's role migration on the profile side --
-- legacy "USER" profiles become "TRAVELER". ADMIN rows are untouched.
UPDATE users.user_profiles SET role = 'TRAVELER' WHERE role = 'USER';
