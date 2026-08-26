import { expect, it } from "bun:test";
import {
	cardTypeByte,
	cardTypeFromByte,
	decodeCardType,
	encodeCardType,
	type CardTypeName,
} from "../src/type.ts";

const cases: { type: CardTypeName; encoded: string }[] = [
	{ type: "AvanzaTopUp", encoded: "02699F000000000000000000000000F4" },
	{
		type: "AvanzaPersonalUnlimited",
		encoded: "0A9775000000000000000000000000E8",
	},
	{ type: "LazoTopUp", encoded: "0D371F00000000000000000000000025" },
];

it("decodes card types", () => {
	for (const { type, encoded } of cases) {
		expect(decodeCardType(Uint8Array.fromHex(encoded))).toEqual(type);
	}
});

it("encodes card types", () => {
	for (const { type, encoded } of cases) {
		expect(encodeCardType(type)).toEqual(Uint8Array.fromHex(encoded));
	}
});

it("maps the first byte to the type", () => {
	for (const { type } of cases) {
		expect(cardTypeFromByte(cardTypeByte(type))).toBe(type);
	}
});
