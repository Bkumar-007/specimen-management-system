/** Specimen REST paths (backend API only). */
export const List = "/api/specimens";
export const Get = (id) => `/api/specimens/${id}`;
export const Timeline = (id) => `/api/specimens/${id}/timeline`;
export const Status = (id) => `/api/specimens/${id}/status`;
export const Results = (id) => `/api/specimens/${id}/results`;
export const Stats = "/api/stats";
