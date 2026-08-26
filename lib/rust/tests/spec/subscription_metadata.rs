use zgz_transport::{Date, Error, SubscriptionMetadata};

use crate::hex::array;

fn cases() -> Vec<(&'static str, SubscriptionMetadata)> {
	vec![
		(
			"1101342F00210000001E002100000015",
			SubscriptionMetadata {
				product_id: 0x11,
				unknown1: 1,
				purchased_at: Date {
					year: 2026,
					month: 1,
					day: 15,
				},
				unknown2: [0x00, 0x21, 0x00, 0x00],
				validity_days: 30,
				unknown3: [0x00, 0x21, 0x00, 0x00, 0x00],
			},
		),
		(
			"2A0234410102030401F40506070809A9",
			SubscriptionMetadata {
				product_id: 0x2a,
				unknown1: 2,
				purchased_at: Date {
					year: 2026,
					month: 2,
					day: 1,
				},
				unknown2: [0x01, 0x02, 0x03, 0x04],
				validity_days: 500,
				unknown3: [0x05, 0x06, 0x07, 0x08, 0x09],
			},
		),
	]
}

#[test]
fn decodes_subscription_metadata() {
	for (encoded, decoded) in cases() {
		assert_eq!(SubscriptionMetadata::decode(&array(encoded)), Ok(decoded));
	}
}

#[test]
fn encodes_subscription_metadata() {
	for (encoded, decoded) in cases() {
		assert_eq!(decoded.encode(), Ok(array(encoded)));
	}
}

#[test]
fn rejects_a_wrong_checksum() {
	let mut block: [u8; 16] = array(cases()[0].0);
	block[15] = 0;
	assert_eq!(
		SubscriptionMetadata::decode(&block),
		Err(Error::Checksum {
			expected: 0x15,
			got: 0,
		})
	);
}

#[test]
fn rejects_an_invalid_purchase_date() {
	let mut block: [u8; 16] = array(cases()[0].0);
	block[3] = 0x20;
	block[15] ^= 0x2f ^ 0x20;
	assert!(matches!(
		SubscriptionMetadata::decode(&block),
		Err(Error::Range { name: "day", .. })
	));
}
