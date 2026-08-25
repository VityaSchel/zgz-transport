import { expect, it } from "bun:test";
import { archiveBlock, decodeTransactionLog } from "../src/log";
import { transactions } from "./fixtures/transactions";

const record = (index: number) =>
	Uint8Array.fromHex(transactions[index]!.encoded);
const stub = Uint8Array.fromHex("00000000000000000000000000000004");

const blocks: Record<number, Uint8Array> = {
	5: record(7),
	28: record(4),
	29: record(5),
	30: record(6),
	32: new Uint8Array(16),
	33: stub,
};

it("maps sequence numbers to archive blocks", () => {
	expect([0, 1, 2, 3, 4].map(archiveBlock)).toEqual([28, 29, 30, 32, 33]);
});

it("orders the ring by time and skips empty slots", () => {
	const log = decodeTransactionLog((index) => blocks[index]!);
	expect(log).toEqual(
		[4, 5, 6, 7].map((index) => transactions[index]!.decoded),
	);
});
