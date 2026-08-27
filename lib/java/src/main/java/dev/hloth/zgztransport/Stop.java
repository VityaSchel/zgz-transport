package dev.hloth.zgztransport;

/**
 * Where a transaction happened, bytes 5 and 6 of a transaction. Bit 15 marks an
 * urban bus stop; without it the value is a tram stop on the tram route and an
 * id of another operator elsewhere.
 */
public sealed interface Stop extends Encodable {

	/** Bytes a stop takes. */
	int BYTES = 2;

	/** Largest id the fifteen free bits hold. */
	int MAX_ID = 0x7fff;

	/**
	 * An urban bus stop, which the operator numbers internally.
	 *
	 * @param id
	 *            the stop id, {@code 0} to {@link #MAX_ID}
	 */
	record Urban(int id) implements Stop {
		/**
		 * Checks the range of the id.
		 *
		 * @throws IllegalArgumentException
		 *             if the id does not fit in fifteen bits
		 */
		public Urban {
			Bytes.checkRange("urban stop id", id, 0, MAX_ID);
		}

		@Override
		public byte[] encode() {
			return Bytes.twoBytes(0x8000 | id);
		}
	}

	/**
	 * A tram stop, whose number the card stores multiplied by one hundred.
	 *
	 * @param value
	 *            the stored value, the stop number times one hundred
	 */
	record Tram(int value) implements Stop {
		/**
		 * Checks the range of the value.
		 *
		 * @throws IllegalArgumentException
		 *             if the value does not fit in fifteen bits
		 */
		public Tram {
			Bytes.checkRange("tram stop", value, 0, MAX_ID);
		}

		@Override
		public byte[] encode() {
			return Bytes.twoBytes(value);
		}

		/**
		 * The stop number as the operator prints it.
		 *
		 * @return the stored value divided by one hundred
		 */
		public int number() {
			return value / 100;
		}
	}

	/**
	 * A stop of an operator other than the urban buses and the tram.
	 *
	 * @param id
	 *            the id that operator uses
	 */
	record Other(int id) implements Stop {
		/**
		 * Checks the range of the id.
		 *
		 * @throws IllegalArgumentException
		 *             if the id does not fit in fifteen bits
		 */
		public Other {
			Bytes.checkRange("stop id", id, 0, MAX_ID);
		}

		@Override
		public byte[] encode() {
			return Bytes.twoBytes(id);
		}
	}

	/**
	 * Decodes bytes 5 and 6 of a transaction.
	 *
	 * @param bytes
	 *            the two bytes
	 * @param route
	 *            the route of the same transaction, which tells the tram from other
	 *            operators
	 * @return the stop
	 * @throws CardFormatException
	 *             if the input is not two bytes
	 */
	static Stop decode(byte[] bytes, Route route) {
		Bytes.exact(bytes, BYTES, "stop");
		int value = Bytes.u16(bytes, 0);
		if ((value & 0x8000) != 0) {
			return new Urban(value & MAX_ID);
		}
		if (route.equals(Route.TRAM)) {
			return new Tram(value);
		}
		return new Other(value);
	}

}
