use zgz_transport::CardType::{
	AvanzaPersonal, AvanzaPersonalAbono, AvanzaTopUp, LazoTopUp371F, LazoTopUp375F,
};
use zgz_transport::{CardType, SectorKeys};

use crate::hex::array;

fn keys(a: Option<&str>, b: Option<&str>) -> SectorKeys {
	SectorKeys {
		a: a.map(array),
		b: b.map(array),
	}
}

#[test]
fn returns_avanza_keys_per_sector_and_product() {
	let operator = Some(keys(Some("04000C0F0903"), Some("0B02070A0409")));
	let unused = Some(keys(Some("A0A1A2A3A4A5"), Some("B0B1B2B3B4B5")));
	for sector in 0..=8 {
		assert_eq!(AvanzaTopUp.keys(sector), operator);
		assert_eq!(AvanzaPersonal.keys(sector), operator);
		assert_eq!(AvanzaPersonalAbono.keys(sector), operator);
	}
	for sector in 9..=15 {
		assert_eq!(AvanzaTopUp.keys(sector), unused);
		assert_eq!(AvanzaPersonal.keys(sector), operator);
		assert_eq!(AvanzaPersonalAbono.keys(sector), operator);
	}
	assert_eq!(AvanzaTopUp.keys(16), None);
	assert_eq!(AvanzaPersonal.keys(16), None);
	assert_eq!(AvanzaPersonalAbono.keys(16), None);
}

#[test]
fn returns_the_same_lazo_keys_per_sector_for_both_lazo_types() {
	let shared = Some(keys(Some("4E303D402F20"), Some("243372407C2E")));
	let factory = Some(keys(Some("FFFFFFFFFFFF"), Some("FFFFFFFFFFFF")));
	for lazo in [LazoTopUp371F, LazoTopUp375F] {
		for sector in 0..=31 {
			assert_eq!(lazo.keys(sector), shared);
		}
		assert_eq!(
			lazo.keys(32),
			Some(keys(Some("216F5B212A7A"), Some("44202E476E5B")))
		);
		assert_eq!(
			lazo.keys(33),
			Some(keys(Some("5148755C3427"), Some("3C4520753758")))
		);
		assert_eq!(lazo.keys(34), Some(keys(None, Some("206F7C4C4F36"))));
		assert_eq!(lazo.keys(35), Some(keys(Some("5246612E7C4B"), None)));
		assert_eq!(
			lazo.keys(36),
			Some(keys(Some("354B39454861"), Some("567D734C403C")))
		);
		assert_eq!(
			lazo.keys(37),
			Some(keys(Some("455D732C385F"), Some("2426217B3B3B")))
		);
		assert_eq!(lazo.keys(38), factory);
		assert_eq!(lazo.keys(39), factory);
		assert_eq!(lazo.keys(40), None);
	}
}

#[test]
fn every_type_covers_exactly_the_sectors_of_its_chip() {
	for card_type in CardType::ALL {
		let sectors = card_type.chip().sectors();
		for sector in 0..sectors {
			assert!(
				card_type.keys(sector).is_some(),
				"{card_type:?} has no keys for sector {sector}"
			);
		}
		assert_eq!(
			card_type.keys(sectors),
			None,
			"{card_type:?} must stop at sector {sectors}"
		);
	}
}
