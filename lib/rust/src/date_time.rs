use core::fmt;

use crate::date::Date;
use crate::error::Result;
use crate::time::Time;

/// A [`Date`] followed by a [`Time`], five bytes in total.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct DateTime {
	/// Bytes 0 and 1.
	pub date: Date,
	/// Bytes 2 to 4.
	pub time: Time,
}

impl DateTime {
	/// Decodes a date followed by a time.
	///
	/// # Errors
	/// The errors of [`Date::decode`] and [`Time::decode`].
	pub fn decode([d0, d1, hour, minute, second]: [u8; 5]) -> Result<Self> {
		Ok(Self {
			date: Date::decode([d0, d1])?,
			time: Time::decode([hour, minute, second])?,
		})
	}

	/// Encodes into a date followed by a time.
	///
	/// # Errors
	/// The errors of [`Date::encode`] and [`Time::encode`].
	pub fn encode(self) -> Result<[u8; 5]> {
		let [d0, d1] = self.date.encode()?;
		let [hour, minute, second] = self.time.encode()?;
		Ok([d0, d1, hour, minute, second])
	}
}

impl fmt::Display for DateTime {
	fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
		write!(f, "{} {}", self.date, self.time)
	}
}
