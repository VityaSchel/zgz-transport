import { expect, it } from "bun:test";
import {
	decodeTransaction,
	encodeTransaction,
	isCheckOut,
	isFree,
	isTransfer,
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

it("tells free journeys from paid ones and top ups", () => {
	expect(transactions.map(({ decoded }) => isFree(decoded))).toEqual([
		...Array<boolean>(9).fill(false),
		true,
	]);
});

it("tells a transfer from a check-out", () => {
	const transfer = decodeTransaction(
		Uint8Array.fromHex("0D0000000105DCD202013518162C2000"),
	);
	const checkOut = decodeTransaction(
		Uint8Array.fromHex("0D000000002303A9010234FE09331E01"),
	);
	const paid = decodeTransaction(
		Uint8Array.fromHex("0D0002260181801F011C3518160B2C04"),
	);
	expect([
		isFree(transfer),
		isTransfer(transfer),
		isCheckOut(transfer),
	]).toEqual([true, true, false]);
	expect([
		isFree(checkOut),
		isTransfer(checkOut),
		isCheckOut(checkOut),
	]).toEqual([true, false, true]);
	expect([isFree(paid), isTransfer(paid), isCheckOut(paid)]).toEqual([
		false,
		false,
		false,
	]);
});

it("does not call an unlimited pass journey a transfer", () => {
	const pass = decodeTransaction(
		Uint8Array.fromHex("0A0200000180010C021134590F1A0302"),
	);
	expect([isFree(pass), isTransfer(pass)]).toEqual([true, false]);
});

it("rejects an unknown kind byte", () => {
	const block = Uint8Array.fromHex(transactions[0]!.encoded);
	block[8] = 3;
	expect(() => decodeTransaction(block)).toThrow();
});
