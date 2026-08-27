package dev.hloth.zgztransport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class TransactionTest {

	@Test
	void decodesTransactions() {
		for (Fixtures.Encoded<Transaction> testCase : Fixtures.transactions()) {
			assertEquals(testCase.decoded(), Transaction.decode(Hex.bytes(testCase.hex())));
		}
	}

	@Test
	void encodesTransactions() {
		for (Fixtures.Encoded<Transaction> testCase : Fixtures.transactions()) {
			assertArrayEquals(Hex.bytes(testCase.hex()), testCase.decoded().encode());
		}
	}

	@Test
	void tellsFreeJourneysFromPaidOnesAndTopUps() {
		Transaction paid = Fixtures.transactions().get(0).decoded();
		Transaction topUp = Fixtures.transactions().get(8).decoded();
		Transaction transfer = Fixtures.transactions().get(10).decoded();
		assertFalse(paid.isFree());
		assertFalse(topUp.isFree());
		assertTrue(transfer.isFree());
		assertFalse(new Transaction(topUp.cardType(), 0, 0, 0, topUp.stop(), topUp.route(), new TransactionKind.TopUp(),
				0, topUp.createdAt(), 0).isFree());
	}

	@Test
	void tellsATransferFromACheckOut() {
		Transaction transfer = Transaction.decode(Hex.bytes("0D0000000105DCD202013518162C2000"));
		Transaction checkOut = Transaction.decode(Hex.bytes("0D000000002303A9010234FE09331E01"));
		Transaction pass = Transaction.decode(Hex.bytes("0A0200000180010C021134590F1A0302"));
		assertTrue(transfer.isTransfer());
		assertFalse(transfer.isCheckOut());
		assertTrue(checkOut.isCheckOut());
		assertFalse(checkOut.isTransfer());
		assertTrue(pass.isFree());
		assertFalse(pass.isTransfer());
	}

	@Test
	void givesTheDirectionOfAJourneyOnly() {
		assertEquals(Optional.of(Direction.ONE), Fixtures.transactions().get(0).decoded().direction());
		assertEquals(Optional.empty(), Fixtures.transactions().get(8).decoded().direction());
	}

	@Test
	void rejectsAnUnknownKindByte() {
		byte[] block = Hex.bytes(Fixtures.transactions().get(0).hex());
		block[8] = 3;
		assertThrows(CardFormatException.class, () -> Transaction.decode(block));
		block[8] = 0;
		assertThrows(CardFormatException.class, () -> Transaction.decode(block));
	}

	@Test
	void checksTheCardTypeThenTheTimestampThenTheKind() {
		byte[] block = Hex.bytes(Fixtures.transactions().get(0).hex());
		block[0] = 0x0b;
		block[12] = 24;
		block[8] = 3;
		CardFormatException unknownType = assertThrows(CardFormatException.class, () -> Transaction.decode(block));
		assertTrue(unknownType.getMessage().contains("card type byte"), unknownType.getMessage());
		block[0] = 0x02;
		CardFormatException badHour = assertThrows(CardFormatException.class, () -> Transaction.decode(block));
		assertTrue(badHour.getMessage().contains("hour"), badHour.getMessage());
		block[12] = 19;
		CardFormatException badKind = assertThrows(CardFormatException.class, () -> Transaction.decode(block));
		assertTrue(badKind.getMessage().contains("kind"), badKind.getMessage());
	}

	@Test
	void rejectsFieldsOutsideTheirRange() {
		Transaction ride = Fixtures.transactions().get(0).decoded();
		assertThrows(IllegalArgumentException.class, () -> new Transaction(ride.cardType(), 0, 0x10000, 1, ride.stop(),
				ride.route(), ride.kind(), 0, ride.createdAt(), 0));
		assertThrows(NullPointerException.class,
				() -> new Transaction(ride.cardType(), 0, 0, 1, ride.stop(), ride.route(), ride.kind(), 0, null, 0));
	}

	@Test
	void buildsTransactionsFieldByField() {
		Transaction ride = Fixtures.transactions().get(0).decoded();
		assertEquals(ride, Transaction.builder().cardType(ride.cardType()).amount(ride.amount())
				.consecutivePayments(ride.consecutivePayments()).stop(ride.stop()).route(ride.route()).kind(ride.kind())
				.dutyTrip(ride.dutyTrip()).createdAt(ride.createdAt()).sequence(ride.sequence()).build());
	}

	@Test
	void theBuilderStartsAtTheValuesATopUpCarries() {
		Transaction topUp = Transaction.builder().cardType(CardType.AVANZA_TOP_UP).stop(new Stop.Other(7980))
				.route(new Route(0)).kind(new TransactionKind.TopUp())
				.createdAt(CardDateTime.of(2026, 4, 4, 13, 18, 54)).build();
		assertEquals(0, topUp.networkFlag());
		assertEquals(0, topUp.amount());
		assertEquals(0, topUp.consecutivePayments());
		assertEquals(0, topUp.dutyTrip());
		assertEquals(0, topUp.sequence());
		assertThrows(NullPointerException.class, () -> Transaction.builder().build());
	}

	@Test
	void rejectsTheWrongNumberOfBytes() {
		assertThrows(CardFormatException.class, () -> Transaction.decode(new byte[32]));
	}
}
