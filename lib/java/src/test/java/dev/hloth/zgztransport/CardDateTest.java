package dev.hloth.zgztransport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CardDateTest {

	private static final List<Fixtures.Encoded<CardDate>> CASES = List.of(
			new Fixtures.Encoded<>("0022", new CardDate(2000, 1, 2)),
			new Fixtures.Encoded<>("1F7F", new CardDate(2015, 11, 31)),
			new Fixtures.Encoded<>("344E", new CardDate(2026, 2, 14)),
			new Fixtures.Encoded<>("3383", new CardDate(2025, 12, 3)));

	@Test
	void decodesDates() {
		for (Fixtures.Encoded<CardDate> testCase : CASES) {
			assertEquals(testCase.decoded(), CardDate.decode(Hex.bytes(testCase.hex())));
		}
	}

	@Test
	void encodesDates() {
		for (Fixtures.Encoded<CardDate> testCase : CASES) {
			assertArrayEquals(Hex.bytes(testCase.hex()), testCase.decoded().encode());
		}
	}

	@Test
	void coversTheWholeRangeOfTheSevenBitYear() {
		CardDate last = new CardDate(2127, 12, 31);
		assertArrayEquals(new byte[]{(byte) 0xff, (byte) 0x9f}, last.encode());
		assertEquals(last, CardDate.decode(new byte[]{(byte) 0xff, (byte) 0x9f}));
	}

	@Test
	void rejectsFieldsOutsideTheirRange() {
		assertThrows(CardFormatException.class, () -> CardDate.decode(Hex.bytes("0002")));
		assertThrows(CardFormatException.class, () -> CardDate.decode(Hex.bytes("0020")));
		assertThrows(CardFormatException.class, () -> CardDate.decode(Hex.bytes("01A1")));
		assertThrows(IllegalArgumentException.class, () -> new CardDate(1999, 1, 1));
		assertThrows(IllegalArgumentException.class, () -> new CardDate(2128, 1, 1));
		assertThrows(IllegalArgumentException.class, () -> new CardDate(2026, 13, 1));
		assertThrows(IllegalArgumentException.class, () -> new CardDate(2026, 1, 32));
	}

	@Test
	void rejectsTheWrongNumberOfBytes() {
		assertThrows(CardFormatException.class, () -> CardDate.decode(new byte[3]));
	}

	@Test
	void keepsTheDaysTheCalendarDoesNotHave() {
		CardDate february30 = CardDate.decode(Hex.bytes("345E"));
		assertEquals(new CardDate(2026, 2, 30), february30);
		assertEquals(Optional.empty(), february30.toLocalDate());
		assertEquals(Optional.of(LocalDate.of(2026, 2, 14)), new CardDate(2026, 2, 14).toLocalDate());
	}

	@Test
	void ordersAndPrintsDates() {
		assertTrue(new CardDate(2025, 12, 31).compareTo(new CardDate(2026, 1, 1)) < 0);
		assertTrue(new CardDate(2026, 1, 31).compareTo(new CardDate(2026, 2, 1)) < 0);
		assertEquals("2026-02-14", new CardDate(2026, 2, 14).toString());
	}
}
