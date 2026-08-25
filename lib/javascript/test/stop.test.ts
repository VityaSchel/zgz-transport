import { expect, it } from "bun:test";
import { decodeStop, encodeStop, type Stop } from "../src/stop";

const cases: { encoded: string; route: number; stop: Stop }[] = [
	{ encoded: "81DB", route: 31, stop: { network: "urban", id: 475 } },
	{ encoded: "8001", route: 35, stop: { network: "urban", id: 1 } },
	{ encoded: "05DC", route: 210, stop: { network: "tram", stop: 1500 } },
	{ encoded: "1F2C", route: 0, stop: { network: "other", id: 7980 } },
	{ encoded: "0005", route: 169, stop: { network: "other", id: 5 } },
];

it("decodes stops by network", () => {
	for (const { encoded, route, stop } of cases) {
		expect(decodeStop(Uint8Array.fromHex(encoded), route)).toEqual(stop);
	}
});

it("encodes stops", () => {
	for (const { encoded, stop } of cases) {
		expect(encodeStop(stop)).toEqual(Uint8Array.fromHex(encoded));
	}
});
