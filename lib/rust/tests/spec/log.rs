use zgz_transport::{Block, Error, Transaction};

use crate::fixtures::transactions::transactions;
use crate::hex::array;

const STUB: &str = "00000000000000000000000000000004";
const EMPTY: &str = "00000000000000000000000000000000";

fn ring(slots: [&str; 6]) -> [Block; 6] {
	slots.map(array)
}

fn decoded(indices: &[usize]) -> Vec<Transaction> {
	let transactions = transactions();
	indices.iter().map(|&index| transactions[index].1).collect()
}

#[test]
fn maps_sequence_numbers_to_archive_blocks() {
	assert_eq!(Transaction::LIVE_BLOCK, 5);
	assert_eq!(Transaction::ARCHIVE_BLOCKS, [28, 29, 30, 32, 33]);
	assert_eq!(Transaction::LOG_BLOCKS, [5, 28, 29, 30, 32, 33]);
	assert_eq!(
		[0, 1, 2, 3, 4, 5].map(Transaction::archive_block),
		[Some(28), Some(29), Some(30), Some(32), Some(33), None]
	);
}

#[test]
fn orders_the_ring_by_time_and_skips_empty_slots() {
	let transactions = transactions();
	let blocks = ring([
		transactions[7].0,
		transactions[4].0,
		transactions[5].0,
		transactions[6].0,
		EMPTY,
		STUB,
	]);
	assert_eq!(
		Transaction::decode_log(blocks.each_ref()),
		Ok(decoded(&[4, 5, 6, 7]))
	);
}

#[test]
fn sorts_regardless_of_slot_order() {
	let transactions = transactions();
	let blocks = ring([
		transactions[0].0,
		transactions[8].0,
		transactions[3].0,
		transactions[1].0,
		transactions[2].0,
		transactions[10].0,
	]);
	assert_eq!(
		Transaction::decode_log(blocks.each_ref()),
		Ok(decoded(&[0, 1, 2, 3, 8, 10]))
	);
}

#[test]
fn orders_impossible_dates_without_normalizing_them() {
	let february_30 = "02000226018001230209345E0C051B00";
	let march_1 = "0200022601800123020934610C051B01";
	let blocks = ring([february_30, march_1, STUB, STUB, STUB, STUB]);
	let dates: Vec<String> = Transaction::decode_log(blocks.each_ref())
		.unwrap()
		.iter()
		.map(|transaction| transaction.created_at.date.to_string())
		.collect();
	assert_eq!(dates, ["2026-02-30", "2026-03-01"]);
}

#[test]
fn keeps_slot_order_on_equal_timestamps() {
	let live = transactions()[4].0;
	let twin = "02000226018002230209344E0C051B03";
	let sequences = |slots| {
		Transaction::decode_log(ring(slots).each_ref())
			.unwrap()
			.iter()
			.map(|transaction| transaction.sequence)
			.collect::<Vec<u8>>()
	};
	assert_eq!(sequences([live, twin, STUB, STUB, STUB, STUB]), [0, 3]);
	assert_eq!(sequences([twin, live, STUB, STUB, STUB, STUB]), [3, 0]);
}

#[test]
fn propagates_decoding_errors() {
	let transactions = transactions();
	let mut blocks = ring([transactions[7].0, STUB, STUB, STUB, STUB, STUB]);
	blocks[2] = array(transactions[0].0);
	blocks[2][8] = 3;
	assert_eq!(
		Transaction::decode_log(blocks.each_ref()),
		Err(Error::TransactionKind(3))
	);
}
