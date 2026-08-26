use zgz_transport::CardType::{AvanzaPersonalUnlimited, AvanzaTopUp, LazoTopUp};
use zgz_transport::SectorKeys;

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
		assert_eq!(AvanzaPersonalUnlimited.keys(sector), operator);
	}
	for sector in 9..=15 {
		assert_eq!(AvanzaTopUp.keys(sector), unused);
		assert_eq!(AvanzaPersonalUnlimited.keys(sector), operator);
	}
	assert_eq!(AvanzaTopUp.keys(16), None);
	assert_eq!(AvanzaPersonalUnlimited.keys(16), None);
}

#[test]
fn returns_lazo_keys_per_sector() {
	let shared = Some(keys(Some("4E303D402F20"), Some("243372407C2E")));
	for sector in 0..=31 {
		assert_eq!(LazoTopUp.keys(sector), shared);
	}
	assert_eq!(
		LazoTopUp.keys(32),
		Some(keys(Some("216F5B212A7A"), Some("44202E476E5B")))
	);
	assert_eq!(
		LazoTopUp.keys(33),
		Some(keys(Some("5148755C3427"), Some("3C4520753758")))
	);
	assert_eq!(LazoTopUp.keys(34), Some(keys(None, Some("206F7C4C4F36"))));
	assert_eq!(LazoTopUp.keys(35), Some(keys(Some("5246612E7C4B"), None)));
	assert_eq!(
		LazoTopUp.keys(36),
		Some(keys(Some("354B39454861"), Some("567D734C403C")))
	);
	assert_eq!(
		LazoTopUp.keys(37),
		Some(keys(Some("455D732C385F"), Some("2426217B3B3B")))
	);
	let factory = Some(keys(Some("FFFFFFFFFFFF"), Some("FFFFFFFFFFFF")));
	assert_eq!(LazoTopUp.keys(38), factory);
	assert_eq!(LazoTopUp.keys(39), factory);
	assert_eq!(LazoTopUp.keys(40), None);
}
