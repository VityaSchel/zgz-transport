package dev.hloth.zgztransport;

import java.util.List;
import java.util.Optional;

/**
 * The products these cards come as, which bytes 0 to 2 of block 1 tell apart.
 */
public enum CardType implements Encodable {
	/**
	 * A balance top up Avanza Tarjeta Bus, which pays each journey out of its
	 * balance.
	 */
	AVANZA_TOP_UP(0x02_69_9f, Chip.CLASSIC_1K, List.of(), true, Keys.AVANZA_TOP_UP),
	/**
	 * A personal Avanza Tarjeta Bus, which travels on a subscription and never
	 * spends.
	 */
	AVANZA_PERSONAL_UNLIMITED(0x0a_97_75, Chip.CLASSIC_1K, List.of(3, 4), false, Keys.AVANZA_PERSONAL),
	/** A balance top up Lazo card. */
	LAZO_TOP_UP(0x0d_37_1f, Chip.CLASSIC_4K, List.of(), true, Keys.LAZO);

	private final int value;
	private final Chip chip;
	private final List<Integer> productSectors;
	private final boolean recordsJourneySummary;
	private final List<Keys.Range> keyTable;

	CardType(int value, Chip chip, List<Integer> productSectors, boolean recordsJourneySummary,
			List<Keys.Range> keyTable) {
		this.value = value;
		this.chip = chip;
		this.productSectors = productSectors;
		this.recordsJourneySummary = recordsJourneySummary;
		this.keyTable = keyTable;
	}

	/**
	 * Bytes 0 to 2 of block 1.
	 *
	 * @return the three bytes as one integer
	 */
	public int value() {
		return value;
	}

	/**
	 * The first byte of the product, which transactions and the journey summary
	 * carry.
	 *
	 * @return byte 0 of block 1
	 */
	public int firstByte() {
		return value >> 16;
	}

	/**
	 * The chip this product comes on.
	 *
	 * @return the chip
	 */
	public Chip chip() {
		return chip;
	}

	/**
	 * The sectors holding a subscription product on cards of this product, one
	 * product per sector.
	 *
	 * @return the sectors, empty on a product that pays each journey out of its
	 *         balance
	 */
	public List<Integer> productSectors() {
		return productSectors;
	}

	/**
	 * Whether cards of this product rewrite block 10 on every journey. A product
	 * that travels on a subscription leaves a constant there instead, which
	 * {@link JourneySummary#decode(byte[])} rejects.
	 *
	 * @return true when block 10 holds a journey summary
	 */
	public boolean recordsJourneySummary() {
		return recordsJourneySummary;
	}

	/**
	 * The product with those three bytes.
	 *
	 * @param value
	 *            bytes 0 to 2 of block 1 as one integer
	 * @return the product
	 * @throws CardFormatException
	 *             if no known product has those bytes
	 */
	public static CardType ofValue(int value) {
		for (CardType type : values()) {
			if (type.value == value) {
				return type;
			}
		}
		throw new CardFormatException("unknown card type " + String.format("%06x", value & 0xff_ff_ff));
	}

	/**
	 * The product whose first byte is the one given, as transactions carry it.
	 *
	 * @param firstByte
	 *            byte 0 of a transaction or of the journey summary
	 * @return the product
	 * @throws CardFormatException
	 *             if no known product starts with that byte
	 */
	public static CardType ofFirstByte(int firstByte) {
		for (CardType type : values()) {
			if (type.firstByte() == firstByte) {
				return type;
			}
		}
		throw new CardFormatException("unknown card type byte " + firstByte);
	}

	/**
	 * Decodes block 1, which says which product a card is.
	 *
	 * @param block
	 *            the sixteen bytes
	 * @return the product
	 * @throws CardFormatException
	 *             if the block is not sixteen bytes, its checksum does not match,
	 *             bytes 3 to 14 are not zero, or no known product has those first
	 *             three bytes
	 */
	public static CardType decode(byte[] block) {
		Bytes.block(block, "card type block");
		Bytes.checkChecksum(block);
		if (!Bytes.isZero(block, 3, 15)) {
			throw new CardFormatException("card type block bytes 3 to 14 must be zero");
		}
		return ofValue((int) Bytes.unsigned(block, 0, 3));
	}

	/**
	 * Encodes this product into block 1, with its checksum.
	 *
	 * @return the sixteen bytes
	 */
	public byte[] encode() {
		byte[] block = new byte[Bytes.BLOCK_SIZE];
		Bytes.write(block, 0, value, 3);
		return Bytes.withChecksum(block);
	}

	/**
	 * The keys of one sector of a card of this product, read from the table it
	 * carries.
	 *
	 * <p>
	 * Sectors 0 to 8 hold the same keys on both Avanza products, so either of them
	 * opens an Avanza card before block 1 has been read.
	 *
	 * @param sector
	 *            the sector, counting from zero
	 * @return the keys, or empty for a sector the table does not cover, which is
	 *         every sector past the last one {@link Chip#sectors()} counts
	 * @throws IllegalArgumentException
	 *             if the sector is negative
	 */
	public Optional<SectorKeys> keys(int sector) {
		Bytes.checkRange("sector", sector, 0, Integer.MAX_VALUE);
		for (Keys.Range range : keyTable) {
			if (sector >= range.first() && sector <= range.last()) {
				return Optional.of(range.keys());
			}
		}
		return Optional.empty();
	}
}
