describe('Job Tracking Flow', () => {
  const password = 'e2ePWD123';
  const email = `e2e_test_user@test.com`;

  const registerUser = () => {
    return cy.request({
      method: 'POST',
      url: '/api/auth/register?email=e2e_test_user@test.com&password=e2ePWD123',
      headers: {
        'Content-Type': 'application/json',
      },
      failOnStatusCode: false, // do not fail on 4xx / 5xx
    });
  };

  const login = () => {
    cy.get('[data-cy="email-input"]').clear().type(email);
    cy.get('[data-cy="password-input"]').clear().type(password);
    cy.get('[data-cy="login-submit"]').click();
  };

  const logout = () => {
    cy.contains('Dashboard').should('be.visible');
    cy.get('[data-cy="logout-button"]').click();

    cy.get('[data-cy="login-submit"]').should('be.visible');
  }

  beforeEach(() => {
    cy.visit('/');
  });

  it('allows a user to register, login and logout', () => {
    registerUser()

    cy.visit('/')

    login()

    cy.getCookie('XSRF-TOKEN').should('exist');

    logout()

  });
});
