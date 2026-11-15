"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import styles from "./dashboard.module.css";

export default function DashboardPage() {
  const router = useRouter();
  const [user, setUser] = useState(null);

  useEffect(() => {
    const token = localStorage.getItem("sms_token");
    const raw = localStorage.getItem("sms_user");
    if (!token || !raw) {
      router.replace("/login");
      return;
    }
    setUser(JSON.parse(raw));
  }, [router]);

  function logout() {
    localStorage.removeItem("sms_token");
    localStorage.removeItem("sms_user");
    router.replace("/login");
  }

  if (!user) {
    return <main className={styles.shell}><p>Loading…</p></main>;
  }

  return (
    <main className={styles.shell}>
      <header className={styles.header}>
        <div>
          <p className={styles.brand}>Specimen Management</p>
          <h1 className={styles.title}>Dashboard</h1>
        </div>
        <button className={styles.logout} type="button" onClick={logout}>
          Sign out
        </button>
      </header>
      <section className={styles.panel}>
        <p>
          Signed in as <strong>{user.username}</strong> ({user.role}).
        </p>
        <p className={styles.note}>
          Phase 0 bootstrap is ready. Orders, specimens, and results come in later phases.
        </p>
      </section>
    </main>
  );
}
