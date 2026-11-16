const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export function getApiBase() {
  return API_BASE;
}

export function getToken() {
  return typeof window === "undefined" ? null : localStorage.getItem("sms_token");
}

export function getUser() {
  if (typeof window === "undefined") return null;
  const raw = localStorage.getItem("sms_user");
  try {
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function clearSession() {
  localStorage.removeItem("sms_token");
  localStorage.removeItem("sms_user");
}

export async function login(username, password) {
  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || "Login failed");
  }

  return res.json();
}

export class ApiError extends Error {
  constructor(message, status, fieldErrors) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

/** Authenticated JSON request. Redirects to /login when the session is missing or expired. */
export async function api(path, { method = "GET", body, params } = {}) {
  const url = new URL(`${API_BASE}${path}`);
  if (params) {
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && String(v).trim() !== "") url.searchParams.set(k, v);
    });
  }

  let res;
  try {
    res = await fetch(url, {
      method,
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${getToken() ?? ""}`,
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError("Cannot reach the API. Is the backend running?", 0);
  }

  if (res.status === 401) {
    clearSession();
    if (typeof window !== "undefined") window.location.href = "/login";
    throw new ApiError("Session expired", 401);
  }

  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    throw new ApiError(data?.message || `Request failed (${res.status})`, res.status, data?.fieldErrors);
  }
  return data;
}
