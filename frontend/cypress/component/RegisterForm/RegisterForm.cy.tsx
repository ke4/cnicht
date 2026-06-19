import RegisterForm from '../../../src/components/RegisterForm.tsx';

describe('RegisterForm.cy.tsx', () => {
  it('displays validation messages and blocks submit for invalid input', () => {
    const onRegister = cy.stub().as('onRegisterStub');
    cy.mount(<RegisterForm onRegister={onRegister} />);

    // Click register with empty fields
    cy.get('[data-cy="register-submit"]').click();

    // Assert validation messages (assuming specific data-cy for errors)
    cy.get('[data-cy="email-error"]').should('be.visible');
    cy.get('[data-cy="password-error"]').should('be.visible');
    cy.get('@onRegisterStub').should('not.have.been.called');

    // Invalid email pattern
    cy.get('[data-cy="email-input"]').type('invalid-email');
    cy.get('[data-cy="register-submit"]').click();
    cy.get('[data-cy="email-error"]').should('contain', 'Invalid email pattern');
    cy.get('@onRegisterStub').should('not.have.been.called');
  });

  it('fires onRegister callback for valid input', () => {
    const onRegister = cy.stub().as('onRegisterStub');
    cy.mount(<RegisterForm onRegister={onRegister} />);

    cy.get('[data-cy="email-input"]').type('test@example.com');
    cy.get('[data-cy="password-input"]').type('Password123!');
    cy.get('[data-cy="confirm-password-input"]').type('Password123!');
    
    cy.get('[data-cy="register-submit"]').click();

    cy.get('@onRegisterStub').should('have.been.calledWith', {
      email: 'test@example.com',
      password: 'Password123!',
      confirmPassword: 'Password123!'
    });
  });
});
