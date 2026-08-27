package dev.hloth.zgztransport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CardTypeTest {

	private static final List<Fixtures.Encoded<CardType>> CASES = List.of(
			new Fixtures.Encoded<>("02699F000000000000000000000000F4", CardType.AVANZA_TOP_UP),
			new Fixtures.Encoded<>("0A9775000000000000000000000000E8", CardType.AVANZA_PERSONAL_UNLIMITED),
			new Fixtures.Encoded<>("0D371F00000000000000000000000025", CardType.LAZO_TOP_UP));

	@Test
	void decodesCardTypes() {
		for (Fixtures.Encoded<CardType> testCase : CASES) {
			assertEquals(testCase.decoded(), CardType.decode(Hex.bytes(testCase.hex())));
		}
	}

	@Test
	void encodesCardTypes() {
		for (Fixtures.Encoded<CardType> testCase : CASES) {
			assertArrayEquals(Hex.bytes(testCase.hex()), testCase.decoded().encode());
		}
	}

	@Test
	void mapsValuesAndFirstBytesBackToTheProduct() {
		for (Fixtures.Encoded<CardType> testCase : CASES) {
			CardType type = testCase.decoded();
			assertEquals(type, CardType.ofValue(type.value()));
			assertEquals(type, CardType.ofFirstByte(type.firstByte()));
		}
		assertEquals(Chip.CLASSIC_1K, CardType.AVANZA_TOP_UP.chip());
		assertEquals(Chip.CLASSIC_4K, CardType.LAZO_TOP_UP.chip());
		assertThrows(CardFormatException.class, () -> CardType.ofFirstByte(0x0b));
		assertThrows(CardFormatException.class, () -> CardType.ofValue(0xff_ff_ff));
	}

	@Test
	void statesItsChipProductSectorsAndJourneySummaryAsData() {
		assertEquals(List.of(), CardType.AVANZA_TOP_UP.productSectors());
		assertEquals(List.of(3, 4), CardType.AVANZA_PERSONAL_UNLIMITED.productSectors());
		assertEquals(List.of(), CardType.LAZO_TOP_UP.productSectors());
		assertTrue(CardType.AVANZA_TOP_UP.recordsJourneySummary());
		assertFalse(CardType.AVANZA_PERSONAL_UNLIMITED.recordsJourneySummary());
		assertTrue(CardType.LAZO_TOP_UP.recordsJourneySummary());
	}

	@Test
	void checksTheChecksumThenThePaddingThenTheType() {
		byte[] block = Hex.bytes("FFFFFF01000000000000000000000000");
		assertThrows(CardFormatException.class, () -> CardType.decode(block));
		block[15] = (byte) 0xfe;
		assertThrows(CardFormatException.class, () -> CardType.decode(block));
		block[3] = 0;
		block[15] = (byte) 0xff;
		assertThrows(CardFormatException.class, () -> CardType.decode(block));
	}

	@Test
	void rejectsTheWrongNumberOfBytes() {
		assertThrows(CardFormatException.class, () -> CardType.decode(new byte[15]));
	}
}
