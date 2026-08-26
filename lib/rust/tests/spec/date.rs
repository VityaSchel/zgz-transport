use zgz_transport::{Date, Error};

use crate::hex::array;

const fn date(year: u16, month: u8, day: u8) -> Date {
	Date { year, month, day }
}

const CASES: [(Date, &str); 4] = [
	(date(2000, 1, 2), "0022"),
	(date(2015, 11, 31), "1F7F"),
	(date(2026, 2, 14), "344E"),
	(date(2025, 12, 3), "3383"),
];

#[test]
fn decodes_dates() {
	for (date, encoded) in CASES {
		assert_eq!(Date::decode(array(encoded)), Ok(date));
	}
}

#[test]
fn encodes_dates() {
	for (date, encoded) in CASES {
		assert_eq!(date.encode(), Ok(array(encoded)));
	}
}

#[test]
fn covers_the_whole_range() {
	assert_eq!(date(2127, 12, 31).encode(), Ok([0xff, 0x9f]));
	assert_eq!(Date::decode([0xff, 0x9f]), Ok(date(2127, 12, 31)));
}

#[test]
fn rejects_invalid_months_and_days() {
	let month = |value| Error::Range {
		name: "month",
		value,
		min: 1,
		max: 12,
	};
	let day = |value| Error::Range {
		name: "day",
		value,
		min: 1,
		max: 31,
	};
	assert_eq!(Date::decode(array("0002")), Err(month(0)));
	assert_eq!(Date::decode(array("01A1")), Err(month(13)));
	assert_eq!(Date::decode(array("0020")), Err(day(0)));
	assert_eq!(date(2026, 0, 1).encode(), Err(month(0)));
	assert_eq!(date(2026, 13, 1).encode(), Err(month(13)));
	assert_eq!(date(2026, 1, 0).encode(), Err(day(0)));
	assert_eq!(date(2026, 1, 32).encode(), Err(day(32)));
}

#[test]
fn rejects_years_outside_the_seven_bits() {
	for year in [1999, 2128] {
		assert_eq!(
			date(year, 1, 1).encode(),
			Err(Error::Range {
				name: "year",
				value: u32::from(year),
				min: 2000,
				max: 2127,
			})
		);
	}
}

#[test]
fn orders_and_displays_dates() {
	assert!(date(2025, 12, 31) < date(2026, 1, 1));
	assert!(date(2026, 1, 31) < date(2026, 2, 1));
	assert_eq!(date(2026, 2, 14).to_string(), "2026-02-14");
}
