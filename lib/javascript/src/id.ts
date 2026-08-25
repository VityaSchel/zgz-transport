import {
	assertChecksum,
	assertLength,
	BLOCK_SIZE,
	withChecksum,
} from "./bytes";

const ID_FORMAT = /^[A-Z]{2}[0-9]{6,26}$/;

export function encodeId(id: string): Uint8Array {
	if (!ID_FORMAT.test(id)) {
		throw new Error(
			`id must be two capital letters and 6 to 26 digits, got ${id}`,
		);
	}
	const block = new Uint8Array(BLOCK_SIZE);
	block[0] = id.charCodeAt(0);
	block[1] = id.charCodeAt(1);
	block.set(Uint8Array.fromHex(id.slice(2)), 2);
	return withChecksum(block);
}

export function decodeId(block: Uint8Array): string {
	assertLength(block, BLOCK_SIZE, "id block");
	assertChecksum(block);
	const prefix = String.fromCharCode(block[0]!, block[1]!);
	let digits = block.subarray(2, 15).toHex();
	while (digits.length > 6 && digits.endsWith("00"))
		digits = digits.slice(0, -2);
	return prefix + digits;
}
