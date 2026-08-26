use crate::bytes::in_range;
use crate::error::Result;
use crate::route::Route;

/// Where a transaction happened, bytes 5 and 6 of a transaction.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Stop {
	/// Bit 15 set: an urban bus stop with an internal stop id.
	Urban(u16),
	/// Bit 15 clear on the tram route: the stop number × 100.
	Tram(u16),
	/// Bit 15 clear on any other route: an id of that operator.
	Other(u16),
}

const URBAN_FLAG: u16 = 0x8000;
const MAX_ID: u32 = 0x7fff;

impl Stop {
	/// Decodes two big-endian bytes; `route` tells the tram from other operators.
	#[must_use]
	pub fn decode(bytes: [u8; 2], route: Route) -> Self {
		let value = u16::from_be_bytes(bytes);
		if value & URBAN_FLAG != 0 {
			Self::Urban(value & !URBAN_FLAG)
		} else if route == Route::TRAM {
			Self::Tram(value)
		} else {
			Self::Other(value)
		}
	}

	/// Encodes into two big-endian bytes.
	///
	/// # Errors
	/// [`Error::Range`](crate::Error::Range) above `0x7FFF`.
	pub fn encode(self) -> Result<[u8; 2]> {
		let value = match self {
			Self::Urban(id) => URBAN_FLAG | in_range("urban stop id", id, 0, MAX_ID)?,
			Self::Tram(stop) => in_range("tram stop", stop, 0, MAX_ID)?,
			Self::Other(id) => in_range("stop id", id, 0, MAX_ID)?,
		};
		Ok(value.to_be_bytes())
	}
}
