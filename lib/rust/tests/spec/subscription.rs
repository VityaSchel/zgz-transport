use zgz_transport::{Date, Error, Subscription};

use crate::fixtures::at;
use crate::hex::array;

fn cases() -> Vec<(&'static str, Subscription)> {
	let unused = Subscription {
		starts_at: Date {
			year: 2026,
			month: 1,
			day: 15,
		},
		ends_at: Date {
			year: 2026,
			month: 2,
			day: 14,
		},
		unknown1: [0, 0],
		unknown2: [1, 2, 3, 4],
		last_used_at: None,
	};
	vec![
		("342F344E000001020304000000000065", unused),
		(
			"342F344E0000010203043441081E0006",
			Subscription {
				last_used_at: Some(at(2026, 2, 1, 8, 30, 0)),
				..unused
			},
		),
	]
}

#[test]
fn decodes_subscriptions() {
	for (encoded, decoded) in cases() {
		assert_eq!(Subscription::decode(&array(encoded)), Ok(decoded));
	}
}

#[test]
fn encodes_subscriptions() {
	for (encoded, decoded) in cases() {
		assert_eq!(decoded.encode(), Ok(array(encoded)));
	}
}

#[test]
fn rejects_a_wrong_checksum() {
	let mut block: [u8; 16] = array(cases()[1].0);
	block[15] ^= 0x80;
	assert_eq!(
		Subscription::decode(&block),
		Err(Error::Checksum {
			expected: 0x06,
			got: 0x86,
		})
	);
}

#[test]
fn rejects_a_partial_last_usage() {
	let mut block: [u8; 16] = array(cases()[0].0);
	block[12] = 8;
	block[15] ^= 8;
	assert!(matches!(
		Subscription::decode(&block),
		Err(Error::Range { name: "month", .. })
	));
}
