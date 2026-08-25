import { expect, it } from "bun:test";
import { routeName } from "../src/route";

it("names routes like the operator", () => {
	expect([11, 14, 31, 111, 117, 210].map(routeName)).toEqual([
		"Ci1",
		"Ci4",
		"31",
		"N1",
		"N7",
		"L1",
	]);
});
