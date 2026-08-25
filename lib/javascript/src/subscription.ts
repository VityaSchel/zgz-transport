import {
	assertChecksum,
	assertLength,
	BLOCK_SIZE,
	isZero,
	withChecksum,
} from "./bytes";
import { decodeDate, encodeDate, type Date16Bit } from "./date";
import { decodeTime, encodeTime, type Time } from "./time";

export type Subscription = {
	startsAt: Date16Bit;
	endsAt: Date16Bit;
	unknown1: Uint8Array;
	unknown2: Uint8Array;
	lastUsedAt?: Date16Bit & Time;
};

export function decodeSubscription(block: Uint8Array): Subscription {
	assertLength(block, BLOCK_SIZE, "subscription block");
	assertChecksum(block);
	const lastUsed = block.subarray(10, 15);
	return {
		startsAt: decodeDate(block.subarray(0, 2)),
		endsAt: decodeDate(block.subarray(2, 4)),
		unknown1: block.slice(4, 6),
		unknown2: block.slice(6, 10),
		lastUsedAt: isZero(lastUsed)
			? undefined
			: {
					...decodeDate(lastUsed.subarray(0, 2)),
					...decodeTime(lastUsed.subarray(2, 5)),
				},
	};
}

export function encodeSubscription(subscription: Subscription): Uint8Array {
	assertLength(subscription.unknown1, 2, "unknown1");
	assertLength(subscription.unknown2, 4, "unknown2");
	const block = new Uint8Array(BLOCK_SIZE);
	block.set(encodeDate(subscription.startsAt), 0);
	block.set(encodeDate(subscription.endsAt), 2);
	block.set(subscription.unknown1, 4);
	block.set(subscription.unknown2, 6);
	if (subscription.lastUsedAt) {
		block.set(encodeDate(subscription.lastUsedAt), 10);
		block.set(encodeTime(subscription.lastUsedAt), 12);
	}
	return withChecksum(block);
}
