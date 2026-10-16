import { describe, expect, it } from 'vitest';
import { isCurrentSection, sections } from './sections.js';

describe('isCurrentSection', () => {
	it('matches the section path and its subroutes', () => {
		expect(isCurrentSection('/clients', '/clients')).toBe(true);
		expect(isCurrentSection('/clients/12', '/clients')).toBe(true);
	});

	it('does not match a longer sibling path', () => {
		expect(isCurrentSection('/clientsx', '/clients')).toBe(false);
	});

	it('matches no section on the home page', () => {
		for (const s of sections) expect(isCurrentSection('/', s.href)).toBe(false);
	});
});
