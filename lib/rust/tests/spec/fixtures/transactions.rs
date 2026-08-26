use zgz_transport::Direction::{One, Two};
use zgz_transport::Stop::{Other, Tram, Urban};
use zgz_transport::{CardType, Route, Transaction, TransactionKind};

use super::at;

pub fn transactions() -> Vec<(&'static str, Transaction)> {
	let journey =
		|amount, stop, route, direction, run_counter, (y, mo, d, h, mi, s), sequence| Transaction {
			card_type: CardType::AvanzaTopUp,
			network_flag: 0,
			amount,
			consecutive_payments: 1,
			stop,
			route: Route(route),
			kind: TransactionKind::Journey(direction),
			run_counter,
			created_at: at(y, mo, d, h, mi, s),
			sequence,
		};
	vec![
		(
			"020002F8010320D201FE2AD8132D1201",
			journey(760, Tram(800), 210, One, 254, (2021, 6, 24, 19, 45, 18), 1),
		),
		(
			"020002F80105DCD2026B2AD90D333B02",
			journey(760, Tram(1500), 210, Two, 107, (2021, 6, 25, 13, 51, 59), 2),
		),
		(
			"0200022601817E1F0211344D10163003",
			journey(550, Urban(382), 31, Two, 17, (2026, 2, 13, 16, 22, 48), 3),
		),
		(
			"020002260180011F0112344D14100704",
			journey(550, Urban(1), 31, One, 18, (2026, 2, 13, 20, 16, 7), 4),
		),
		(
			"02000226018001230209344E0C051B00",
			journey(550, Urban(1), 35, Two, 9, (2026, 2, 14, 12, 5, 27), 0),
		),
		(
			"0200022601817F160105344E0D1F2001",
			journey(550, Urban(383), 22, One, 5, (2026, 2, 14, 13, 31, 32), 1),
		),
		(
			"0200022601801E23020934540F153502",
			journey(550, Urban(30), 35, Two, 9, (2026, 2, 20, 15, 21, 53), 2),
		),
		(
			"020002260180CE16010D3454102D2403",
			journey(550, Urban(206), 22, One, 13, (2026, 2, 20, 16, 45, 36), 3),
		),
		(
			"02001388001F2C00080034840D123604",
			Transaction {
				card_type: CardType::AvanzaTopUp,
				network_flag: 0,
				amount: 5000,
				consecutive_payments: 0,
				stop: Other(7980),
				route: Route(0),
				kind: TransactionKind::TopUp,
				run_counter: 0,
				created_at: at(2026, 4, 4, 13, 18, 54),
				sequence: 4,
			},
		),
		(
			"0A0100000181F40B0107346F09291B04",
			Transaction {
				card_type: CardType::AvanzaPersonalUnlimited,
				network_flag: 1,
				amount: 0,
				consecutive_payments: 1,
				stop: Urban(500),
				route: Route(11),
				kind: TransactionKind::Journey(One),
				run_counter: 7,
				created_at: at(2026, 3, 15, 9, 41, 27),
				sequence: 4,
			},
		),
		(
			"0D0000000105DCD202013518162C2000",
			Transaction {
				card_type: CardType::LazoTopUp,
				network_flag: 0,
				amount: 0,
				consecutive_payments: 1,
				stop: Tram(1500),
				route: Route::TRAM,
				kind: TransactionKind::Journey(Two),
				run_counter: 1,
				created_at: at(2026, 8, 24, 22, 44, 32),
				sequence: 0,
			},
		),
	]
}
