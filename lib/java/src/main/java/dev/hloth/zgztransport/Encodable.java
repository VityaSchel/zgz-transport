package dev.hloth.zgztransport;

/**
 * A structure the card stores as bytes. Every implementation checks its fields
 * when it is built, so writing them back never fails.
 */
public interface Encodable {

	/**
	 * Writes this structure as the card stores it.
	 *
	 * @return the bytes, sixteen of them for a whole block
	 */
	byte[] encode();
}
