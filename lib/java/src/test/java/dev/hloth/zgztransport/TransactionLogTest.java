package dev.hloth.zgztransport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class TransactionLogTest {

	private static final String EMPTY = "00000000000000000000000000000000";
	private static final String STUB = "00000000000000000000000000000004";

	private static byte[][] ring(String... slots) {
		byte[][] blocks = new byte[slots.length][];
		for (int slot = 0; slot < slots.length; slot++) {
			blocks[slot] = Hex.bytes(slots[slot]);
		}
		return blocks;
	}

	private static List<Transaction> expected(int... indices) {
		List<Fixtures.Encoded<Transaction>> fixtures = Fixtures.transactions();
		List<Transaction> transactions = new ArrayList<>();
		for (int index : indices) {
			transactions.add(fixtures.get(index).decoded());
		}
		return transactions;
	}

	private static String hex(int index) {
		return Fixtures.transactions().get(index).hex();
	}

	@Test
	void mapsSequenceNumbersToArchiveBlocks() {
		assertEquals(5, TransactionLog.LIVE_BLOCK);
		assertEquals(List.of(28, 29, 30, 32, 33), TransactionLog.ARCHIVE_BLOCKS);
		assertEquals(List.of(5, 28, 29, 30, 32, 33), TransactionLog.BLOCKS);
		assertEquals(OptionalInt.of(28), TransactionLog.archiveBlock(0));
		assertEquals(OptionalInt.of(33), TransactionLog.archiveBlock(4));
		assertEquals(OptionalInt.empty(), TransactionLog.archiveBlock(5));
		assertEquals(OptionalInt.empty(), TransactionLog.archiveBlock(-1));
	}

	@Test
	void ordersTheRingByTimeAndSkipsEmptySlots() {
		byte[][] ring = ring(hex(7), hex(4), hex(5), hex(6), EMPTY, STUB);
		assertEquals(expected(4, 5, 6, 7), TransactionLog.decode(ring));
	}

	@Test
	void sortsRegardlessOfSlotOrder() {
		byte[][] ring = ring(hex(0), hex(8), hex(3), hex(1), hex(2), hex(10));
		assertEquals(expected(0, 1, 2, 3, 8, 10), TransactionLog.decode(ring));
	}

	@Test
	void keepsSlotOrderOnEqualTimestamps() {
		String twin = "02000226018002230209344E0C051B03";
		assertEquals(List.of(0, 3), TransactionLog.decode(ring(hex(4), twin, STUB, STUB, STUB, STUB)).stream()
				.map(Transaction::sequence).toList());
		assertEquals(List.of(3, 0), TransactionLog.decode(ring(twin, hex(4), STUB, STUB, STUB, STUB)).stream()
				.map(Transaction::sequence).toList());
	}

	@Test
	void ordersImpossibleDatesWithoutNormalizingThem() {
		String february30 = "02000226018001230209345E0C051B00";
		String march1 = "0200022601800123020934610C051B01";
		assertEquals(List.of("2026-02-30", "2026-03-01"),
				TransactionLog.decode(ring(february30, march1, STUB, STUB, STUB, STUB)).stream()
						.map(transaction -> transaction.createdAt().date().toString()).toList());
	}

	@Test
	void propagatesDecodingErrors() {
		byte[][] ring = ring(hex(7), STUB, hex(0), STUB, STUB, STUB);
		ring[2][8] = 3;
		assertThrows(CardFormatException.class, () -> TransactionLog.decode(ring));
	}

	@Test
	void rejectsARingThatIsNotSixSlots() {
		assertThrows(CardFormatException.class, () -> TransactionLog.decode(ring(STUB, STUB, STUB)));
		assertThrows(CardFormatException.class, () -> TransactionLog.decode(
				new byte[][]{new byte[15], new byte[16], new byte[16], new byte[16], new byte[16], new byte[16]}));
	}
}
