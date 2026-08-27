package dev.hloth.zgztransport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;

/**
 * The ring of six blocks a card keeps its last transactions in: block 5 holds
 * the newest record, and the five before it live in the archive blocks, each in
 * the slot its sequence byte picks.
 */
public final class TransactionLog {

	/** The block holding the newest record. */
	public static final int LIVE_BLOCK = 5;

	/**
	 * The blocks holding the five records before the newest, indexed by sequence
	 * byte.
	 */
	public static final List<Integer> ARCHIVE_BLOCKS = List.of(28, 29, 30, 32, 33);

	/** The six slots, in the order {@link #decode(byte[][])} expects. */
	public static final List<Integer> BLOCKS = blocks();

	private TransactionLog() {
	}

	/**
	 * The block a record moves to when a newer one replaces it.
	 *
	 * @param sequence
	 *            the sequence byte of the record
	 * @return the block number, or empty when the byte is not one the ring uses
	 */
	public static OptionalInt archiveBlock(int sequence) {
		return sequence >= 0 && sequence < ARCHIVE_BLOCKS.size()
				? OptionalInt.of(ARCHIVE_BLOCKS.get(sequence))
				: OptionalInt.empty();
	}

	/**
	 * Decodes the six slots of the ring, oldest record first. Slots whose first
	 * byte is zero hold no record and are skipped; records that share a timestamp
	 * keep the order of the slots.
	 *
	 * @param slots
	 *            the six blocks, in the order of {@link #BLOCKS}
	 * @return the records the ring holds
	 * @throws CardFormatException
	 *             if there are not six slots or an occupied one does not decode
	 */
	public static List<Transaction> decode(byte[][] slots) {
		if (slots.length != BLOCKS.size()) {
			throw new CardFormatException("log must be " + BLOCKS.size() + " slots, got " + slots.length);
		}
		List<Transaction> log = new ArrayList<>(slots.length);
		for (byte[] slot : slots) {
			Bytes.block(slot, "transaction block");
			if (slot[0] != 0) {
				log.add(Transaction.decode(slot));
			}
		}
		log.sort(Comparator.comparing(Transaction::createdAt));
		return List.copyOf(log);
	}

	private static List<Integer> blocks() {
		List<Integer> blocks = new ArrayList<>();
		blocks.add(LIVE_BLOCK);
		blocks.addAll(ARCHIVE_BLOCKS);
		return List.copyOf(blocks);
	}
}
