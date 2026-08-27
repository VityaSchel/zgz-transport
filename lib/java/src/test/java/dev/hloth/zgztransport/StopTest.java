package dev.hloth.zgztransport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class StopTest {

	@Test
	void decodesStopsByNetwork() {
		assertEquals(new Stop.Urban(475), Stop.decode(Hex.bytes("81DB"), new Route(31)));
		assertEquals(new Stop.Urban(1), Stop.decode(Hex.bytes("8001"), new Route(35)));
		assertEquals(new Stop.Tram(1500), Stop.decode(Hex.bytes("05DC"), Route.TRAM));
		assertEquals(new Stop.Other(7980), Stop.decode(Hex.bytes("1F2C"), new Route(0)));
		assertEquals(new Stop.Other(5), Stop.decode(Hex.bytes("0005"), new Route(169)));
	}

	@Test
	void encodesStops() {
		assertArrayEquals(Hex.bytes("81DB"), new Stop.Urban(475).encode());
		assertArrayEquals(Hex.bytes("05DC"), new Stop.Tram(1500).encode());
		assertArrayEquals(Hex.bytes("1F2C"), new Stop.Other(7980).encode());
	}

	@Test
	void tellsTheTramStopNumberTheOperatorPrints() {
		assertEquals(15, new Stop.Tram(1500).number());
	}

	@Test
	void keepsTheUrbanFlagOutOfIds() {
		assertThrows(IllegalArgumentException.class, () -> new Stop.Urban(0x8000));
		assertThrows(IllegalArgumentException.class, () -> new Stop.Tram(0x8000));
		assertThrows(IllegalArgumentException.class, () -> new Stop.Other(0x8000));
		assertArrayEquals(Hex.bytes("FFFF"), new Stop.Urban(0x7fff).encode());
	}

	@Test
	void rejectsTheWrongNumberOfBytes() {
		assertThrows(CardFormatException.class, () -> Stop.decode(new byte[1], Route.TRAM));
	}
}
