import { expect, it } from "bun:test";
import { encodeBalance } from "../src/balance.ts";
import { withChecksum } from "../src/bytes.ts";
import { decodeCard } from "../src/card.ts";
import { encodeId } from "../src/id.ts";
import {
	decodeJourneySummary,
	encodeJourneySummary,
	PERSONAL_JOURNEY_SUMMARY,
} from "../src/journey-summary.ts";
import { encodeTransaction } from "../src/transaction.ts";
import { encodeCardType } from "../src/type.ts";
import { journeySummaries } from "./fixtures/journey-summaries.ts";
import { personalJourney, transactions } from "./fixtures/transactions.ts";

const AVANZA_BLOCK_0 = "1D68C3A91F880400C8000020000000AB";
const LAZO_BLOCK_0 = "0468C3A9BF12341802008100000023AA";
const LAZO_BLOCK_0_SHORT_UID = "0468C3A906180200800000000000AA23";
const METADATA = "1101342F00210000001E002100000015";
const SUBSCRIPTION = "342F344E0000010203043441081E0006";
const PAID_RIDE = "0200022601817E1F0211348410163003";
const ARCHIVE_BLOCKS = [28, 29, 30, 32, 33];
const record = (index: number) => transactions[index]!.encoded;

function dump(blocks: Record<number, string | Uint8Array>): Uint8Array {
	const dump = new Uint8Array(36 * 16);
	for (const [index, block] of Object.entries(blocks)) {
		dump.set(
			typeof block === "string" ? Uint8Array.fromHex(block) : block,
			Number(index) * 16,
		);
	}
	return dump;
}

function topUpCard(balance: number, live: string, archive: string[]) {
	const blocks: Record<number, string | Uint8Array> = {
		0: AVANZA_BLOCK_0,
		1: encodeCardType("AvanzaTopUp"),
		2: encodeId("BE123456"),
		5: live,
		8: encodeBalance(balance),
		9: encodeBalance(balance),
		10: journeySummaries[2]!.encoded,
	};
	archive.forEach((block, slot) => {
		blocks[ARCHIVE_BLOCKS[slot]!] = block;
	});
	return dump(blocks);
}

function undecodableSummary() {
	const block = encodeJourneySummary(journeySummaries[2]!.decoded);
	block[10] = 0x3d;
	return withChecksum(block);
}

it("decodes a top up card", () => {
	const card = decodeCard(topUpCard(4450, record(7), [4, 5, 6].map(record)));
	expect(card.chip).toBe("1K");
	expect(card.uid).toBe("1D68C3A9");
	expect(card.type).toBe("AvanzaTopUp");
	expect(card.id).toBe("BE123456");
	expect(card.balance).toBe(4450);
	expect(card.transactions).toEqual(
		[4, 5, 6, 7].map((index) => transactions[index]!.decoded),
	);
	expect(card.journeySummary).toEqual({
		state: "read",
		summary: journeySummaries[2]!.decoded,
	});
	expect(card.products).toEqual([]);
});

it("replays the balance across dumps", () => {
	const before = decodeCard(topUpCard(1000, record(7), [4, 5, 6].map(record)));
	const topUp = decodeCard(topUpCard(6000, record(8), [5, 6, 7].map(record)));
	const bus = decodeCard(topUpCard(5450, PAID_RIDE, [6, 7, 8].map(record)));
	const topUpRecord = topUp.transactions.at(-1)!;
	const busRecord = bus.transactions.at(-1)!;
	expect(topUpRecord.kind).toBe("topUp");
	expect(topUp.balance - before.balance).toBe(topUpRecord.amount);
	expect(busRecord.kind).toBe("journey");
	expect(topUp.balance - bus.balance).toBe(busRecord.amount);
});

