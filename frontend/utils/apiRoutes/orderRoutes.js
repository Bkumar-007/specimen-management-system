/** Order REST paths (backend API only). */
export const List = "/api/orders";
export const Get = (id) => `/api/orders/${id}`;
export const Timeline = (id) => `/api/orders/${id}/timeline`;
export const Specimens = (id) => `/api/orders/${id}/specimens`;
