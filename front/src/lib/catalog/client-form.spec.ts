import { expect, it } from 'vitest';
import { clientPayload, EMAIL_PATTERN, type ClientValues } from './client-form';

const base: ClientValues = {
	code: 7,
	name: '  Bar  ',
	businessName: ' Bar SL ',
	nif: '',
	address: '',
	postalCode: '',
	city: '',
	province: '',
	phone: '',
	phone2: '',
	email: '',
	vatType: 'WITH_VAT',
	surchargeType: 'WITHOUT_SURCHARGE',
	defaultDeliveryNoteFormat: 'PLAIN'
};

it('trims names and sends empty optionals as null', () => {
	const p = clientPayload(base);
	expect(p.name).toBe('Bar');
	expect(p.businessName).toBe('Bar SL');
	expect(p.email).toBeNull();
	expect(p.postalCode).toBeNull();
	expect(p.nif).toBeNull();
	expect(p.vatType).toBe('WITH_VAT');
});

it('trims filled optionals', () => {
	expect(clientPayload({ ...base, nif: ' B123 ' }).nif).toBe('B123');
});

it('checks emails like the API', () => {
	expect(EMAIL_PATTERN.test('pedidos@frutas.es')).toBe(true);
	expect(EMAIL_PATTERN.test('pepe@')).toBe(false);
});
