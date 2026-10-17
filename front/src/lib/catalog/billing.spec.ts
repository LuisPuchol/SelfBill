import { expect, it } from 'vitest';
import { missingBillingData, missingBillingMessage } from './billing';

it('lists the missing billing data in order', () => {
	expect(missingBillingData({ nif: '', address: 'x', postalCode: '', city: 'x' })).toEqual([
		'NIF',
		'código postal'
	]);
	expect(missingBillingData({ nif: ' ', address: ' ', postalCode: '', city: '' })).toEqual([
		'NIF',
		'dirección',
		'código postal',
		'población'
	]);
});

it('words the missing data', () => {
	expect(missingBillingMessage(['NIF'])).toBe('Para poder facturarle faltan: NIF.');
	expect(missingBillingMessage(['NIF', 'código postal'])).toBe(
		'Para poder facturarle faltan: NIF y código postal.'
	);
	expect(missingBillingMessage(['NIF', 'dirección', 'código postal', 'población'])).toBe(
		'Para poder facturarle faltan: NIF, dirección, código postal y población.'
	);
	expect(missingBillingMessage([])).toBeNull();
});
