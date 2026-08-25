import { expect, it } from "bun:test";
import { existsSync, readdirSync } from "node:fs";
import path from "node:path";
import { decodeCard } from "../src/card";

const sampleDir = path.join(import.meta.dir, "../../../sample");
const samples = existsSync(sampleDir)
	? readdirSync(sampleDir).filter((name) => /\.(bin|mfd)$/.test(name))
	: [];
const read = async (name: string) =>
	decodeCard(await Bun.file(path.join(sampleDir, name)).bytes());

it.skipIf(samples.length === 0)("decodes every sample dump", async () => {
	for (const name of samples) {
		const card = await read(name);
		expect(card.id).toMatch(/^B[EP]\d{6}$/);
		if (card.type === "AvanzaPersonalUnlimited") {
			expect(card.balance).toBe(0);
			expect(card.products).toHaveLength(2);
		}
	}
});

it.skipIf(samples.length === 0)(
	"replays the balance across dumps",
	async () => {
		const before = await read("before-buses-2026-04-04.bin");
		const topUp = await read("after-topup-2026-04-04.bin");
		const bus = await read("after-bus-2026-04-04.bin");
		const topUpRecord = topUp.transactions.at(-1)!;
		const busRecord = bus.transactions.at(-1)!;
		expect(topUpRecord.kind).toBe("topUp");
		expect(topUp.balance - before.balance).toBe(topUpRecord.amount);
		expect(busRecord.kind).toBe("journey");
		expect(topUp.balance - bus.balance).toBe(busRecord.amount);
	},
);
