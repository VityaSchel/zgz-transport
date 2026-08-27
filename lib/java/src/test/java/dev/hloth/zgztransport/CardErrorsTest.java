package dev.hloth.zgztransport;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CardErrorsTest {

	@Test
	void rejectsADumpThatIsNotWholeBlocksOrStopsTooEarly() {
		byte[] dump = Dumps.topUpCard().build().bytes();
		assertThrows(CardFormatException.class, () -> Card.decode(Arrays.copyOf(dump, 33 * 16)));
		assertThrows(CardFormatException.class, () -> Card.decode(Arrays.copyOf(dump, 35 * 16 + 1)));
	}

	@Test
	void reportsTheFirstFaultInBlockOrder() {
		byte[] dump = Dumps.topUpCard().build().bytes();
		dump[5] = 0;
		dump[9 * 16] ^= 1;
		dump[16] = 0x0b;
		dump[2 * 16 - 1] ^= 0x02 ^ 0x0b;
		assertMessage(dump, "SAK");
		dump[5] = (byte) 0x88;
		assertMessage(dump, "differ");
		dump[9 * 16] ^= 1;
		assertMessage(dump, "unknown card type");
	}

	@Test
	void propagatesProductBlockErrors() {
		byte[] dump = Dumps.personalCard().block(12, Hex.bytes(Fixtures.METADATA))
				.block(13, Hex.bytes(Fixtures.SUBSCRIPTION)).build().bytes();
		dump[14 * 16 - 1] ^= 1;
		assertMessage(dump, "checksum");
	}

	@Test
	void propagatesTransactionErrors() {
		byte[] dump = Dumps.topUpCard().block(5, Hex.bytes(Fixtures.transactions().get(0).hex())).build().bytes();
		dump[5 * 16 + 8] = 3;
		assertMessage(dump, "kind");
	}

	private static void assertMessage(byte[] dump, String fragment) {
		CardFormatException thrown = assertThrows(CardFormatException.class, () -> Card.decode(dump));
		assertTrue(thrown.getMessage().contains(fragment), "expected " + fragment + " in " + thrown.getMessage());
	}
}
