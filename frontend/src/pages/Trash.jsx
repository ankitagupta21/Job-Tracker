import { useEffect, useState } from "react";
import { getDeletedApplications, restoreApplication } from "../api/applicationApi";
import StatusBadge from "../components/StatusBadge";

export default function Trash() {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDeleted();
  }, []);

  const fetchDeleted = async () => {
    setLoading(true);
    try {
      const res = await getDeletedApplications();
      setApplications(res.data.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleRestore = async (id, e) => {
    e.stopPropagation();
    await restoreApplication(id);
    fetchDeleted();
  };

  return (
    <div className="max-w-6xl mx-auto px-6 py-8">
      <h1 className="text-xl font-bold text-gray-800 mb-6">Trash</h1>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-gray-400">Loading...</div>
        ) : applications.length === 0 ? (
          <div className="p-8 text-center text-gray-400">
            No deleted applications.
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                {["Company", "Role", "Status", "Source", "Applied Date", "Actions"].map(
                  (h) => (
                    <th
                      key={h}
                      className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide"
                    >
                      {h}
                    </th>
                  ),
                )}
              </tr>
            </thead>
            <tbody>
              {applications.map((app) => (
                <tr
                  key={app.id}
                  className="border-b border-gray-100 hover:bg-gray-50 transition"
                >
                  <td className="px-4 py-3 font-medium text-gray-800">
                    {app.companyName}
                  </td>
                  <td className="px-4 py-3 text-gray-600">{app.role}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={app.status} />
                  </td>
                  <td className="px-4 py-3 text-gray-500">{app.source}</td>
                  <td className="px-4 py-3 text-gray-500">
                    {app.appliedDate || "—"}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={(e) => handleRestore(app.id, e)}
                      className="text-indigo-600 hover:text-indigo-800 text-xs font-medium"
                    >
                      Restore
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
