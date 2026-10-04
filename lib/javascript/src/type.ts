import {
	assertChecksum,
	assertLength,
	BLOCK_SIZE,
	isZero,
	withChecksum,
} from "./bytes.ts";

/** Known card types and their value in bytes 0 to 2 of block 1. */
export const CardType = {
	AvanzaTopUp: 0x02699f,
	AvanzaPersonal: 0x0a9775,
	/** Printed "Abono de transporte"; what separates it from `AvanzaPersonal` is unknown. */
	AvanzaPersonalAbono: 0x0a98da,
	LazoTopUp371F: 0x0d371f,
	/**
	 * What separates it from `LazoTopUp371F` is unknown: a card carrying it holds the same keys,
	 * the same balance and transaction ring, and empty product sectors.
	 */
	LazoTopUp375F: 0x0d375f,
} as const;

/** Name of a known card type. */
export type CardTypeName = keyof typeof CardType;

const names = Object.keys(CardType) as CardTypeName[];

const PERSONAL: CardTypeName[] = ["AvanzaPersonal", "AvanzaPersonalAbono"];

/** Whether a card type carries subscription products instead of a balance. */
export const isPersonalCardType = (type: CardTypeName): boolean =>
	PERSONAL.includes(type);

/**
 * Decodes the card type from block 1.
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
 * Encodes a card type into block 1.
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
