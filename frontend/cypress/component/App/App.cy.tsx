import App from '../../../src/App'

describe('App.cy.tsx', () => {
  it('renders', () => {
    cy.mount(<App />)
    cy.get('h1').should('exist')
  })
})
