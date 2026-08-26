use core::fmt;
use core::str::FromStr;

use crate::bytes::{BLOCK_SIZE, Block, check_checksum, with_checksum};
use crate::error::{Error, Result};

/// Printed card id of block 2: two ASCII letters and 6 to 26 digits, e.g. `BE322743`.
#[derive(Clone, Copy, PartialEq, Eq, Hash)]
pub struct CardId([u8; 15]);

impl CardId {
	/// Decodes block 2, checking only its checksum.
	///
	/// # Errors
	/// [`Error::Checksum`](crate::Error::Checksum).
	pub fn decode(block: &Block) -> Result<Self> {
		check_checksum(block)?;
		let [id @ .., _] = *block;
		Ok(Self(id))
	}

	/// Encodes into block 2 with its checksum.
	#[must_use]
	pub fn encode(self) -> Block {
		let mut block = [0; BLOCK_SIZE];
		block[..15].copy_from_slice(&self.0);
		with_checksum(block)
	}
}

impl FromStr for CardId {
	type Err = Error;

	fn from_str(id: &str) -> Result<Self> {
		let (prefix, digits) = id
			.split_at_checked(2)
			.filter(|(prefix, digits)| {
				prefix.bytes().all(|byte| byte.is_ascii_uppercase())
					&& (6..=26).contains(&digits.len())
					&& digits.len().is_multiple_of(2)
					&& digits.bytes().all(|byte| byte.is_ascii_digit())
			})
			.ok_or(Error::IdFormat)?;
		let mut bytes = [0; 15];
		bytes[..2].copy_from_slice(prefix.as_bytes());
		for (byte, pair) in bytes[2..].iter_mut().zip(digits.as_bytes().chunks(2)) {
			*byte = ((pair[0] - b'0') << 4) | (pair[1] - b'0');
		}
		Ok(Self(bytes))
	}
}

/// The prefix, then the digits with trailing zero bytes trimmed down to six digits.
impl fmt::Display for CardId {
	fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
		let mut end = 15;
		while end > 5 && self.0[end - 1] == 0 {
			end -= 1;
		}
		write!(f, "{}{}", char::from(self.0[0]), char::from(self.0[1]))?;
		for byte in &self.0[2..end] {
			write!(f, "{byte:02x}")?;
		}
		Ok(())
	}
}

impl fmt::Debug for CardId {
	fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
		write!(f, "CardId({self})")
	}
}
