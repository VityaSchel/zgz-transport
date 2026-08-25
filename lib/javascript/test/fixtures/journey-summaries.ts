import type { JourneySummary } from "../../src/journey-summary";

export const journeySummaries: { encoded: string; decoded: JourneySummary }[] =
	[
		{
			encoded: "00002ADC091B010200D2020000630054",
			decoded: {
				previous: undefined,
				lastPaidAt: { year: 2021, month: 6, day: 28, hour: 9, minute: 27 },
				consecutivePayments: 1,
				cardType: "AvanzaTopUp",
				free: false,
				route: 210,
				direction: 2,
				transfersLeft: 0x63,
			},
		},
		{
			encoded: "D2012AD90D33010200D20200006300AE",
			decoded: {
				previous: { route: 210, direction: 1 },
				lastPaidAt: { year: 2021, month: 6, day: 25, hour: 13, minute: 51 },
				consecutivePayments: 1,
				cardType: "AvanzaTopUp",
				free: false,
				route: 210,
				direction: 2,
				transfersLeft: 0x63,
			},
		},
		{
			encoded: "23023454102D0102001601000063000B",
			decoded: {
				previous: { route: 35, direction: 2 },
				lastPaidAt: { year: 2026, month: 2, day: 20, hour: 16, minute: 45 },
				consecutivePayments: 1,
				cardType: "AvanzaTopUp",
				free: false,
				route: 22,
				direction: 1,
				transfersLeft: 0x63,
			},
		},
		{
			encoded: "16013468002B04020016020000630011",
			decoded: {
				previous: { route: 22, direction: 1 },
				lastPaidAt: { year: 2026, month: 3, day: 8, hour: 0, minute: 43 },
				consecutivePayments: 4,
				cardType: "AvanzaTopUp",
				free: false,
				route: 22,
				direction: 2,
				transfersLeft: 0x63,
			},
		},
		{
			encoded: "1F013518160B010D01D2020000620091",
			decoded: {
				previous: { route: 31, direction: 1 },
				lastPaidAt: { year: 2026, month: 8, day: 24, hour: 22, minute: 11 },
				consecutivePayments: 1,
				cardType: "LazoTopUp",
				free: true,
				route: 210,
				direction: 2,
				transfersLeft: 0x62,
			},
		},
	];
