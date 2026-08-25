import {
	assertChecksum,
	assertLength,
	BLOCK_SIZE,
	isZero,
	withChecksum,
} from "./bytes";

export const CardType = {
	AvanzaTopUp: 0x02699f,
	AvanzaPersonalUnlimited: 0x0a9775,
	LazoTopUp: 0x0d371f,
} as const;

export type CardTypeName = keyof typeof CardType;

const names = Object.keys(CardType) as CardTypeName[];

export const cardTypeByte = (type: CardTypeName) => CardType[type] >> 16;

export function cardTypeFromByte(byte: number): CardTypeName {
	const name = names.find((type) => cardTypeByte(type) === byte);
	if (!name) throw new Error(`unknown card type byte ${byte}`);
	return name;
}

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

export function encodeCardType(type: CardTypeName): Uint8Array {
	const value = CardType[type];
	const block = new Uint8Array(BLOCK_SIZE);
	block[0] = value >> 16;
	block[1] = (value >> 8) & 0xff;
	block[2] = value & 0xff;
	return withChecksum(block);
}
