use zgz_transport::{
	BLOCK_SIZE, Balance, Block, Card, CardId, CardType, Chip, JourneySummary, Subscription,
	SubscriptionMetadata, Transaction, TransactionKind, Uid,
};

use crate::fixtures::transactions::transactions;

use crate::hex::{array, hex};

pub const BLOCKS: usize = 36;
pub const AVANZA_BLOCK_0: &str = "1D68C3A9BF880400C8000020000000AB";
pub const LAZO_BLOCK_0: &str = "0468C3A9BF12341802008100000023AA";

pub fn dump(blocks: &[(usize, Block)]) -> Vec<u8> {
	let mut dump = vec![0; BLOCKS * BLOCK_SIZE];
	for (index, block) in blocks {
		dump[index * BLOCK_SIZE..(index + 1) * BLOCK_SIZE].copy_from_slice(block);
	}
	dump
}

pub fn id(id: &str) -> Block {
	id.parse::<CardId>().unwrap().encode()
}

pub fn top_up_card(balance: u32, live: &str, archive: &[&str]) -> Vec<u8> {
	let balance = Balance(balance).encode().unwrap();
	let mut blocks = vec![
		(0, array(AVANZA_BLOCK_0)),
		(1, CardType::AvanzaTopUp.encode()),
		(2, id("BE123456")),
		(5, array(live)),
		(8, balance),
		(9, balance),
		(10, array("23023454102D0102001601000063000B")),
	];
	blocks.extend(
		Transaction::ARCHIVE_BLOCKS
			.iter()
			.zip(archive)
			.map(|(&index, block)| (index, array(block))),
	);
	dump(&blocks)
}

pub fn top_up_dump() -> Vec<u8> {
	let transactions = transactions();
	top_up_card(
		4450,
		transactions[7].0,
		&[
			transactions[4].0,
			transactions[5].0,
			transactions[6].0,
			"00000000000000000000000000000000",
			"00000000000000000000000000000004",
		],
	)
}

#[test]
fn decodes_a_top_up_card() {
	let dump = top_up_dump();
	let card = Card::decode(&dump).unwrap();
	let block = |index: usize| -> Block {
		dump[index * BLOCK_SIZE..(index + 1) * BLOCK_SIZE]
			.try_into()
			.unwrap()
	};
	assert_eq!(card.uid, Uid::Single(array("1D68C3A9")));
	assert_eq!(card.uid.chip(), Chip::Classic1K);
	assert_eq!(card.uid.to_string(), "1D68C3A9");
	assert_eq!(card.card_type, CardType::AvanzaTopUp);
	assert_eq!(card.id.to_string(), "BE123456");
	assert_eq!(card.balance, Balance(4450));
	assert_eq!(
		card.transactions,
		[28, 29, 30, 5].map(|index| Transaction::decode(&block(index)).unwrap())
	);
	assert_eq!(
		card.journey_summary,
		Some(JourneySummary::decode(&block(10)).unwrap())
	);
	assert_eq!(card.products, [None, None]);
}

#[test]
fn decodes_a_lazo_card_without_journeys() {
	let balance = Balance(600).encode().unwrap();
	let mut dump = dump(&[
		(0, array(LAZO_BLOCK_0)),
		(1, CardType::LazoTopUp.encode()),
		(2, id("CT123456")),
		(8, balance),
		(9, balance),
	]);
	dump.extend(vec![0; 220 * BLOCK_SIZE]);
	let card = Card::decode(&dump).unwrap();
	assert_eq!(card.uid, Uid::Double(array("0468C3A9BF1234")));
	assert_eq!(card.uid.chip(), Chip::Classic4K);
	assert_eq!(card.uid.as_bytes(), hex("0468C3A9BF1234"));
	assert_eq!(card.card_type, CardType::LazoTopUp);
	assert_eq!(card.id.to_string(), "CT123456");
	assert_eq!(card.balance, Balance(600));
	assert_eq!(card.transactions, []);
	assert_eq!(card.journey_summary, None);
}

#[test]
fn decodes_a_personal_card() {
	let balance = Balance(0).encode().unwrap();
	let metadata: Block = array("1101342F00210000001E002100000015");
	let subscription: Block = array("342F344E0000010203043441081E0006");
	let dump = dump(&[
		(0, array(AVANZA_BLOCK_0)),
		(1, CardType::AvanzaPersonalUnlimited.encode()),
		(2, id("BP123456")),
		(8, balance),
		(9, balance),
		(10, JourneySummary::PERSONAL),
		(16, metadata),
		(17, subscription),
		(18, subscription),
	]);
	let card = Card::decode(&dump).unwrap();
	assert_eq!(card.card_type, CardType::AvanzaPersonalUnlimited);
	assert_eq!(card.balance, Balance(0));
	assert_eq!(card.journey_summary, None);
	assert_eq!(card.products[0], None);
	let product = card.products[1].unwrap();
	assert_eq!(
		product.metadata,
		SubscriptionMetadata::decode(&metadata).unwrap()
	);
	assert_eq!(
		product.subscription,
		Subscription::decode(&subscription).unwrap()
	);
}

#[test]
fn replays_the_balance_across_dumps() {
	let transactions = transactions();
	let record = |index: usize| transactions[index].0;
	let paid_ride = "0200022601817E1F0211348410163003";
	let before = Card::decode(&top_up_card(
		1000,
		record(7),
		&[record(4), record(5), record(6)],
	))
	.unwrap();
	let top_up = Card::decode(&top_up_card(
		6000,
		record(8),
		&[record(5), record(6), record(7)],
	))
	.unwrap();
	let bus = Card::decode(&top_up_card(
		5450,
		paid_ride,
		&[record(6), record(7), record(8)],
	))
	.unwrap();
	let top_up_record = top_up.transactions.last().unwrap();
	let bus_record = bus.transactions.last().unwrap();
	assert_eq!(top_up_record.kind, TransactionKind::TopUp);
	assert_eq!(
		top_up.balance.0 - before.balance.0,
		u32::from(top_up_record.amount)
	);
	assert!(matches!(bus_record.kind, TransactionKind::Journey(_)));
	assert_eq!(
		top_up.balance.0 - bus.balance.0,
		u32::from(bus_record.amount)
	);
}
