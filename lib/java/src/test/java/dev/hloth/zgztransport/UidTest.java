package dev.hloth.zgztransport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UidTest {

	@Test
	void decodesBlockZeroBySak() {
		Uid single = Uid.decode(Hex.bytes(Fixtures.AVANZA_BLOCK_0));
		assertEquals(Chip.CLASSIC_1K, single.chip());
		assertEquals("1D68C3A9", single.toString());
		Uid twin = Uid.decode(Hex.bytes(Fixtures.LAZO_BLOCK_0));
		assertEquals(Chip.CLASSIC_4K, twin.chip());
		assertEquals("0468C3A9BF1234", twin.toString());
	}

	@Test
	void readsTheFourKilobyteSakBeforeTheOneKilobyteOne() {
		Uid uid = Uid.decode(Hex.bytes("0468C3A9BF88341802008100000023AA"));
		assertEquals(Chip.CLASSIC_4K, uid.chip());
		assertEquals("0468C3A9BF8834", uid.toString());
	}

	@Test
	void printsBytesBelowSixteenWithTwoDigits() {
		assertEquals("04000AFF", Uid.of(Hex.bytes("04000AFF")).toString());
		assertEquals("04000AFF010203", Uid.of(Hex.bytes("04000AFF010203")).toString());
	}

	@Test
	void isAValueObjectThatCopiesItsBytes() {
		Uid uid = Uid.of(Hex.bytes("1D68C3A9"));
		assertEquals(uid, Uid.decode(Hex.bytes(Fixtures.AVANZA_BLOCK_0)));
		assertEquals(uid.hashCode(), Uid.of(Hex.bytes("1D68C3A9")).hashCode());
		assertArrayEquals(Hex.bytes("1D68C3A9"), uid.bytes());
		uid.bytes()[0] = 0;
		assertEquals("1D68C3A9", uid.toString());
	}

	@Test
	void picksTheChipByUidLength() {
		assertEquals(Optional.of(Chip.CLASSIC_1K), Chip.ofUidLength(4));
		assertEquals(Optional.of(Chip.CLASSIC_4K), Chip.ofUidLength(7));
		assertEquals(Optional.empty(), Chip.ofUidLength(5));
		for (Chip chip : Chip.values()) {
			assertEquals(chip, Uid.of(new byte[chip.uidLength()]).chip());
		}
	}

	@Test
	void triesTheLongerUidFirst() {
		assertEquals(List.of(Chip.CLASSIC_4K, Chip.CLASSIC_1K), Chip.DETECTION_ORDER);
	}

	@Test
	void rejectsBlocksWithNoKnownSak() {
		byte[] block = Hex.bytes(Fixtures.AVANZA_BLOCK_0);
		block[5] = 0;
		assertThrows(CardFormatException.class, () -> Uid.decode(block));
		assertThrows(CardFormatException.class, () -> Uid.decode(new byte[15]));
		assertThrows(CardFormatException.class, () -> Uid.of(new byte[5]));
	}
}
