import { describe, expect, it } from 'vitest';
import { fieldErrorMessages } from './field-errors';

describe('fieldErrorMessages', () => {
	it('falls back to a generic Spanish text', () => {
		expect(fieldErrorMessages({ nif: 'NIF cannot be blank' })).toEqual({ nif: 'Revisa este dato' });
	});

	it('uses the known text for the exact path', () => {
		expect(fieldErrorMessages({ nif: 'x' }, { nif: 'Falta el NIF' })).toEqual({
			nif: 'Falta el NIF'
		});
	});

	it('matches indexed paths with the index emptied', () => {
		expect(
			fieldErrorMessages({ 'lines[2].quantity': 'x' }, { 'lines[].quantity': 'Pon la cantidad' })
		).toEqual({ 'lines[2].quantity': 'Pon la cantidad' });
	});
});
