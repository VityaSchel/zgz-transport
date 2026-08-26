pub mod journey_summaries;
pub mod transactions;

use zgz_transport::{Date, DateTime, Time};

pub fn at(year: u16, month: u8, day: u8, hour: u8, minute: u8, second: u8) -> DateTime {
	DateTime {
		date: Date { year, month, day },
		time: Time {
			hour,
			minute,
			second,
		},
	}
}
