import type { Component } from 'svelte';
import type { RouteId } from '$app/types';
import { FileText, Receipt, Users, Package, ScanSearch, Settings } from '@lucide/svelte';

export interface Section {
	digit: string;
	title: string;
	href: RouteId;
	icon: Component;
}

export const sections: readonly Section[] = [
	{ digit: '1', title: 'Albaranes', href: '/delivery-notes', icon: FileText },
	{ digit: '2', title: 'Facturas', href: '/invoices', icon: Receipt },
	{ digit: '3', title: 'Clientes', href: '/clients', icon: Users },
	{ digit: '4', title: 'Artículos', href: '/articles', icon: Package },
	{ digit: '5', title: 'Trazabilidad', href: '/traceability', icon: ScanSearch },
	{ digit: '6', title: 'Ajustes', href: '/settings', icon: Settings }
];

/** True for the section's own path and anything below it (`/clients/12`), not `/clientsx`. */
export function isCurrentSection(pathname: string, href: string): boolean {
	return pathname === href || pathname.startsWith(href + '/');
}
