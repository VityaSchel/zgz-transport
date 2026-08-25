# zgz-transport

Encoder and decoder for the Zaragoza and Aragon transport cards (Avanza Tarjeta Bus, Lazo), based on the reverse-engineered card spec at [https://git.hloth.dev/hloth/zgz-transport](https://git.hloth.dev/hloth/zgz-transport).

Install from [npm](https://www.npmjs.com/package/zgz-transport):

```sh
bun add zgz-transport
# npm install zgz-transport
# yarn install zgz-transport
# pnpm install zgz-transport
```

And [JSR](https://jsr.io/@hloth/zgz-transport):

```sh
bunx jsr add @hloth/zgz-transport
# npx jsr add @hloth/zgz-transport
# deno add jsr:@hloth/zgz-transport
# yarn add jsr:@hloth/zgz-transport
# pnpm add jsr:@hloth/zgz-transport
```

```ts
import { decodeCard, routeName } from "zgz-transport";

const card = decodeCard(await Bun.file("dump.bin").bytes());
console.log(card.id, card.balance / 1000);
for (const t of card.transactions) {
	console.log(t.createdAt, t.kind, routeName(t.route), t.amount);
}
```

Every block structure has its own `decode*`/`encode*` pair, see [src/index.ts](src/index.ts).
