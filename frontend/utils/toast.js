const listeners = new Set();

/** Fire a short success toast (picked up by ToastHost in AppShell). */
export function toast(message) {
  listeners.forEach((fn) => fn(message));
}

export function subscribeToast(fn) {
  listeners.add(fn);
  return () => listeners.delete(fn);
}
