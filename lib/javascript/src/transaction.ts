import {
	assertInRange,
	assertLength,
	BLOCK_SIZE,
	readUint16,
	writeUint16,
} from "./bytes";
import { decodeDate, encodeDate, type Date16Bit } from "./date";
import { decodeStop, encodeStop, type Stop } from "./stop";
import { decodeTime, encodeTime, type Time } from "./time";
import { cardTypeByte, cardTypeFromByte, type CardTypeName } from "./type";

export type Direction = 1 | 2;

const TOP_UP_KIND = 8;

type TransactionBase = {
	cardType: CardTypeName;
	networkFlag: number;
	amount: number;
	consecutivePayments: number;
	stop: Stop;
	route: number;
	runCounter: number;
	createdAt: Date16Bit & Time;
	sequence: number;
};

export type Journey = TransactionBase & {
	kind: "journey";
	direction: Direction;
};
export type TopUp = TransactionBase & { kind: "topUp" };
export type Transaction = Journey | TopUp;

export const isFreeTransfer = (transaction: Transaction) =>
	transaction.kind === "journey" && transaction.amount === 0;

export function decodeTransaction(block: Uint8Array): Transaction {
	assertLength(block, BLOCK_SIZE, "transaction block");
	const route = block[7]!;
	const base: TransactionBase = {
		cardType: cardTypeFromByte(block[0]!),
		networkFlag: block[1]!,
		amount: readUint16(block, 2),
		consecutivePayments: block[4]!,
		stop: decodeStop(block.subarray(5, 7), route),
		route,
		runCounter: block[9]!,
		createdAt: {
			...decodeDate(block.subarray(10, 12)),
			...decodeTime(block.subarray(12, 15)),
		},
		sequence: block[15]!,
	};
	const kind = block[8]!;
	if (kind === TOP_UP_KIND) return { ...base, kind: "topUp" };
	if (kind === 1 || kind === 2)
		return { ...base, kind: "journey", direction: kind };
	throw new Error(`transaction kind byte must be 1, 2 or 8, got ${kind}`);
}

export function encodeTransaction(transaction: Transaction): Uint8Array {
	assertInRange("amount", transaction.amount, 0, 0xffff);
	assertInRange(
		"consecutive payments",
		transaction.consecutivePayments,
		0,
		0xff,
	);
	assertInRange("route", transaction.route, 0, 0xff);
	assertInRange("run counter", transaction.runCounter, 0, 0xff);
	assertInRange("sequence", transaction.sequence, 0, 0xff);
	const block = new Uint8Array(BLOCK_SIZE);
	block[0] = cardTypeByte(transaction.cardType);
	block[1] = transaction.networkFlag;
	writeUint16(block, 2, transaction.amount);
	block[4] = transaction.consecutivePayments;
	block.set(encodeStop(transaction.stop), 5);
	block[7] = transaction.route;
	block[8] = transaction.kind === "topUp" ? TOP_UP_KIND : transaction.direction;
	block[9] = transaction.runCounter;
	block.set(encodeDate(transaction.createdAt), 10);
	block.set(encodeTime(transaction.createdAt), 12);
	block[15] = transaction.sequence;
	return block;
}
