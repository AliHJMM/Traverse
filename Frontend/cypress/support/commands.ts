/// <reference types="cypress" />

export type Role = 'ADMIN' | 'TRAVEL_MANAGER' | 'TRAVELER';

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      /**
       * Stub the httpOnly-cookie session by intercepting the /api/auth/me
       * probe every guard makes, so a spec can land on any authed route
       * without driving the real login each time.
       */
      stubSession(role: Role, id?: number): Chainable<void>;
    }
  }
}

Cypress.Commands.add('stubSession', (role: Role, id = 1) => {
  cy.intercept('GET', '/api/auth/me', {
    statusCode: 200,
    body: { id, email: `${role.toLowerCase()}@example.com`, role },
  }).as('me');
});

export {};
