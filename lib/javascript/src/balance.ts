import {
	assertInRange,
	assertLength,
	BLOCK_SIZE,
	readUint32LE,
	writeUint32LE,
} from "./bytes";

export const UNITS_PER_EURO = 1000;

const ADDRESS = Uint8Array.from([0x02, 0xfd, 0x02, 0xfd]);

export function encodeBalance(units: number): Uint8Array {
	assertInRange("balance", units, 0, 0x7fffffff);
	const block = new Uint8Array(BLOCK_SIZE);
	writeUint32LE(block, 0, units);
	writeUint32LE(block, 4, ~units);
	writeUint32LE(block, 8, units);
	block.set(ADDRESS, 12);
	return block;
}

export function decodeBalance(block: Uint8Array): number {
	assertLength(block, BLOCK_SIZE, "balance block");
	const units = readUint32LE(block, 0);
	if (readUint32LE(block, 4) !== ~units >>> 0) {
		throw new Error("balance complement does not match");
	}
	if (readUint32LE(block, 8) !== units) {
		throw new Error("balance copy does not match");
	}
	return units;
}
