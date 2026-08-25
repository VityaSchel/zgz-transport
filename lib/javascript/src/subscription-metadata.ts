import {
	assertChecksum,
	assertInRange,
	assertLength,
	BLOCK_SIZE,
	readUint16,
	withChecksum,
	writeUint16,
} from "./bytes";
import { decodeDate, encodeDate, type Date16Bit } from "./date";

export type SubscriptionMetadata = {
	productId: number;
	unknown1: number;
	purchasedAt: Date16Bit;
	unknown2: Uint8Array;
	validityDays: number;
	unknown3: Uint8Array;
};

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
