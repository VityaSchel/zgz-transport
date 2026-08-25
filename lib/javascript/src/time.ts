import { assertInRange, assertLength } from "./bytes";

export type Time = {
	hour: number;
	minute: number;
	second: number;
};

export function decodeTime(bytes: Uint8Array): Time {
	assertLength(bytes, 3, "time");
	const time = { hour: bytes[0]!, minute: bytes[1]!, second: bytes[2]! };
	assertInRange("hour", time.hour, 0, 23);
	assertInRange("minute", time.minute, 0, 59);
	assertInRange("second", time.second, 0, 59);
	return time;
}

export function encodeTime({ hour, minute, second }: Time): Uint8Array {
	assertInRange("hour", hour, 0, 23);
	assertInRange("minute", minute, 0, 59);
	assertInRange("second", second, 0, 59);
	return Uint8Array.from([hour, minute, second]);
}
