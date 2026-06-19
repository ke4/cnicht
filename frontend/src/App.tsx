import { useState } from 'react'
import './App.css'
import LoginForm from './components/LoginForm'
import RegisterForm from './components/RegisterForm'
import Dashboard from './components/Dashboard'
import { AuthProvider, useAuth } from './context/AuthContext'

function AppContent() {
  const { user, login, register, loading } = useAuth();
  const [view, setView] = useState<'login' | 'register'>('login');

  const handleLogin = async (credentials: any) => {
    try {
      await login(credentials);
    } catch (err) {
      console.error('Login failed', err);
      alert('Login failed. Please check your credentials.');
    }
  };

  const handleRegister = async (data: any) => {
    try {
      await register(data);
      alert('Registration successful! Please login.');
      setView('login');
    } catch (err) {
      console.error('Registration failed', err);
      alert('Registration failed. Email might already be in use.');
    }
  };

  const handleNavigate = (path: string) => {
    if (path === '/login') setView('login');
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-xl font-semibold">Loading...</div>
      </div>
    );
  }

  if (user) {
    return <Dashboard onNavigate={handleNavigate} />;
  }

  return (
    <div className="min-h-screen bg-gray-100 flex flex-col items-center justify-center p-4">
      <div className="max-w-md w-full">
        {view === 'login' ? (
          <>
            <h1 className="text-3xl font-bold text-center mb-8">Login to Cnicht</h1>
            <LoginForm onLogin={handleLogin} />
            <p className="register mt-4 text-center">
              Don't have an account?{' '}
              <button 
                data-cy="switch-to-register"
                onClick={() => setView('register')} 
                className="text-blue-500 hover:underline"
              >
                Register
              </button>
            </p>
          </>
        ) : (
          <>
            <h1 className="text-3xl font-bold text-center mb-8">Create Account</h1>
            <RegisterForm onRegister={handleRegister} />
            <p className="mt-4 text-center">
              Already have an account?{' '}
              <button 
                data-cy="switch-to-login"
                onClick={() => setView('login')} 
                className="text-blue-500 hover:underline"
              >
                Login
              </button>
            </p>
          </>
        )}
      </div>
    </div>
  );
}

function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  )
}

export default App
