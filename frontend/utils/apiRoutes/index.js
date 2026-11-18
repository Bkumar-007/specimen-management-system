import * as authRoutes from "./authRoutes";
import * as orderRoutes from "./orderRoutes";
import * as referenceRoutes from "./referenceRoutes";
import * as specimenRoutes from "./specimenRoutes";

export const routesMapping = {
  auth: authRoutes,
  order: orderRoutes,
  specimen: specimenRoutes,
  reference: referenceRoutes,
};
