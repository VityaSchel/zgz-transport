package dev.hloth.zgztransport;

import java.time.LocalTime;
import java.util.Objects;

/**
 * A {@link CardDate} followed by a time of day, five bytes in total: the packed
 * date, then hour, minute and second as plain binary.
 *
 * @param date
 *            the packed date, bytes 0 and 1
 * @param time
 *            the time of day, bytes 2 to 4, whole seconds
 */
public record CardDateTime(CardDate date, LocalTime time) implements Comparable<CardDateTime>, Encodable {

	/** Bytes a date and time take. */
	public static final int BYTES = 5;

	/**
	 * Checks that both fields are present and that the time has no fraction of a
	 * second.
	 *
	 * @throws IllegalArgumentException
	 *             if the time carries nanoseconds, which the card cannot hold
	 * @throws NullPointerException
	 *             if a field is null
	 */
	public CardDateTime {
		Objects.requireNonNull(date, "date");
		Objects.requireNonNull(time, "time");
		if (time.getNano() != 0) {
			throw new IllegalArgumentException("time must be whole seconds, got " + time);
		}
	}

	/**
	 * Builds a date and time from its fields.
	 *
	 * @param year
	 *            {@code 2000} to {@code 2127}
	 * @param month
	 *            {@code 1} to {@code 12}
	 * @param day
	 *            {@code 1} to {@code 31}
	 * @param hour
	 *            {@code 0} to {@code 23}
	 * @param minute
	 *            {@code 0} to {@code 59}
	 * @param second
	 *            {@code 0} to {@code 59}
	 * @return the date and time
	 * @throws IllegalArgumentException
	 *             if a field is outside its range
	 */
	public static CardDateTime of(int year, int month, int day, int hour, int minute, int second) {
		return new CardDateTime(new CardDate(year, month, day), time(hour, minute, second));
	}

	/**
	 * Decodes the five bytes of a packed date and time.
	 *
	 * @param bytes
	 *            the five bytes
	 * @return the date and time they hold
	 * @throws CardFormatException
	 *             if the input is not five bytes or a field is outside its range
	 */
	public static CardDateTime decode(byte[] bytes) {
		Bytes.exact(bytes, BYTES, "date and time");
		byte[] date = {bytes[0], bytes[1]};
		try {
			return new CardDateTime(CardDate.decode(date),
					time(Bytes.u8(bytes[2]), Bytes.u8(bytes[3]), Bytes.u8(bytes[4])));
		} catch (CardFormatException alreadyDescribed) {
			throw alreadyDescribed;
		} catch (IllegalArgumentException cause) {
			throw new CardFormatException(cause.getMessage());
		}
	}

	/**
	 * Encodes this date and time into five bytes.
	 *
	 * @return the five bytes
	 */
	public byte[] encode() {
		byte[] bytes = new byte[BYTES];
		System.arraycopy(date.encode(), 0, bytes, 0, CardDate.BYTES);
		bytes[2] = (byte) time.getHour();
		bytes[3] = (byte) time.getMinute();
		bytes[4] = (byte) time.getSecond();
		return bytes;
	}

	private static LocalTime time(int hour, int minute, int second) {
		return LocalTime.of(Bytes.checkRange("hour", hour, 0, 23), Bytes.checkRange("minute", minute, 0, 59),
				Bytes.checkRange("second", second, 0, 59));
	}

	@Override
	public int compareTo(CardDateTime other) {
		int byDate = date.compareTo(other.date);
		return byDate != 0 ? byDate : time.compareTo(other.time);
	}

	/**
	 * The date and time in {@code YYYY-MM-DD HH:MM:SS} form, with the seconds
	 * always written.
	 *
	 * @return the printed date and time
	 */
	@Override
	public String toString() {
		return String.format("%s %02d:%02d:%02d", date, time.getHour(), time.getMinute(), time.getSecond());
	}
}
