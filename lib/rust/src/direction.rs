use crate::error::{Error, Result};

/// Direction of a journey along its route, byte 8 of a transaction.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
#[repr(u8)]
pub enum Direction {
	/// Byte `01`, south on the tram.
	One = 1,
	/// Byte `02`, north on the tram.
	Two = 2,
}

impl TryFrom<u8> for Direction {
	type Error = Error;

	fn try_from(byte: u8) -> Result<Self> {
		match byte {
			1 => Ok(Self::One),
			2 => Ok(Self::Two),
			_ => Err(Error::Direction(byte)),
		}
	}
}

impl From<Direction> for u8 {
	fn from(direction: Direction) -> Self {
		direction as Self
	}
}
