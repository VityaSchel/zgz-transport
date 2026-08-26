use core::fmt;

/// Route id of the operator's GTFS feed, byte 7 of a transaction; `0` on top ups made off board.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
#[repr(transparent)]
pub struct Route(pub u8);

impl Route {
	/// The tram, `L1`.
	pub const TRAM: Self = Self(210);
}

/// Buses keep their number, `11` to `14` are `Ci1` to `Ci4`, `111` to `117` are `N1` to `N7` and the tram is `L1`.
impl fmt::Display for Route {
	fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
		match *self {
			Self::TRAM => f.write_str("L1"),
			Self(route @ 11..=14) => write!(f, "Ci{}", route - 10),
			Self(route @ 111..=117) => write!(f, "N{}", route - 110),
			Self(route) => write!(f, "{route}"),
		}
	}
}
