import { assertInRange, assertLength, readUint16, writeUint16 } from "./bytes";
import { TRAM_ROUTE } from "./route";

export type Stop =
	| { network: "urban"; id: number }
	| { network: "tram"; stop: number }
	| { network: "other"; id: number };

const URBAN_FLAG = 0x8000;

export function decodeStop(bytes: Uint8Array, route: number): Stop {
	assertLength(bytes, 2, "stop");
	const value = readUint16(bytes, 0);
	if (value & URBAN_FLAG) return { network: "urban", id: value & ~URBAN_FLAG };
	if (route === TRAM_ROUTE) return { network: "tram", stop: value };
	return { network: "other", id: value };
}

export function encodeStop(stop: Stop): Uint8Array {
	const bytes = new Uint8Array(2);
	switch (stop.network) {
		case "urban":
			assertInRange("urban stop id", stop.id, 0, URBAN_FLAG - 1);
			writeUint16(bytes, 0, URBAN_FLAG | stop.id);
			break;
		case "tram":
			assertInRange("tram stop", stop.stop, 0, URBAN_FLAG - 1);
			writeUint16(bytes, 0, stop.stop);
			break;
		case "other":
			assertInRange("stop id", stop.id, 0, URBAN_FLAG - 1);
			writeUint16(bytes, 0, stop.id);
	}
	return bytes;
}
