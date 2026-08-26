import { expect, it } from "bun:test";
import {
	decodeTransaction,
	encodeTransaction,
	isFreeTransfer,
} from "../src/transaction.ts";
import { transactions } from "./fixtures/transactions.ts";

it("decodes transactions", () => {
	for (const { encoded, decoded } of transactions) {
		expect(decodeTransaction(Uint8Array.fromHex(encoded))).toEqual(decoded);
	}
});

it("encodes transactions", () => {
	for (const { encoded, decoded } of transactions) {
		expect(encodeTransaction(decoded)).toEqual(Uint8Array.fromHex(encoded));
	}
});

it("tells free transfers from paid journeys and top ups", () => {
	expect(transactions.map(({ decoded }) => isFreeTransfer(decoded))).toEqual([
		...Array<boolean>(9).fill(false),
		true,
	]);
});

it("rejects an unknown kind byte", () => {
	const block = Uint8Array.fromHex(transactions[0]!.encoded);
	block[8] = 3;
	expect(() => decodeTransaction(block)).toThrow();
});
