import { decodeBalance } from "./balance.ts";
import { BLOCK_SIZE, isZero } from "./bytes.ts";
import { decodeId } from "./id.ts";
import {
	decodeJourneySummary,
	type JourneySummary,
} from "./journey-summary.ts";
import { decodeTransactionLog } from "./log.ts";
import { decodeSubscription, type Subscription } from "./subscription.ts";
import {
	decodeSubscriptionMetadata,
	type SubscriptionMetadata,
} from "./subscription-metadata.ts";
import type { Transaction } from "./transaction.ts";
import { decodeCardType, type CardTypeName } from "./type.ts";

/** MIFARE Classic variant, told apart by the SAK byte in block 0. */
export type Chip = "1K" | "4K";

/** A subscription product of a personal card. */
export type Product = {
	/** Block 12 or 16. */
	metadata: SubscriptionMetadata;
	/** Block 13 or 17. */
	subscription: Subscription;
};

/** Everything decodable from a dump. */
export type Card = {
	/** `1K` for Avanza cards, `4K` for Lazo cards. */
	chip: Chip;
	/** Upper case hex, 4 bytes on 1K and 7 bytes on 4K chips. */
	uid: string;
	/** Product from block 1. */
	type: CardTypeName;
	/** Printed card id from block 2, e.g. `BE322743`. */
	id: string;
	/** Balance in {@link UNITS_PER_EURO} units, always `0` on personal cards. */
	balance: number;
	/** The last six transactions, oldest first. */
	transactions: Transaction[];
	/** Block 10; absent on personal cards and before the first journey. */
	journeySummary?: JourneySummary;
	/** Subscription products, empty on top up cards. */
	products: Product[];
};

const SAK_1K = 0x88;
const SAK_4K = 0x18;
const UID_LENGTHS: Record<Chip, number> = { "1K": 4, "4K": 7 };
const LAST_USED_BLOCK = 33;
const PRODUCT_SECTORS = [3, 4];

function chipOf(dump: Uint8Array): Chip {
	if (dump[7] === SAK_4K) return "4K";
	if (dump[5] === SAK_1K) return "1K";
	throw new Error("block 0 carries neither a 1K nor a 4K SAK");
}

/**
 * Decodes a raw dump of either card, as read by MifareClassicTool or a Proxmark.
 * Partial dumps are accepted as long as they reach block 33.
 * @param dump Consecutive 16-byte blocks starting at block 0.
 * @throws When block 0 has no known SAK or any block fails its checks.
 */
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
