package dev.hloth.zgztransport;

import java.util.Objects;
import java.util.Optional;

/**
 * Block 10 of a top up card, which the card rewrites on every journey and
 * leaves alone on a top up. A card that has only ever been topped up holds all
 * zeroes here, and a personal card holds a constant that
 * {@link #decode(byte[])} rejects, see {@link #personalBlock()}.
 *
 * @param previous
 *            the journey before the current one, absent on the first ever
 *            journey
 * @param lastPaidAt
 *            when the last paid journey happened, which a free transfer leaves
 *            pointing at the ride it belongs to
 * @param consecutivePayments
 *            the same counter as byte 4 of the current transaction
 * @param cardType
 *            the product of the card
 * @param free
 *            whether the current journey was a free transfer
 * @param route
 *            the route of the current journey
 * @param direction
 *            the direction of the current journey
 * @param transfersLeft
 *            {@code 0x63} after a paid journey and {@code 0x62} after a free
 *            transfer
 */
public record JourneySummary(Optional<Leg> previous, LastPaid lastPaidAt, int consecutivePayments, CardType cardType,
		boolean free, Route route, Direction direction, int transfersLeft) implements Encodable {

	/**
	 * A route and the direction travelled along it.
	 *
	 * @param route
	 *            the route
	 * @param direction
	 *            the direction
	 */
	public record Leg(Route route, Direction direction) {
		/**
		 * Checks that both fields are present and that the route is one the card can
		 * point back to, which is never zero.
		 *
		 * @throws IllegalArgumentException
		 *             if the route is zero
		 * @throws NullPointerException
		 *             if a field is null
		 */
		public Leg {
			Objects.requireNonNull(route, "route");
			Objects.requireNonNull(direction, "direction");
			Bytes.checkRange("previous route", route.id(), 1, 0xff);
		}
	}

	/**
	 * When the last paid journey happened, which the card records to the minute.
	 *
	 * @param date
	 *            the day
	 * @param hour
	 *            {@code 0} to {@code 23}
	 * @param minute
	 *            {@code 0} to {@code 59}
	 */
	public record LastPaid(CardDate date, int hour, int minute) {
		/**
		 * Checks the ranges of the two fields and that the date is present.
		 *
		 * @throws IllegalArgumentException
		 *             if the hour or the minute is outside its range
		 * @throws NullPointerException
		 *             if the date is null
		 */
		public LastPaid {
			Objects.requireNonNull(date, "date");
			Bytes.checkRange("hour", hour, 0, 23);
			Bytes.checkRange("minute", minute, 0, 59);
		}
	}

	/**
	 * Checks the ranges of the numeric fields and that the others are present.
	 *
	 * @throws IllegalArgumentException
	 *             if a field is outside the range its byte allows
	 * @throws NullPointerException
	 *             if a field is null
	 */
	public JourneySummary {
		Objects.requireNonNull(previous, "previous");
		Objects.requireNonNull(lastPaidAt, "lastPaidAt");
		Objects.requireNonNull(cardType, "cardType");
		Objects.requireNonNull(route, "route");
		Objects.requireNonNull(direction, "direction");
		Bytes.checkRange("consecutivePayments", consecutivePayments, 0, 0xff);
		Bytes.checkRange("transfersLeft", transfersLeft, 0, 0xff);
	}

	/**
	 * The constant block 10 that personal cards hold, which stands for no journey
	 * at all.
	 *
	 * @return the sixteen bytes
	 */
	public static byte[] personalBlock() {
		byte[] block = new byte[Bytes.BLOCK_SIZE];
		block[7] = 0x0a;
		block[15] = 0x0a;
		return block;
	}

	/**
	 * Decodes block 10 of a top up card.
	 *
	 * @param block
	 *            the sixteen bytes
	 * @return the summary
	 * @throws CardFormatException
	 *             if the block is not sixteen bytes, its checksum does not match,
	 *             the bytes the card keeps at zero are set, or a field is outside
	 *             its range
	 */
	public static JourneySummary decode(byte[] block) {
		Bytes.block(block, "journey summary block");
		Bytes.checkChecksum(block);
		if (block[11] != 0 || block[12] != 0 || block[14] != 0) {
			throw new CardFormatException("journey summary bytes 11, 12 and 14 must be zero");
		}
		LastPaid lastPaidAt = lastPaid(block);
		Optional<Leg> previous = Bytes.u8(block[0]) == 0
				? Optional.empty()
				: Optional.of(new Leg(new Route(Bytes.u8(block[0])), Direction.ofValue(Bytes.u8(block[1]))));
		return new JourneySummary(previous, lastPaidAt, Bytes.u8(block[6]), CardType.ofFirstByte(Bytes.u8(block[7])),
				Bytes.u8(block[8]) == 1, new Route(Bytes.u8(block[9])), Direction.ofValue(Bytes.u8(block[10])),
				Bytes.u8(block[13]));
	}

	private static LastPaid lastPaid(byte[] block) {
		try {
			return new LastPaid(CardDate.decode(new byte[]{block[2], block[3]}), Bytes.u8(block[4]),
					Bytes.u8(block[5]));
		} catch (CardFormatException alreadyDescribed) {
			throw alreadyDescribed;
		} catch (IllegalArgumentException cause) {
			throw new CardFormatException(cause.getMessage());
		}
	}

	/**
	 * Encodes this summary into block 10, with its checksum.
	 *
	 * @return the sixteen bytes
	 */
	public byte[] encode() {
		byte[] block = new byte[Bytes.BLOCK_SIZE];
		previous.ifPresent(leg -> {
			block[0] = (byte) leg.route().id();
			block[1] = (byte) leg.direction().value();
		});
		System.arraycopy(lastPaidAt.date().encode(), 0, block, 2, CardDate.BYTES);
		block[4] = (byte) lastPaidAt.hour();
		block[5] = (byte) lastPaidAt.minute();
		block[6] = (byte) consecutivePayments;
		block[7] = (byte) cardType.firstByte();
		block[8] = (byte) (free ? 1 : 0);
		block[9] = (byte) route.id();
		block[10] = (byte) direction.value();
		block[13] = (byte) transfersLeft;
		return Bytes.withChecksum(block);
	}
}
