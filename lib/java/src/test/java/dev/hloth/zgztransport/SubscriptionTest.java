package dev.hloth.zgztransport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class SubscriptionTest {

	private static final String UNUSED = "342F344E000001020304000000000065";

	private static Subscription unused() {
		return new Subscription(new CardDate(2026, 1, 15), new CardDate(2026, 2, 14), 0, 0x0102_0304L,
				Optional.empty());
	}

	@Test
	void decodesAndEncodesAnUnusedSubscription() {
		assertEquals(unused(), Subscription.decode(Hex.bytes(UNUSED)));
		assertArrayEquals(Hex.bytes(UNUSED), unused().encode());
	}

	@Test
	void decodesAndEncodesAUsedSubscription() {
		Subscription used = new Subscription(unused().startsAt(), unused().endsAt(), 0, 0x0102_0304L,
				Optional.of(CardDateTime.of(2026, 2, 1, 8, 30, 0)));
		assertEquals(used, Subscription.decode(Hex.bytes(Fixtures.SUBSCRIPTION)));
		assertArrayEquals(Hex.bytes(Fixtures.SUBSCRIPTION), used.encode());
	}

	@Test
	void rejectsABlockThatDoesNotMatchItsChecksum() {
		byte[] block = Hex.bytes(Fixtures.SUBSCRIPTION);
		block[15] = (byte) (block[15] ^ 0x80);
		assertThrows(CardFormatException.class, () -> Subscription.decode(block));
	}

	@Test
	void rejectsAPartialLastUsage() {
		byte[] block = Hex.bytes(UNUSED);
		block[12] = 8;
		assertThrows(CardFormatException.class, () -> Subscription.decode(Hex.checksummed(block)));
	}

	@Test
	void rejectsFieldsOutsideTheirRange() {
		assertThrows(IllegalArgumentException.class,
				() -> new Subscription(unused().startsAt(), unused().endsAt(), 0x10000, 0, Optional.empty()));
		assertThrows(NullPointerException.class,
				() -> new Subscription(unused().startsAt(), unused().endsAt(), 0, 0, null));
	}
}
