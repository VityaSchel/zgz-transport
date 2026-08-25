import { isZero } from "./bytes";
import { decodeTransaction, type Transaction } from "./transaction";

export const LIVE_BLOCK = 5;
export const ARCHIVE_BLOCKS = [28, 29, 30, 32, 33] as const;

export const archiveBlock = (sequence: number) => ARCHIVE_BLOCKS[sequence];

const holdsRecord = (block: Uint8Array) => !isZero(block) && block[0] !== 0;

const timestamp = ({ createdAt: t }: Transaction) =>
	Date.UTC(t.year, t.month - 1, t.day, t.hour, t.minute, t.second);

export function decodeTransactionLog(
	block: (index: number) => Uint8Array,
): Transaction[] {
	return [LIVE_BLOCK, ...ARCHIVE_BLOCKS]
		.map(block)
		.filter(holdsRecord)
		.map(decodeTransaction)
		.sort((a, b) => timestamp(a) - timestamp(b));
}
