use alloc::vec::Vec;

use crate::bytes::Block;
use crate::error::Result;
use crate::transaction::Transaction;

impl Transaction {
	/// Block holding the newest transaction.
	pub const LIVE_BLOCK: usize = 5;

	/// Blocks holding the five transactions before the newest, indexed by sequence.
	pub const ARCHIVE_BLOCKS: [usize; 5] = [28, 29, 30, 32, 33];

	/// [`LIVE_BLOCK`](Self::LIVE_BLOCK) then [`ARCHIVE_BLOCKS`](Self::ARCHIVE_BLOCKS), the slot order [`decode_log`](Self::decode_log) takes.
	pub const LOG_BLOCKS: [usize; 6] = {
		let [a, b, c, d, e] = Self::ARCHIVE_BLOCKS;
		[Self::LIVE_BLOCK, a, b, c, d, e]
	};

	/// Block a record moves to when a newer one replaces it, by its sequence byte.
	#[must_use]
	pub fn archive_block(sequence: u8) -> Option<usize> {
		Self::ARCHIVE_BLOCKS.get(usize::from(sequence)).copied()
	}

	/// Decodes the six slots of the transaction ring, oldest first, skipping empty slots.
	///
	/// # Errors
	/// The errors of [`Transaction::decode`] for any occupied slot.
	pub fn decode_log(slots: [&Block; 6]) -> Result<Vec<Self>> {
		let mut log = slots
			.into_iter()
			.filter(|slot| slot[0] != 0)
			.map(Self::decode)
			.collect::<Result<Vec<_>>>()?;
		log.sort_by_key(|transaction| transaction.created_at);
		Ok(log)
	}
}
