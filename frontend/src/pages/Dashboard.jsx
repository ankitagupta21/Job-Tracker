import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getAllApplications, deleteApplication } from "../api/applicationApi";
import StatusBadge from "../components/StatusBadge";

const STATUSES = [
  "ALL",
  "APPLIED",
  "ONLINE_TEST",
  "INTERVIEW",
  "OFFERED",
  "REJECTED",
];

export default function Dashboard() {
  const [applications, setApplications] = useState([]);
  const [filter, setFilter] = useState("ALL");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    fetchApplications();
  }, [filter]);

  const fetchApplications = async () => {
    setLoading(true);
    try {
      const params = filter !== "ALL" ? { status: filter } : {};
      const res = await getAllApplications(params);
      setApplications(res.data.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id, e) => {
    e.stopPropagation();
    if (!window.confirm("Delete this application?")) return;
    await deleteApplication(id);
    fetchApplications();
  };

  const filtered = applications.filter((app) =>
    app.companyName.toLowerCase().includes(search.toLowerCase()),
  );

  // Stats
  const stats = {
    total: applications.length,
    interview: applications.filter((a) => a.status === "INTERVIEW").length,
    offered: applications.filter((a) => a.status === "OFFERED").length,
    rejected: applications.filter((a) => a.status === "REJECTED").length,
  };

  return (
    <div className="max-w-6xl mx-auto px-6 py-8">
      {/* Stats */}
      <div className="grid grid-cols-4 gap-4 mb-8">
        {[
          {
            label: "Total Applied",
            value: stats.total,
            color: "text-indigo-600",
          },
          {
            label: "Interviews",
            value: stats.interview,
            color: "text-purple-600",
          },
          { label: "Offers", value: stats.offered, color: "text-green-600" },
          { label: "Rejected", value: stats.rejected, color: "text-red-500" },
        ].map((s) => (
          <div
            key={s.label}
            className="bg-white rounded-xl border border-gray-200 p-5"
          >
            <p className="text-sm text-gray-500">{s.label}</p>
            <p className={`text-3xl font-bold mt-1 ${s.color}`}>{s.value}</p>
          </div>
        ))}
      </div>

      {/* Filters + Search */}
      <div className="flex items-center justify-between mb-4 gap-4">
        <div className="flex gap-2 flex-wrap">
          {STATUSES.map((s) => (
            <button
              key={s}
              onClick={() => setFilter(s)}
              className={`px-3 py-1 rounded-full text-xs font-medium border transition ${
                filter === s
                  ? "bg-indigo-600 text-white border-indigo-600"
                  : "bg-white text-gray-600 border-gray-300 hover:border-indigo-400"
              }`}
            >
              {s === "ALL" ? "All" : s.replace("_", " ")}
            </button>
          ))}
        </div>
        <input
          type="text"
          placeholder="Search company..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="border border-gray-300 rounded-lg px-3 py-2 text-sm w-48 focus:outline-none focus:ring-2 focus:ring-indigo-300"
        />
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-gray-400">Loading...</div>
        ) : filtered.length === 0 ? (
          <div className="p-8 text-center text-gray-400">
            No applications found.
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                {[
                  "Company",
                  "Role",
                  "Status",
                  "Source",
                  "Applied Date",
                  "Actions",
                ].map((h) => (
                  <th
                    key={h}
                    className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide"
                  >
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {filtered.map((app) => (
                <tr
                  key={app.id}
                  onClick={() => navigate(`/application/${app.id}`)}
                  className="border-b border-gray-100 hover:bg-indigo-50 cursor-pointer transition"
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
                      onClick={(e) => handleDelete(app.id, e)}
                      className="text-red-400 hover:text-red-600 text-xs font-medium"
                    >
                      Delete
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
