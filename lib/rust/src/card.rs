use alloc::vec::Vec;

use crate::balance::Balance;
use crate::bytes::{BLOCK_SIZE, is_zero};
use crate::card_type::CardType;
use crate::error::{Error, Result};
use crate::id::CardId;
use crate::journey_summary::JourneySummary;
use crate::subscription::Subscription;
use crate::subscription_metadata::SubscriptionMetadata;
use crate::transaction::Transaction;
use crate::uid::Uid;

/// A subscription product of a personal card.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct Product {
	/// Block 12 or 16.
	pub metadata: SubscriptionMetadata,
	/// Block 13 or 17.
	pub subscription: Subscription,
}

/// Everything decodable from a dump.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Card {
	/// Block 0.
	pub uid: Uid,
	/// Block 1.
	pub card_type: CardType,
	/// Block 2.
	pub id: CardId,
	/// Blocks 8 and 9.
	pub balance: Balance,
	/// The last six transactions, oldest first.
	pub transactions: Vec<Transaction>,
	/// Block 10; absent on personal cards and before the first journey.
	pub journey_summary: Option<JourneySummary>,
	/// Sector 3 (blocks 12 and 13) then sector 4 (blocks 16 and 17); both absent on top up cards.
	pub products: [Option<Product>; 2],
}

const LAST_USED_BLOCK: usize = 33;

impl Card {
	/// Decodes a raw dump of either card, consecutive 16-byte blocks starting at block 0.
	/// Partial dumps are accepted as long as they reach block 33.
	///
	/// # Errors
	/// [`Error::DumpSize`](crate::Error::DumpSize), [`Error::Sak`](crate::Error::Sak), [`Error::BalanceBlocksDiffer`](crate::Error::BalanceBlocksDiffer) or any error of the blocks decoded.
	pub fn decode(dump: &[u8]) -> Result<Self> {
		let minimum = (LAST_USED_BLOCK + 1) * BLOCK_SIZE;
		if !dump.len().is_multiple_of(BLOCK_SIZE) || dump.len() < minimum {
			return Err(Error::DumpSize {
				minimum,
				got: dump.len(),
			});
		}
		let (blocks, _) = dump.as_chunks::<BLOCK_SIZE>();
		let uid = Uid::decode(&blocks[0])?;
		if blocks[8] != blocks[9] {
			return Err(Error::BalanceBlocksDiffer);
		}
		let card_type = CardType::decode(&blocks[1])?;
		let personal = card_type == CardType::AvanzaPersonalUnlimited;
		let product = |sector: usize| -> Result<Option<Product>> {
			let index = sector * 4;
			if !personal || is_zero(&blocks[index]) {
				return Ok(None);
			}
			Ok(Some(Product {
				metadata: SubscriptionMetadata::decode(&blocks[index])?,
				subscription: Subscription::decode(&blocks[index + 1])?,
			}))
		};
		Ok(Self {
			uid,
			card_type,
			id: CardId::decode(&blocks[2])?,
			balance: Balance::decode(&blocks[8])?,
			transactions: Transaction::decode_log(
				Transaction::LOG_BLOCKS.map(|index| &blocks[index]),
			)?,
			journey_summary: if personal || is_zero(&blocks[10]) {
				None
			} else {
				Some(JourneySummary::decode(&blocks[10])?)
			},
			products: [product(3)?, product(4)?],
		})
	}
}
