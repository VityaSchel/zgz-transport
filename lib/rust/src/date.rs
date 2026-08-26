use core::fmt;

use crate::bytes::in_range;
use crate::error::Result;

/// A calendar date as packed into two bytes: 7 bits of year since 2000, 4 of month, 5 of day.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct Date {
	/// `2000` to `2127`.
	pub year: u16,
	/// `1` to `12`.
	pub month: u8,
	/// `1` to `31`.
	pub day: u8,
}

impl Date {
	/// Decodes two big-endian bytes.
	///
	/// # Errors
	/// [`Error::Range`](crate::Error::Range) for a month or day of zero or a month above 12.
	pub fn decode([high, low]: [u8; 2]) -> Result<Self> {
		Ok(Self {
			year: 2000 + u16::from(high >> 1),
			month: in_range("month", ((high & 1) << 3) | (low >> 5), 1, 12)?,
			day: in_range("day", low & 0x1f, 1, 31)?,
		})
	}

	/// Encodes into two big-endian bytes.
	///
	/// # Errors
	/// [`Error::Range`](crate::Error::Range) for a year outside 2000 to 2127, a month outside 1 to 12 or a day outside 1 to 31.
	pub fn encode(self) -> Result<[u8; 2]> {
		let year = in_range("year", self.year, 2000, 2127)? - 2000;
		let month = in_range("month", self.month, 1, 12)?;
		let day = in_range("day", self.day, 1, 31)?;
		Ok(((year << 9) | (u16::from(month) << 5) | u16::from(day)).to_be_bytes())
	}
}

impl fmt::Display for Date {
	fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
		write!(f, "{:04}-{:02}-{:02}", self.year, self.month, self.day)
	}
}
