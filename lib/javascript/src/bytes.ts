export const BLOCK_SIZE = 16;

export function assertLength(bytes: Uint8Array, length: number, name: string) {
	if (bytes.length !== length) {
		throw new Error(`${name} must be ${length} bytes, got ${bytes.length}`);
	}
}

export function assertInRange(
	name: string,
	value: number,
	min: number,
	max: number,
) {
	if (!Number.isInteger(value) || value < min || value > max) {
		throw new Error(
			`${name} must be an integer in ${min}..${max}, got ${value}`,
		);
	}
}

export const isZero = (bytes: Uint8Array) => bytes.every((byte) => byte === 0);

export const xor = (bytes: Uint8Array) =>
	bytes.reduce((acc, byte) => acc ^ byte, 0);

export function assertChecksum(block: Uint8Array) {
	const expected = xor(block.subarray(0, 15));
	if (block[15] !== expected) {
		throw new Error(`checksum ${block[15]} does not match ${expected}`);
	}
}

export function withChecksum(block: Uint8Array) {
	block[15] = xor(block.subarray(0, 15));
	return block;
}

export const readUint16 = (bytes: Uint8Array, offset: number) =>
	(bytes[offset]! << 8) | bytes[offset + 1]!;

export function writeUint16(bytes: Uint8Array, offset: number, value: number) {
	bytes[offset] = value >> 8;
	bytes[offset + 1] = value & 0xff;
}

export const readUint32LE = (bytes: Uint8Array, offset: number) =>
	(bytes[offset]! |
		(bytes[offset + 1]! << 8) |
		(bytes[offset + 2]! << 16) |
		(bytes[offset + 3]! << 24)) >>>
	0;

export function writeUint32LE(
	bytes: Uint8Array,
	offset: number,
	value: number,
) {
	for (let i = 0; i < 4; i++) bytes[offset + i] = (value >>> (i * 8)) & 0xff;
}
