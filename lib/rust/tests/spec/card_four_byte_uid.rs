use std::collections::HashSet;

use zgz_transport::{
	BLOCK_SIZE, Balance, Block, Card, CardType, Chip, Error, JourneySummary, Transaction, Uid,
};

use crate::card::{LAZO_FOUR_BYTE_BLOCK_0, TRAILING_4K_BLOCKS, dump, id};
use crate::fixtures::journey_summaries::journey_summaries;
use crate::fixtures::transactions::transactions;
use crate::hex::{array, checksummed};

#[test]
fn decodes_a_four_byte_uid_4k_card_and_keeps_an_unreadable_journey_summary() {
	let records = transactions();
	let balance = Balance(1000).encode().unwrap();
	let mut summary: Block = array(journey_summaries()[0].0);
	summary[10] = 0x3d;
	let summary = checksummed(summary);
	let mut blocks = vec![
		(0, array(LAZO_FOUR_BYTE_BLOCK_0)),
		(1, CardType::LazoTopUp375F.encode()),
		(2, id("CT123456")),
		(5, array(records[7].0)),
		(8, balance),
		(9, balance),
		(10, summary),
	];
	blocks.extend(
		Transaction::ARCHIVE_BLOCKS
			.iter()
			.zip([records[4].0, records[5].0, records[6].0])
			.map(|(&index, block)| (index, array(block))),
	);
	let mut dump = dump(&blocks);
	dump.extend(vec![0; TRAILING_4K_BLOCKS * BLOCK_SIZE]);
	let card = Card::decode(&dump).unwrap();
	assert_eq!(card.chip, Chip::Classic4K);
	assert_eq!(card.uid, Uid::Single(array("0468C3A9")));
	assert_eq!(card.uid.to_string(), "0468C3A9");
	assert_eq!(card.card_type, CardType::LazoTopUp375F);
	assert_eq!(card.id.to_string(), "CT123456");
	assert_eq!(card.balance, Balance(1000));
	assert_eq!(card.transactions.len(), 4);
	assert_eq!(card.journey_summary, Some(Err(Error::Direction(0x3d))));
	assert_eq!(
		JourneySummary::decode(&summary),
		Err(Error::Direction(0x3d))
	);
	assert_eq!(card.products, [None, None]);
	let cards: HashSet<Card> = [card.clone(), card].into_iter().collect();
	assert_eq!(cards.len(), 1);
}
