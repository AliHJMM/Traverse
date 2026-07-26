package com.traverse.auth.entity;

/**
 * Part 2 roles. The legacy Part-1 `USER` role was migrated to `TRAVELER`
 * (see Flyway V2) -- a plain traveler is the default for public signups.
 * TRAVEL_MANAGER can create/manage their own travels; ADMIN oversees all.
 */
public enum Role {
    ADMIN,
    TRAVEL_MANAGER,
    TRAVELER
}
