package dev.hloth.zgztransport;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * The UID of block 0, four or seven bytes, together with the chip whose SAK and
 * ATQA sit beside it. The length belongs to the block rather than to the chip:
 * a 4K card comes with either.
 */
public final class Uid {

	private final byte[] bytes;
	private final Chip chip;

	private Uid(byte[] bytes, Chip chip) {
		this.bytes = bytes;
		this.chip = chip;
	}

	/**
	 * The UID those bytes hold, on the chip it was read from.
	 *
	 * @param bytes
	 *            the UID bytes, four or seven of them, which are copied
	 * @param chip
	 *            the chip the card is built on
	 * @return the UID
	 * @throws CardFormatException
	 *             if the UID is neither four nor seven bytes long
	 * @throws NullPointerException
	 *             if the chip is null
	 */
	public static Uid of(byte[] bytes, Chip chip) {
		Objects.requireNonNull(chip, "chip");
		if (bytes.length != Layout.FOUR_BYTE.uidLength && bytes.length != Layout.SEVEN_BYTE.uidLength) {
			throw new CardFormatException("uid must be " + Layout.FOUR_BYTE.uidLength + " or "
					+ Layout.SEVEN_BYTE.uidLength + " bytes, got " + bytes.length);
		}
		return new Uid(bytes.clone(), chip);
	}

	/**
	 * Decodes block 0 by trying the two layouts a block 0 comes in, the seven byte
	 * one first, and reading the chip off the SAK and the ATQA each of them puts
	 * after the UID. A seven byte UID can hold a known SAK in byte 5 by chance, so
	 * the BCC and the ATQA position are what tell the two apart.
	 *
	 * @param block
	 *            the sixteen bytes
	 * @return the UID and the chip it was read beside
	 * @throws CardFormatException
	 *             if the block is not sixteen bytes, or neither layout holds a
	 *             known SAK and ATQA with a matching BCC
	 */
	public static Uid decode(byte[] block) {
		Bytes.block(block, "block 0");
		for (Layout layout : Layout.values()) {
			Optional<Chip> chip = layout.chipIn(block);
			if (chip.isPresent()) {
				return new Uid(Arrays.copyOf(block, layout.uidLength), chip.orElseThrow());
			}
		}
		throw new CardFormatException(String.format(
				"block 0 matches no known chip: byte %d is %02x and byte %d is %02x, expected SAK 18 with ATQA 0200"
						+ " or SAK 88 with ATQA 0400 after the UID, and a valid BCC on a 4-byte UID",
				Layout.SEVEN_BYTE.sakOffset, Bytes.u8(block[Layout.SEVEN_BYTE.sakOffset]), Layout.FOUR_BYTE.sakOffset,
				Bytes.u8(block[Layout.FOUR_BYTE.sakOffset])));
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
	 * The chip this UID was read beside.
	 *
	 * @return the chip
	 */
	public Chip chip() {
		return chip;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Uid uid && chip == uid.chip && Arrays.equals(bytes, uid.bytes);
	}

	@Override
	public int hashCode() {
		return 31 * Arrays.hashCode(bytes) + chip.hashCode();
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

	/** A shape block 0 comes in, tried in the order declared here. */
	private enum Layout {
		/** Seven UID bytes, then the SAK at byte 7 and the ATQA at bytes 8 and 9. */
		SEVEN_BYTE(7, 7, false),
		/**
		 * Four UID bytes, their BCC at byte 4, then the SAK at byte 5 and the ATQA at
		 * bytes 6 and 7.
		 */
		FOUR_BYTE(4, 5, true);

		private final int uidLength;
		private final int sakOffset;
		private final boolean checksBcc;

		Layout(int uidLength, int sakOffset, boolean checksBcc) {
			this.uidLength = uidLength;
			this.sakOffset = sakOffset;
			this.checksBcc = checksBcc;
		}

		Optional<Chip> chipIn(byte[] blockZero) {
			if (checksBcc && Bytes.xor(blockZero, 0, uidLength) != blockZero[uidLength]) {
				return Optional.empty();
			}
			for (Chip chip : Chip.values()) {
				if (chip.answersAt(blockZero, sakOffset)) {
					return Optional.of(chip);
				}
			}
			return Optional.empty();
		}
	}
}
