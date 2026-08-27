package dev.hloth.zgztransport;

/**
 * Direction of a journey along its route, byte 8 of a transaction: the operator
 * GTFS {@code direction_id} plus one.
 */
public enum Direction {
	/** Byte {@code 01}; south on the tram. */
	ONE(1),
	/** Byte {@code 02}; north on the tram. */
	TWO(2);

	private final int value;

	Direction(int value) {
		this.value = value;
	}

	/**
	 * The byte the card stores.
	 *
	 * @return {@code 1} or {@code 2}
	 */
	public int value() {
		return value;
	}

	/**
	 * The direction a byte stands for.
	 *
	 * @param value
	 *            the byte
	 * @return the direction
	 * @throws CardFormatException
	 *             if the byte is neither {@code 1} nor {@code 2}
	 */
	public static Direction ofValue(int value) {
		for (Direction direction : values()) {
			if (direction.value == value) {
				return direction;
			}
		}
		throw new CardFormatException("direction must be 1 or 2, got " + value);
	}
}
