/// <reference types="cypress" />

describe('Authentication & role-based access', () => {
  it('redirects an unauthenticated visitor to the login page', () => {
    cy.intercept('GET', '/api/auth/me', { statusCode: 401, body: {} });
    cy.visit('/dashboard');
    cy.location('pathname').should('eq', '/login');
    cy.contains('Sign in');
  });

  it('logs a traveler in and lands on their dashboard', () => {
    const traveler = { id: 2, email: 'traveler@example.com', role: 'TRAVELER' };
    cy.intercept('POST', '/api/auth/login', { statusCode: 200, body: traveler }).as('login');
    cy.intercept('GET', '/api/auth/me', { statusCode: 200, body: traveler });
    cy.intercept('GET', '/api/travels/stats/traveler/me', {
      body: { travelerId: 2, activeTrips: 0, cancellations: 0, feedbackGiven: 0, reportsFiled: 0 },
    });
    cy.intercept('GET', '/api/payments', { body: [] });

    cy.visit('/login');
    cy.get('input[type=email]').type('traveler@example.com');
    cy.get('input[type=password]').type('password123');
    cy.get('button[type=submit]').click();

    cy.wait('@login');
    cy.location('pathname').should('eq', '/dashboard');
    // Traveler-specific navigation is present…
    cy.contains('a', 'Browse').should('be.visible');
    // …and admin-only navigation is not.
    cy.contains('a', 'Users').should('not.exist');
  });

  it('keeps a traveler out of the admin-only Users page', () => {
    cy.stubSession('TRAVELER', 2);
    cy.intercept('GET', '/api/travels/stats/traveler/me', {
      body: { travelerId: 2, activeTrips: 0, cancellations: 0, feedbackGiven: 0, reportsFiled: 0 },
    });
    cy.intercept('GET', '/api/payments', { body: [] });

    cy.visit('/users');
    cy.location('pathname').should('eq', '/dashboard');
  });
});
