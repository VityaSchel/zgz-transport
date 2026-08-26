use zgz_transport::{Error, Route, Stop};

use crate::hex::array;

const CASES: [(&str, u8, Stop); 5] = [
	("81DB", 31, Stop::Urban(475)),
	("8001", 35, Stop::Urban(1)),
	("05DC", 210, Stop::Tram(1500)),
	("1F2C", 0, Stop::Other(7980)),
	("0005", 169, Stop::Other(5)),
];

#[test]
fn decodes_stops_by_network() {
	for (encoded, route, stop) in CASES {
		assert_eq!(Stop::decode(array(encoded), Route(route)), stop);
	}
}

#[test]
fn encodes_stops() {
	for (encoded, _, stop) in CASES {
		assert_eq!(stop.encode(), Ok(array(encoded)));
	}
}

#[test]
fn keeps_the_urban_flag_out_of_ids() {
	let range = |name| Error::Range {
		name,
		value: 0x8000,
		min: 0,
		max: 0x7fff,
	};
	assert_eq!(Stop::Urban(0x8000).encode(), Err(range("urban stop id")));
	assert_eq!(Stop::Tram(0x8000).encode(), Err(range("tram stop")));
	assert_eq!(Stop::Other(0x8000).encode(), Err(range("stop id")));
	assert_eq!(Stop::Urban(0x7fff).encode(), Ok([0xff, 0xff]));
}
