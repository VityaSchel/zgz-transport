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
		(Error::UnknownCardTypeByte(11), "unknown card type byte 11"),
		(Error::Direction(3), "direction must be 1 or 2, got 3"),
		(
			Error::TransactionKind(0),
			"transaction kind byte must be 1, 2 or 8, got 0",
		),
		(Error::Sak, "block 0 carries neither a 1K nor a 4K SAK"),
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
	let error: Box<dyn std::error::Error> = Box::new(Error::Sak);
	assert_eq!(error.to_string(), Error::Sak.to_string());
	let set: HashSet<Error> = [Error::Sak, Error::Sak, Error::BalanceCopy]
		.into_iter()
		.collect();
	assert_eq!(set.len(), 2);
}
