package dev.hloth.zgztransport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Everything a dump of one card holds.
 *
 * @param uid
 *            the UID of block 0
 * @param cardType
 *            the product of block 1
 * @param id
 *            the printed id of block 2
 * @param balance
 *            the balance of blocks 8 and 9, always zero on a personal card
 * @param transactions
 *            the last six transactions, oldest first
 * @param journeySummary
 *            block 10, absent on a personal card and before the first journey
 * @param products
 *            the subscription products, empty on a top up card
 */
public record Card(Uid uid, CardType cardType, CardId id, Balance balance, List<Transaction> transactions,
		Optional<JourneySummary> journeySummary, List<Product> products) {

	/** The first block a dump must reach to hold everything this decodes. */
	public static final int LAST_USED_BLOCK = 33;

	/**
	 * Checks that every field is present and copies the lists.
	 *
	 * @throws NullPointerException
	 *             if a field is null
	 */
	public Card {
		Objects.requireNonNull(uid, "uid");
		Objects.requireNonNull(cardType, "cardType");
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(balance, "balance");
		Objects.requireNonNull(journeySummary, "journeySummary");
		transactions = List.copyOf(transactions);
		products = List.copyOf(products);
	}

	/**
	 * Decodes a dump of either card, as MIFARE Classic Tool or a Proxmark writes
	 * it. A dump that stops after block {@value #LAST_USED_BLOCK} is enough, and
	 * anything past it is ignored.
	 *
	 * @param bytes
	 *            the blocks, one after another from block 0
	 * @return the card
	 * @throws CardFormatException
	 *             if the bytes are not whole blocks or the card does not decode
	 */
	public static Card decode(byte[] bytes) {
		return decode(Dump.of(bytes));
	}

	/**
	 * Decodes a dump of either card.
	 *
	 * @param dump
	 *            the blocks, from block 0
	 * @return the card
	 * @throws CardFormatException
	 *             if the dump stops before block {@value #LAST_USED_BLOCK}, carries
	 *             no known SAK, holds two different balance blocks, or any block it
	 *             reads does not decode
	 */
	public static Card decode(Dump dump) {
		if (dump.blockCount() <= LAST_USED_BLOCK) {
			throw new CardFormatException(
					"dump must reach block " + LAST_USED_BLOCK + ", got " + dump.blockCount() + " blocks");
		}
		Uid uid = Uid.decode(dump.block(0));
		if (!Arrays.equals(dump.block(8), dump.block(9))) {
			throw new CardFormatException("balance blocks 8 and 9 differ");
		}
		CardType cardType = CardType.decode(dump.block(1));
		byte[][] slots = new byte[TransactionLog.BLOCKS.size()][];
		for (int slot = 0; slot < slots.length; slot++) {
			slots[slot] = dump.block(TransactionLog.BLOCKS.get(slot));
		}
		return new Card(uid, cardType, CardId.decode(dump.block(2)), Balance.decode(dump.block(8)),
				TransactionLog.decode(slots), journeySummary(dump, cardType), products(dump, cardType));
	}

	private static Optional<JourneySummary> journeySummary(Dump dump, CardType cardType) {
		if (!cardType.recordsJourneySummary()) {
			return Optional.empty();
		}
		byte[] block = dump.block(10);
		if (Bytes.isZero(block, 0, block.length)) {
			return Optional.empty();
		}
		return Optional.of(JourneySummary.decode(block));
	}

	private static List<Product> products(Dump dump, CardType cardType) {
		List<Product> products = new ArrayList<>(cardType.productSectors().size());
		for (int sector : cardType.productSectors()) {
			byte[] metadata = dump.block(sector * 4);
			if (!Bytes.isZero(metadata, 0, metadata.length)) {
				products.add(new Product(sector, SubscriptionMetadata.decode(metadata),
						Subscription.decode(dump.block(sector * 4 + 1))));
			}
		}
		return products;
	}
}
