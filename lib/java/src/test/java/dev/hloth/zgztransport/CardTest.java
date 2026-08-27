package dev.hloth.zgztransport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CardTest {

	@Test
	void decodesATopUpCard() {
		List<Fixtures.Encoded<Transaction>> log = Fixtures.transactions();
		byte[] dump = Dumps.topUpCard().block(5, Hex.bytes(log.get(7).hex()))
				.block(10, Hex.bytes(Fixtures.journeySummaries().get(2).hex())).block(28, Hex.bytes(log.get(4).hex()))
				.block(29, Hex.bytes(log.get(5).hex())).block(30, Hex.bytes(log.get(6).hex())).build().bytes();
		Card card = Card.decode(dump);
		assertEquals(Chip.CLASSIC_1K, card.uid().chip());
		assertEquals("1D68C3A9", card.uid().toString());
		assertEquals(CardType.AVANZA_TOP_UP, card.cardType());
		assertEquals("BE123456", card.id().toString());
		assertEquals(new Balance(4450), card.balance());
		assertEquals(List.of(log.get(4).decoded(), log.get(5).decoded(), log.get(6).decoded(), log.get(7).decoded()),
				card.transactions());
		assertEquals(Optional.of(Fixtures.journeySummaries().get(2).decoded()), card.journeySummary());
		assertEquals(List.of(), card.products());
	}

	@Test
	void decodesALazoCardWithoutJourneys() {
		byte[] dump = Dumps.lazoCard().build().bytes();
		Card card = Card.decode(dump);
		assertEquals(Chip.CLASSIC_4K, card.uid().chip());
		assertEquals("0468C3A9BF1234", card.uid().toString());
		assertEquals(CardType.LAZO_TOP_UP, card.cardType());
		assertEquals("CT123456", card.id().toString());
		assertEquals(new Balance(600), card.balance());
		assertEquals(List.of(), card.transactions());
		assertEquals(Optional.empty(), card.journeySummary());
	}

	@Test
	void keepsProductsInTheirSectors() {
		Product expected = new Product(4, SubscriptionMetadata.decode(Hex.bytes(Fixtures.METADATA)),
				Subscription.decode(Hex.bytes(Fixtures.SUBSCRIPTION)));
		byte[] dump = Dumps.personalCard().block(16, Hex.bytes(Fixtures.METADATA))
				.block(17, Hex.bytes(Fixtures.SUBSCRIPTION)).block(18, Hex.bytes(Fixtures.SUBSCRIPTION)).build()
				.bytes();
		Card card = Card.decode(dump);
		assertEquals(List.of(expected), card.products());
		assertEquals(Optional.empty(), card.journeySummary());
		assertEquals(new Balance(0), card.balance());

		byte[] both = Dumps.personalCard().block(12, Hex.bytes(Fixtures.METADATA))
				.block(13, Hex.bytes(Fixtures.SUBSCRIPTION)).block(16, Hex.bytes(Fixtures.METADATA))
				.block(17, Hex.bytes(Fixtures.SUBSCRIPTION)).build().bytes();
		assertEquals(List.of(3, 4), Card.decode(both).products().stream().map(Product::sector).toList());
	}

	@Test
	void ignoresProductBlocksOnATopUpCard() {
		byte[] dump = Dumps.topUpCard().block(12, Hex.bytes(Fixtures.METADATA))
				.block(13, Hex.bytes(Fixtures.SUBSCRIPTION)).build().bytes();
		assertEquals(List.of(), Card.decode(dump).products());
	}

	@Test
	void acceptsADumpThatStopsAfterTheLastBlockItReads() {
		byte[] dump = Dumps.topUpCard().build().bytes();
		byte[] shortest = java.util.Arrays.copyOf(dump, (Card.LAST_USED_BLOCK + 1) * 16);
		assertEquals(new Balance(4450), Card.decode(shortest).balance());
	}

	@Test
	void keepsItsListsImmutable() {
		Card card = Card.decode(Dumps.topUpCard().block(5, Hex.bytes(Fixtures.transactions().get(0).hex())).build());
		assertTrue(card.transactions().getClass().getName().contains("Immutable"));
	}
}
