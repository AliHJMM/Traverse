/// <reference types="cypress" />

describe('Filing a report', () => {
  it('lets a traveler file a report and shows it in their list', () => {
    cy.stubSession('TRAVELER', 2);
    cy.intercept('GET', '/api/travels/reports/mine', { body: [] }).as('mineEmpty');

    cy.visit('/reports');
    cy.wait('@mineEmpty');

    const report = {
      id: 1,
      reporterId: 2,
      subjectType: 'MANAGER',
      subjectId: 10,
      travelId: null,
      reason: 'Unprofessional conduct',
      status: 'OPEN',
      createdAt: '2026-07-01T00:00:00Z',
    };
    cy.intercept('POST', '/api/travels/reports', { statusCode: 201, body: report }).as('file');
    // After filing, the list reloads with the new report.
    cy.intercept('GET', '/api/travels/reports/mine', { body: [report] }).as('mineFilled');

    cy.contains('button', 'File a report').click();
    cy.get('input[formcontrolname=subjectId]').type('10', { force: true });
    cy.get('textarea[formcontrolname=reason]').type('Unprofessional conduct', { force: true });
    cy.contains('button', 'File report').click();

    cy.wait('@file');
    cy.wait('@mineFilled');
    cy.contains('Unprofessional conduct').should('be.visible');
  });
});
