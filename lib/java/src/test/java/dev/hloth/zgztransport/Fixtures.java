package dev.hloth.zgztransport;

import java.util.List;
import java.util.Optional;

final class Fixtures {

	static final String AVANZA_BLOCK_0 = "1D68C3A9BF880400C8000020000000AB";
	static final String LAZO_BLOCK_0 = "0468C3A9BF12341802008100000023AA";
	static final String METADATA = "1101342F00210000001E002100000015";
	static final String SUBSCRIPTION = "342F344E0000010203043441081E0006";

	private Fixtures() {
	}

	record Encoded<T>(String hex, T decoded) {
	}

	static List<Encoded<Transaction>> transactions() {
		return List.of(
				new Encoded<>("020002F8010320D201FE2AD8132D1201",
						journey(760, new Stop.Tram(800), 210, Direction.ONE, 254,
								CardDateTime.of(2021, 6, 24, 19, 45, 18), 1)),
				new Encoded<>("020002F80105DCD2026B2AD90D333B02",
						journey(760, new Stop.Tram(1500), 210, Direction.TWO, 107,
								CardDateTime.of(2021, 6, 25, 13, 51, 59), 2)),
				new Encoded<>("0200022601817E1F0211344D10163003",
						journey(550, new Stop.Urban(382), 31, Direction.TWO, 17,
								CardDateTime.of(2026, 2, 13, 16, 22, 48), 3)),
				new Encoded<>("020002260180011F0112344D14100704",
						journey(550, new Stop.Urban(1), 31, Direction.ONE, 18, CardDateTime.of(2026, 2, 13, 20, 16, 7),
								4)),
				new Encoded<>("02000226018001230209344E0C051B00",
						journey(550, new Stop.Urban(1), 35, Direction.TWO, 9, CardDateTime.of(2026, 2, 14, 12, 5, 27),
								0)),
				new Encoded<>("0200022601817F160105344E0D1F2001",
						journey(550, new Stop.Urban(383), 22, Direction.ONE, 5,
								CardDateTime.of(2026, 2, 14, 13, 31, 32), 1)),
				new Encoded<>("0200022601801E23020934540F153502",
						journey(550, new Stop.Urban(30), 35, Direction.TWO, 9, CardDateTime.of(2026, 2, 20, 15, 21, 53),
								2)),
				new Encoded<>("020002260180CE16010D3454102D2403",
						journey(550, new Stop.Urban(206), 22, Direction.ONE, 13,
								CardDateTime.of(2026, 2, 20, 16, 45, 36), 3)),
				new Encoded<>("02001388001F2C00080034840D123604",
						new Transaction(CardType.AVANZA_TOP_UP, 0, 5000, 0, new Stop.Other(7980), new Route(0),
								new TransactionKind.TopUp(), 0, CardDateTime.of(2026, 4, 4, 13, 18, 54), 4)),
				new Encoded<>("0A0100000181F40B0107346F09291B04",
						new Transaction(CardType.AVANZA_PERSONAL_UNLIMITED, 1, 0, 1, new Stop.Urban(500), new Route(11),
								new TransactionKind.Journey(Direction.ONE), 7, CardDateTime.of(2026, 3, 15, 9, 41, 27),
								4)),
				new Encoded<>("0D0000000105DCD202013518162C2000",
						new Transaction(CardType.LAZO_TOP_UP, 0, 0, 1, new Stop.Tram(1500), Route.TRAM,
								new TransactionKind.Journey(Direction.TWO), 1, CardDateTime.of(2026, 8, 24, 22, 44, 32),
								0)));
	}

	static List<Encoded<JourneySummary>> journeySummaries() {
		return List.of(
				new Encoded<>("00002ADC091B010200D2020000630054",
						summary(Optional.empty(), lastPaid(2021, 6, 28, 9, 27), 1, CardType.AVANZA_TOP_UP, false, 210,
								Direction.TWO, 0x63)),
				new Encoded<>("D2012AD90D33010200D20200006300AE",
						summary(leg(210, Direction.ONE), lastPaid(2021, 6, 25, 13, 51), 1, CardType.AVANZA_TOP_UP,
								false, 210, Direction.TWO, 0x63)),
				new Encoded<>("23023454102D0102001601000063000B",
						summary(leg(35, Direction.TWO), lastPaid(2026, 2, 20, 16, 45), 1, CardType.AVANZA_TOP_UP, false,
								22, Direction.ONE, 0x63)),
				new Encoded<>("16013468002B04020016020000630011",
						summary(leg(22, Direction.ONE), lastPaid(2026, 3, 8, 0, 43), 4, CardType.AVANZA_TOP_UP, false,
								22, Direction.TWO, 0x63)),
				new Encoded<>("1F013518160B010D01D2020000620091", summary(leg(31, Direction.ONE),
						lastPaid(2026, 8, 24, 22, 11), 1, CardType.LAZO_TOP_UP, true, 210, Direction.TWO, 0x62)));
	}

	private static Transaction journey(int amount, Stop stop, int route, Direction direction, int dutyTrip,
			CardDateTime createdAt, int sequence) {
		return new Transaction(CardType.AVANZA_TOP_UP, 0, amount, 1, stop, new Route(route),
				new TransactionKind.Journey(direction), dutyTrip, createdAt, sequence);
	}

	private static JourneySummary summary(Optional<JourneySummary.Leg> previous, JourneySummary.LastPaid lastPaidAt,
			int consecutivePayments, CardType cardType, boolean free, int route, Direction direction,
			int transfersLeft) {
		return new JourneySummary(previous, lastPaidAt, consecutivePayments, cardType, free, new Route(route),
				direction, transfersLeft);
	}

	private static Optional<JourneySummary.Leg> leg(int route, Direction direction) {
		return Optional.of(new JourneySummary.Leg(new Route(route), direction));
	}

	private static JourneySummary.LastPaid lastPaid(int year, int month, int day, int hour, int minute) {
		return new JourneySummary.LastPaid(new CardDate(year, month, day), hour, minute);
	}
}
