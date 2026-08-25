import { decodeBalance } from "./balance";
import { BLOCK_SIZE, isZero } from "./bytes";
import { decodeId } from "./id";
import { decodeJourneySummary, type JourneySummary } from "./journey-summary";
import { decodeTransactionLog } from "./log";
import { decodeSubscription, type Subscription } from "./subscription";
import {
	decodeSubscriptionMetadata,
	type SubscriptionMetadata,
} from "./subscription-metadata";
import type { Transaction } from "./transaction";
import { decodeCardType, type CardTypeName } from "./type";

export type Chip = "1K" | "4K";

export type Product = {
	metadata: SubscriptionMetadata;
	subscription: Subscription;
};

export type Card = {
	chip: Chip;
	uid: string;
	type: CardTypeName;
	id: string;
	balance: number;
	transactions: Transaction[];
	journeySummary?: JourneySummary;
	products: Product[];
};

const SAK_1K = 0x88;
const SAK_4K = 0x18;
const UID_LENGTHS: Record<Chip, number> = { "1K": 4, "4K": 7 };
const LAST_USED_BLOCK = 33;
const PRODUCT_SECTORS = [3, 4];

function chipOf(dump: Uint8Array): Chip {
	if (dump[5] === SAK_1K) return "1K";
	if (dump[7] === SAK_4K) return "4K";
	throw new Error("block 0 carries neither a 1K nor a 4K SAK");
}

export function decodeCard(dump: Uint8Array): Card {
	const minimum = (LAST_USED_BLOCK + 1) * BLOCK_SIZE;
	if (dump.length % BLOCK_SIZE !== 0 || dump.length < minimum) {
		throw new Error(
			`dump must be whole blocks and at least ${minimum} bytes, got ${dump.length}`,
		);
	}
	const chip = chipOf(dump);
	const block = (index: number) =>
		dump.subarray(index * BLOCK_SIZE, (index + 1) * BLOCK_SIZE);
	if (block(8).toHex() !== block(9).toHex()) {
		throw new Error("balance blocks 8 and 9 differ");
	}
	const type = decodeCardType(block(1));
	const personal = type === "AvanzaPersonalUnlimited";
	return {
		chip,
		uid: block(0).subarray(0, UID_LENGTHS[chip]).toHex().toUpperCase(),
		type,
		id: decodeId(block(2)),
		balance: decodeBalance(block(8)),
		transactions: decodeTransactionLog(block),
		journeySummary:
			personal || isZero(block(10))
				? undefined
				: decodeJourneySummary(block(10)),
		products: personal
			? PRODUCT_SECTORS.map((sector) => sector * 4)
					.filter((index) => !isZero(block(index)))
					.map((index) => ({
						metadata: decodeSubscriptionMetadata(block(index)),
						subscription: decodeSubscription(block(index + 1)),
					}))
			: [],
	};
}
