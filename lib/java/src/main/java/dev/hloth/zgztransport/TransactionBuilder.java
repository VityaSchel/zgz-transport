package dev.hloth.zgztransport;

/**
 * Collects the fields of a {@link Transaction} one at a time, which reads
 * better than a constructor of ten arguments and cannot mix up the counters
 * that sit next to each other.
 *
 * <p>
 * The amount, the counters and the network flag start at zero, the values a top
 * up carries. The product, the stop, the route, the kind and the timestamp have
 * no default and have to be set. Nothing is checked until {@link #build()}
 * runs.
 *
 * <pre>{@code
 * Transaction ride = Transaction.builder().cardType(CardType.AVANZA_TOP_UP).amount(550).consecutivePayments(1)
 * 		.stop(new Stop.Urban(500)).route(new Route(31)).kind(new TransactionKind.Journey(Direction.TWO)).dutyTrip(7)
 * 		.createdAt(CardDateTime.of(2026, 8, 26, 9, 41, 27)).sequence(1).build();
 * }</pre>
 */
public final class TransactionBuilder {

	private CardType cardType;
	private int networkFlag;
	private int amount;
	private int consecutivePayments;
	private Stop stop;
	private Route route;
	private TransactionKind kind;
	private int dutyTrip;
	private CardDateTime createdAt;
	private int sequence;

	TransactionBuilder() {
	}

	public TransactionBuilder cardType(CardType cardType) {
		this.cardType = cardType;
		return this;
	}

	public TransactionBuilder networkFlag(int networkFlag) {
		this.networkFlag = networkFlag;
		return this;
	}

	public TransactionBuilder amount(int amount) {
		this.amount = amount;
		return this;
	}

	public TransactionBuilder consecutivePayments(int consecutivePayments) {
		this.consecutivePayments = consecutivePayments;
		return this;
	}

	public TransactionBuilder stop(Stop stop) {
		this.stop = stop;
		return this;
	}

	public TransactionBuilder route(Route route) {
		this.route = route;
		return this;
	}

	public TransactionBuilder kind(TransactionKind kind) {
		this.kind = kind;
		return this;
	}

	public TransactionBuilder dutyTrip(int dutyTrip) {
		this.dutyTrip = dutyTrip;
		return this;
	}

	public TransactionBuilder createdAt(CardDateTime createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	public TransactionBuilder sequence(int sequence) {
		this.sequence = sequence;
		return this;
	}

	/**
	 * Builds the transaction.
	 *
	 * @return the transaction
	 * @throws IllegalArgumentException
	 *             if a field is outside the range its byte allows
	 * @throws NullPointerException
	 *             if a field that has no default was not set
	 */
	public Transaction build() {
		return new Transaction(cardType, networkFlag, amount, consecutivePayments, stop, route, kind, dutyTrip,
				createdAt, sequence);
	}
}
