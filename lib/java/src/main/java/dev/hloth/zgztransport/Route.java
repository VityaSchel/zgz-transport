package dev.hloth.zgztransport;

/**
 * Route id of the operator's GTFS feed, byte 7 of a transaction.
 *
 * @param id
 *            the route id; {@code 0} on top ups made off board
 */
public record Route(int id) {

	/** The tram, printed as {@code L1}. */
	public static final Route TRAM = new Route(210);

	public Route {
		Bytes.checkRange("route", id, 0, 0xff);
	}

	/**
	 * The name the operator prints: buses keep their number, {@code 11} to
	 * {@code 14} are {@code Ci1} to {@code Ci4}, {@code 111} to {@code 117} are
	 * {@code N1} to {@code N7} and the tram is {@code L1}.
	 *
	 * @return the public name of this route
	 */
	@Override
	public String toString() {
		if (id == TRAM.id) {
			return "L1";
		}
		if (id >= 11 && id <= 14) {
			return "Ci" + (id - 10);
		}
		if (id >= 111 && id <= 117) {
			return "N" + (id - 110);
		}
		return Integer.toString(id);
	}
}
