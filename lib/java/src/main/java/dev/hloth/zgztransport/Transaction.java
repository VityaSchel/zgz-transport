package dev.hloth.zgztransport;

import java.util.Objects;
import java.util.Optional;

/**
 * One record of the transaction log: a journey or a top up, with where and when
 * it happened.
 *
 * @param cardType
 *            the product of the card that made it
 * @param networkFlag
 *            byte 1, {@code 0} on top up cards and {@code 1} or {@code 2} on
 *            personal ones
 * @param amount
 *            the money moved, in {@link Balance#UNITS_PER_EURO} units,
 *            {@code 0} when the journey cost nothing
 * @param consecutivePayments
 *            payments of this card at one terminal in a row, counting from
 *            {@code 1}, or {@code 0} on a top up
 * @param stop
 *            where it happened
 * @param route
 *            the route, or {@link Route} {@code 0} on a top up made off board
 * @param kind
 *            whether it was a journey or a top up
 * @param dutyTrip
 *            byte 9, on buses most likely which trip of the vehicle daily duty
 *            this is, counting from {@code 1}
 * @param createdAt
 *            when the card wrote the record
 * @param sequence
 *            {@code 0} to {@code 4}, which picks the archive block the record
 *            moves to
 */
public record Transaction(CardType cardType, int networkFlag, int amount, int consecutivePayments, Stop stop,
		Route route, TransactionKind kind, int dutyTrip, CardDateTime createdAt, int sequence) implements Encodable {

	/**
	 * Checks the ranges of the numeric fields and that the others are present.
	 *
	 * @throws IllegalArgumentException
	 *             if a field is outside the range its byte allows
	 * @throws NullPointerException
	 *             if a field is null
	 */
	public Transaction {
		Objects.requireNonNull(cardType, "cardType");
		Objects.requireNonNull(stop, "stop");
		Objects.requireNonNull(route, "route");
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(createdAt, "createdAt");
		Bytes.checkRange("networkFlag", networkFlag, 0, 0xff);
		Bytes.checkRange("amount", amount, 0, 0xffff);
		Bytes.checkRange("consecutivePayments", consecutivePayments, 0, 0xff);
		Bytes.checkRange("dutyTrip", dutyTrip, 0, 0xff);
		Bytes.checkRange("sequence", sequence, 0, 0xff);
	}

	/**
	 * Decodes one record of the log.
	 *
	 * @param block
	 *            the sixteen bytes
	 * @return the transaction
	 * @throws CardFormatException
	 *             if the block is not sixteen bytes, no known product starts with
	 *             byte 0, the timestamp is out of range, or byte 8 is neither a
	 *             direction nor a top up
	 */
	public static Transaction decode(byte[] block) {
		Bytes.block(block, "transaction block");
		Route route = new Route(Bytes.u8(block[7]));
		CardType cardType = CardType.ofFirstByte(Bytes.u8(block[0]));
		CardDateTime createdAt = CardDateTime.decode(new byte[]{block[10], block[11], block[12], block[13], block[14]});
		return new Transaction(cardType, Bytes.u8(block[1]), Bytes.u16(block, 2), Bytes.u8(block[4]),
				Stop.decode(new byte[]{block[5], block[6]}, route), route, TransactionKind.ofValue(Bytes.u8(block[8])),
				Bytes.u8(block[9]), createdAt, Bytes.u8(block[15]));
	}

	/**
	 * Encodes this transaction into one record of the log.
	 *
	 * @return the sixteen bytes
	 */
	public byte[] encode() {
		byte[] block = new byte[Bytes.BLOCK_SIZE];
		block[0] = (byte) cardType.firstByte();
		block[1] = (byte) networkFlag;
		Bytes.write(block, 2, amount, 2);
		block[4] = (byte) consecutivePayments;
		System.arraycopy(stop.encode(), 0, block, 5, Stop.BYTES);
		block[7] = (byte) route.id();
		block[8] = (byte) kind.value();
		block[9] = (byte) dutyTrip;
		System.arraycopy(createdAt.encode(), 0, block, 10, CardDateTime.BYTES);
		block[15] = (byte) sequence;
		return block;
	}

	/**
	 * A builder, which spares the caller a constructor of ten arguments.
	 *
	 * @return an empty builder
	 */
	public static TransactionBuilder builder() {
		return new TransactionBuilder();
	}

	/**
	 * Whether this is a ride that cost nothing. Every journey on a personal
	 * unlimited card is free.
	 *
	 * @return true when this is a journey of amount zero
	 */
	public boolean isFree() {
		return kind instanceof TransactionKind.Journey && amount == 0;
	}

	/**
	 * Whether this is the free transfer a balance card earns after a paid ride,
	 * which the operator grants once per ride, to another route, within 60 minutes
	 * on the urban network and 75 when a CTAZ card enters Zaragoza.
	 *
	 * @return true when this is a free journey carrying a payment counter
	 */
	public boolean isTransfer() {
		return isFree() && consecutivePayments > 0 && cardType != CardType.AVANZA_PERSONAL_UNLIMITED;
	}

	/**
	 * Whether this is a check-out at a gated station, which carries no payment
	 * counter. Rests on the single Cercanias check-out in the dumps, so treat it as
	 * provisional.
	 *
	 * @return true when this is a free journey without a payment counter
	 */
	public boolean isCheckOut() {
		return isFree() && consecutivePayments == 0;
	}

	/**
	 * The direction of this transaction when it is a journey.
	 *
	 * @return the direction, or empty on a top up
	 */
	public Optional<Direction> direction() {
		return kind instanceof TransactionKind.Journey journey ? Optional.of(journey.direction()) : Optional.empty();
	}
}
