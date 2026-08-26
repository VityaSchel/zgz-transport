use core::fmt;

use crate::bytes::in_range;
use crate::error::Result;

/// A time of day stored as three plain binary bytes.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct Time {
	/// `0` to `23`.
	pub hour: u8,
	/// `0` to `59`.
	pub minute: u8,
	/// `0` to `59`.
	pub second: u8,
}

impl Time {
	/// Decodes hour, minute and second bytes.
	///
	/// # Errors
	/// [`Error::Range`](crate::Error::Range) for an hour above 23 or a minute or second above 59.
	pub fn decode([hour, minute, second]: [u8; 3]) -> Result<Self> {
		Ok(Self {
			hour: in_range("hour", hour, 0, 23)?,
			minute: in_range("minute", minute, 0, 59)?,
			second: in_range("second", second, 0, 59)?,
		})
	}

	/// Encodes into hour, minute and second bytes.
	///
	/// # Errors
	/// [`Error::Range`](crate::Error::Range) for an hour above 23 or a minute or second above 59.
	pub fn encode(self) -> Result<[u8; 3]> {
		Ok([
			in_range("hour", self.hour, 0, 23)?,
			in_range("minute", self.minute, 0, 59)?,
			in_range("second", self.second, 0, 59)?,
		])
	}
}

impl fmt::Display for Time {
	fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
		write!(f, "{:02}:{:02}:{:02}", self.hour, self.minute, self.second)
	}
}
