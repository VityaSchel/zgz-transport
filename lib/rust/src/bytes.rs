use crate::error::{Error, Result};

/// Size of a MIFARE Classic block in bytes.
pub const BLOCK_SIZE: usize = 16;

/// One MIFARE Classic block.
pub type Block = [u8; BLOCK_SIZE];

pub(crate) fn in_range<T: Into<u32> + Copy>(
	name: &'static str,
	value: T,
	min: u32,
	max: u32,
) -> Result<T> {
	let number = value.into();
	if (min..=max).contains(&number) {
		Ok(value)
	} else {
		Err(Error::Range {
			name,
			value: number,
			min,
			max,
		})
	}
}

pub(crate) fn chunk<const N: usize>(bytes: &[u8], offset: usize) -> [u8; N] {
	core::array::from_fn(|i| bytes[offset + i])
}

pub(crate) fn is_zero(bytes: &[u8]) -> bool {
	bytes.iter().all(|&byte| byte == 0)
}

pub(crate) fn xor(bytes: &[u8]) -> u8 {
	bytes.iter().fold(0, |acc, &byte| acc ^ byte)
}

pub(crate) fn check_checksum(block: &Block) -> Result<()> {
	let expected = xor(&block[..15]);
	if block[15] == expected {
		Ok(())
	} else {
		Err(Error::Checksum {
			expected,
			got: block[15],
		})
	}
}

pub(crate) fn with_checksum(mut block: Block) -> Block {
	block[15] = xor(&block[..15]);
	block
}
