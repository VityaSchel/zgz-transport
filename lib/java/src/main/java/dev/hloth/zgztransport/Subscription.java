package dev.hloth.zgztransport;

import java.util.Objects;
import java.util.Optional;

/**
 * Block 13 or 17 of a personal card, which the card copies into block 14 or 18.
 * Several fields have not been worked out yet and are named after the bytes
 * they hold.
 *
 * @param startsAt
 *            the first day of validity
 * @param endsAt
 *            the last day of validity
 * @param unknown1
 *            bytes 4 and 5, big-endian, always {@code 0000} so far
 * @param unknown2
 *            bytes 6 to 9, big-endian
 * @param lastUsedAt
 *            the last use the pass recorded, absent while it is unused
 */
public record Subscription(CardDate startsAt, CardDate endsAt, int unknown1, long unknown2,
		Optional<CardDateTime> lastUsedAt) implements Encodable {

	/**
	 * Checks the ranges of the numeric fields and that the others are present.
	 *
	 * @throws IllegalArgumentException
	 *             if a field is outside the range its bytes allow
	 * @throws NullPointerException
	 *             if a field is null
	 */
	public Subscription {
		Objects.requireNonNull(startsAt, "startsAt");
		Objects.requireNonNull(endsAt, "endsAt");
		Objects.requireNonNull(lastUsedAt, "lastUsedAt");
		Bytes.checkRange("unknown1", unknown1, 0, 0xffff);
		Bytes.checkRange("unknown2", unknown2, 0, 0xffff_ffffL);
	}

	/**
	 * Decodes block 13 or 17.
	 *
	 * @param block
	 *            the sixteen bytes
	 * @return the subscription
	 * @throws CardFormatException
	 *             if the block is not sixteen bytes, its checksum does not match or
	 *             a date is out of range
	 */
	public static Subscription decode(byte[] block) {
		Bytes.block(block, "subscription block");
		Bytes.checkChecksum(block);
		Optional<CardDateTime> lastUsedAt = Bytes.isZero(block, 10, 15)
				? Optional.empty()
				: Optional.of(CardDateTime.decode(new byte[]{block[10], block[11], block[12], block[13], block[14]}));
		return new Subscription(CardDate.decode(new byte[]{block[0], block[1]}),
				CardDate.decode(new byte[]{block[2], block[3]}), Bytes.u16(block, 4), Bytes.unsigned(block, 6, 4),
				lastUsedAt);
	}

	/**
	 * Encodes this subscription into block 13 or 17, with its checksum.
	 *
	 * @return the sixteen bytes
	 */
	public byte[] encode() {
		byte[] block = new byte[Bytes.BLOCK_SIZE];
		System.arraycopy(startsAt.encode(), 0, block, 0, CardDate.BYTES);
		System.arraycopy(endsAt.encode(), 0, block, 2, CardDate.BYTES);
		Bytes.write(block, 4, unknown1, 2);
		Bytes.write(block, 6, unknown2, 4);
		lastUsedAt.ifPresent(moment -> System.arraycopy(moment.encode(), 0, block, 10, CardDateTime.BYTES));
		return Bytes.withChecksum(block);
	}
}
