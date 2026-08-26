use core::fmt;

use crate::bytes::{Block, chunk};
use crate::error::{Error, Result};

/// MIFARE Classic variant, told apart by the SAK byte in block 0.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Chip {
	/// Avanza cards.
	Classic1K,
	/// Lazo cards.
	Classic4K,
}

/// UID of block 0.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Uid {
	/// 4 bytes, MIFARE Classic 1K.
	Single([u8; 4]),
	/// 7 bytes, MIFARE Classic 4K.
	Double([u8; 7]),
}

const SAK_1K: u8 = 0x88;
const SAK_4K: u8 = 0x18;

impl Uid {
	/// Decodes block 0: SAK `18` at byte 7 means 4K, else SAK `88` at byte 5 means 1K.
	///
	/// # Errors
	/// [`Error::Sak`](crate::Error::Sak).
	pub fn decode(block: &Block) -> Result<Self> {
		if block[7] == SAK_4K {
			Ok(Self::Double(chunk(block, 0)))
		} else if block[5] == SAK_1K {
			Ok(Self::Single(chunk(block, 0)))
		} else {
			Err(Error::Sak)
		}
	}

	/// The chip the UID length implies.
	#[must_use]
	pub const fn chip(self) -> Chip {
		match self {
			Self::Single(_) => Chip::Classic1K,
			Self::Double(_) => Chip::Classic4K,
		}
	}

	/// The 4 or 7 UID bytes.
	#[must_use]
	pub const fn as_bytes(&self) -> &[u8] {
		match self {
			Self::Single(bytes) => bytes,
			Self::Double(bytes) => bytes,
		}
	}
}

/// Upper case hex, e.g. `1D68C3A9`.
impl fmt::Display for Uid {
	fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
		for byte in self.as_bytes() {
			write!(f, "{byte:02X}")?;
		}
		Ok(())
	}
}
