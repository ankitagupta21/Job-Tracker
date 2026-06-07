import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  getApplicationById,
  updateApplication,
  getApplicationHistory,
} from "../api/applicationApi";
import StatusBadge from "../components/StatusBadge";

const STATUSES = ["APPLIED", "ONLINE_TEST", "INTERVIEW", "OFFERED", "REJECTED"];

export default function ApplicationDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [app, setApp] = useState(null);
  const [history, setHistory] = useState([]);
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();
  }, [id]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [appRes, histRes] = await Promise.all([
        getApplicationById(id),
        getApplicationHistory(id),
      ]);
      setApp(appRes.data.data);
      setForm(appRes.data.data);
      setHistory(histRes.data.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    await updateApplication(id, {
      companyName: form.companyName,
      role: form.role,
      jobUrl: form.jobUrl,
      notes: form.notes,
      status: form.status,
    });
    setEditing(false);
    fetchData();
  };

  if (loading)
    return <div className="p-8 text-center text-gray-400">Loading...</div>;
  if (!app)
    return <div className="p-8 text-center text-gray-400">Not found.</div>;

  return (
    <div className="max-w-3xl mx-auto px-6 py-8">
      <button
        onClick={() => navigate("/")}
        className="text-sm text-indigo-600 hover:underline mb-6 block"
      >
        ← Back to Dashboard
      </button>

      {/* Header */}
      <div className="bg-white border border-gray-200 rounded-xl p-6 mb-6">
        <div className="flex items-start justify-between mb-4">
          <div>
            {editing ? (
              <input
                value={form.companyName}
                onChange={(e) =>
                  setForm({ ...form, companyName: e.target.value })
                }
                className="text-2xl font-bold border-b border-indigo-300 focus:outline-none w-full mb-1"
              />
            ) : (
              <h1 className="text-2xl font-bold text-gray-800">
                {app.companyName}
              </h1>
            )}
            {editing ? (
              <input
                value={form.role}
                onChange={(e) => setForm({ ...form, role: e.target.value })}
                className="text-gray-500 border-b border-indigo-300 focus:outline-none w-full"
              />
            ) : (
              <p className="text-gray-500 mt-1">{app.role}</p>
            )}
          </div>
          <div className="flex gap-2">
            {editing ? (
              <>
                <button
                  onClick={handleSave}
                  className="bg-indigo-600 text-white px-4 py-1.5 rounded-lg text-sm hover:bg-indigo-700"
                >
                  Save
                </button>
                <button
                  onClick={() => {
                    setEditing(false);
                    setForm(app);
                  }}
                  className="border border-gray-300 text-gray-600 px-4 py-1.5 rounded-lg text-sm hover:bg-gray-50"
                >
                  Cancel
                </button>
              </>
            ) : (
              <button
                onClick={() => setEditing(true)}
                className="border border-gray-300 text-gray-600 px-4 py-1.5 rounded-lg text-sm hover:bg-gray-50"
              >
                Edit
              </button>
            )}
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <p className="text-gray-400 text-xs uppercase mb-1">Status</p>
            {editing ? (
              <select
                value={form.status}
                onChange={(e) => setForm({ ...form, status: e.target.value })}
                className="border border-gray-300 rounded-lg px-2 py-1 text-sm focus:outline-none"
              >
                {STATUSES.map((s) => (
                  <option key={s}>{s}</option>
                ))}
              </select>
            ) : (
              <StatusBadge status={app.status} />
            )}
          </div>
          <div>
            <p className="text-gray-400 text-xs uppercase mb-1">Source</p>
            <p className="text-gray-700">{app.source}</p>
          </div>
          <div>
            <p className="text-gray-400 text-xs uppercase mb-1">Applied Date</p>
            <p className="text-gray-700">{app.appliedDate || "—"}</p>
          </div>
          <div>
            <p className="text-gray-400 text-xs uppercase mb-1">Job URL</p>
            {editing ? (
              <input
                value={form.jobUrl || ""}
                onChange={(e) => setForm({ ...form, jobUrl: e.target.value })}
                className="border border-gray-300 rounded px-2 py-1 text-sm w-full focus:outline-none"
              />
            ) : app.jobUrl ? (
              <a
                href={app.jobUrl}
                target="_blank"
                rel="noreferrer"
                className="text-indigo-600 hover:underline truncate block"
              >
                {app.jobUrl}
              </a>
            ) : (
              <p className="text-gray-400">—</p>
            )}
          </div>
          <div className="col-span-2">
            <p className="text-gray-400 text-xs uppercase mb-1">Notes</p>
            {editing ? (
              <textarea
                value={form.notes || ""}
                onChange={(e) => setForm({ ...form, notes: e.target.value })}
                rows={3}
                className="border border-gray-300 rounded-lg px-2 py-1 text-sm w-full focus:outline-none"
              />
            ) : (
              <p className="text-gray-700">{app.notes || "—"}</p>
            )}
          </div>
        </div>
      </div>

      {/* Status History */}
      <div className="bg-white border border-gray-200 rounded-xl p-6">
        <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wide mb-4">
          Status History
        </h2>
        {history.length === 0 ? (
          <p className="text-gray-400 text-sm">No status changes yet.</p>
        ) : (
          <div className="space-y-3">
            {history.map((h) => (
              <div key={h.id} className="flex items-center gap-3 text-sm">
                <div className="w-2 h-2 rounded-full bg-indigo-400 flex-shrink-0" />
                <StatusBadge status={h.oldStatus} />
                <span className="text-gray-400">→</span>
                <StatusBadge status={h.newStatus} />
                <span className="text-gray-400 text-xs ml-auto">
                  {new Date(h.changedAt).toLocaleString()} · {h.changedBy}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
