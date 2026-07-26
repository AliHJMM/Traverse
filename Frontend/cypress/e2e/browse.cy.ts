/// <reference types="cypress" />

const travel = {
  id: 1,
  title: 'Paris Getaway',
  startDate: '2026-09-01',
  endDate: '2026-09-05',
  durationDays: 5,
  price: 500,
  managerId: 10,
  destinations: [{ city: 'Paris', country: 'France', arrivalDate: null, departureDate: null }],
  activities: [],
  accommodations: [],
  transportations: [],
  createdAt: '2026-07-01T00:00:00Z',
};

describe('Traveler browse, search & subscribe', () => {
  beforeEach(() => {
    cy.stubSession('TRAVELER', 2);
    cy.intercept('GET', '/api/travels/recommendations', { body: [] });
    cy.intercept('GET', '/api/travels', { body: [travel] });
    cy.intercept('GET', '/api/travels/subscriptions/mine', { body: [] });
  });

  it('lists available trips and searches via Elasticsearch', () => {
    cy.intercept('GET', '/api/search/autocomplete*', { body: { suggestions: ['Paris'] } });
    cy.intercept('GET', '/api/search/travels*', {
      body: [
        {
          id: '1',
          title: 'Paris Getaway',
          destinationCities: ['Paris'],
          destinationCountries: ['France'],
          activities: ['Museum tour'],
          accommodations: [],
          transportationTypes: [],
          startDate: '2026-09-01',
          endDate: '2026-09-05',
          durationDays: 5,
          managerId: 10,
        },
      ],
    }).as('search');

    cy.visit('/browse');
    cy.contains('Paris Getaway').should('be.visible');

    cy.get('input[matinput]').first().type('paris');
    cy.wait('@search');
    cy.contains('Museum tour').should('be.visible');
  });

  it('subscribes to a trip', () => {
    cy.intercept('POST', '/api/travels/1/subscribe', {
      statusCode: 201,
      body: { id: 9, travelId: 1, travelerId: 2, status: 'SUBSCRIBED', createdAt: '' },
    }).as('subscribe');

    cy.visit('/browse');
    cy.contains('button', 'Subscribe').first().click();
    cy.wait('@subscribe');
    cy.contains('Subscribed to trip.').should('be.visible');
  });
});
