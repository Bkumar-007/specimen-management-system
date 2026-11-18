import { api } from "@utils/api";
import { routesMapping } from "@utils/apiRoutes";

const routes = routesMapping.order;

export function listOrders(q = "") {
  return api(routes.List, { params: { q } });
}

export function getOrder(id) {
  return api(routes.Get(id));
}

export function getOrderHistory(id) {
  return api(routes.Timeline(id));
}

/** Detail + timeline for the history page. */
export function loadOrderHistoryPage(id) {
  return Promise.all([getOrder(id), getOrderHistory(id)]).then(([order, timeline]) => ({
    order,
    timeline,
  }));
}

export function createOrder(body) {
  return api(routes.List, { method: "POST", body });
}

export function accessionSpecimen(orderId, body) {
  return api(routes.Specimens(orderId), { method: "POST", body });
}

export function deleteOrder(id) {
  return api(routes.Get(id), { method: "DELETE" });
}
