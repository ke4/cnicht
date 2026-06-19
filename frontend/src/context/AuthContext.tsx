import { createContext, useState, useContext, useEffect } from 'react';
import type { ReactNode } from 'react';
import axios from 'axios';

// Configure axios
axios.defaults.withCredentials = true;

interface User {
  email: string;
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  setUser: (user: User | null) => void;
  login: (credentials: { email: string; password: string }) => Promise<void>;
  register: (data: { email: string; password: string }) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Interceptor to handle CSRF
    const interceptor = axios.interceptors.request.use(config => {
      const xsrfToken = document.cookie
        .split('; ')
        .find(row => row.startsWith('XSRF-TOKEN='))
        ?.split('=')[1];

      if (xsrfToken && config.headers) {
        config.headers['X-XSRF-TOKEN'] = xsrfToken;
      }
      return config;
    });

    const checkAuth = async () => {
      try {
        const response = await axios.get('/api/auth/status');
        if (response.data && response.data.username) {
          setUser({ email: response.data.username });
        }
      } catch (err) {
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    checkAuth();

    return () => axios.interceptors.request.eject(interceptor);
  }, []);

  const logout = async () => {
    try {
      await axios.post('/api/auth/logout');
    } finally {
      setUser(null);
    }
  };

  const login = async (credentials: { email: string; password: string }) => {
    const params = new URLSearchParams();
    params.append('username', credentials.email);
    params.append('password', credentials.password);

    const response = await axios.post('/api/auth/login', params);
    if (response.data && response.data.username) {
      setUser({ email: response.data.username });
    }
  };

  const register = async (data: { email: string; password: string }) => {
    const params = new URLSearchParams();
    params.append('email', data.email);
    params.append('password', data.password);
    await axios.post('/api/auth/register', params);
  };

  return (
    <AuthContext.Provider value={{ user, loading, setUser, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
