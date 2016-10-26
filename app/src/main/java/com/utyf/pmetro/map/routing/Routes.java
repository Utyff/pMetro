package com.utyf.pmetro.map.routing;

import android.util.Log;

/**
 * Contains collection of computed routes. Routes must have at least one route.
 */
class Routes {
    private RouteInfo bestRoute;
    private RouteInfo[] alternativeRoutes;
    private int alternativeRouteIndex = -1;

    public Routes(RouteInfo bestRoute, RouteInfo[] alternativeRoutes) {
        this.bestRoute = bestRoute;
        this.alternativeRoutes = alternativeRoutes;
        this.alternativeRouteIndex = -1;
    }

    public void selectBestRoute() {
        alternativeRouteIndex = -1;
    }

    public void selectAlternativeRoute(int index) {
        if (alternativeRoutes != null && index >= 0 && index < alternativeRoutes.length) {
            alternativeRouteIndex = index;
        }
        else {
            Log.e("Routes", "Trying to select invalid alternative route");
        }
    }

    public RouteInfo getCurrentRoute() {
        if (alternativeRouteIndex == -1) {
            return bestRoute;
        }
        else {
            return alternativeRoutes[alternativeRouteIndex];
        }
    }

    public RouteInfo[] getAllRoutes() {
        // Append alternativeRoutes to bestRoute
        RouteInfo[] routes = new RouteInfo[1 + alternativeRoutes.length];
        routes[0] = bestRoute;
        System.arraycopy(alternativeRoutes, 0, routes, 1, alternativeRoutes.length);
        return routes;
    }
}
