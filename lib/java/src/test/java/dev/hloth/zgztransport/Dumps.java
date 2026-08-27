package dev.hloth.zgztransport;

final class Dumps {

	private Dumps() {
	}

	static Dump.Builder topUpCard() {
		return Dump.builder(Chip.CLASSIC_1K).block(0, Hex.bytes(Fixtures.AVANZA_BLOCK_0))
				.block(1, CardType.AVANZA_TOP_UP).block(2, CardId.parse("BE123456")).block(8, new Balance(4450))
				.block(9, new Balance(4450));
	}

	static Dump.Builder personalCard() {
		return Dump.builder(Chip.CLASSIC_1K).block(0, Hex.bytes(Fixtures.AVANZA_BLOCK_0))
				.block(1, CardType.AVANZA_PERSONAL_UNLIMITED).block(2, CardId.parse("BP123456"))
				.block(8, new Balance(0)).block(9, new Balance(0)).block(10, JourneySummary.personalBlock());
	}

	static Dump.Builder lazoCard() {
		return Dump.builder(Chip.CLASSIC_4K).block(0, Hex.bytes(Fixtures.LAZO_BLOCK_0)).block(1, CardType.LAZO_TOP_UP)
				.block(2, CardId.parse("CT123456")).block(8, new Balance(600)).block(9, new Balance(600));
	}
}
