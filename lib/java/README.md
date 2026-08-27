# zgz-transport

Encoder and decoder for the Zaragoza and Aragon transport cards (Avanza Tarjeta Bus, Lazo), based on the reverse-engineered card spec at [https://git.hloth.dev/hloth/zgz-transport](https://git.hloth.dev/hloth/zgz-transport).

Zero runtime dependencies, usable from Java >= 17, Kotlin and Android API >= 26, MIT License.

## Install

**From [Maven Central](https://central.sonatype.com/artifact/dev.hloth/zgz-transport):**

Java:

```xml
<dependency>
	<groupId>dev.hloth</groupId>
	<artifactId>zgz-transport</artifactId>
	<version>2.0.0</version>
</dependency>
```

Kotlin:

```kotlin
implementation("dev.hloth:zgz-transport:2.0.0")
```

**From [git.hloth.dev Maven repository](https://git.hloth.dev/hloth/-/packages/maven/dev.hloth:zgz-transport/2.0.0):**

Java:

```xml
<repositories>
	<repository>
		<id>forgejo</id>
		<url>https://git.hloth.dev/api/packages/hloth/maven</url>
	</repository>
</repositories>
```

```xml
<dependency>
	<groupId>dev.hloth</groupId>
	<artifactId>zgz-transport</artifactId>
	<version>2.0.0</version>
</dependency>
```

Kotlin:

```kotlin
repositories {
	maven("https://git.hloth.dev/api/packages/hloth/maven")
}
```

```kotlin
implementation("dev.hloth:zgz-transport:2.0.0")
```

## Usage

Decode a dump read with MIFARE Classic Tool or a Proxmark, at least blocks 0 to 33:

```java
Card card = Card.decode(Files.readAllBytes(Path.of("dump.bin")));
System.out.println(card.uid() + " " + card.id() + " " + card.balance());
for (Transaction t : card.transactions()) {
	System.out.println(t.createdAt() + " " + t.route() + " " + t.amount());
}
```

`Dump` addresses the blocks of a card, so one structure can be read without slicing the array, and builds a dump block by block:

```java
Dump dump = Dump.of(Files.readAllBytes(Path.of("dump.bin")));
Transaction newest = Transaction.decode(dump.block(TransactionLog.LIVE_BLOCK));

Dump written = Dump.builder(Chip.CLASSIC_1K)
		.block(1, CardType.AVANZA_TOP_UP)
		.block(2, CardId.parse("BE123456"))
		.block(8, new Balance(4450))
		.build();
```

Every structure the card holds is a type with a `decode` that reads its bytes and an `encode` that writes them back. The types check their fields when they are built, so encoding never fails, and decoding throws `CardFormatException` when the bytes do not hold what the spec says:

```java
Transaction ride = Transaction.builder()
		.cardType(CardType.AVANZA_TOP_UP)
		.amount(550)
		.consecutivePayments(1)
		.stop(new Stop.Urban(500))
		.route(new Route(31))
		.kind(new TransactionKind.Journey(Direction.TWO))
		.runCounter(7)
		.createdAt(CardDateTime.of(2026, 8, 26, 9, 41, 27))
		.sequence(1)
		.build();

byte[] block5 = ride.encode();
byte[] block8 = new Balance(4450).encode();
byte[] block2 = CardId.parse("BE123456").encode();
SectorKeys keys = CardType.AVANZA_TOP_UP.keys(0).orElseThrow();
```

Kotlin sees the record components as properties and gets exhaustive `when` over the sealed types:

```kotlin
val card = Card.decode(Files.readAllBytes(Path.of("dump.bin")))
println("${card.uid} ${card.id} ${card.balance}")

for (transaction in card.transactions) {
	val where = when (val stop = transaction.stop) {
		is Stop.Urban -> "urban stop ${stop.id}"
		is Stop.Tram -> "tram stop ${stop.number()}"
		is Stop.Other -> "operator stop ${stop.id}"
	}
	val what = when (val kind = transaction.kind) {
		is TransactionKind.Journey -> "journey ${kind.direction}"
		is TransactionKind.TopUp -> "top up"
	}
	println("${transaction.createdAt} ${transaction.route} $what at $where")
}

val summary: JourneySummary? = card.journeySummary.getOrNull()
```

Build transactions with the builder rather than constructor in Kotlin.

## Verifying the build

The published jars are reproducible. Build from a git clone, since the timestamps inside the archives come from the release commit, with `umask 022` and the JDK the release workflow pins, the exact version must be Eclipse Temurin 21.0.12.1+1. OS and arch do not matter.

```sh
git clone https://git.hloth.dev/hloth/zgz-transport.git && cd zgz-transport/lib/java
git checkout v2.0.0
./mvnw -B -DskipTests package
shasum -a 256 target/zgz-transport-*.jar

curl -sO https://repo1.maven.org/maven2/dev/hloth/zgz-transport/2.0.0/zgz-transport-2.0.0.jar
shasum -a 256 zgz-transport-2.0.0.jar
```

## License

[MIT](../../LICENSE)

## Donate

[hloth.dev/donate](https://hloth.dev/donate)
