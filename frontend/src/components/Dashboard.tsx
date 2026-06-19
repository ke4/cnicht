import { useAuth } from '../context/AuthContext';

interface DashboardProps {
  onNavigate?: (path: string) => void;
}

const Dashboard = ({ onNavigate }: DashboardProps) => {
  const { logout } = useAuth();

  const handleLogout = async () => {
    await logout();
    if (onNavigate) {
      onNavigate('/login');
    }
  };

  return (
    <div className="max-w-md w-full">
      <h1 className="text-3xl font-bold text-center mb-8">Dashboard</h1>
      <button
        data-cy="logout-button"
        onClick={handleLogout}
        className="bg-red-500 text-white px-4 py-2 rounded hover:bg-red-600"
        style={{
            position: "absolute",
            top: "1rem",
            right: "1rem",
        }}
      >
        Log Out
      </button>
    </div>
  );
};

export default Dashboard;
