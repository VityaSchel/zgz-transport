package dev.hloth.zgztransport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class JourneySummaryTest {

	@Test
	void decodesJourneySummaries() {
		for (Fixtures.Encoded<JourneySummary> testCase : Fixtures.journeySummaries()) {
			assertEquals(testCase.decoded(), JourneySummary.decode(Hex.bytes(testCase.hex())));
		}
	}

	@Test
	void encodesJourneySummaries() {
		for (Fixtures.Encoded<JourneySummary> testCase : Fixtures.journeySummaries()) {
			assertArrayEquals(Hex.bytes(testCase.hex()), testCase.decoded().encode());
		}
	}

	@Test
	void rejectsTheBlockPersonalCardsHold() {
		assertArrayEquals(Hex.bytes("000000000000000A000000000000000A"), JourneySummary.personalBlock());
		assertThrows(CardFormatException.class, () -> JourneySummary.decode(JourneySummary.personalBlock()));
	}

	@Test
	void rejectsNonZeroReservedBytes() {
		for (int index : new int[]{11, 12, 14}) {
			byte[] block = Hex.bytes(Fixtures.journeySummaries().get(0).hex());
			block[index] = 1;
			assertThrows(CardFormatException.class, () -> JourneySummary.decode(Hex.checksummed(block)));
		}
	}

	@Test
	void rejectsABlockThatDoesNotMatchItsChecksum() {
		byte[] block = Hex.bytes(Fixtures.journeySummaries().get(0).hex());
		block[15] ^= 1;
		assertThrows(CardFormatException.class, () -> JourneySummary.decode(block));
	}

	@Test
	void rejectsFieldsOutsideTheirRange() {
		byte[] base = Hex.bytes(Fixtures.journeySummaries().get(0).hex());
		for (int[] change : new int[][]{{4, 24}, {5, 60}, {7, 0x0b}, {10, 3}}) {
			byte[] block = base.clone();
			block[change[0]] = (byte) change[1];
			assertThrows(CardFormatException.class, () -> JourneySummary.decode(Hex.checksummed(block)));
		}
	}

	@Test
	void readsAPreviousLegOnlyWhenTheRouteIsSet() {
		JourneySummary first = Fixtures.journeySummaries().get(0).decoded();
		assertEquals(Optional.empty(), first.previous());
		JourneySummary second = Fixtures.journeySummaries().get(1).decoded();
		assertTrue(second.previous().isPresent());
		assertEquals(Route.TRAM, second.previous().orElseThrow().route());
		assertThrows(IllegalArgumentException.class, () -> new JourneySummary.Leg(new Route(0), Direction.ONE));
	}

	@Test
	void rejectsTheWrongNumberOfBytes() {
		assertThrows(CardFormatException.class, () -> JourneySummary.decode(new byte[8]));
	}
}
