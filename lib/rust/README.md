# zgz-transport

Encoder and decoder for the Zaragoza and Aragon transport cards (Avanza Tarjeta Bus, Lazo), based on the reverse-engineered card spec at [https://git.hloth.dev/hloth/zgz-transport](https://git.hloth.dev/hloth/zgz-transport).

Install from [crates.io](https://crates.io/crates/zgz-transport):

```sh
cargo add zgz-transport
```

```rust,no_run
use zgz_transport::Card;

fn main() -> Result<(), Box<dyn std::error::Error>> {
	let card = Card::decode(&std::fs::read("dump.bin")?)?;
	println!("{} {} {}", card.uid, card.id, card.balance);
	for t in &card.transactions {
		println!("{} {:?} {} {}", t.created_at, t.kind, t.route, t.amount);
	}
	Ok(())
}
```

Every block structure is a type with a `decode`/`encode` pair taking and returning fixed-size byte arrays; `CardType::keys` gives the sector keys of a product. The crate is `no_std` and only needs `alloc`.
