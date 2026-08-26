use zgz_transport::{Balance, Error};

use crate::hex::array;

const CASES: [(u32, &str); 3] = [
	(600, "58020000A7FDFFFF5802000002FD02FD"),
	(5000, "8813000077ECFFFF8813000002FD02FD"),
	(0, "00000000FFFFFFFF0000000002FD02FD"),
];

#[test]
fn decodes_balance() {
	for (units, encoded) in CASES {
		assert_eq!(Balance::decode(&array(encoded)), Ok(Balance(units)));
	}
}

#[test]
fn encodes_balance() {
	for (units, encoded) in CASES {
		assert_eq!(Balance(units).encode(), Ok(array(encoded)));
	}
}

#[test]
fn encodes_the_largest_balance() {
	assert_eq!(
		Balance(0x7fff_ffff).encode(),
		Ok(array("FFFFFF7F00000080FFFFFF7F02FD02FD"))
	);
	assert_eq!(
		Balance(0x8000_0000).encode(),
		Err(Error::Range {
			name: "balance",
			value: 0x8000_0000,
			min: 0,
			max: 0x7fff_ffff,
		})
	);
}

#[test]
fn rejects_mismatching_complement() {
	let mut block: [u8; 16] = array(CASES[1].1);
	block[4] ^= 1;
	assert_eq!(Balance::decode(&block), Err(Error::BalanceComplement));
}

#[test]
fn rejects_mismatching_copy() {
	let mut block: [u8; 16] = array(CASES[1].1);
	block[8] ^= 1;
	assert_eq!(Balance::decode(&block), Err(Error::BalanceCopy));
}

#[test]
fn decodes_without_a_range_or_address_check() {
	assert_eq!(
		Balance::decode(&array("FFFFFFFF00000000FFFFFFFF02FD02FD")),
		Ok(Balance(0xffff_ffff))
	);
	assert_eq!(
		Balance::decode(&array("58020000A7FDFFFF5802000000000000")),
		Ok(Balance(600))
	);
	assert_eq!(Balance(0x7fff_ffff).to_string(), "2147483.647");
}

#[test]
fn displays_euros_with_three_decimals() {
	assert_eq!(Balance::UNITS_PER_EURO, 1000);
	assert_eq!(Balance(4450).to_string(), "4.450");
	assert_eq!(Balance(600).to_string(), "0.600");
	assert_eq!(Balance(0).to_string(), "0.000");
	assert!(Balance(600) < Balance(4450));
}
