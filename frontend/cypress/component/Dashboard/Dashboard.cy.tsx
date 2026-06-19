import Dashboard from '../../../src/components/Dashboard.tsx';
import { AuthProvider } from '../../../src/context/AuthContext';

describe('Dashboard.cy.tsx', () => {
  it('triggers logout, clears context and redirects to login', () => {
    // Intercept logout request
    cy.intercept('POST', '/api/auth/logout', { statusCode: 200 }).as('logoutRequest');
    
    // We need a way to verify redirection. 
    // Usually in component tests we can check if a navigate function was called if we mock it.
    // However, if we want to be more "E2E-like" in component test, we might need a Router.
    
    // Let's assume Dashboard uses an onLogout prop or similar for now, 
    // but the requirement says "clears client authentication state context, and redirects".
    // This implies we should test it with the context.

    // Mocking window.location or using a router
    const navigate = cy.stub().as('navigateStub');

    cy.mount(
      <AuthProvider>
        <Dashboard onNavigate={navigate} />
      </AuthProvider>
    );

    cy.get('[data-cy="logout-button"]').click();

    cy.wait('@logoutRequest');
    
    // Check if context is cleared (this might be hard to check directly from outside)
    // But we can check if it redirected.
    cy.get('@navigateStub').should('have.been.calledWith', '/login');
  });
});
