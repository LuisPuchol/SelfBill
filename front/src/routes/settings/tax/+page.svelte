<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { todayIso } from '#lib/format/date.js';
	import { recallList, rememberList } from '#lib/records/list-memory.js';
	import TaxList from '#lib/settings/TaxList.svelte';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();
</script>

<TaxList
	periods={data.periods}
	today={todayIso()}
	selectId={recallList('settings-tax')?.openedId}
	onNew={() => void goto(resolve('/settings/tax/new'))}
	onOpen={(period) => {
		rememberList('settings-tax', { href: '/settings/tax', openedId: period.id });
		void goto(resolve('/settings/tax/[validFrom]', { validFrom: period.validFrom! }));
	}}
	onExit={() => void goto(resolve('/settings'))}
/>
