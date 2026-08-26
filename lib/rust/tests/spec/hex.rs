pub fn hex(text: &str) -> Vec<u8> {
	(0..text.len())
		.step_by(2)
		.map(|i| u8::from_str_radix(&text[i..i + 2], 16).unwrap())
		.collect()
}

pub fn array<const N: usize>(text: &str) -> [u8; N] {
	hex(text).try_into().unwrap()
}

pub fn checksummed(mut block: [u8; 16]) -> [u8; 16] {
	block[15] = block[..15].iter().fold(0, |acc, byte| acc ^ byte);
	block
}
