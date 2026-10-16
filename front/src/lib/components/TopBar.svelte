<script lang="ts">
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { isCurrentSection, sections } from '../sections.js';
</script>

<header class="flex items-center gap-6 border-b border-line bg-surface px-4 py-2">
	<a href={resolve('/')} class="text-xl font-semibold">SelfBill</a>
	<nav class="flex gap-2">
		{#each sections as section (section.href)}
			{@const Icon = section.icon}
			<a
				href={resolve(section.href)}
				aria-current={isCurrentSection(page.url.pathname, resolve(section.href))
					? 'page'
					: undefined}
				class="flex items-center gap-2 rounded px-3 py-1 aria-[current=page]:bg-accent aria-[current=page]:text-accent-ink"
			>
				<Icon size={20} aria-hidden="true" />
				{section.title}
			</a>
		{/each}
	</nav>
</header>
