export const TRAM_ROUTE = 210;

export function routeName(route: number): string {
	if (route === TRAM_ROUTE) return "L1";
	if (route >= 11 && route <= 14) return `Ci${route - 10}`;
	if (route >= 111 && route <= 117) return `N${route - 110}`;
	return String(route);
}
