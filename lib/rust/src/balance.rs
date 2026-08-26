use core::fmt;

use crate::bytes::{BLOCK_SIZE, Block, chunk, in_range};
use crate::error::{Error, Result};

/// Balance of blocks 8 and 9 in 1/1000th of a euro, always `0` on personal cards.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct Balance(pub u32);

const ADDRESS: [u8; 4] = [0x02, 0xfd, 0x02, 0xfd];

impl Balance {
	/// Units in one euro.
	pub const UNITS_PER_EURO: u32 = 1000;

	/// Decodes a value block, checking the complement and the copy.
	///
	/// # Errors
	/// [`Error::BalanceComplement`](crate::Error::BalanceComplement) or [`Error::BalanceCopy`](crate::Error::BalanceCopy).
	pub fn decode(block: &Block) -> Result<Self> {
		let units = u32::from_le_bytes(chunk(block, 0));
		if u32::from_le_bytes(chunk(block, 4)) != !units {
			return Err(Error::BalanceComplement);
		}
		if u32::from_le_bytes(chunk(block, 8)) != units {
			return Err(Error::BalanceCopy);
		}
		Ok(Self(units))
	}

	/// Encodes into a value block.
	///
	/// # Errors
	/// [`Error::Range`](crate::Error::Range) above `0x7FFFFFFF`.
	pub fn encode(self) -> Result<Block> {
		let units = in_range("balance", self.0, 0, 0x7fff_ffff)?;
		let mut block = [0; BLOCK_SIZE];
		block[..4].copy_from_slice(&units.to_le_bytes());
		block[4..8].copy_from_slice(&(!units).to_le_bytes());
		block[8..12].copy_from_slice(&units.to_le_bytes());
		block[12..].copy_from_slice(&ADDRESS);
		Ok(block)
	}
}

/// Euros with three decimals, e.g. `4.450`.
impl fmt::Display for Balance {
	fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
		write!(
			f,
			"{}.{:03}",
			self.0 / Self::UNITS_PER_EURO,
			self.0 % Self::UNITS_PER_EURO
		)
	}
}
