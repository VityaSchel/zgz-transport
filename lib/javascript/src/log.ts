import { isZero } from "./bytes.ts";
import { decodeTransaction, type Transaction } from "./transaction.ts";

/** Block holding the newest transaction. */
export const LIVE_BLOCK = 5;
/** Blocks holding the five transactions before the newest, indexed by sequence. */
export const ARCHIVE_BLOCKS = [28, 29, 30, 32, 33] as const;

/** Block a record moves to when a newer one replaces it, by its sequence byte. */
export const archiveBlock = (sequence: number) => ARCHIVE_BLOCKS[sequence];

const holdsRecord = (block: Uint8Array) => !isZero(block) && block[0] !== 0;

const timestamp = ({ createdAt: t }: Transaction) =>
	Date.UTC(t.year, t.month - 1, t.day, t.hour, t.minute, t.second);

/**
 * Decodes the transaction ring of a card.
 * @param block Returns the 16 bytes of a block by index.
 * @returns Transactions oldest first, skipping empty slots.
 */
export function decodeTransactionLog(
	block: (index: number) => Uint8Array,
): Transaction[] {
	return [LIVE_BLOCK, ...ARCHIVE_BLOCKS]
		.map(block)
		.filter(holdsRecord)
		.map(decodeTransaction)
		.sort((a, b) => timestamp(a) - timestamp(b));
}
