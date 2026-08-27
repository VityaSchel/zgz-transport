package dev.hloth.zgztransport;

import java.util.Arrays;

/**
 * The card id printed on the plastic, which block 2 holds as two letters and
 * one decimal digit per nibble, such as {@code BE322743}.
 */
public final class CardId implements Encodable {

	private static final int DIGIT_BYTES = 13;

	private final byte[] bytes;

	private CardId(byte[] bytes) {
		this.bytes = bytes;
	}

	/**
	 * The id that text spells.
	 *
	 * @param text
	 *            two capital letters followed by an even count of 6 to 26 digits
	 * @return the id
	 * @throws IllegalArgumentException
	 *             if the text is not in that form
	 */
	public static CardId parse(String text) {
		String digits = text.length() >= 2 ? text.substring(2) : "";
		if (text.length() < 8 || !isUpperCaseLetter(text.charAt(0)) || !isUpperCaseLetter(text.charAt(1))
				|| digits.length() > DIGIT_BYTES * 2 || digits.length() % 2 != 0
				|| !digits.chars().allMatch(character -> character >= '0' && character <= '9')) {
			throw new IllegalArgumentException(
					"id must be two capital letters and an even count of 6 to 26 digits, got " + text);
		}
		byte[] bytes = new byte[Bytes.BLOCK_SIZE - 1];
		bytes[0] = (byte) text.charAt(0);
		bytes[1] = (byte) text.charAt(1);
		for (int i = 0; i < digits.length() / 2; i++) {
			bytes[2 + i] = (byte) (((digits.charAt(i * 2) - '0') << 4) | (digits.charAt(i * 2 + 1) - '0'));
		}
		return new CardId(bytes);
	}

	/**
	 * Decodes block 2. Only the checksum is checked, so an id printed from a
	 * damaged block can hold bytes that {@link #parse(String)} would not accept.
	 *
	 * @param block
	 *            the sixteen bytes
	 * @return the id
	 * @throws CardFormatException
	 *             if the block is not sixteen bytes or its checksum does not match
	 */
	public static CardId decode(byte[] block) {
		Bytes.block(block, "id block");
		Bytes.checkChecksum(block);
		return new CardId(Arrays.copyOf(block, Bytes.BLOCK_SIZE - 1));
	}

	/**
	 * Encodes this id into block 2, with its checksum.
	 *
	 * @return the sixteen bytes
	 */
	public byte[] encode() {
		byte[] block = Arrays.copyOf(bytes, Bytes.BLOCK_SIZE);
		return Bytes.withChecksum(block);
	}

	private static boolean isUpperCaseLetter(char character) {
		return character >= 'A' && character <= 'Z';
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof CardId id && Arrays.equals(bytes, id.bytes);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(bytes);
	}

	/**
	 * The id as printed on the card: the two letters, then the digits with the
	 * trailing zero bytes the card pads with trimmed off.
	 *
	 * @return the printed id
	 */
	@Override
	public String toString() {
		int end = bytes.length;
		while (end > 5 && bytes[end - 1] == 0) {
			end--;
		}
		return "" + (char) Bytes.u8(bytes[0]) + (char) Bytes.u8(bytes[1])
				+ Bytes.hex(bytes, 2, end).toLowerCase(java.util.Locale.ROOT);
	}
}
