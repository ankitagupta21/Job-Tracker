import axios from "axios";

const BASE_URL = "http://localhost:8080";

const api = axios.create({ baseURL: BASE_URL });

export const getGmailStatus = () => api.get("/auth/gmail/status");

export const disconnectGmail = () => api.post("/auth/gmail/disconnect");

export const connectGmail = () => {
  window.location.href = `${BASE_URL}/auth/gmail`;
};
