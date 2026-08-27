package dev.hloth.zgztransport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RouteTest {

	@Test
	void namesRoutesLikeTheOperator() {
		assertEquals("L1", Route.TRAM.toString());
		assertEquals("Ci1", new Route(11).toString());
		assertEquals("Ci4", new Route(14).toString());
		assertEquals("N1", new Route(111).toString());
		assertEquals("N7", new Route(117).toString());
		assertEquals("31", new Route(31).toString());
	}

	@Test
	void keepsTheNumbersAroundTheNamedRanges() {
		assertEquals("10", new Route(10).toString());
		assertEquals("15", new Route(15).toString());
		assertEquals("110", new Route(110).toString());
		assertEquals("118", new Route(118).toString());
		assertEquals("0", new Route(0).toString());
		assertEquals("255", new Route(255).toString());
	}

	@Test
	void rejectsIdsOutsideAByte() {
		assertThrows(IllegalArgumentException.class, () -> new Route(-1));
		assertThrows(IllegalArgumentException.class, () -> new Route(256));
		assertEquals(210, Route.TRAM.id());
	}
}
