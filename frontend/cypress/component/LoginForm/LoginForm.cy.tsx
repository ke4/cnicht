import LoginForm from '../../../src/components/LoginForm.tsx';

describe('LoginForm.cy.tsx', () => {
  it('prompts an error reading "Email and password are required" for empty inputs', () => {
    const onLogin = cy.stub().as('onLoginStub');
    cy.mount(<LoginForm onLogin={onLogin} />);

    cy.get('[data-cy="login-submit"]').click();

    cy.get('[data-cy="login-error"]').should('contain', 'Email and password are required');
    cy.get('@onLoginStub').should('not.have.been.called');
  });

  it('executes onLogin with the target credentials for valid input', () => {
    const onLogin = cy.stub().as('onLoginStub');
    cy.mount(<LoginForm onLogin={onLogin} />);

    cy.get('[data-cy="email-input"]').type('user@example.com');
    cy.get('[data-cy="password-input"]').type('secret123');
    
    cy.get('[data-cy="login-submit"]').click();

    cy.get('@onLoginStub').should('have.been.calledWith', {
      email: 'user@example.com',
      password: 'secret123'
    });
  });
});
