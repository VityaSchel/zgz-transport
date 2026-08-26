use zgz_transport::{Error, JourneySummary, LastPaidAt, Leg, Route};

use crate::fixtures::journey_summaries::journey_summaries;
use crate::hex::{array, checksummed};

#[test]
fn decodes_journey_summaries() {
	for (encoded, decoded) in journey_summaries() {
		assert_eq!(JourneySummary::decode(&array(encoded)), Ok(decoded));
	}
}

#[test]
fn encodes_journey_summaries() {
	for (encoded, decoded) in journey_summaries() {
		assert_eq!(decoded.encode(), Ok(array(encoded)));
	}
}

#[test]
fn rejects_the_personal_card_constant() {
	assert_eq!(
		JourneySummary::PERSONAL,
		array::<16>("000000000000000A000000000000000A")
	);
	assert_eq!(
		JourneySummary::decode(&JourneySummary::PERSONAL),
		Err(Error::Range {
			name: "month",
			value: 0,
			min: 1,
			max: 12,
		})
	);
}

#[test]
fn rejects_non_zero_reserved_bytes() {
	for index in [11, 12, 14] {
		let mut block: [u8; 16] = array(journey_summaries()[0].0);
		block[index] = 1;
		block[15] ^= 1;
		assert_eq!(
			JourneySummary::decode(&block),
			Err(Error::NonZero("journey summary bytes 11, 12 and 14"))
		);
	}
}

#[test]
fn rejects_a_wrong_checksum() {
	let mut block: [u8; 16] = array(journey_summaries()[0].0);
	block[15] ^= 1;
	assert_eq!(
		JourneySummary::decode(&block),
		Err(Error::Checksum {
			expected: 0x54,
			got: 0x55,
		})
	);
}

#[test]
fn rejects_invalid_directions() {
	let mut block: [u8; 16] = array(journey_summaries()[0].0);
	block[10] = 3;
	block[15] ^= 0x02 ^ 0x03;
	assert_eq!(JourneySummary::decode(&block), Err(Error::Direction(3)));
	let mut block: [u8; 16] = array(journey_summaries()[1].0);
	block[1] = 0;
	block[15] ^= 0x01;
	assert_eq!(JourneySummary::decode(&block), Err(Error::Direction(0)));
}

#[test]
fn rejects_out_of_range_fields_when_decoding() {
	let base: [u8; 16] = array(journey_summaries()[0].0);
	let with = |index: usize, value: u8| {
		let mut block = base;
		block[index] = value;
		JourneySummary::decode(&checksummed(block))
	};
	assert!(matches!(
		with(4, 24),
		Err(Error::Range {
			name: "hour",
			value: 24,
			..
		})
	));
	assert!(matches!(
		with(5, 60),
		Err(Error::Range {
			name: "minute",
			value: 60,
			..
		})
	));
	assert_eq!(with(7, 0x0b), Err(Error::UnknownCardTypeByte(0x0b)));
}

#[test]
fn checks_hour_then_previous_direction_then_date_then_card_type() {
	let mut block: [u8; 16] = array(journey_summaries()[1].0);
	let original = block;
	block[4] = 24;
	block[1] = 3;
	block[2] = 0;
	block[3] = 0;
	block[7] = 0x0b;
	assert!(matches!(
		JourneySummary::decode(&checksummed(block)),
		Err(Error::Range { name: "hour", .. })
	));
	block[4] = original[4];
	assert_eq!(
		JourneySummary::decode(&checksummed(block)),
		Err(Error::Direction(3))
	);
	block[1] = original[1];
	assert!(matches!(
		JourneySummary::decode(&checksummed(block)),
		Err(Error::Range { name: "month", .. })
	));
	block[2] = original[2];
	block[3] = original[3];
	assert_eq!(
		JourneySummary::decode(&checksummed(block)),
		Err(Error::UnknownCardTypeByte(0x0b))
	);
}

#[test]
fn checks_the_time_before_the_previous_route_when_encoding() {
	let (_, summary) = journey_summaries()[1];
	let broken = JourneySummary {
		previous: Some(Leg {
			route: Route(0),
			..summary.previous.unwrap()
		}),
		last_paid_at: LastPaidAt {
			hour: 24,
			..summary.last_paid_at
		},
		..summary
	};
	assert!(matches!(
		broken.encode(),
		Err(Error::Range {
			name: "hour",
			value: 24,
			..
		})
	));
}

#[test]
fn rejects_out_of_range_fields_when_encoding() {
	let (_, summary) = journey_summaries()[1];
	assert_eq!(
		JourneySummary {
			previous: Some(Leg {
				route: Route(0),
				..summary.previous.unwrap()
			}),
			..summary
		}
		.encode(),
		Err(Error::Range {
			name: "previous route",
			value: 0,
			min: 1,
			max: 0xff,
		})
	);
	assert_eq!(
		JourneySummary {
			last_paid_at: LastPaidAt {
				minute: 60,
				..summary.last_paid_at
			},
			..summary
		}
		.encode(),
		Err(Error::Range {
			name: "minute",
			value: 60,
			min: 0,
			max: 59,
		})
	);
}
