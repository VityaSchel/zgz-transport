package dev.hloth.zgztransport;

import java.util.Arrays;

/** A six byte MIFARE Classic sector key. */
public final class Key {

	/** Bytes a key takes. */
	public static final int BYTES = 6;

	private final byte[] bytes;

	private Key(byte[] bytes) {
		this.bytes = bytes;
	}

	/**
	 * The key those bytes hold.
	 *
	 * @param bytes
	 *            the six bytes, which are copied
	 * @return the key
	 * @throws CardFormatException
	 *             if the input is not six bytes
	 */
	public static Key of(byte[] bytes) {
		return new Key(Bytes.exact(bytes, BYTES, "key").clone());
	}

	/**
	 * The key those twelve hex digits spell, as key tables and dump tools print
	 * them.
	 *
	 * @param hex
	 *            the twelve hex digits, in either case
	 * @return the key
	 * @throws IllegalArgumentException
	 *             if the text is not twelve hex digits
	 */
	public static Key of(String hex) {
		return of(Bytes.fromHex(hex, "key"));
	}

	/**
	 * The bytes of this key.
	 *
	 * @return a copy of the six bytes
	 */
	public byte[] bytes() {
		return bytes.clone();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Key key && Arrays.equals(bytes, key.bytes);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(bytes);
	}

	/**
	 * The key as twelve upper case hex digits.
	 *
	 * @return the printed key
	 */
	@Override
	public String toString() {
		return Bytes.hex(bytes, 0, BYTES);
	}
}
