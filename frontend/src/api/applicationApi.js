import axios from "axios";

const BASE_URL = "http://localhost:8080/api";

const api = axios.create({
  baseURL: BASE_URL,
  headers: { "Content-Type": "application/json" },
});

// Applications
export const getAllApplications = (params) =>
  api.get("/applications", { params });

export const getApplicationById = (id) => api.get(`/applications/${id}`);

export const createApplication = (data) => api.post("/applications", data);

export const updateApplication = (id, data) =>
  api.put(`/applications/${id}`, data);

export const deleteApplication = (id) => api.delete(`/applications/${id}`);

export const getApplicationHistory = (id) =>
  api.get(`/applications/${id}/history`);
