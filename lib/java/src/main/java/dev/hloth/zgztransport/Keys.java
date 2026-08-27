package dev.hloth.zgztransport;

import java.util.List;

final class Keys {

	static final SectorKeys AVANZA_OPERATOR = SectorKeys.of("04000C0F0903", "0B02070A0409");
	static final SectorKeys AVANZA_UNUSED = SectorKeys.of("A0A1A2A3A4A5", "B0B1B2B3B4B5");
	static final SectorKeys LAZO_SHARED = SectorKeys.of("4E303D402F20", "243372407C2E");
	static final SectorKeys FACTORY = SectorKeys.of("FFFFFFFFFFFF", "FFFFFFFFFFFF");

	/** The keys of the sectors {@code first} to {@code last}, both included. */
	record Range(int first, int last, SectorKeys keys) {
	}

	/**
	 * Sectors 9 to 15 are never personalised, so they keep the keys the factory
	 * shipped them with.
	 */
	static final List<Range> AVANZA_TOP_UP = List.of(new Range(0, 8, AVANZA_OPERATOR), new Range(9, 15, AVANZA_UNUSED));

	/** Personal cards carry the operator keys on every sector, used or not. */
	static final List<Range> AVANZA_PERSONAL = List.of(new Range(0, 15, AVANZA_OPERATOR));

	/** Sectors 34 and 35 have only revealed one of their two keys so far. */
	static final List<Range> LAZO = List.of(new Range(0, 31, LAZO_SHARED),
			new Range(32, 32, SectorKeys.of("216F5B212A7A", "44202E476E5B")),
			new Range(33, 33, SectorKeys.of("5148755C3427", "3C4520753758")),
			new Range(34, 34, SectorKeys.of(null, "206F7C4C4F36")),
			new Range(35, 35, SectorKeys.of("5246612E7C4B", null)),
			new Range(36, 36, SectorKeys.of("354B39454861", "567D734C403C")),
			new Range(37, 37, SectorKeys.of("455D732C385F", "2426217B3B3B")), new Range(38, 39, FACTORY));

	private Keys() {
	}
}
