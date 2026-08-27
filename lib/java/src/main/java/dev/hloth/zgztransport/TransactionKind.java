package dev.hloth.zgztransport;

import java.util.Objects;

/**
 * What a transaction record stands for, which byte 8 tells: a journey or a top
 * up.
 */
public sealed interface TransactionKind {

	/**
	 * The byte the card stores.
	 *
	 * @return byte 8 of the record
	 */
	int value();

	/**
	 * A ride, paid out of the balance or free as a transfer.
	 *
	 * @param direction
	 *            the direction travelled along the route
	 */
	record Journey(Direction direction) implements TransactionKind {
		/**
		 * Checks that the direction is present.
		 *
		 * @throws NullPointerException
		 *             if the direction is null
		 */
		public Journey {
			Objects.requireNonNull(direction, "direction");
		}

		@Override
		public int value() {
			return direction.value();
		}
	}

	/** Money added to the balance. */
	record TopUp() implements TransactionKind {
		@Override
		public int value() {
			return 8;
		}
	}

	/**
	 * The kind a byte stands for.
	 *
	 * @param value
	 *            byte 8 of a record
	 * @return the kind
	 * @throws CardFormatException
	 *             if the byte is neither a direction nor a top up
	 */
	static TransactionKind ofValue(int value) {
		if (value == new TopUp().value()) {
			return new TopUp();
		}
		if (value == 1 || value == 2) {
			return new Journey(Direction.ofValue(value));
		}
		throw new CardFormatException("transaction kind byte must be 1, 2 or 8, got " + value);
	}
}
