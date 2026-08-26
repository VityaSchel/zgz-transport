use zgz_transport::{Error, Time};

use crate::hex::array;

const TIME: Time = Time {
	hour: 19,
	minute: 45,
	second: 18,
};

#[test]
fn decodes_time() {
	assert_eq!(Time::decode(array("132D12")), Ok(TIME));
}

#[test]
fn encodes_time() {
	assert_eq!(TIME.encode(), Ok([0x13, 0x2d, 0x12]));
}

#[test]
fn rejects_out_of_range_fields() {
	let range = |name, value, max| Error::Range {
		name,
		value,
		min: 0,
		max,
	};
	assert_eq!(Time::decode([24, 0, 0]), Err(range("hour", 24, 23)));
	assert_eq!(Time::decode([0, 60, 0]), Err(range("minute", 60, 59)));
	assert_eq!(Time::decode([0, 0, 60]), Err(range("second", 60, 59)));
	assert_eq!(
		Time { hour: 24, ..TIME }.encode(),
		Err(range("hour", 24, 23))
	);
	assert_eq!(
		Time { second: 60, ..TIME }.encode(),
		Err(range("second", 60, 59))
	);
}

#[test]
fn orders_and_displays_time() {
	assert!(
		Time {
			hour: 9,
			minute: 59,
			second: 59
		} < Time {
			hour: 10,
			minute: 0,
			second: 0
		}
	);
	assert_eq!(TIME.to_string(), "19:45:18");
}
