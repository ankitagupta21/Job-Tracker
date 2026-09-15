import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import {
  connectGmail,
  disconnectGmail,
  getGmailStatus,
} from "../api/gmailApi";

export default function Settings() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [status, setStatus] = useState({ connected: false, email: null });
  const [loading, setLoading] = useState(true);

  const gmailResult = searchParams.get("gmail");

  useEffect(() => {
    fetchStatus();
  }, []);

  useEffect(() => {
    if (gmailResult) {
      searchParams.delete("gmail");
      setSearchParams(searchParams, { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [gmailResult]);

  const fetchStatus = async () => {
    setLoading(true);
    try {
      const res = await getGmailStatus();
      setStatus(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleDisconnect = async () => {
    if (!window.confirm("Disconnect Gmail?")) return;
    await disconnectGmail();
    fetchStatus();
  };

  return (
    <div className="max-w-2xl mx-auto px-6 py-8">
      <h1 className="text-xl font-bold text-gray-800 mb-6">Settings</h1>

      {gmailResult === "connected" && (
        <div className="mb-4 rounded-lg border border-green-200 bg-green-50 text-green-700 text-sm px-4 py-3">
          Gmail connected successfully.
        </div>
      )}
      {gmailResult === "error" && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 text-red-700 text-sm px-4 py-3">
          Something went wrong connecting Gmail. Please try again.
        </div>
      )}

      <div className="bg-white rounded-xl border border-gray-200 p-5">
        <div className="flex items-center justify-between">
          <div>
            <p className="font-medium text-gray-800">Gmail</p>
            <p className="text-sm text-gray-500 mt-1">
              {loading
                ? "Checking status..."
                : status.connected
                  ? `Connected as ${status.email || "unknown"}`
                  : "Not connected"}
            </p>
          </div>
          {!loading &&
            (status.connected ? (
              <button
                onClick={handleDisconnect}
                className="text-sm font-medium px-4 py-2 rounded-lg border border-red-300 text-red-600 hover:bg-red-50 transition"
              >
                Disconnect
              </button>
            ) : (
              <button
                onClick={connectGmail}
                className="bg-indigo-600 text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-indigo-700 transition"
              >
                Connect Gmail
              </button>
            ))}
        </div>
      </div>
    </div>
  );
}
