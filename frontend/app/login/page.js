"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { login } from "../../lib/api";
import styles from "./login.module.css";

export default function LoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const data = await login(username, password);
      localStorage.setItem("sms_token", data.token);
      localStorage.setItem("sms_user", JSON.stringify({
        username: data.username,
        role: data.role,
      }));
      router.push("/dashboard");
    } catch {
      setError("Invalid username or password");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className={styles.shell}>
      <section className={styles.panel}>
        <p className={styles.brand}>Specimen Management</p>
        <h1 className={styles.title}>Sign in</h1>
        <p className={styles.subtitle}>Lab tech and admin access for demo workflows.</p>

        <form className={styles.form} onSubmit={handleSubmit}>
          <label className={styles.label}>
            Username
            <input
              className={styles.input}
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              required
            />
          </label>
          <label className={styles.label}>
            Password
            <div className={styles.passwordRow}>
              <input
                className={styles.input}
                type={showPassword ? "text" : "password"}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                required
              />
              <button
                className={styles.reveal}
                type="button"
                onClick={() => setShowPassword((v) => !v)}
                aria-pressed={showPassword}
                aria-label={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? "Hide" : "Show"}
              </button>
            </div>
          </label>
          {error ? <p className={styles.error}>{error}</p> : null}
          <button className={styles.button} type="submit" disabled={loading}>
            {loading ? "Signing in…" : "Sign in"}
          </button>
        </form>

        <p className={styles.hint}>
          Seed users: <code>tech / tech123</code> or <code>admin / admin123</code>
        </p>
      </section>
    </main>
  );
}
