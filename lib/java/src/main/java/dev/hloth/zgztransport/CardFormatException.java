package dev.hloth.zgztransport;

/**
 * Thrown when a block does not hold what the card specification says it should:
 * a wrong length, a checksum that does not match, a field outside its range or
 * a value no known product uses.
 *
 * <p>
 * It extends {@link IllegalArgumentException}, as do the range checks the
 * constructors of this package perform, so a caller can catch both with one
 * clause.
 */
public class CardFormatException extends IllegalArgumentException {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates the exception.
	 *
	 * @param message
	 *            what was wrong with the bytes
	 */
	public CardFormatException(String message) {
		super(message);
	}
}
