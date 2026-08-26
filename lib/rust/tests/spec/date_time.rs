use zgz_transport::{DateTime, Error};

use crate::fixtures::at;
use crate::hex::array;

#[test]
fn decodes_date_and_time() {
	assert_eq!(
		DateTime::decode(array("344E0C051B")),
		Ok(at(2026, 2, 14, 12, 5, 27))
	);
}

#[test]
fn encodes_date_and_time() {
	assert_eq!(at(2026, 2, 14, 12, 5, 27).encode(), Ok(array("344E0C051B")));
}

#[test]
fn rejects_invalid_parts() {
	assert!(matches!(
		DateTime::decode(array("00000C051B")),
		Err(Error::Range { name: "month", .. })
	));
	assert!(matches!(
		DateTime::decode(array("344E18051B")),
		Err(Error::Range { name: "hour", .. })
	));
}

#[test]
fn orders_by_date_then_time_and_displays() {
	assert!(at(2026, 2, 14, 23, 59, 59) < at(2026, 2, 15, 0, 0, 0));
	assert!(at(2026, 2, 14, 12, 5, 27) < at(2026, 2, 14, 12, 5, 28));
	assert_eq!(
		at(2026, 2, 14, 12, 5, 27).to_string(),
		"2026-02-14 12:05:27"
	);
}
