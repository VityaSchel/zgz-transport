/** Size of a MIFARE Classic block in bytes. */
export const BLOCK_SIZE = 16;

/** Throws unless `bytes` is exactly `length` bytes long. */
export function assertLength(bytes: Uint8Array, length: number, name: string) {
	if (bytes.length !== length) {
		throw new Error(`${name} must be ${length} bytes, got ${bytes.length}`);
	}
}

/** Throws unless `value` is an integer within `min..max`. */
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

/** Whether every byte is zero. */
export const isZero = (bytes: Uint8Array) => bytes.every((byte) => byte === 0);

/** XOR of all bytes. */
export const xor = (bytes: Uint8Array) =>
	bytes.reduce((acc, byte) => acc ^ byte, 0);

/** Throws unless byte 15 is the XOR of bytes 0 to 14. */
export function assertChecksum(block: Uint8Array) {
	const expected = xor(block.subarray(0, 15));
	if (block[15] !== expected) {
		throw new Error(`checksum ${block[15]} does not match ${expected}`);
	}
}

/** Writes the XOR of bytes 0 to 14 into byte 15 and returns the block. */
export function withChecksum(block: Uint8Array) {
	block[15] = xor(block.subarray(0, 15));
	return block;
}

/** Reads a big-endian 16-bit integer. */
export const readUint16 = (bytes: Uint8Array, offset: number) =>
	(bytes[offset]! << 8) | bytes[offset + 1]!;

/** Writes a big-endian 16-bit integer. */
export function writeUint16(bytes: Uint8Array, offset: number, value: number) {
	bytes[offset] = value >> 8;
	bytes[offset + 1] = value & 0xff;
}

/** Reads a little-endian unsigned 32-bit integer. */
export const readUint32LE = (bytes: Uint8Array, offset: number) =>
	(bytes[offset]! |
		(bytes[offset + 1]! << 8) |
		(bytes[offset + 2]! << 16) |
		(bytes[offset + 3]! << 24)) >>>
	0;

/** Writes a little-endian 32-bit integer. */
export function writeUint32LE(
	bytes: Uint8Array,
	offset: number,
	value: number,
) {
	for (let i = 0; i < 4; i++) bytes[offset + i] = (value >>> (i * 8)) & 0xff;
}
