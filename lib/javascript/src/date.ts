import { assertInRange, assertLength, readUint16, writeUint16 } from "./bytes";

export type Date16Bit = {
	year: number;
	month: number;
	day: number;
};

export function decodeDate(bytes: Uint8Array): Date16Bit {
	assertLength(bytes, 2, "date");
	const packed = readUint16(bytes, 0);
	const date = {
		year: (packed >> 9) + 2000,
		month: (packed >> 5) & 0x0f,
		day: packed & 0x1f,
	};
	assertInRange("month", date.month, 1, 12);
	assertInRange("day", date.day, 1, 31);
	return date;
}

export function encodeDate({ year, month, day }: Date16Bit): Uint8Array {
	assertInRange("year", year, 2000, 2127);
	assertInRange("month", month, 1, 12);
	assertInRange("day", day, 1, 31);
	const bytes = new Uint8Array(2);
	writeUint16(bytes, 0, ((year - 2000) << 9) | (month << 5) | day);
	return bytes;
}
