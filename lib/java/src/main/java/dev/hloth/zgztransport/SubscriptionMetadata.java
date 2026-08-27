package dev.hloth.zgztransport;

import java.util.Objects;

/**
 * Block 12 or 16 of a personal card, which describes one subscription product
 * the card carries. Several fields have not been worked out yet and are named
 * after the bytes they hold.
 *
 * @param productId
 *            byte 0, which tells the products of one card apart
 * @param unknown1
 *            byte 1, always {@code 1} so far
 * @param purchasedAt
 *            when the product was bought
 * @param unknown2
 *            bytes 4 to 7, big-endian, always {@code 00210000} so far
 * @param validityDays
 *            how many days the product is valid for
 * @param unknown3
 *            bytes 10 to 14, big-endian, always {@code 0021000000} so far
 */
public record SubscriptionMetadata(int productId, int unknown1, CardDate purchasedAt, long unknown2, int validityDays,
		long unknown3) implements Encodable {

	/**
	 * Checks the ranges of the numeric fields and that the date is present.
	 *
	 * @throws IllegalArgumentException
	 *             if a field is outside the range its bytes allow
	 * @throws NullPointerException
	 *             if the date is null
	 */
	public SubscriptionMetadata {
		Objects.requireNonNull(purchasedAt, "purchasedAt");
		Bytes.checkRange("productId", productId, 0, 0xff);
		Bytes.checkRange("unknown1", unknown1, 0, 0xff);
		Bytes.checkRange("unknown2", unknown2, 0, 0xffff_ffffL);
		Bytes.checkRange("validityDays", validityDays, 0, 0xffff);
		Bytes.checkRange("unknown3", unknown3, 0, 0xff_ffff_ffffL);
	}

	/**
	 * Decodes block 12 or 16.
	 *
	 * @param block
	 *            the sixteen bytes
	 * @return the metadata
	 * @throws CardFormatException
	 *             if the block is not sixteen bytes, its checksum does not match or
	 *             the purchase date is out of range
	 */
	public static SubscriptionMetadata decode(byte[] block) {
		Bytes.block(block, "subscription metadata block");
		Bytes.checkChecksum(block);
		return new SubscriptionMetadata(Bytes.u8(block[0]), Bytes.u8(block[1]),
				CardDate.decode(new byte[]{block[2], block[3]}), Bytes.unsigned(block, 4, 4), Bytes.u16(block, 8),
				Bytes.unsigned(block, 10, 5));
	}

	/**
	 * Encodes this metadata into block 12 or 16, with its checksum.
	 *
	 * @return the sixteen bytes
	 */
	public byte[] encode() {
		byte[] block = new byte[Bytes.BLOCK_SIZE];
		block[0] = (byte) productId;
		block[1] = (byte) unknown1;
		System.arraycopy(purchasedAt.encode(), 0, block, 2, CardDate.BYTES);
		Bytes.write(block, 4, unknown2, 4);
		Bytes.write(block, 8, validityDays, 2);
		Bytes.write(block, 10, unknown3, 5);
		return Bytes.withChecksum(block);
	}
}
