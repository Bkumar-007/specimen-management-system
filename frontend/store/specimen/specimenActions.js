import { api } from "@utils/api";
import { routesMapping } from "@utils/apiRoutes";

const routes = routesMapping.specimen;

export function listSpecimens(params = {}) {
  return api(routes.List, { params });
}

export function getSpecimen(id) {
  return api(routes.Get(id));
}

export function getSpecimenTimeline(id) {
  return api(routes.Timeline(id));
}

export function loadSpecimenPage(id) {
  return Promise.all([getSpecimen(id), getSpecimenTimeline(id)]).then(([specimen, timeline]) => ({
    specimen,
    timeline,
  }));
}

export function changeSpecimenStatus(id, body) {
  return api(routes.Status(id), { method: "PATCH", body });
}

export function addSpecimenResults(id, body) {
  return api(routes.Results(id), { method: "POST", body });
}

export function getStats() {
  return api(routes.Stats);
}
