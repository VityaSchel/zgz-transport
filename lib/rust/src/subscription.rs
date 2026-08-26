use crate::bytes::{BLOCK_SIZE, Block, check_checksum, chunk, is_zero, with_checksum};
use crate::date::Date;
use crate::date_time::DateTime;
use crate::error::Result;

/// Block 13 or 17 of a personal card, copied in 14 and 18.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct Subscription {
	/// First day of validity.
	pub starts_at: Date,
	/// Last day of validity.
	pub ends_at: Date,
	/// Bytes 4 and 5, always `0000` so far.
	pub unknown1: [u8; 2],
	/// Bytes 6 to 9.
	pub unknown2: [u8; 4],
	/// Last use the pass recorded; absent while unused.
	pub last_used_at: Option<DateTime>,
}

impl Subscription {
	/// Decodes block 13 or 17.
	///
	/// # Errors
	/// [`Error::Checksum`](crate::Error::Checksum) or the errors of [`Date::decode`] and [`DateTime::decode`].
	pub fn decode(block: &Block) -> Result<Self> {
		check_checksum(block)?;
		let last_used = &block[10..15];
		Ok(Self {
			starts_at: Date::decode(chunk(block, 0))?,
			ends_at: Date::decode(chunk(block, 2))?,
			unknown1: chunk(block, 4),
			unknown2: chunk(block, 6),
			last_used_at: if is_zero(last_used) {
				None
			} else {
				Some(DateTime::decode(chunk(block, 10))?)
			},
		})
	}

	/// Encodes into block 13 or 17 with its checksum.
	///
	/// # Errors
	/// The errors of [`Date::encode`] and [`DateTime::encode`].
	pub fn encode(self) -> Result<Block> {
		let mut block = [0; BLOCK_SIZE];
		block[..2].copy_from_slice(&self.starts_at.encode()?);
		block[2..4].copy_from_slice(&self.ends_at.encode()?);
		block[4..6].copy_from_slice(&self.unknown1);
		block[6..10].copy_from_slice(&self.unknown2);
		if let Some(last_used_at) = self.last_used_at {
			block[10..15].copy_from_slice(&last_used_at.encode()?);
		}
		Ok(with_checksum(block))
	}
}
