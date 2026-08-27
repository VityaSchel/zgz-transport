package dev.hloth.zgztransport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class SectorKeysTest {

	private static Optional<SectorKeys> keys(String a, String b) {
		return Optional.of(new SectorKeys(Optional.ofNullable(a).map(Key::of), Optional.ofNullable(b).map(Key::of)));
	}

	@Test
	void givesAvanzaKeysPerSectorAndProduct() {
		Optional<SectorKeys> operator = keys("04000C0F0903", "0B02070A0409");
		Optional<SectorKeys> unused = keys("A0A1A2A3A4A5", "B0B1B2B3B4B5");
		for (int sector = 0; sector <= 8; sector++) {
			assertEquals(operator, CardType.AVANZA_TOP_UP.keys(sector));
			assertEquals(operator, CardType.AVANZA_PERSONAL_UNLIMITED.keys(sector));
		}
		for (int sector = 9; sector <= 15; sector++) {
			assertEquals(unused, CardType.AVANZA_TOP_UP.keys(sector));
			assertEquals(operator, CardType.AVANZA_PERSONAL_UNLIMITED.keys(sector));
		}
		assertEquals(Optional.empty(), CardType.AVANZA_TOP_UP.keys(16));
		assertEquals(Optional.empty(), CardType.AVANZA_PERSONAL_UNLIMITED.keys(16));
	}

	@Test
	void givesLazoKeysPerSector() {
		Optional<SectorKeys> shared = keys("4E303D402F20", "243372407C2E");
		for (int sector = 0; sector <= 31; sector++) {
			assertEquals(shared, CardType.LAZO_TOP_UP.keys(sector));
		}
		assertEquals(keys("216F5B212A7A", "44202E476E5B"), CardType.LAZO_TOP_UP.keys(32));
		assertEquals(keys("5148755C3427", "3C4520753758"), CardType.LAZO_TOP_UP.keys(33));
		assertEquals(keys(null, "206F7C4C4F36"), CardType.LAZO_TOP_UP.keys(34));
		assertEquals(keys("5246612E7C4B", null), CardType.LAZO_TOP_UP.keys(35));
		assertEquals(keys("354B39454861", "567D734C403C"), CardType.LAZO_TOP_UP.keys(36));
		assertEquals(keys("455D732C385F", "2426217B3B3B"), CardType.LAZO_TOP_UP.keys(37));
		Optional<SectorKeys> factory = keys("FFFFFFFFFFFF", "FFFFFFFFFFFF");
		assertEquals(factory, CardType.LAZO_TOP_UP.keys(38));
		assertEquals(factory, CardType.LAZO_TOP_UP.keys(39));
		assertEquals(Optional.empty(), CardType.LAZO_TOP_UP.keys(40));
	}

	@Test
	void everyProductCoversExactlyItsChipsSectors() {
		for (CardType type : CardType.values()) {
			for (int sector = 0; sector < type.chip().sectors(); sector++) {
				assertTrue(type.keys(sector).isPresent(), type + " has no keys for sector " + sector);
			}
			assertEquals(Optional.empty(), type.keys(type.chip().sectors()),
					type + " should stop at sector " + type.chip().sectors());
		}
		assertEquals(16, Chip.CLASSIC_1K.sectors());
		assertEquals(40, Chip.CLASSIC_4K.sectors());
	}

	@Test
	void keysAreValueObjectsPrintedAsHex() {
		Key key = Key.of("04000C0F0903");
		assertEquals(key, Key.of(Hex.bytes("04000C0F0903")));
		assertEquals(key.hashCode(), Key.of("04000c0f0903").hashCode());
		assertEquals("04000C0F0903", key.toString());
		assertArrayEquals(Hex.bytes("04000C0F0903"), key.bytes());
		key.bytes()[0] = 0x7f;
		assertEquals("04000C0F0903", key.toString());
		assertTrue(CardType.LAZO_TOP_UP.keys(34).orElseThrow().a().isEmpty());
	}

	@Test
	void rejectsKeysThatAreNotSixBytes() {
		assertThrows(CardFormatException.class, () -> Key.of(new byte[5]));
		assertThrows(IllegalArgumentException.class, () -> Key.of("04000C0F09"));
		assertThrows(IllegalArgumentException.class, () -> Key.of("04000C0F09ZZ"));
		assertThrows(IllegalArgumentException.class, () -> CardType.LAZO_TOP_UP.keys(-1));
	}
}
