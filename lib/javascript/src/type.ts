import {
	assertChecksum,
	assertLength,
	BLOCK_SIZE,
	isZero,
	withChecksum,
} from "./bytes.ts";

/** Known products and their value in bytes 0 to 2 of block 1. */
export const CardType = {
	AvanzaTopUp: 0x02699f,
	AvanzaPersonalUnlimited: 0x0a9775,
	LazoTopUp: 0x0d371f,
} as const;

/** Name of a known product. */
export type CardTypeName = keyof typeof CardType;

const names = Object.keys(CardType) as CardTypeName[];

/** First byte of a product, the one transactions and the journey summary carry. */
export const cardTypeByte = (type: CardTypeName) => CardType[type] >> 16;

/**
 * Finds the product whose first byte is `byte`.
 * @throws When no product starts with that byte.
 */
export function cardTypeFromByte(byte: number): CardTypeName {
	const name = names.find((type) => cardTypeByte(type) === byte);
	if (!name) throw new Error(`unknown card type byte ${byte}`);
	return name;
}

/**
 * Decodes the product from block 1.
 * @param block The 16-byte block.
 */
export function decodeCardType(block: Uint8Array): CardTypeName {
	assertLength(block, BLOCK_SIZE, "card type block");
	assertChecksum(block);
	if (!isZero(block.subarray(3, 15))) {
		throw new Error("card type block bytes 03..14 must be zero");
	}
	const value = (block[0]! << 16) | (block[1]! << 8) | block[2]!;
	const name = names.find((type) => CardType[type] === value);
	if (!name) throw new Error(`unknown card type ${value.toString(16)}`);
	return name;
}

/**
 * Encodes a product into block 1.
 * @returns The 16-byte block with its checksum.
 */
export function encodeCardType(type: CardTypeName): Uint8Array {
	const value = CardType[type];
	const block = new Uint8Array(BLOCK_SIZE);
	block[0] = value >> 16;
	block[1] = (value >> 8) & 0xff;
	block[2] = value & 0xff;
	return withChecksum(block);
}
