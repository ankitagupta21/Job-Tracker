import { Link, useLocation } from "react-router-dom";

export default function Navbar() {
  const location = useLocation();

  return (
    <nav className="bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
      <div className="flex items-center gap-2">
        <span className="text-xl font-bold text-indigo-600">JobTracker</span>
      </div>
      <div className="flex items-center gap-4">
        <Link
          to="/"
          className={`text-sm font-medium ${
            location.pathname === "/"
              ? "text-indigo-600"
              : "text-gray-500 hover:text-gray-800"
          }`}
        >
          Dashboard
        </Link>
        <Link
          to="/add"
          className="bg-indigo-600 text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-indigo-700 transition"
        >
          + Add Application
        </Link>
      </div>
    </nav>
  );
}
