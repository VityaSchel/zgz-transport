import { decodeBalance } from "./balance.ts";
import { BLOCK_SIZE, isZero, xor } from "./bytes.ts";
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
import {
	decodeCardType,
	isPersonalCardType,
	type CardTypeName,
} from "./type.ts";

/** MIFARE Classic variant, told apart by the SAK and the ATQA in block 0. */
export type Chip = "1K" | "4K";

/** A subscription product of a personal card. */
export type Product = {
	/** Block 12 or 16. */
	metadata: SubscriptionMetadata;
	/** Block 13 or 17. */
	subscription: Subscription;
};

/**
 * Block 10 as the whole card decoder found it: either read, or `unknown` because the block is not
 * all zero and does not follow the known layout, carrying the error {@link decodeJourneySummary}
 * threw.
 */
export type JourneySummarySlot =
	| { state: "read"; summary: JourneySummary }
	| { state: "unknown"; error: Error };

/** Everything decodable from a dump. */
export type Card = {
	/** `1K` for Avanza cards, `4K` for Lazo cards. */
	chip: Chip;
	/** Upper case hex, 4 bytes on 1K chips and 4 or 7 bytes on 4K chips. */
	uid: string;
	/** Card type from block 1. */
	type: CardTypeName;
	/** Printed card id from block 2, e.g. `BE322743`. */
	id: string;
	/** Balance in {@link UNITS_PER_EURO} units, always `0` on personal cards. */
	balance: number;
	/** The last six transactions, oldest first. */
	transactions: Transaction[];
	/** Block 10; absent on personal cards and before the first journey. */
	journeySummary?: JourneySummarySlot;
	/** Subscription products, empty on top up cards. */
	products: Product[];
};

const CHIPS: { sak: number; atqa: [number, number]; chip: Chip }[] = [
	{ sak: 0x88, atqa: [0x04, 0x00], chip: "1K" },
	{ sak: 0x18, atqa: [0x02, 0x00], chip: "4K" },
];

/**
 * Block 0 layouts, longest UID first: a 7-byte UID can carry a 4-byte SAK value by chance, so the
 * 4-byte layout is only accepted when byte 4 is the BCC of the four UID bytes.
 */
const BLOCK_0_LAYOUTS = [
	{ uidLength: 7, sakOffset: 7 },
	{ uidLength: 4, sakOffset: 5 },
];

const LAST_USED_BLOCK = 33;
const PRODUCT_SECTORS = [3, 4];
const hex = (byte: number) => byte.toString(16).padStart(2, "0");

function readBlock0(dump: Uint8Array): { chip: Chip; uid: string } {
	for (const { uidLength, sakOffset } of BLOCK_0_LAYOUTS) {
		if (uidLength === 4 && dump[4] !== xor(dump.subarray(0, 4))) continue;
		const match = CHIPS.find(
			({ sak, atqa }) =>
				dump[sakOffset] === sak &&
				dump[sakOffset + 1] === atqa[0] &&
				dump[sakOffset + 2] === atqa[1],
		);
		if (match) {
			return {
				chip: match.chip,
				uid: dump.subarray(0, uidLength).toHex().toUpperCase(),
			};
		}
	}
	throw new Error(
		`block 0 matches no known chip: byte 7 is ${hex(dump[7]!)} and byte 5 is ${hex(dump[5]!)}, expected SAK 18 with ATQA 0200 or SAK 88 with ATQA 0400 after the UID, and a valid BCC on a 4-byte UID`,
	);
}

function journeySummarySlot(block: Uint8Array): JourneySummarySlot | undefined {
	if (isZero(block)) return undefined;
	try {
		return { state: "read", summary: decodeJourneySummary(block) };
	} catch (error) {
		return {
			state: "unknown",
			error: error instanceof Error ? error : new Error(String(error)),
		};
	}
}

/**
 * Decodes a raw dump of either card, as read by MifareClassicTool or a Proxmark.
 * Partial dumps are accepted as long as they reach block 33.
 * @param dump Consecutive 16-byte blocks starting at block 0.
 * @throws When block 0 matches no known chip or any block but 10 fails its checks.
 */
export function decodeCard(dump: Uint8Array): Card {
	const minimum = (LAST_USED_BLOCK + 1) * BLOCK_SIZE;
	if (dump.length % BLOCK_SIZE !== 0 || dump.length < minimum) {
		throw new Error(
			`dump must be whole blocks and at least ${minimum} bytes, got ${dump.length}`,
		);
	}
	const { chip, uid } = readBlock0(dump);
	const block = (index: number) =>
		dump.subarray(index * BLOCK_SIZE, (index + 1) * BLOCK_SIZE);
	if (block(8).toHex() !== block(9).toHex()) {
		throw new Error("balance blocks 8 and 9 differ");
	}
	const type = decodeCardType(block(1));
	const personal = isPersonalCardType(type);
	return {
		chip,
		uid,
		type,
		id: decodeId(block(2)),
		balance: decodeBalance(block(8)),
		transactions: decodeTransactionLog(block),
		journeySummary: personal ? undefined : journeySummarySlot(block(10)),
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
