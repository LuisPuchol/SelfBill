import type { components } from '../api/schema';
import { trimOrNull } from '../records/payload';

type ClientRequest = components['schemas']['ClientRequest'];

export interface ClientValues {
	code: number | null;
	name: string;
	businessName: string;
	nif: string;
	address: string;
	postalCode: string;
	city: string;
	province: string;
	phone: string;
	phone2: string;
	email: string;
	vatType: 'WITH_VAT' | 'WITHOUT_VAT';
	surchargeType: 'WITH_SURCHARGE' | 'WITHOUT_SURCHARGE';
	defaultDeliveryNoteFormat: 'PLAIN' | 'VALUED';
}

/** The API's own email pattern. */
export const EMAIL_PATTERN = /^[\w.-]+@[\w.-]+\.[a-zA-Z]{2,6}$/;

/** Names trimmed; optional texts trimmed, and null when empty. */
export function clientPayload(v: ClientValues): ClientRequest {
	return {
		code: v.code as number,
		name: v.name.trim(),
		businessName: v.businessName.trim(),
		// The API takes null for "nothing"; the generated type only models absence.
		nif: trimOrNull(v.nif) as string,
		address: trimOrNull(v.address) as string,
		postalCode: trimOrNull(v.postalCode) as string,
		city: trimOrNull(v.city) as string,
		province: trimOrNull(v.province) as string,
		phone: trimOrNull(v.phone) as string,
		phone2: trimOrNull(v.phone2) as string,
		email: trimOrNull(v.email) as string,
		vatType: v.vatType,
		surchargeType: v.surchargeType,
		defaultDeliveryNoteFormat: v.defaultDeliveryNoteFormat
	};
}

/** A new client: the next code and the usual defaults. */
export function blankClient(code: number | null): ClientValues {
	return {
		code,
		name: '',
		businessName: '',
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
}

/** The form's values for a client the API returned. */
export function clientValues(c: components['schemas']['ClientResponse']): ClientValues {
	return {
		code: c.code ?? null,
		name: c.name ?? '',
		businessName: c.businessName ?? '',
		nif: c.nif ?? '',
		address: c.address ?? '',
		postalCode: c.postalCode ?? '',
		city: c.city ?? '',
		province: c.province ?? '',
		phone: c.phone ?? '',
		phone2: c.phone2 ?? '',
		email: c.email ?? '',
		vatType: c.vatType ?? 'WITH_VAT',
		surchargeType: c.surchargeType ?? 'WITHOUT_SURCHARGE',
		defaultDeliveryNoteFormat: c.defaultDeliveryNoteFormat ?? 'PLAIN'
	};
}
