package dev.hloth.zgztransport;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * A date as the card packs it into two bytes: 7 bits of year since 2000, 4 bits
 * of month and 5 bits of day.
 *
 * <p>
 * The card does not check the calendar, so a day of 31 is accepted in any
 * month; this type accepts the same values instead of rejecting dumps that hold
 * them. Use {@link #toLocalDate()} to reach a calendar date.
 *
 * @param year
 *            {@code 2000} to {@code 2127}
 * @param month
 *            {@code 1} to {@code 12}
 * @param day
 *            {@code 1} to {@code 31}
 */
public record CardDate(int year, int month, int day) implements Comparable<CardDate>, Encodable {

	/** Bytes a packed date takes. */
	public static final int BYTES = 2;

	/**
	 * Checks the ranges of the three fields.
	 *
	 * @throws IllegalArgumentException
	 *             if a field is outside the range the bits allow
	 */
	public CardDate {
		Bytes.checkRange("year", year, 2000, 2127);
		Bytes.checkRange("month", month, 1, 12);
		Bytes.checkRange("day", day, 1, 31);
	}

	/**
	 * Decodes the two big-endian bytes of a packed date.
	 *
	 * @param bytes
	 *            the two bytes
	 * @return the date they hold
	 * @throws CardFormatException
	 *             if the input is not two bytes or holds a zero month or day
	 */
	public static CardDate decode(byte[] bytes) {
		Bytes.exact(bytes, BYTES, "date");
		int packed = Bytes.u16(bytes, 0);
		try {
			return new CardDate(2000 + (packed >> 9), (packed >> 5) & 0x0f, packed & 0x1f);
		} catch (IllegalArgumentException cause) {
			throw new CardFormatException(cause.getMessage());
		}
	}

	/**
	 * Encodes this date into two big-endian bytes.
	 *
	 * @return the two bytes
	 */
	public byte[] encode() {
		byte[] bytes = new byte[BYTES];
		Bytes.write(bytes, 0, ((long) (year - 2000) << 9) | ((long) month << 5) | day, BYTES);
		return bytes;
	}

	/**
	 * This date as a calendar date.
	 *
	 * @return the calendar date, or empty when the card holds a day the month does
	 *         not have
	 */
	public Optional<LocalDate> toLocalDate() {
		try {
			return Optional.of(LocalDate.of(year, month, day));
		} catch (DateTimeException outsideTheCalendar) {
			return Optional.empty();
		}
	}

	@Override
	public int compareTo(CardDate other) {
		int byYear = Integer.compare(year, other.year);
		if (byYear != 0) {
			return byYear;
		}
		int byMonth = Integer.compare(month, other.month);
		return byMonth != 0 ? byMonth : Integer.compare(day, other.day);
	}

	/**
	 * The date in {@code YYYY-MM-DD} form.
	 *
	 * @return the printed date
	 */
	@Override
	public String toString() {
		return String.format("%04d-%02d-%02d", year, month, day);
	}
}
