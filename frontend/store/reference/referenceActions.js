import { api } from "@utils/api";
import { routesMapping } from "@utils/apiRoutes";

const routes = routesMapping.reference;

export function listClients() {
  return api(routes.Clients);
}

export function createClient(body) {
  return api(routes.Clients, { method: "POST", body });
}

export function listTests() {
  return api(routes.Tests);
}

export function createTest(body) {
  return api(routes.Tests, { method: "POST", body });
}

export function listAudit(params = {}) {
  return api(routes.Audit, { params });
}

export function ingestInstrument(body) {
  return api(routes.InstrumentsIngest, { method: "POST", body });
}
