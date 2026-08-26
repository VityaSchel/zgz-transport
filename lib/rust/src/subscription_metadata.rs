use crate::bytes::{BLOCK_SIZE, Block, check_checksum, chunk, with_checksum};
use crate::date::Date;
use crate::error::Result;

/// Block 12 or 16 of a personal card, one product per sector.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct SubscriptionMetadata {
	/// Byte 0, tells the products of a card apart.
	pub product_id: u8,
	/// Byte 1, always `1` so far.
	pub unknown1: u8,
	/// Bytes 2 and 3.
	pub purchased_at: Date,
	/// Bytes 4 to 7, always `00210000` so far.
	pub unknown2: [u8; 4],
	/// Bytes 8 and 9, big-endian.
	pub validity_days: u16,
	/// Bytes 10 to 14, always `0021000000` so far.
	pub unknown3: [u8; 5],
}

impl SubscriptionMetadata {
	/// Decodes block 12 or 16.
	///
	/// # Errors
	/// [`Error::Checksum`](crate::Error::Checksum) or the errors of [`Date::decode`].
	pub fn decode(block: &Block) -> Result<Self> {
		check_checksum(block)?;
		let [product_id, unknown1, d0, d1, ..] = *block;
		Ok(Self {
			product_id,
			unknown1,
			purchased_at: Date::decode([d0, d1])?,
			unknown2: chunk(block, 4),
			validity_days: u16::from_be_bytes(chunk(block, 8)),
			unknown3: chunk(block, 10),
		})
	}

	/// Encodes into block 12 or 16 with its checksum.
	///
	/// # Errors
	/// The errors of [`Date::encode`].
	pub fn encode(self) -> Result<Block> {
		let mut block = [0; BLOCK_SIZE];
		block[0] = self.product_id;
		block[1] = self.unknown1;
		block[2..4].copy_from_slice(&self.purchased_at.encode()?);
		block[4..8].copy_from_slice(&self.unknown2);
		block[8..10].copy_from_slice(&self.validity_days.to_be_bytes());
		block[10..15].copy_from_slice(&self.unknown3);
		Ok(with_checksum(block))
	}
}
