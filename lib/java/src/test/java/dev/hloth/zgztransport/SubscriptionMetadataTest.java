package dev.hloth.zgztransport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class SubscriptionMetadataTest {

	private static List<Fixtures.Encoded<SubscriptionMetadata>> cases() {
		return List.of(
				new Fixtures.Encoded<>(Fixtures.METADATA,
						new SubscriptionMetadata(0x11, 1, new CardDate(2026, 1, 15), 0x0021_0000L, 30,
								0x00_2100_0000L)),
				new Fixtures.Encoded<>("2A0234410102030401F40506070809A9", new SubscriptionMetadata(0x2a, 2,
						new CardDate(2026, 2, 1), 0x0102_0304L, 500, 0x05_0607_0809L)));
	}

	@Test
	void decodesSubscriptionMetadata() {
		for (Fixtures.Encoded<SubscriptionMetadata> testCase : cases()) {
			assertEquals(testCase.decoded(), SubscriptionMetadata.decode(Hex.bytes(testCase.hex())));
		}
	}

	@Test
	void encodesSubscriptionMetadata() {
		for (Fixtures.Encoded<SubscriptionMetadata> testCase : cases()) {
			assertArrayEquals(Hex.bytes(testCase.hex()), testCase.decoded().encode());
		}
	}

	@Test
	void rejectsABlockThatDoesNotMatchItsChecksum() {
		byte[] block = Hex.bytes(Fixtures.METADATA);
		block[15] = 0;
		assertThrows(CardFormatException.class, () -> SubscriptionMetadata.decode(block));
	}

	@Test
	void rejectsAnInvalidPurchaseDate() {
		byte[] block = Hex.bytes(Fixtures.METADATA);
		block[3] = 0x20;
		assertThrows(CardFormatException.class, () -> SubscriptionMetadata.decode(Hex.checksummed(block)));
	}

	@Test
	void rejectsFieldsOutsideTheirRange() {
		CardDate date = new CardDate(2026, 1, 15);
		assertThrows(IllegalArgumentException.class, () -> new SubscriptionMetadata(0x100, 1, date, 0, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> new SubscriptionMetadata(1, 1, date, 0x1_0000_0000L, 0, 0));
		assertThrows(IllegalArgumentException.class,
				() -> new SubscriptionMetadata(1, 1, date, 0, 0, 0x1_00_0000_0000L));
	}
}
