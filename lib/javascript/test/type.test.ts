import { expect, it } from "bun:test";
import {
	CardType,
	decodeCardType,
	encodeCardType,
	isPersonalCardType,
	type CardTypeName,
} from "../src/type.ts";

const cases: { type: CardTypeName; encoded: string; personal: boolean }[] = [
	{
		type: "AvanzaTopUp",
		encoded: "02699F000000000000000000000000F4",
		personal: false,
	},
	{
		type: "AvanzaPersonal",
		encoded: "0A9775000000000000000000000000E8",
		personal: true,
	},
	{
		type: "AvanzaPersonalAbono",
		encoded: "0A98DA00000000000000000000000048",
		personal: true,
	},
	{
		type: "LazoTopUp371F",
		encoded: "0D371F00000000000000000000000025",
		personal: false,
	},
	{
		type: "LazoTopUp375F",
		encoded: "0D375F00000000000000000000000065",
		personal: false,
	},
];

it("covers every known card type", () => {
	expect(cases.map(({ type }) => type).sort()).toEqual(
		(Object.keys(CardType) as CardTypeName[]).sort(),
	);
});

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

it("tells personal card types from balance ones", () => {
	for (const { type, personal } of cases) {
		expect(isPersonalCardType(type)).toBe(personal);
	}
});

it("rejects an unknown card type", () => {
	expect(() =>
		decodeCardType(Uint8Array.fromHex("0A98DB00000000000000000000000049")),
	).toThrow("unknown card type");
});
