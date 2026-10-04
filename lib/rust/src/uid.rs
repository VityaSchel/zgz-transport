use core::fmt;

use crate::bytes::{Block, chunk, xor};
use crate::error::{Error, Result};

/// MIFARE Classic variant, told apart by the SAK and ATQA bytes of block 0.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Chip {
	/// Avanza cards.
	Classic1K,
	/// Lazo cards.
	Classic4K,
}

impl Chip {
	/// Sectors the chip holds.
	#[must_use]
	pub const fn sectors(self) -> u8 {
		match self {
			Self::Classic1K => 16,
			Self::Classic4K => 40,
		}
	}
}

/// UID of block 0. The chip does not fix its length: a 4K card carries either form.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Uid {
	/// 4 bytes, followed by their XOR as a BCC.
	Single([u8; 4]),
	/// 7 bytes, with no BCC.
	Double([u8; 7]),
}

/// The SAK and ATQA one chip answers with, as block 0 stores them.
struct Candidate {
	sak: u8,
	atqa: [u8; 2],
	chip: Chip,
}

const CANDIDATES: [Candidate; 2] = [
	Candidate {
		sak: 0x88,
		atqa: [0x04, 0x00],
		chip: Chip::Classic1K,
	},
	Candidate {
		sak: 0x18,
		atqa: [0x02, 0x00],
		chip: Chip::Classic4K,
	},
];

/// Byte holding the SAK of a 7-byte UID, the ATQA following it.
const SEVEN_BYTE_SAK: usize = 7;
/// Byte holding the SAK of a 4-byte UID and its BCC, the ATQA following it.
const FOUR_BYTE_SAK: usize = 5;

impl Candidate {
	fn matches(&self, block: &Block, sak: usize) -> bool {
		block[sak] == self.sak && block[sak + 1..sak + 3] == self.atqa
	}
}

impl Uid {
	/// Decodes block 0 into the UID and the chip, which are independent: the SAK and the ATQA sit
	/// after the UID, and after its BCC when the UID is 4 bytes. The 7-byte layout is tried first,
	/// because a 7-byte UID can hold a SAK value in the byte a 4-byte one puts its SAK in.
	///
	/// # Errors
	/// [`Error::Sak`](crate::Error::Sak).
	pub fn decode(block: &Block) -> Result<(Self, Chip)> {
		for candidate in &CANDIDATES {
			if candidate.matches(block, SEVEN_BYTE_SAK) {
				return Ok((Self::Double(chunk(block, 0)), candidate.chip));
			}
		}
		if block[4] == xor(&block[..4]) {
			for candidate in &CANDIDATES {
				if candidate.matches(block, FOUR_BYTE_SAK) {
					return Ok((Self::Single(chunk(block, 0)), candidate.chip));
				}
			}
		}
		Err(Error::Sak {
			byte_5: block[FOUR_BYTE_SAK],
			byte_7: block[SEVEN_BYTE_SAK],
		})
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
