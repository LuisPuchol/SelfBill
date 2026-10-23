import { defineConfig } from '@playwright/test';

export default defineConfig({
	testDir: 'e2e',
	testMatch: '**/*.e2e.ts',
	fullyParallel: false,
	workers: 1,
	use: {
		baseURL: 'http://127.0.0.1:18080',
		// The full Chromium, like the browser the app runs in: the default headless shell has no PDF
		// viewer, so a PDF in a frame (printing) becomes a download and never loads.
		channel: 'chromium'
	},
	webServer: {
		command: 'node e2e/serve-jar.mjs',
		url: 'http://127.0.0.1:18080/',
		reuseExistingServer: false,
		timeout: 120_000
	}
});
