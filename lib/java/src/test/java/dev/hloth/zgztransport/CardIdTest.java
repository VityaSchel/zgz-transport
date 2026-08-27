package dev.hloth.zgztransport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class CardIdTest {

	private static final String BLOCK = "42451234560000000000000000000077";

	@Test
	void decodesAndEncodesTheIdOfBlockTwo() {
		CardId id = CardId.decode(Hex.bytes(BLOCK));
		assertEquals("BE123456", id.toString());
		assertEquals(id, CardId.parse("BE123456"));
		assertArrayEquals(Hex.bytes(BLOCK), CardId.parse("BE123456").encode());
		assertEquals(id.hashCode(), CardId.parse("BE123456").hashCode());
		assertNotEquals(id, CardId.parse("BE123457"));
	}

	@Test
	void roundTripsEveryLengthTheCardHolds() {
		for (String text : List.of("BE123456", "BE12345678", "CT123456", "BP1234567890123456")) {
			assertEquals(text, CardId.decode(CardId.parse(text).encode()).toString());
		}
	}

	@Test
	void fillsAllThirteenDigitBytes() {
		String longest = "BE12345678901234567890123456";
		assertEquals(longest, CardId.decode(CardId.parse(longest).encode()).toString());
	}

	@Test
	void trimsTheTrailingZeroBytesTheCardPadsWith() {
		assertEquals("BE120000", CardId.parse("BE12000000").toString());
		assertEquals("BE120000", CardId.parse("BE1200000000").toString());
	}

	@Test
	void rejectsMalformedIds() {
		for (String text : List.of("", "BE", "BE12345", "BE1234567", "be123456", "B3123456", "BE12345A",
				"BE123456789012345678901234567890", "ÉE123456")) {
			assertThrows(IllegalArgumentException.class, () -> CardId.parse(text));
		}
	}

	@Test
	void printsWhateverBytesAChecksummedBlockHolds() {
		byte[] block = Hex.checksummed(Hex.bytes("C9004ABF12000000000000000000000000".substring(0, 32)));
		CardId id = CardId.decode(block);
		assertEquals("\u00c9\u00004abf12", id.toString());
		assertThrows(IllegalArgumentException.class, () -> CardId.parse(id.toString()));
	}

	@Test
	void rejectsABlockThatDoesNotMatchItsChecksum() {
		byte[] block = Hex.bytes(BLOCK);
		block[15] ^= 1;
		assertThrows(CardFormatException.class, () -> CardId.decode(block));
		assertThrows(CardFormatException.class, () -> CardId.decode(new byte[17]));
	}
}
