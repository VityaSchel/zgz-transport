import { assertInRange, assertLength } from "./bytes.ts";

/** A time of day. */
export type Time = {
	/** `0` to `23`. */
	hour: number;
	/** `0` to `59`. */
	minute: number;
	/** `0` to `59`. */
	second: number;
};

/**
 * Decodes a time stored as three plain binary bytes.
 * @param bytes Hour, minute and second.
 */
export function decodeTime(bytes: Uint8Array): Time {
	assertLength(bytes, 3, "time");
	const time = { hour: bytes[0]!, minute: bytes[1]!, second: bytes[2]! };
	assertInRange("hour", time.hour, 0, 23);
	assertInRange("minute", time.minute, 0, 59);
	assertInRange("second", time.second, 0, 59);
	return time;
}

/** Encodes a time into three plain binary bytes. */
export function encodeTime({ hour, minute, second }: Time): Uint8Array {
	assertInRange("hour", hour, 0, 23);
	assertInRange("minute", minute, 0, 59);
	assertInRange("second", second, 0, 59);
	return Uint8Array.from([hour, minute, second]);
}
