# Zaragoza & Aragon Transport Card Spec

[![NPM Version](https://img.shields.io/npm/v/zgz-transport?style=flat-square&logo=npm&logoColor=23ffffff%&label=%20&color=%23cd0000)](https://www.npmjs.com/package/zgz-transport) [![JSR Version](https://img.shields.io/jsr/v/%40hloth/zgz-transport?style=flat-square&logo=jsr&logoColor=%23f7df1e&label=%20&color=%23155775)](https://jsr.io/@hloth/zgz-transport) [![Crates.io Version](https://img.shields.io/crates/v/zgz-transport?style=flat-square&logo=rust&label=%20&color=black)
](https://crates.io/crates/zgz-transport) [![Maven Central Version](https://img.shields.io/maven-central/v/dev.hloth/zgz-transport?style=flat-square&logo=apache-maven&logoColor=%23f18900&label=%20&color=%2327282c)
](https://central.sonatype.com/artifact/dev.hloth/zgz-transport)

**See also: [Zaragoza Tarjeta Bus Android App](https://git.hloth.dev/hloth/zaragoza-tarjeta-bus-android)**

> [!IMPORTANT]
> ⚠️ **Aviso legal:** Este repositorio es un proyecto de investigación de seguridad independiente. \
> No está destinado a cometer fraude ni a facilitar el uso indebido del transporte público. \
> No se proporcionará asistencia para ningún uso ilícito. \
> Consulta [LEGAL.md](./LEGAL.md) para el descargo de responsabilidad completo.

Zaragoza and Aragon Avanza/Lazo bus & tram public transport card full up-to-date specification, reverse engineered from multiple MIFARE Classic card dumps. This project intends to be a security research paper, publicly available for anyone for free and is not qualified as a guide. There are no step-by-step instructions on how to commit fraud.

- [Zaragoza \& Aragon Transport Card Spec](#zaragoza--aragon-transport-card-spec)
	- [Getting started](#getting-started)
	- [Avanza Tarjeta Bus](#avanza-tarjeta-bus)
		- [Avanza keys](#avanza-keys)
		- [Avanza blocks](#avanza-blocks)
	- [Lazo card](#lazo-card)
		- [Lazo keys](#lazo-keys)
		- [Lazo blocks](#lazo-blocks)
	- [Data structures](#data-structures)
		- [Date](#date)
		- [Card ID](#card-id)
		- [Card type](#card-type)
		- [Balance](#balance)
		- [Transaction log](#transaction-log)
		- [Journey summary (block 10)](#journey-summary-block-10)
		- [Subscription metadata](#subscription-metadata)
		- [Subscription](#subscription)
	- [Unconfirmed](#unconfirmed)
		- [Byte 09 of a transaction](#byte-09-of-a-transaction)
		- [Route ids 152 and 251](#route-ids-152-and-251)
		- [Cercanias](#cercanias)
		- [Stop ids](#stop-ids)
		- [Block 24](#block-24)
		- [Block 4](#block-4)
		- [Subscription blocks](#subscription-blocks)
		- [Notes](#notes)
	- [Implementations](#implementations)
		- [JavaScript, TypeScript](#javascript-typescript)
		- [Rust](#rust)
		- [Java, Kotlin](#java-kotlin)
	- [Contributing](#contributing)
	- [Acknowledgements](#acknowledgements)
	- [License](#license)
	- [Donate](#donate)

## Getting started

Zaragoza and Aragon public transport uses Avanza and Lazo cards, both of which are MIFARE Classic, meaning they're vulnerable to the Crypto-1 attack. The keys can be dumped with a Proxmark, but since they're static and well-known, you can use the ones listed for each card below.

Every sector has two keys (Key A and Key B) that control access to its blocks, and the last block of every sector is its trailer, holding both keys and the access conditions.

|         | [Avanza Tarjeta Bus](#avanza-tarjeta-bus) | [Lazo card](#lazo-card)                               |
| ------- | ----------------------------------------- | ----------------------------------------------------- |
| Chip    | MIFARE Classic 1K                         | MIFARE Classic 4K                                     |
| SAK     | `88`                                      | `18`                                                  |
| UID     | 4 bytes                                   | 7 bytes                                               |
| Sectors | 16                                        | 40 (sectors 0-31 with 4 blocks, 32-39 with 16 blocks) |
| Blocks  | 64                                        | 256                                                   |

What is stored inside the blocks is the same on both cards and is documented once under [data structures](#data-structures). Block 1 tells the products apart, see [card type](#card-type). Everything that is still unresolved is collected under [unconfirmed](#unconfirmed).

## Avanza Tarjeta Bus

### Avanza keys

| Sectors                         | Key A          | Key B          |
| ------------------------------- | -------------- | -------------- |
| 0-8                             | `04000C0F0903` | `0B02070A0409` |
| 9-15 (unused) on top up cards   | `A0A1A2A3A4A5` | `B0B1B2B3B4B5` |
| 9-15 (unused) on personal cards | `04000C0F0903` | `0B02070A0409` |

On top up cards Key B can rewrite the keys and access conditions of every sector (trailer access bits `011`); on personal cards the trailers are locked (`110`). The access conditions of the data blocks are the same on both.

### Avanza blocks

| Sector | Block | Description                                                                                                                                                                            | Template                           | Access Conditions    |
| ------ | ----- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------- | -------------------- |
| 0      | 0     | [00-03] RFID's UID<br>[04] BCC (checksum byte)<br>[05] SAK (`88` for MIFARE Classic 1K)<br>[15] last two digits of the manufacturing year (`20` for 2020, `25` for 2025)               | `..,,..,,..880400C8,,0020000000..` | Read-only            |
|        | 1     | [Card type](#card-type)                                                                                                                                                                | `..,,..000000000000000000000000,,` | Only Key B can write |
|        | 2     | [Card ID](#card-id)                                                                                                                                                                    | `42,,..,,..00000000000000000000..` | Read-only            |
|        | 3     | [0th sector's trailer block](https://github.com/andrea-peter/nfc_mifare_classic_notes/blob/main/mifare-classic.md#sector-trailer-block)                                                | `04000C0F0903..,,..,,0B02070A0409` | _Trailer_            |
| 1      | 4     | Empty on top up cards, see [block 4](#block-4)<br>[15] XOR of all previous bytes                                                                                                       | `00000000000000000000000000000000` | Only Key B can write |
|        | 5     | Latest [transaction log](#transaction-log) entry                                                                                                                                       | `02..,,..,,..,,..,,..,,..,,..,,..` | No restrictions      |
|        | 6     | _Appears_ to always be empty                                                                                                                                                           | `00000000000000000000000000000000` | No restrictions      |
|        | 7     | 1st sector's trailer block                                                                                                                                                             | `04000C0F0903..,,..,,0B02070A0409` | _Trailer_            |
| 2      | 8     | [Balance](#balance)                                                                                                                                                                    | `..,,0000..,,FFFF..,,000002FD02FD` | Value block          |
|        | 9     | Always has the same value as block 8                                                                                                                                                   | `..,,0000..,,FFFF..,,000002FD02FD` | Value block          |
|        | 10    | [Journey summary](#journey-summary-block-10), empty on a new card<br>Always `000000000000000A000000000000000A` on unlimited personal cards                                             | `..,,..,,..,,..02..,,..0000..00..` | No restrictions      |
|        | 11    | 2nd sector's trailer block                                                                                                                                                             | `04000C0F0903..,,..,,0B02070A0409` | _Trailer_            |
| 3      | 12    | [Subscription metadata](#subscription-metadata)                                                                                                                                        | `00000000000000000000000000000000` | Only Key B can write |
|        | 13    | [Subscription](#subscription) on personal unlimited cards                                                                                                                              | `00000000000000000000000000000000` | Only Key B can write |
|        | 14    | Copy of block 13                                                                                                                                                                       | `00000000000000000000000000000000` | Only Key B can write |
|        | 15    | 3rd sector's trailer block                                                                                                                                                             | `04000C0F0903..,,..,,0B02070A0409` | _Trailer_            |
| 4      | 16    | [Subscription metadata](#subscription-metadata) of a second product                                                                                                                    | `00000000000000000000000000000000` | Only Key B can write |
|        | 17    | [Subscription](#subscription) of the second product                                                                                                                                    | `00000000000000000000000000000000` | Only Key B can write |
|        | 18    | Copy of block 17                                                                                                                                                                       | `00000000000000000000000000000000` | Only Key B can write |
|        | 19    | 4th sector's trailer block                                                                                                                                                             | `04000C0F0903..,,..,,0B02070A0409` | _Trailer_            |
| 5      | 20    | Empty                                                                                                                                                                                  | `00000000000000000000000000000000` | Only Key B can write |
|        | 21    | Empty                                                                                                                                                                                  | `00000000000000000000000000000000` | Only Key B can write |
|        | 22    | Empty                                                                                                                                                                                  | `00000000000000000000000000000000` | Only Key B can write |
|        | 23    | 5th sector's trailer blocks                                                                                                                                                            | `04000C0F0903..,,..,,0B02070A0409` | _Trailer_            |
| 6      | 24    | Empty on most cards, see [block 24](#block-24)                                                                                                                                         | `00000000000000000000000000000000` | Only Key B can write |
|        | 25    | Empty                                                                                                                                                                                  | `00000000000000000000000000000000` | Only Key B can write |
|        | 26    | Empty                                                                                                                                                                                  | `00000000000000000000000000000000` | Only Key B can write |
|        | 27    | 6th sector's trailer block                                                                                                                                                             | `04000C0F0903..,,..,,0B02070A0409` | _Trailer_            |
| 7      | 28    | [Transaction logs](#transaction-log); value of block 5 right before overwriting it, archived here when its sequence counter is `0` and in blocks 29, 30, 32 and 33 for counters 1 to 4 | `02..,,..,,..,,..,,..,,..,,..,,..` | No restrictions      |
|        | 29    | See block 28                                                                                                                                                                           | `02..,,..,,..,,..,,..,,..,,..,,..` | No restrictions      |
|        | 30    | See block 28                                                                                                                                                                           | `02..,,..,,..,,..,,..,,..,,..,,..` | No restrictions      |
|        | 31    | 7th sector's trailer block                                                                                                                                                             | `04000C0F0903..,,..,,0B02070A0409` | _Trailer_            |
| 8      | 32    | See block 28                                                                                                                                                                           | `02..,,..,,..,,..,,..,,..,,..,,..` | No restrictions      |
|        | 33    | See block 28                                                                                                                                                                           | `02..,,..,,..,,..,,..,,..,,..,,..` | No restrictions      |
|        | 34    | Expiration date, encoding unknown; always `00000000FFFFFFFF0000000000FF00FF` on top up cards                                                                                           | `00000000FFFFFFFF0000000000FF00FF` | Value block          |
|        | 35    | 8th sector's trailer block                                                                                                                                                             | `04000C0F0903..,,..,,0B02070A0409` | _Trailer_            |

Blocks 8, 9 and 34 are value blocks: a 32-bit integer, its bitwise complement, then the integer again, with value block access conditions (Key A can read, decrement, restore and transfer, Key B can also write and increment).

## Lazo card

### Lazo keys

| Sectors        | Blocks  | Key A          | Key B          |
| -------------- | ------- | -------------- | -------------- |
| 0-31           | 0-127   | `4E303D402F20` | `243372407C2E` |
| 32             | 128-143 | `216F5B212A7A` | `44202E476E5B` |
| 33             | 144-159 | `5148755C3427` | `3C4520753758` |
| 34             | 160-175 | Unknown        | `206F7C4C4F36` |
| 35             | 176-191 | `5246612E7C4B` | Unknown        |
| 36             | 192-207 | `354B39454861` | `567D734C403C` |
| 37             | 208-223 | `455D732C385F` | `2426217B3B3B` |
| 38-39 (unused) | 224-255 | `FFFFFFFFFFFF` | `FFFFFFFFFFFF` |

Sectors 38 and 39 still have the factory default keys and access conditions (`FF0780`).

### Lazo blocks

| Sector | Block                                  | Description                                                                                                                                                                                               | Template                           | Access Conditions             |
| ------ | -------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------- | ----------------------------- |
| 0      | 0                                      | [00-06] RFID's UID (7 bytes, so no BCC)<br>[07] SAK (`18` for MIFARE Classic 4K)<br>[08-09] ATQA (`0200`)<br>[10-15] manufacturer data; [15] is most likely the last two digits of the manufacturing year | `..,,..,,..,,..1802008100000023..` | Read-only                     |
|        | 1                                      | [Card type](#card-type)                                                                                                                                                                                   | `..,,..000000000000000000000000,,` | No restrictions               |
|        | 2                                      | [Card ID](#card-id)                                                                                                                                                                                       | `4354..,,..00000000000000000000..` | Only Key B can write          |
|        | 3                                      | [0th sector's trailer block](https://github.com/andrea-peter/nfc_mifare_classic_notes/blob/main/mifare-classic.md#sector-trailer-block)                                                                   | `4E303D402F20..,,..,,243372407C2E` | _Trailer_                     |
| 1      | 4                                      | _Appears_ to always be empty                                                                                                                                                                              | `00000000000000000000000000000000` | No restrictions               |
|        | 5                                      | Latest [transaction log](#transaction-log) entry                                                                                                                                                          | `0D..,,..,,..,,..,,..,,..,,..,,..` | No restrictions               |
|        | 6                                      | _Appears_ to always be empty                                                                                                                                                                              | `00000000000000000000000000000000` | No restrictions               |
|        | 7                                      | 1st sector's trailer block                                                                                                                                                                                | `4E303D402F20..,,..,,243372407C2E` | _Trailer_                     |
| 2      | 8                                      | [Balance](#balance)                                                                                                                                                                                       | `..,,0000..,,FFFF..,,000002FD02FD` | No restrictions               |
|        | 9                                      | Always has the same value as block 8                                                                                                                                                                      | `..,,0000..,,FFFF..,,000002FD02FD` | No restrictions               |
|        | 10                                     | [Journey summary](#journey-summary-block-10), empty on a new card                                                                                                                                         | `..,,..,,..,,..0D..,,..0000..00..` | No restrictions               |
|        | 11                                     | 2nd sector's trailer block                                                                                                                                                                                | `4E303D402F20..,,..,,243372407C2E` | _Trailer_                     |
| 3-6    | 12, 16, 20, 24                         | Empty on top up cards                                                                                                                                                                                     | `00000000000000000000000000000000` | Only Key B can write          |
|        | 13-14, 17-18, 21-22, 25-26             | Empty on top up cards                                                                                                                                                                                     | `00000000000000000000000000000000` | No restrictions               |
|        | 15, 19, 23, 27                         | Trailer blocks of sectors 3-6                                                                                                                                                                             | `4E303D402F20..,,..,,243372407C2E` | _Trailer_                     |
| 7      | 28-30                                  | [Transaction log](#transaction-log) archive slots for sequence counters 0, 1 and 2                                                                                                                        | `0D..,,..,,..,,..,,..,,..,,..,,..` | No restrictions               |
|        | 31                                     | 7th sector's trailer block                                                                                                                                                                                | `4E303D402F20..,,..,,243372407C2E` | _Trailer_                     |
| 8      | 32-33                                  | [Transaction log](#transaction-log) archive slots for sequence counters 3 and 4                                                                                                                           | `0D..,,..,,..,,..,,..,,..,,..,,..` | No restrictions               |
|        | 34                                     | Always empty                                                                                                                                                                                              | `00000000000000000000000000000000` | No restrictions               |
|        | 35                                     | 8th sector's trailer block                                                                                                                                                                                | `4E303D402F20..,,..,,243372407C2E` | _Trailer_                     |
| 9      | 36-38                                  | Empty                                                                                                                                                                                                     | `00000000000000000000000000000000` | No restrictions               |
| 10, 15 | 40-42, 60-62                           | Empty                                                                                                                                                                                                     | `00000000000000000000000000000000` | Only Key B can write          |
| 11-14  | data blocks 44-46, 48-50, 52-54, 56-58 | Empty                                                                                                                                                                                                     | `00000000000000000000000000000000` | Only Key B can read and write |
| 16-31  | data blocks 64-126                     | Empty                                                                                                                                                                                                     | `00000000000000000000000000000000` | Value block                   |
| 32-37  | data blocks 128-222                    | Empty                                                                                                                                                                                                     | `00000000000000000000000000000000` | Only Key B can write          |
| 38-39  | data blocks 224-254                    | Empty, never personalized                                                                                                                                                                                 | `00000000000000000000000000000000` | No restrictions               |

The trailer blocks of sectors 9-39 (39, 43, ..., 127, then 143, 159, 175, 191, 207, 223, 239 and 255) hold the keys from the table above.

Blocks 8 and 9 use the value block format (integer, complement, integer) but their sector is unrestricted, so both keys can write them directly. Only the empty sectors 16-31 carry real value block access conditions.

## Data structures

These are the same on both cards. Where a field differs on personal unlimited cards, the field list says so.

### Date

Two bytes, read as 16 bits:

- 7 bits: year, 2000-based
- 4 bits: month (1-12)
- 5 bits: day (1-31)

`34 4E` = `0011010 0010 01110` = 2026-02-14.

See implementation for [JS](lib/javascript/src/date.ts), [Rust](lib/rust/src/date.rs), [Java](lib/java/src/main/java/dev/hloth/zgztransport/CardDate.java).

### Card ID

- [00-01] ASCII prefix: `BE` and `BP` on Avanza cards, `CT` on Lazo cards
- [02-04] Card number, one decimal digit per nibble
- [05-14] zero
- [15] XOR of all previous bytes

See implementation for [JS](lib/javascript/src/id.ts), [Rust](lib/rust/src/id.rs), [Java](lib/java/src/main/java/dev/hloth/zgztransport/CardId.java).

### Card type

Block 1 says which product a card is:

- [00-02] Card type
- [03-14] zero
- [15] XOR of all previous bytes

| Card type                     | Chip              | Block 1 value                    |
| ----------------------------- | ----------------- | -------------------------------- |
| Balance top-up Avanza card    | MIFARE Classic 1K | `02699F000000000000000000000000` |
| Personal expiring Avanza card | MIFARE Classic 1K | `0A9775000000000000000000000000` |
| Balance top-up Lazo card      | MIFARE Classic 4K | `0D371F000000000000000000000000` |

Top up cards pay for each journey out of a [balance](#balance); personal cards use a [subscription](#subscription) and never change balance.

See implementation for [JS](lib/javascript/src/type.ts), [Rust](lib/rust/src/card_type.rs), [Java](lib/java/src/main/java/dev/hloth/zgztransport/CardType.java).

### Balance

Blocks 8 and 9, identical, in the MIFARE value block format. €1.00 = 1000 units.

- [00-03] units, little-endian
- [04-07] bitwise complement of [00-03]
- [08-11] [00-03] again
- [12-15] address bytes, always `02FD02FD`

€5.00 is `8813000077ECFFFF8813000002FD02FD`. Personal unlimited cards always hold zero: `00000000FFFFFFFF0000000002FD02FD`.

See implementation for [JS](lib/javascript/src/balance.ts), [Rust](lib/rust/src/balance.rs), [Java](lib/java/src/main/java/dev/hloth/zgztransport/Balance.java).

### Transaction log

Six 16-byte records: block 5 holds the newest, blocks 28, 29, 30, 32 and 33 the five before it. When a new transaction is written, the previous block 5 is copied into the archive slot selected by its sequence byte [15] (`0` to block 28, `1` to 29, `2` to 30, `3` to 32, `4` to 33), so the counter wraps every five transactions. Block 34 is never part of the ring.

- [00] First byte of the [card type](#card-type)
- [01] `00` on top up cards; `01` or `02` on personal cards, matching bit 15 of [05-06]
- [02-03] Amount, big-endian, in [balance](#balance) units; `0000` when free of charge, always `0000` on personal cards
- [04] Consecutive payments of this card at one terminal, counting from 1; `00` on top ups and on a check-out
- [05-06] Stop. Bit 15 set: an urban bus stop, the other 15 bits an internal stop id. Bit 15 clear: the tram, where the value is the stop number × 100 (`0514` = 1300 = Plaza España), or another operator
- [07] Route id of the operator's GTFS feed: numbered buses have their number, Ci1 to Ci4 are 11 to 14, N1 to N7 are 111 to 117, the tram is 210
- [08] `01` or `02`: a journey and its direction; `08`: a top up
- [09] See [byte 09](#byte-09-of-a-transaction)
- [10-11] [Date](#date)
- [12] Hour, [13] minute, [14] second, plain binary
- [15] Sequence counter, 0 to 4

A journey subtracts the amount from the balance, a top up adds it. A journey with amount `0000` is a free transfer. The operator grants [one per paid ride, to a different line](https://hola-zaragoza.avanzagrupo.com/hc/es-es/articles/37258377107860--C%C3%B3mo-funciona-el-transbordo-con-la-Tarjeta-BUS), within 60 minutes for the urban Tarjeta BUS and 75 when a CTAZ card enters Zaragoza; bus and tram count as one network. A top up carries the point of sale id in [05-06] and zeros in [04], [07] and [09], unless it was made on board, where it carries the line and stop like a journey. Top ups do not touch [block 10](#journey-summary-block-10), and the balance a new card is sold with leaves no record.

Byte [08] is the operator's GTFS `direction_id` plus one, so it picks one of the two headsigns the feed gives that route: `01` is `direction_id` 0 and `02` is `direction_id` 1. The ring buses only ever run `01`. The tram directions are still unconfirmed, probably `01` runs south to Mago de Oz and `02` north to Avenida de la Academia.

See implementation for [JS](lib/javascript/src/transaction.ts), [Rust](lib/rust/src/transaction.rs), [Java](lib/java/src/main/java/dev/hloth/zgztransport/Transaction.java).

### Journey summary (block 10)

Rewritten on every journey, untouched by top ups, so it stays all zero on a card that has only ever been topped up. On personal unlimited cards it is always `000000000000000A000000000000000A`; the layout below is the top up card one.

- [00-01] Line and direction of the previous journey, `0000` on the first; consecutive payments at one terminal count as one journey
- [02-03] [Date](#date) and [04-05] hour and minute of the last journey that was charged; on a free transfer these still point at the paid ride it belongs to
- [06] Consecutive payments counter of the current journey, the same as byte [04] of block 5
- [07] First byte of the [card type](#card-type)
- [08] `01` when the current journey was free, `00` when paid
- [09-10] Line and direction of the current journey
- [11-12] `0000`
- [13] `63` after a paid journey, `62` after a free transfer
- [14] `00`
- [15] XOR of all previous bytes

A free transfer onto the tram 33 minutes after a paid ride on bus 31, block 5 above block 10:

```text
    ty ?1 amt  cp stop ln dr ?9 date HH MM SS sq
 5: 0D 00 0000 01 05DC D2 02 01 3518 16 2C 20 00
10: 1F 01 3518 16 0B 01 0D 01 D2 02 0000 62 00 91
    pl pd date HH MM cp ty fr ln dr 0000 tr 00 xr
    ty = card type byte      cp = consecutive payments     fr = free flag
   amt = amount              ln dr = line, direction       tr = 63 paid / 62 free
  stop = stop id             pl pd = previous line, direction
  date = date                HH MM SS = time               sq = sequence
    ?1 = byte 01             ?9 = byte 09                  xr = XOR
```

See implementation for [JS](lib/javascript/src/journey-summary.ts), [Rust](lib/rust/src/journey_summary.rs), [Java](lib/java/src/main/java/dev/hloth/zgztransport/JourneySummary.java).

### Subscription metadata

Blocks 12 and 16 on personal cards, one product per sector. Still [work in progress](#subscription-blocks).

- [00] Unknown
- [01] Unknown, appears to always be `01`
- [02-03] Purchase [date](#date)
- [04-07] Unknown, appears to always be `00210000`
- [08-09] Validity in days
- [10-14] Unknown, appears to always be `0021000000`
- [15] XOR of all previous bytes

See implementation for [JS](lib/javascript/src/subscription-metadata.ts), [Rust](lib/rust/src/subscription_metadata.rs), [Java](lib/java/src/main/java/dev/hloth/zgztransport/SubscriptionMetadata.java).

### Subscription

Blocks 13 and 14 (a copy) for the product of block 12, blocks 17 and 18 for the product of block 16. Still [work in progress](#subscription-blocks).

- [00-01] Start [date](#date)
- [02-03] End date
- [04-05] Unknown, appears to always be `0000`
- [06-09] Unknown
- [10-11] [Date](#date) of the last usage, [12] hour, [13] minute, [14] second
- [15] XOR of all previous bytes

See implementation for [JS](lib/javascript/src/subscription.ts), [Rust](lib/rust/src/subscription.rs), [Java](lib/java/src/main/java/dev/hloth/zgztransport/Subscription.java).

## Unconfirmed

Theories and open questions.

### Byte 09 of a transaction

On buses it's most likely which trip of the vehicle's daily duty this is, counting from 1. Avanza's GTFS names every trip `<service>__<N>`, where `N` is the trip's chronological position within one vehicle's day, and the byte matches the `N` of the trip in progress. It never exceeds the longest duty on its route, which a count of the day's trips would.

It's incremented through the day, stays same across consecutive payments and across an on-board top up and the ride paid in the same second, repeats at the same hour on different days, and differs widely between routes at one hour. It is neither the stop's position along the route nor the passenger's order at the stop.

On the tram it is something else. On Avanza balance cards it's incrementing by one per validation. On a Lazo card it's `01`. On a personal Avanza card off the Avanza network it's `00`.

### Route ids 152 and 251

None of the three is in Avanza's feed, whose ids stop at 210, and their stop ids are not in the urban or the tram space.

152 and 251 appear only on personal cards and are unidentified.

### Cercanias

Cercanías is the Renfe commuter rail that runs under the city. It charges its own fare, and a journey is a check-in and a check-out where the second validation costs nothing.

Byte [04] is `00`. Byte [06] looks like the station's position along the line, counting from the far terminal stop. Byte [05] is `00` on the check-in and `23` on the check-out.

Cercanías route IDs gathered so far: 169.


### Stop ids

The urban stop id in [05-06] is scoped to the route rather than shared across the network: routes with no stop in common still use the same ids. Within one route it means a location rather than a platform, the same id appearing in both directions. No public identifier has been found. Neither the `PA` number on the stop nor the GTFS `stop_id`, nor the rank of the stop in any ordering of the pole data, nor a position along the route.

| Line | 05-06 bytes | 09 byte | Pole | Name                      |
| ---- | ----------- | ------- | ---- | ------------------------- |
| 22   | `81AF`      | `12`    | 676  | P. María Agustín 37       |
| 35   | `804C`      | `11`    | 471  | Fueros de Aragón 15       |
| 31   | `81DB`      | `0B`    | 3071 | Av. de Madrid / Aljafería |
| 35   | `8099`      | `09`    | 707  | Plaza Aragón 1            |
| 31   | `807E`      | `08`    | 147  | Av. Francisco de Goya 83  |
| 22   | `81C8`      | `0D`    | 434  | Duquesa Villahermosa 3    |
| 30   | `802E`      | `0F`    | 430  | Doctor Iranzo N.º 61      |
| 40   | `805D`      | `25`    | 633  | P. de la Constitución 16  |
| Ci4  | `808F`      | `0C`    | 3030 | Av. de San José 7         |

The values 1, 3, 4 and 5 appear on unrelated routes and may be placeholders.

### Block 24

Empty on most top up cards. On the others it's `0200`, something shaped like a [date](#date) at [02-03] repeated at [10-11], zeros, `01` at [14] and an XOR of all previous bytes at [15].

### Block 4

Empty on top up cards. On personal cards it's `0600030A1204` followed by zeros: the product ids of blocks 12 and 16 next to their sector numbers.

### Subscription blocks

Personal cards have one product per sector in sectors 3 and 4, each with its own metadata and subscription blocks. The last usage field does not move on every journey and may only log usage on one network.

### Notes

- A top up can carry `21` in the sequence byte instead of 0 to 4
- Before a ring has wrapped, an unused archive slot can hold `00000000000000000000000000000004` instead of all zeroes

## Implementations

### JavaScript, TypeScript

[lib/javascript](lib/javascript) is the spec implementation library in JavaScript/TypeScript. Zero dependencies, compatible with Node.js >= 25, Bun, Deno and 2025 browsers, MIT License. The package is published on [npm](https://www.npmjs.com/package/zgz-transport) as `zgz-transport` and on [JSR](https://jsr.io/@hloth/zgz-transport) as `@hloth/zgz-transport`.

Start from [src/index.ts](lib/javascript/src/index.ts).

### Rust

[lib/rust](lib/rust) is the spec implementation library in Rust. Zero dependencies, `no_std` with `alloc`, MIT License. The crate is published on [crates.io](https://crates.io/crates/zgz-transport) as `zgz-transport`.

Start from [src/lib.rs](lib/rust/src/lib.rs).

### Java, Kotlin

[lib/java](lib/java) is the spec implementation library in Java. Zero dependencies, usable from Java >= 17, Kotlin and Android API >= 26, MIT License. The library is published on [Maven Central](https://central.sonatype.com/artifact/dev.hloth/zgz-transport) and on self-hosted [Maven repository at git.hloth.dev](https://git.hloth.dev/hloth/-/packages/maven/dev.hloth:zgz-transport) as `dev.hloth:zgz-transport`.

Start from [src/main/java/dev/hloth/zgztransport/Card.java](lib/java/src/main/java/dev/hloth/zgztransport/Card.java).

## Contributing

If you'd like to contribute to the project's development, consider the following resources:

- [MifareClassicTool for Android](https://github.com/ikarus23/MifareClassicTool)
- GTFS feeds mirrored by the Mobility Database: [Avanza urban buses](https://files.mobilitydatabase.org/mdb-2773/latest.zip) and [the tram](https://files.mobilitydatabase.org/mdb-2801/latest.zip); the originals are on the [Spanish National Access Point](https://nap.transportes.gob.es/Files/Detail/975), which needs a login
- Zaragoza open data: [bus lines](https://www.zaragoza.es/sede/servicio/urbanismo-infraestructuras/transporte-urbano/linea-autobus) (routes last updated in 2013, pole numbers have moved since), [bus poles](https://www.zaragoza.es/sede/servicio/urbanismo-infraestructuras/transporte-urbano/poste) (live, with the lines serving each pole) and [tram stops](https://www.zaragoza.es/sede/servicio/urbanismo-infraestructuras/transporte-urbano/parada-tranvia)

The spreadsheet with publicly disclosed dumps and highlights:

<a href="https://docs.google.com/spreadsheets/d/1g89saB1URWRZLWsEJm44vJFTosIDfPkh5u8pfxPWGD0/edit">
	<img alt="Spreadsheet" src="https://git.hloth.dev/hloth/zgz-transport/raw/branch/main/docs/spreadsheet.avif" width="600" />
</a>

## Acknowledgements

Huge thanks to [li0ard](https://li0ard.rest/) for help with decoding RFIDs and dates!

## License

[MIT](./LICENSE)

## Donate

[hloth.dev/donate](https://hloth.dev/donate)
