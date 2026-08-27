package dev.hloth.zgztransport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DumpTest {

	@Test
	void addressesBlocksWithoutIndexArithmetic() {
		Dump dump = Dumps.topUpCard().block(5, Hex.bytes(Fixtures.transactions().get(0).hex())).build();
		assertEquals(Chip.CLASSIC_1K.blocks(), dump.blockCount());
		assertArrayEquals(Hex.bytes(Fixtures.AVANZA_BLOCK_0), dump.block(0));
		assertEquals(Fixtures.transactions().get(0).decoded(),
				Transaction.decode(dump.block(TransactionLog.LIVE_BLOCK)));
		assertArrayEquals(new byte[16], dump.block(63));
	}

	@Test
	void takesTheSizeOfTheChip() {
		assertEquals(64, Dump.builder(Chip.CLASSIC_1K).build().blockCount());
		assertEquals(256, Dump.builder(Chip.CLASSIC_4K).build().blockCount());
		assertEquals(64, Chip.CLASSIC_1K.blocks());
		assertEquals(256, Chip.CLASSIC_4K.blocks());
	}

	@Test
	void writesAnyEncodableIntoABlock() {
		Dump dump = Dump.builder(Chip.CLASSIC_1K).block(1, CardType.LAZO_TOP_UP).block(2, CardId.parse("CT123456"))
				.block(8, new Balance(600)).build();
		assertEquals(CardType.LAZO_TOP_UP, CardType.decode(dump.block(1)));
		assertEquals("CT123456", CardId.decode(dump.block(2)).toString());
		assertEquals(new Balance(600), Balance.decode(dump.block(8)));
	}

	@Test
	void readsTheCardItHolds() {
		Dump dump = Dumps.topUpCard().build();
		assertEquals(Card.decode(dump), dump.card());
		assertEquals(Card.decode(dump.bytes()), dump.card());
	}

	@Test
	void copiesTheBytesItIsGivenAndHandsOut() {
		byte[] bytes = Dumps.topUpCard().build().bytes();
		Dump dump = Dump.of(bytes);
		bytes[0] = 0x7f;
		assertNotEquals(0x7f, dump.block(0)[0]);
		dump.block(0)[0] = 0x7f;
		assertNotEquals(0x7f, dump.block(0)[0]);
		assertEquals(dump, Dump.of(dump.bytes()));
		assertEquals(dump.hashCode(), Dump.of(dump.bytes()).hashCode());
	}

	@Test
	void rejectsBytesThatAreNotWholeBlocks() {
		assertThrows(CardFormatException.class, () -> Dump.of(new byte[17]));
		assertThrows(CardFormatException.class, () -> Dump.of(new byte[0]));
		Dump dump = Dump.builder(Chip.CLASSIC_1K).build();
		assertThrows(CardFormatException.class, () -> dump.block(64));
		assertThrows(CardFormatException.class, () -> dump.block(-1));
		assertThrows(CardFormatException.class, () -> Dump.builder(Chip.CLASSIC_1K).block(64, new Balance(0)));
		assertThrows(CardFormatException.class, () -> Dump.builder(Chip.CLASSIC_1K).block(0, new byte[15]));
	}
}
