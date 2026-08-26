use zgz_transport::{Error, Transaction};

use crate::fixtures::transactions::transactions;
use crate::hex::array;

#[test]
fn decodes_transactions() {
	for (encoded, decoded) in transactions() {
		assert_eq!(Transaction::decode(&array(encoded)), Ok(decoded));
	}
}

#[test]
fn encodes_transactions() {
	for (encoded, decoded) in transactions() {
		assert_eq!(decoded.encode(), Ok(array(encoded)));
	}
}

#[test]
fn tells_free_transfers_from_paid_journeys_and_top_ups() {
	let free: Vec<bool> = transactions()
		.iter()
		.map(|(_, decoded)| decoded.is_free_transfer())
		.collect();
	assert_eq!(free, [[false; 9].as_slice(), &[true, true]].concat());
}

#[test]
fn a_free_transfer_is_a_journey_without_amount() {
	let (_, journey) = transactions()[9];
	let (_, top_up) = transactions()[8];
	assert!(journey.is_free_transfer());
	assert!(
		!Transaction {
			amount: 0,
			..top_up
		}
		.is_free_transfer()
	);
	assert!(
		!Transaction {
			amount: 550,
			..journey
		}
		.is_free_transfer()
	);
}

#[test]
fn rejects_an_unknown_kind_byte() {
	let mut block: [u8; 16] = array(transactions()[0].0);
	block[8] = 3;
	assert_eq!(Transaction::decode(&block), Err(Error::TransactionKind(3)));
	block[8] = 0;
	assert_eq!(Transaction::decode(&block), Err(Error::TransactionKind(0)));
}

#[test]
fn checks_the_timestamp_before_the_kind() {
	let mut block: [u8; 16] = array(transactions()[0].0);
	block[8] = 3;
	block[12] = 24;
	assert_eq!(
		Transaction::decode(&block),
		Err(Error::Range {
			name: "hour",
			value: 24,
			min: 0,
			max: 23,
		})
	);
}

#[test]
fn checks_the_card_type_before_the_timestamp() {
	let mut block: [u8; 16] = array(transactions()[0].0);
	block[0] = 0x0b;
	block[12] = 24;
	assert_eq!(
		Transaction::decode(&block),
		Err(Error::UnknownCardTypeByte(0x0b))
	);
}
