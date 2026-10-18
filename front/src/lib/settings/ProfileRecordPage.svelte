<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { api, request } from '../api/client';
	import type { components } from '../api/schema';
	import { toast } from '../components/toast.svelte';
	import UnsavedGuard from '../components/UnsavedGuard.svelte';
	import ProfileRecord from './ProfileRecord.svelte';

	let { initial }: { initial: components['schemas']['UserProfileResponse'] | null } = $props();

	async function save(body: components['schemas']['UserProfileRequest'], exists: boolean) {
		await request(
			exists ? api.PUT('/api/user-profile', { body }) : api.POST('/api/user-profile', { body })
		);
	}
</script>

<!-- What a page does: the profile is read once, when the screen opens. -->
<ProfileRecord {initial} {save} goBack={() => goto(resolve('/settings'))} {toast}>
	{#snippet guard(form, save)}
		<UnsavedGuard isDirty={() => form.isDirty} {save} restoreFocus={() => form.restoreFocus()} />
	{/snippet}
</ProfileRecord>
