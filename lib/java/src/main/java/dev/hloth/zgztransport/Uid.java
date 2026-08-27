package dev.hloth.zgztransport;

import java.util.Arrays;

/**
 * The UID of block 0, whose length depends on the chip the card is built on.
 */
public final class Uid {

	private final byte[] bytes;
	private final Chip chip;

	private Uid(byte[] bytes, Chip chip) {
		this.bytes = bytes;
		this.chip = chip;
	}

	/**
	 * The UID those bytes hold, whose length says which chip it belongs to.
	 *
	 * @param bytes
	 *            the UID bytes, which are copied
	 * @return the UID
	 * @throws CardFormatException
	 *             if no known chip has UIDs of that length
	 */
	public static Uid of(byte[] bytes) {
		Chip chip = Chip.ofUidLength(bytes.length)
				.orElseThrow(() -> new CardFormatException("no known chip has a uid of " + bytes.length + " bytes"));
		return new Uid(bytes.clone(), chip);
	}

	/**
	 * Decodes block 0 by looking for the SAK of each known chip, in the order
	 * {@link Chip#DETECTION_ORDER} sets.
	 *
	 * @param block
	 *            the sixteen bytes
	 * @return the UID
	 * @throws CardFormatException
	 *             if the block is not sixteen bytes or carries no known SAK
	 */
	public static Uid decode(byte[] block) {
		Bytes.block(block, "block 0");
		for (Chip chip : Chip.DETECTION_ORDER) {
			if (chip.hasSakIn(block)) {
				return new Uid(Arrays.copyOf(block, chip.uidLength()), chip);
			}
		}
		throw new CardFormatException("block 0 carries the SAK of no known chip");
	}

	/**
	 * The bytes of this UID.
	 *
	 * @return a copy of them
	 */
	public byte[] bytes() {
		return bytes.clone();
	}

	/**
	 * The chip this UID belongs to.
	 *
	 * @return the chip
	 */
	public Chip chip() {
		return chip;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Uid uid && Arrays.equals(bytes, uid.bytes);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(bytes);
	}

	/**
	 * The UID as upper case hex, the form dump tools print.
	 *
	 * @return the printed UID
	 */
	@Override
	public String toString() {
		return Bytes.hex(bytes, 0, bytes.length);
	}
}
