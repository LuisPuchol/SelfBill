// Runs the packaged jar (front included) on a port and with a database and key
// that are not the real ones: 18080 and a temp dir, never 8080 or the user's data.
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const targetDir = path.join(root, '..', 'selfbill-api', 'target');
const jarName = (fs.existsSync(targetDir) ? fs.readdirSync(targetDir) : []).find(
	(f) => /^selfbill-api-.*\.jar$/.test(f) && !f.endsWith('.original')
);
if (!jarName) {
	console.error(`No jar in ${targetDir}; run "npm run package" first.`);
	process.exit(1);
}

// Playwright may kill this process outright (no exit handler runs), so also
// sweep the dirs that earlier runs could not remove.
for (const name of fs.readdirSync(os.tmpdir())) {
	if (!name.startsWith('selfbill-e2e-')) continue;
	try {
		fs.rmSync(path.join(os.tmpdir(), name), { recursive: true, force: true });
	} catch {
		// Still in use or locked: leave it.
	}
}

const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'selfbill-e2e-'));
// The jar needs the JDK it was compiled with, which may not be the one on PATH.
const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', 'java') : 'java';

const child = spawn(
	java,
	[
		'-jar',
		path.join(targetDir, jarName),
		'--server.port=18080',
		`--spring.datasource.url=jdbc:sqlite:${path.join(tmp, 'e2e.db')}?foreign_keys=true&journal_mode=WAL&transaction_mode=IMMEDIATE`,
		`--selfbill.credentials.key-file=${path.join(tmp, 'secret.key')}`
	],
	{ stdio: 'inherit' }
);

// Windows does not deliver SIGTERM to a child, so also die when stdin closes.
const cleanup = () => {
	try {
		fs.rmSync(tmp, { recursive: true, force: true, maxRetries: 30, retryDelay: 100 });
	} catch {
		// The JVM may still hold the db open for a moment after kill; rmSync retries.
	}
};
process.on('exit', () => {
	child.kill();
	cleanup();
});
for (const sig of ['SIGINT', 'SIGTERM', 'SIGBREAK']) {
	process.on(sig, () => process.exit());
}
process.stdin.on('close', () => process.exit());
process.stdin.resume();
child.on('error', (err) => {
	console.error(`Could not start Java at "${java}": ${err.message}`);
	process.exit(1);
});
child.on('exit', (code) => process.exit(code ?? 1));
