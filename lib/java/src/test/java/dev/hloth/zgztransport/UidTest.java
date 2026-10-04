package dev.hloth.zgztransport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UidTest {

	@Test
	void decodesBlockZeroBySakAndAtqa() {
		Uid single = Uid.decode(Hex.bytes(Fixtures.AVANZA_BLOCK_0));
		assertEquals(Chip.CLASSIC_1K, single.chip());
		assertEquals("1D68C3A9", single.toString());
		Uid twin = Uid.decode(Hex.bytes(Fixtures.LAZO_BLOCK_0));
		assertEquals(Chip.CLASSIC_4K, twin.chip());
		assertEquals("0468C3A9BF1234", twin.toString());
	}

	@Test
	void readsAFourByteUidOnAFourKilobyteChip() {
		Uid uid = Uid.decode(Hex.bytes(Fixtures.LAZO_FOUR_BYTE_BLOCK_0));
		assertEquals(Chip.CLASSIC_4K, uid.chip());
		assertEquals("0468C3A9", uid.toString());
		assertEquals(4, uid.bytes().length);
	}

	@Test
	void takesTheLengthFromBlockZeroAndNotFromTheChip() {
		assertEquals(4, Uid.decode(Hex.bytes(Fixtures.LAZO_FOUR_BYTE_BLOCK_0)).bytes().length);
		assertEquals(7, Uid.decode(Hex.bytes(Fixtures.LAZO_BLOCK_0)).bytes().length);
		assertEquals(Chip.CLASSIC_4K, Uid.of(Hex.bytes("1D68C3A9"), Chip.CLASSIC_4K).chip());
		assertEquals(Chip.CLASSIC_1K, Uid.of(Hex.bytes("1D68C3A9"), Chip.CLASSIC_1K).chip());
	}

	@Test
	void readsTheSevenByteLayoutBeforeTheFourByteOne() {
		byte[] block = Hex.bytes("0468C3A9BF88341802008100000023AA");
		Uid uid = Uid.decode(block);
		assertEquals(Chip.CLASSIC_4K, uid.chip());
		assertEquals("0468C3A9BF8834", uid.toString());

		block[5] = 0x18;
		Uid collision = Uid.decode(block);
		assertEquals(Chip.CLASSIC_4K, collision.chip());
		assertEquals("0468C3A9BF1834", collision.toString());
	}

	@Test
	void takesAFourByteUidOnlyWithItsBcc() {
		byte[] block = Hex.bytes(Fixtures.LAZO_FOUR_BYTE_BLOCK_0);
		block[4] ^= 1;
		assertThrows(CardFormatException.class, () -> Uid.decode(block));
	}

	@Test
	void printsBytesBelowSixteenWithTwoDigits() {
		assertEquals("04000AFF", Uid.of(Hex.bytes("04000AFF"), Chip.CLASSIC_1K).toString());
		assertEquals("04000AFF010203", Uid.of(Hex.bytes("04000AFF010203"), Chip.CLASSIC_4K).toString());
	}

	@Test
	void isAValueObjectThatCopiesItsBytes() {
		Uid uid = Uid.of(Hex.bytes("1D68C3A9"), Chip.CLASSIC_1K);
		assertEquals(uid, Uid.decode(Hex.bytes(Fixtures.AVANZA_BLOCK_0)));
		assertEquals(uid.hashCode(), Uid.of(Hex.bytes("1D68C3A9"), Chip.CLASSIC_1K).hashCode());
		assertArrayEquals(Hex.bytes("1D68C3A9"), uid.bytes());
		uid.bytes()[0] = 0;
		assertEquals("1D68C3A9", uid.toString());
		assertNotEquals(uid, Uid.of(Hex.bytes("1D68C3A9"), Chip.CLASSIC_4K));
	}

	@Test
	void rejectsBlocksWithNoKnownSak() {
		byte[] block = Hex.bytes(Fixtures.AVANZA_BLOCK_0);
		block[5] = 0;
		CardFormatException thrown = assertThrows(CardFormatException.class, () -> Uid.decode(block));
		assertEquals(
				"block 0 matches no known chip: byte 7 is 00 and byte 5 is 00, expected SAK 18 with ATQA 0200"
						+ " or SAK 88 with ATQA 0400 after the UID, and a valid BCC on a 4-byte UID",
				thrown.getMessage());
		assertThrows(CardFormatException.class, () -> Uid.decode(new byte[15]));
		assertThrows(CardFormatException.class, () -> Uid.of(new byte[5], Chip.CLASSIC_1K));
		assertThrows(NullPointerException.class, () -> Uid.of(new byte[4], null));
	}

	@Test
	void namesTheBytesItProbed() {
		byte[] block = Hex.bytes(Fixtures.LAZO_FOUR_BYTE_BLOCK_0);
		block[5] = 0x44;
		CardFormatException thrown = assertThrows(CardFormatException.class, () -> Uid.decode(block));
		assertTrue(thrown.getMessage().contains("byte 7 is 00 and byte 5 is 44"), thrown.getMessage());
	}
}
