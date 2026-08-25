import {
	assertChecksum,
	assertInRange,
	assertLength,
	BLOCK_SIZE,
	isZero,
	withChecksum,
} from "./bytes";
import { decodeDate, encodeDate, type Date16Bit } from "./date";
import type { Direction } from "./transaction";
import { cardTypeByte, cardTypeFromByte, type CardTypeName } from "./type";

export type JourneySummary = {
	previous?: { route: number; direction: Direction };
	lastPaidAt: Date16Bit & { hour: number; minute: number };
	consecutivePayments: number;
	cardType: CardTypeName;
	free: boolean;
	route: number;
	direction: Direction;
	transfersLeft: number;
};

export const PERSONAL_JOURNEY_SUMMARY = Uint8Array.fromHex(
	"000000000000000A000000000000000A",
);

function direction(byte: number, name: string): Direction {
	if (byte !== 1 && byte !== 2)
		throw new Error(`${name} must be 1 or 2, got ${byte}`);
	return byte;
}

export function decodeJourneySummary(block: Uint8Array): JourneySummary {
	assertLength(block, BLOCK_SIZE, "journey summary block");
	assertChecksum(block);
	if (!isZero(block.subarray(11, 13)) || block[14] !== 0) {
		throw new Error("journey summary bytes 11, 12 and 14 must be zero");
	}
	assertInRange("hour", block[4]!, 0, 23);
	assertInRange("minute", block[5]!, 0, 59);
	const previousRoute = block[0]!;
	return {
		previous:
			previousRoute === 0
				? undefined
				: {
						route: previousRoute,
						direction: direction(block[1]!, "previous direction"),
					},
		lastPaidAt: {
			...decodeDate(block.subarray(2, 4)),
			hour: block[4]!,
			minute: block[5]!,
		},
		consecutivePayments: block[6]!,
		cardType: cardTypeFromByte(block[7]!),
		free: block[8] === 1,
		route: block[9]!,
		direction: direction(block[10]!, "direction"),
		transfersLeft: block[13]!,
	};
}

export function encodeJourneySummary(summary: JourneySummary): Uint8Array {
	assertInRange("hour", summary.lastPaidAt.hour, 0, 23);
	assertInRange("minute", summary.lastPaidAt.minute, 0, 59);
	assertInRange("consecutive payments", summary.consecutivePayments, 0, 0xff);
	assertInRange("route", summary.route, 0, 0xff);
	assertInRange("transfers left", summary.transfersLeft, 0, 0xff);
	const block = new Uint8Array(BLOCK_SIZE);
	if (summary.previous) {
		assertInRange("previous route", summary.previous.route, 1, 0xff);
		block[0] = summary.previous.route;
		block[1] = summary.previous.direction;
	}
	block.set(encodeDate(summary.lastPaidAt), 2);
	block[4] = summary.lastPaidAt.hour;
	block[5] = summary.lastPaidAt.minute;
	block[6] = summary.consecutivePayments;
	block[7] = cardTypeByte(summary.cardType);
	block[8] = summary.free ? 1 : 0;
	block[9] = summary.route;
	block[10] = summary.direction;
	block[13] = summary.transfersLeft;
	return withChecksum(block);
}
