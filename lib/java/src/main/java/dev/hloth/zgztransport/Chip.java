package dev.hloth.zgztransport;

import java.util.List;
import java.util.Optional;

/** A MIFARE Classic variant one of these cards is built on. */
public enum Chip {
	/** MIFARE Classic 1K, which the Avanza cards use. */
	CLASSIC_1K(4, 64, 16, 5, 0x88),
	/** MIFARE Classic 4K, which the Lazo card uses. */
	CLASSIC_4K(7, 256, 40, 7, 0x18);

	/**
	 * The order {@link Uid#decode(byte[])} tries the chips in. A 4K card carries
	 * its SAK at byte 7 and lets its UID run over byte 5, which can then hold the
	 * 1K SAK by chance, so the longer UID has to be ruled out first.
	 */
	static final List<Chip> DETECTION_ORDER = List.of(CLASSIC_4K, CLASSIC_1K);

	private final int uidLength;
	private final int blocks;
	private final int sectors;
	private final int sakOffset;
	private final int sak;

	Chip(int uidLength, int blocks, int sectors, int sakOffset, int sak) {
		this.uidLength = uidLength;
		this.blocks = blocks;
		this.sectors = sectors;
		this.sakOffset = sakOffset;
		this.sak = sak;
	}

	/**
	 * Bytes the UID of this chip takes in block 0.
	 *
	 * @return the length
	 */
	public int uidLength() {
		return uidLength;
	}

	/**
	 * Blocks a card with this chip has.
	 *
	 * @return the block count
	 */
	public int blocks() {
		return blocks;
	}

	/**
	 * Sectors a card with this chip has.
	 *
	 * @return the sector count
	 */
	public int sectors() {
		return sectors;
	}

	/**
	 * The chip whose UID takes that many bytes.
	 *
	 * @param uidLength
	 *            the length of a UID
	 * @return the chip, or empty when no chip has UIDs of that length
	 */
	public static Optional<Chip> ofUidLength(int uidLength) {
		for (Chip chip : values()) {
			if (chip.uidLength == uidLength) {
				return Optional.of(chip);
			}
		}
		return Optional.empty();
	}

	boolean hasSakIn(byte[] blockZero) {
		return Bytes.u8(blockZero[sakOffset]) == sak;
	}
}
