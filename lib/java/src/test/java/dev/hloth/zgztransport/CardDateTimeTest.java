package dev.hloth.zgztransport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class CardDateTimeTest {

	@Test
	void decodesAndEncodesDateAndTime() {
		CardDateTime moment = CardDateTime.of(2026, 2, 14, 12, 5, 27);
		assertEquals(moment, CardDateTime.decode(Hex.bytes("344E0C051B")));
		assertArrayEquals(Hex.bytes("344E0C051B"), moment.encode());
		assertEquals(LocalTime.of(12, 5, 27), moment.time());
	}

	@Test
	void rejectsFieldsOutsideTheirRange() {
		assertThrows(CardFormatException.class, () -> CardDateTime.decode(Hex.bytes("344E18051B")));
		assertThrows(CardFormatException.class, () -> CardDateTime.decode(Hex.bytes("344E0C3C1B")));
		assertThrows(CardFormatException.class, () -> CardDateTime.decode(Hex.bytes("00000C051B")));
		assertThrows(CardFormatException.class, () -> CardDateTime.decode(new byte[4]));
		assertThrows(IllegalArgumentException.class, () -> CardDateTime.of(2026, 1, 1, 24, 0, 0));
	}

	@Test
	void rejectsATimeTheCardCannotHold() {
		assertThrows(IllegalArgumentException.class,
				() -> new CardDateTime(new CardDate(2026, 1, 1), LocalTime.of(1, 2, 3, 4)));
	}

	@Test
	void ordersByDateThenTimeAndPrints() {
		assertTrue(CardDateTime.of(2026, 2, 14, 23, 59, 59).compareTo(CardDateTime.of(2026, 2, 15, 0, 0, 0)) < 0);
		assertTrue(CardDateTime.of(2026, 2, 14, 12, 5, 27).compareTo(CardDateTime.of(2026, 2, 14, 12, 5, 28)) < 0);
		assertEquals("2026-02-14 12:05:27", CardDateTime.of(2026, 2, 14, 12, 5, 27).toString());
		assertEquals("2026-02-14 12:05:00", CardDateTime.of(2026, 2, 14, 12, 5, 0).toString());
		assertEquals("2026-02-14 00:00:00", CardDateTime.of(2026, 2, 14, 0, 0, 0).toString());
	}
}