it("decodes personal cards of both types", () => {
	const balance = encodeBalance(0);
	for (const type of ["AvanzaPersonal", "AvanzaPersonalAbono"] as const) {
		const card = decodeCard(
			dump({
				0: AVANZA_BLOCK_0,
				1: encodeCardType(type),
				2: encodeId("BP123456"),
				5: encodeTransaction(personalJourney),
				8: balance,
				9: balance,
				10: PERSONAL_JOURNEY_SUMMARY,
				12: METADATA,
				13: SUBSCRIPTION,
				16: METADATA,
				17: SUBSCRIPTION,
			}),
		);
		expect(card.type).toBe(type);
		expect(card.balance).toBe(0);
		expect(card.transactions).toEqual([personalJourney]);
		expect(card.journeySummary).toBeUndefined();
		expect(card.products).toHaveLength(2);
		expect(card.products[0]!.metadata.validityDays).toBe(30);
	}
});

it("decodes a Lazo card and reads the SAK at the 7-byte offset first", () => {
	const balance = encodeBalance(600);
	const sevenByte = [
		LAZO_BLOCK_0,
		"0468C3A9BF88341802008100000023AA",
		"0468C3A9BF18341802008100000023AA",
	];
	for (const block0 of sevenByte) {
		const card = decodeCard(
			dump({
				0: block0,
				1: encodeCardType("LazoTopUp371F"),
				2: encodeId("CT123456"),
				8: balance,
				9: balance,
			}),
		);
		expect(card.chip).toBe("4K");
		expect(card.uid).toBe(block0.slice(0, 14));
		expect(card.transactions).toEqual([]);
		expect(card.journeySummary).toBeUndefined();
	}
});

it("decodes a Lazo card with a 4-byte UID on a 4K chip", () => {
	const balance = encodeBalance(10890);
	const card = decodeCard(
		dump({
			0: LAZO_BLOCK_0_SHORT_UID,
			1: encodeCardType("LazoTopUp375F"),
			2: encodeId("CT123456"),
			5: record(7),
			8: balance,
			9: balance,
			10: undecodableSummary(),
		}),
	);
	expect(card.chip).toBe("4K");
	expect(card.uid).toBe("0468C3A9");
	expect(card.type).toBe("LazoTopUp375F");
	expect(card.balance).toBe(10890);
	expect(card.transactions).toEqual([transactions[7]!.decoded]);
	expect(card.journeySummary?.state).toBe("unknown");
});

it("keeps the rest of the card when block 10 does not decode", () => {
	const balance = encodeBalance(10890);
	const block10 = undecodableSummary();
	const card = decodeCard(
		dump({
			0: LAZO_BLOCK_0,
			1: encodeCardType("LazoTopUp375F"),
			2: encodeId("CT123456"),
			5: record(7),
			8: balance,
			9: balance,
			10: block10,
			28: record(4),
			29: record(5),
			30: record(6),
		}),
	);
	expect(card.balance).toBe(10890);
	expect(card.transactions).toEqual(
		[4, 5, 6, 7].map((index) => transactions[index]!.decoded),
	);
	expect(card.journeySummary?.state).toBe("unknown");
	if (card.journeySummary?.state !== "unknown") throw new Error("read");
	expect(card.journeySummary.error.message).toBe(
		"direction must be 1 or 2, got 61",
	);
	expect(() => decodeJourneySummary(block10)).toThrow(
		"direction must be 1 or 2",
	);
});

it("rejects malformed dumps", () => {
	const valid = topUpCard(4450, record(7), []);
	expect(() => decodeCard(valid.subarray(0, 33 * 16))).toThrow();
	expect(() => decodeCard(valid.subarray(0, 35 * 16 + 1))).toThrow();
	expect(decodeCard(valid.subarray(0, 34 * 16)).balance).toBe(4450);
	const unknownSak = valid.slice();
	unknownSak[5] = 0;
	expect(() => decodeCard(unknownSak)).toThrow("block 0 matches no known chip");
	const brokenBcc = valid.slice();
	brokenBcc[4] = brokenBcc[4]! ^ 1;
	expect(() => decodeCard(brokenBcc)).toThrow("BCC");
	const differing = valid.slice();
	differing[9 * 16] = differing[9 * 16]! ^ 1;
	expect(() => decodeCard(differing)).toThrow("differ");
});
