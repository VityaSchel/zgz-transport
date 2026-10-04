use std::collections::HashSet;

use zgz_transport::Error;

#[test]
fn displays_every_variant() {
	let cases = [
		(
			Error::Range {
				name: "hour",
				value: 24,
				min: 0,
				max: 23,
			},
			"hour must be in 0..=23, got 24",
		),
		(
			Error::Checksum {
				expected: 0x54,
				got: 0x55,
			},
			"checksum 85 does not match 84",
		),
		(
			Error::NonZero("journey summary bytes 11, 12 and 14"),
			"journey summary bytes 11, 12 and 14 must be zero",
		),
		(
			Error::BalanceComplement,
			"balance complement does not match",
		),
		(Error::BalanceCopy, "balance copy does not match"),
		(Error::BalanceBlocksDiffer, "balance blocks 8 and 9 differ"),
		(
			Error::IdFormat,
			"id must be two capital letters and an even count of 6 to 26 digits",
		),
		(
			Error::UnknownCardType(0x0b_69_9f),
			"unknown card type b699f",
		),
		(Error::Direction(3), "direction must be 1 or 2, got 3"),
		(
			Error::TransactionKind(0),
			"transaction kind byte must be 1, 2 or 8, got 0",
		),
		(
			Error::Sak {
				byte_5: 0x18,
				byte_7: 0x00,
			},
			"block 0 matches no known chip: byte 7 is 00 and byte 5 is 18, expected SAK 18 with ATQA 0200 or SAK 88 with ATQA 0400 after the UID, and a valid BCC on a 4-byte UID",
		),
		(
			Error::DumpSize {
				minimum: 544,
				got: 528,
			},
			"dump must be whole blocks and at least 544 bytes, got 528",
		),
	];
	for (error, text) in cases {
		assert_eq!(error.to_string(), text);
	}
}

#[test]
fn is_a_copy_hashable_std_error() {
	let sak = Error::Sak {
		byte_5: 0x18,
		byte_7: 0x00,
	};
	let error: Box<dyn std::error::Error> = Box::new(sak);
	assert_eq!(error.to_string(), sak.to_string());
	let set: HashSet<Error> = [sak, sak, Error::BalanceCopy].into_iter().collect();
	assert_eq!(set.len(), 2);
}
