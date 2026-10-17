import { defineConfig } from '@playwright/test';

export default defineConfig({
	testDir: 'e2e',
	testMatch: '**/*.e2e.ts',
	use: { baseURL: 'http://127.0.0.1:18080' },
	webServer: {
		command: 'node e2e/serve-jar.mjs',
		url: 'http://127.0.0.1:18080/',
		reuseExistingServer: false,
		timeout: 120_000
	}
});
