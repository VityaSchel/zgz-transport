use zgz_transport::Route;

#[test]
fn names_routes_like_the_operator() {
	assert_eq!(
		[11, 14, 31, 111, 117, 210].map(|route| Route(route).to_string()),
		["Ci1", "Ci4", "31", "N1", "N7", "L1"]
	);
}

#[test]
fn keeps_numbers_around_the_named_ranges() {
	assert_eq!(
		[0, 10, 15, 110, 118, 255].map(|route| Route(route).to_string()),
		["0", "10", "15", "110", "118", "255"]
	);
	assert_eq!(Route::TRAM, Route(210));
	assert!(Route(31) < Route::TRAM);
}
