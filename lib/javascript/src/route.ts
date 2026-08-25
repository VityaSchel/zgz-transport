/** Route id of the tram. */
export const TRAM_ROUTE = 210;

/**
 * Public name of a route id as used by the operator: buses keep their number,
 * `11` to `14` are `Ci1` to `Ci4`, `111` to `117` are `N1` to `N7` and the tram is `L1`.
 */
export function routeName(route: number): string {
	if (route === TRAM_ROUTE) return "L1";
	if (route >= 11 && route <= 14) return `Ci${route - 10}`;
	if (route >= 111 && route <= 117) return `N${route - 110}`;
	return String(route);
}
