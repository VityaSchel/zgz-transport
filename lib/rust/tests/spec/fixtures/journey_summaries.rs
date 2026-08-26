use zgz_transport::{CardType, Date, Direction, JourneySummary, LastPaidAt, Leg, Route};

fn paid(year: u16, month: u8, day: u8, hour: u8, minute: u8) -> LastPaidAt {
	LastPaidAt {
		date: Date { year, month, day },
		hour,
		minute,
	}
}

fn leg(route: u8, direction: Direction) -> Leg {
	Leg {
		route: Route(route),
		direction,
	}
}

pub fn journey_summaries() -> Vec<(&'static str, JourneySummary)> {
	let summary = |previous,
	               last_paid_at,
	               consecutive_payments,
	               card_type,
	               free,
	               route,
	               direction,
	               transfers_left| {
		JourneySummary {
			previous,
			last_paid_at,
			consecutive_payments,
			card_type,
			free,
			route: Route(route),
			direction,
			transfers_left,
		}
	};
	let avanza = CardType::AvanzaTopUp;
	vec![
		(
			"00002ADC091B010200D2020000630054",
			summary(
				None,
				paid(2021, 6, 28, 9, 27),
				1,
				avanza,
				false,
				210,
				Direction::Two,
				0x63,
			),
		),
		(
			"D2012AD90D33010200D20200006300AE",
			summary(
				Some(leg(210, Direction::One)),
				paid(2021, 6, 25, 13, 51),
				1,
				avanza,
				false,
				210,
				Direction::Two,
				0x63,
			),
		),
		(
			"23023454102D0102001601000063000B",
			summary(
				Some(leg(35, Direction::Two)),
				paid(2026, 2, 20, 16, 45),
				1,
				avanza,
				false,
				22,
				Direction::One,
				0x63,
			),
		),
		(
			"16013468002B04020016020000630011",
			summary(
				Some(leg(22, Direction::One)),
				paid(2026, 3, 8, 0, 43),
				4,
				avanza,
				false,
				22,
				Direction::Two,
				0x63,
			),
		),
		(
			"1F013518160B010D01D2020000620091",
			summary(
				Some(leg(31, Direction::One)),
				paid(2026, 8, 24, 22, 11),
				1,
				CardType::LazoTopUp,
				true,
				210,
				Direction::Two,
				0x62,
			),
		),
	]
}
