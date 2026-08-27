package dev.hloth.zgztransport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class BalanceTest {

	private static final List<Fixtures.Encoded<Balance>> CASES = List.of(
			new Fixtures.Encoded<>("58020000A7FDFFFF5802000002FD02FD", new Balance(600)),
			new Fixtures.Encoded<>("8813000077ECFFFF8813000002FD02FD", new Balance(5000)),
			new Fixtures.Encoded<>("00000000FFFFFFFF0000000002FD02FD", new Balance(0)));

	@Test
	void decodesBalances() {
		for (Fixtures.Encoded<Balance> testCase : CASES) {
			assertEquals(testCase.decoded(), Balance.decode(Hex.bytes(testCase.hex())));
		}
	}

	@Test
	void encodesBalances() {
		for (Fixtures.Encoded<Balance> testCase : CASES) {
			assertArrayEquals(Hex.bytes(testCase.hex()), testCase.decoded().encode());
		}
	}

	@Test
	void encodesEveryBalanceTheValueBlockHolds() {
		assertArrayEquals(Hex.bytes("FFFFFF7F00000080FFFFFF7F02FD02FD"), new Balance(0x7fff_ffffL).encode());
		assertArrayEquals(Hex.bytes("FFFFFFFF00000000FFFFFFFF02FD02FD"), new Balance(Balance.MAX_UNITS).encode());
		assertThrows(IllegalArgumentException.class, () -> new Balance(Balance.MAX_UNITS + 1));
		assertThrows(IllegalArgumentException.class, () -> new Balance(-1));
	}

	@Test
	void rejectsABlockThatDoesNotAgreeWithItself() {
		byte[] complement = Hex.bytes(CASES.get(1).hex());
		complement[4] ^= 1;
		assertThrows(CardFormatException.class, () -> Balance.decode(complement));
		byte[] copy = Hex.bytes(CASES.get(1).hex());
		copy[8] ^= 1;
		assertThrows(CardFormatException.class, () -> Balance.decode(copy));
	}

	@Test
	void readsEveryValueTheValueBlockHolds() {
		assertEquals(new Balance(Balance.MAX_UNITS), Balance.decode(Hex.bytes("FFFFFFFF00000000FFFFFFFF02FD02FD")));
		assertEquals("4294967.295", new Balance(Balance.MAX_UNITS).toString());
	}

	@Test
	void printsEurosWithThreeDecimals() {
		assertEquals(1000, Balance.UNITS_PER_EURO);
		assertEquals("4.450", new Balance(4450).toString());
		assertEquals("0.600", new Balance(600).toString());
		assertEquals("0.000", new Balance(0).toString());
		assertEquals(new BigDecimal("4.450"), new Balance(4450).euros());
	}
}
