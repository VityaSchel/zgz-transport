import {
	assertChecksum,
	assertInRange,
	assertLength,
	BLOCK_SIZE,
	readUint16,
	withChecksum,
	writeUint16,
} from "./bytes.ts";
import { decodeDate, encodeDate, type Date16Bit } from "./date.ts";

/** Block 12 or 16 of a personal card. */
export type SubscriptionMetadata = {
	/** Byte 0, tells the products of a card apart. */
	productId: number;
	/** Byte 1, always `1` so far. */
	unknown1: number;
	/** When the product was bought. */
	purchasedAt: Date16Bit;
	/** Bytes 4 to 7, always `00210000` so far. */
	unknown2: Uint8Array;
	/** How many days the product is valid for. */
	validityDays: number;
	/** Bytes 10 to 14, always `0021000000` so far. */
	unknown3: Uint8Array;
};

/**
 * Decodes a subscription metadata block.
 * @param block The 16-byte block.
 */
export function decodeSubscriptionMetadata(
	block: Uint8Array,
): SubscriptionMetadata {
	assertLength(block, BLOCK_SIZE, "subscription metadata block");
	assertChecksum(block);
	return {
		productId: block[0]!,
		unknown1: block[1]!,
		purchasedAt: decodeDate(block.subarray(2, 4)),
		unknown2: block.slice(4, 8),
		validityDays: readUint16(block, 8),
		unknown3: block.slice(10, 15),
	};
}

/** Encodes subscription metadata into a 16-byte block. */
export function encodeSubscriptionMetadata(
	metadata: SubscriptionMetadata,
): Uint8Array {
	assertInRange("product id", metadata.productId, 0, 0xff);
	assertInRange("unknown1", metadata.unknown1, 0, 0xff);
	assertInRange("validity days", metadata.validityDays, 0, 0xffff);
	assertLength(metadata.unknown2, 4, "unknown2");
	assertLength(metadata.unknown3, 5, "unknown3");
	const block = new Uint8Array(BLOCK_SIZE);
	block[0] = metadata.productId;
	block[1] = metadata.unknown1;
	block.set(encodeDate(metadata.purchasedAt), 2);
	block.set(metadata.unknown2, 4);
	writeUint16(block, 8, metadata.validityDays);
	block.set(metadata.unknown3, 10);
	return withChecksum(block);
}
