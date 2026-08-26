import { expect, it } from "bun:test";
import { encodeBalance } from "../src/balance";
import { decodeCard } from "../src/card";
import { encodeId } from "../src/id";
import { PERSONAL_JOURNEY_SUMMARY } from "../src/journey-summary";
import { encodeCardType } from "../src/type";
import { journeySummaries } from "./fixtures/journey-summaries";
import { transactions } from "./fixtures/transactions";

const AVANZA_BLOCK_0 = "1D68C3A9BF880400C8000020000000AB";
const LAZO_BLOCK_0 = "0468C3A9BF12341802008100000023AA";
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
	expect(card.journeySummary).toEqual(journeySummaries[2]!.decoded);
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

it("decodes a personal card", () => {
	const balance = encodeBalance(0);
	const card = decodeCard(
		dump({
			0: AVANZA_BLOCK_0,
			1: encodeCardType("AvanzaPersonalUnlimited"),
			2: encodeId("BP123456"),
			8: balance,
			9: balance,
			10: PERSONAL_JOURNEY_SUMMARY,
			16: METADATA,
			17: SUBSCRIPTION,
			18: SUBSCRIPTION,
		}),
	);
	expect(card.type).toBe("AvanzaPersonalUnlimited");
	expect(card.balance).toBe(0);
	expect(card.journeySummary).toBeUndefined();
	expect(card.products).toHaveLength(1);
	expect(card.products[0]!.metadata.validityDays).toBe(30);
});

it("decodes a Lazo card and reads the SAK at the 4K offset first", () => {
	const balance = encodeBalance(600);
	for (const block0 of [LAZO_BLOCK_0, "0468C3A9BF88341802008100000023AA"]) {
		const card = decodeCard(
			dump({
				0: block0,
				1: encodeCardType("LazoTopUp"),
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

it("rejects malformed dumps", () => {
	const valid = topUpCard(4450, record(7), []);
	expect(() => decodeCard(valid.subarray(0, 33 * 16))).toThrow();
	expect(() => decodeCard(valid.subarray(0, 35 * 16 + 1))).toThrow();
	expect(decodeCard(valid.subarray(0, 34 * 16)).balance).toBe(4450);
	const unknownSak = valid.slice();
	unknownSak[5] = 0;
	expect(() => decodeCard(unknownSak)).toThrow("SAK");
	const differing = valid.slice();
	differing[9 * 16] = differing[9 * 16]! ^ 1;
	expect(() => decodeCard(differing)).toThrow("differ");
});
