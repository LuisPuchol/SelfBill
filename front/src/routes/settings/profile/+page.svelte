<script lang="ts">
	import ProfileRecordPage from '#lib/settings/ProfileRecordPage.svelte';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();
</script>

<ProfileRecordPage initial={data.profile} />
