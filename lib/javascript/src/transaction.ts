import {
	assertInRange,
	assertLength,
	BLOCK_SIZE,
	readUint16,
	writeUint16,
} from "./bytes.ts";
import { decodeDate, encodeDate, type Date16Bit } from "./date.ts";
import { decodeStop, encodeStop, type Stop } from "./stop.ts";
import { decodeTime, encodeTime, type Time } from "./time.ts";
import { cardTypeByte, cardTypeFromByte, type CardTypeName } from "./type.ts";

/**
 * Direction of a journey along its route, the operator's GTFS `direction_id` plus one:
 * `1` is `direction_id` 0 and `2` is `direction_id` 1.
 */
export type Direction = 1 | 2;

const TOP_UP_KIND = 8;

/** Fields shared by journeys and top ups. */
export type TransactionBase = {
	/** Product of the card that made the transaction. */
	cardType: CardTypeName;
	/** Byte 1: `0` on top up cards, `1` or `2` on personal cards. */
	networkFlag: number;
	/** Money moved in {@link UNITS_PER_EURO} units, `0` when the journey cost nothing. */
	amount: number;
	/** Payments of this card at one terminal in a row, counting from `1`; `0` on top ups. */
	consecutivePayments: number;
	/** Where the transaction happened. */
	stop: Stop;
	/** Route id, see {@link routeName}; `0` on top ups made off board. */
	route: number;
	/** Byte 9, on buses most likely which trip of the vehicle's daily duty this is, counting from 1. */
	dutyTrip: number;
	/** When the transaction was written. */
	createdAt: Date16Bit & Time;
	/** `0` to `4`, selects the archive block, see {@link archiveBlock}. */
	sequence: number;
};

/** A ride, paid or a free transfer. */
export type Journey = TransactionBase & {
	kind: "journey";
	direction: Direction;
};
/** Money added to the balance. */
export type TopUp = TransactionBase & { kind: "topUp" };
/** One 16-byte record of the transaction log. */
export type Transaction = Journey | TopUp;

/** Whether a transaction is a journey that cost nothing. Every journey on a personal unlimited card is free. */
export const isFree = (transaction: Transaction): boolean =>
	transaction.kind === "journey" && transaction.amount === 0;

/**
 * Whether a journey is the free transfer a balance card earns after a paid ride, which the
 * operator grants once per ride, to another route, within 60 minutes on the urban network
 * and 75 when a CTAZ card enters Zaragoza.
 */
export const isTransfer = (transaction: Transaction): boolean =>
	isFree(transaction) &&
	transaction.consecutivePayments > 0 &&
	transaction.cardType !== "AvanzaPersonalUnlimited";

/**
 * Whether a journey is a check-out at a gated station, which carries no payment counter.
 * Rests on the single Cercanías check-out in the dumps, so treat it as provisional.
 */
export const isCheckOut = (transaction: Transaction): boolean =>
	isFree(transaction) && transaction.consecutivePayments === 0;

/**
 * Decodes a transaction record.
 * @param block The 16-byte block.
 * @throws When byte 8 is not a direction or a top up.
 */
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
		dutyTrip: block[9]!,
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

/** Encodes a transaction into a 16-byte record. */
export function encodeTransaction(transaction: Transaction): Uint8Array {
	assertInRange("amount", transaction.amount, 0, 0xffff);
	assertInRange(
		"consecutive payments",
		transaction.consecutivePayments,
		0,
		0xff,
	);
	assertInRange("route", transaction.route, 0, 0xff);
	assertInRange("duty trip", transaction.dutyTrip, 0, 0xff);
	assertInRange("sequence", transaction.sequence, 0, 0xff);
	const block = new Uint8Array(BLOCK_SIZE);
	block[0] = cardTypeByte(transaction.cardType);
	block[1] = transaction.networkFlag;
	writeUint16(block, 2, transaction.amount);
	block[4] = transaction.consecutivePayments;
	block.set(encodeStop(transaction.stop), 5);
	block[7] = transaction.route;
	block[8] = transaction.kind === "topUp" ? TOP_UP_KIND : transaction.direction;
	block[9] = transaction.dutyTrip;
	block.set(encodeDate(transaction.createdAt), 10);
	block.set(encodeTime(transaction.createdAt), 12);
	block[15] = transaction.sequence;
	return block;
}
