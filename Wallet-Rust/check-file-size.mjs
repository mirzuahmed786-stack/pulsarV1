#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';

function resolveRepoRoot() {
  const cwd = process.cwd();
  const candidates = [cwd, path.resolve(cwd, '..')];
  for (const candidate of candidates) {
    if (fs.existsSync(path.join(candidate, 'frontend', 'src'))) {
      return candidate;
    }
  }
  throw new Error(
    `Unable to resolve repo root from ${cwd}. Expected frontend/src in current or parent directory.`,
  );
}

const REPO_ROOT = resolveRepoRoot();

const RULES = [
  {
    label: 'React file',
    root: path.join(REPO_ROOT, 'frontend', 'src'),
    includeExt: '.tsx',
    maxLines: 350,
  },
  {
    label: 'TS service',
    root: path.join(REPO_ROOT, 'frontend', 'src', 'services'),
    includeExt: '.ts',
    maxLines: 250,
  },
];

function listFilesRecursive(dirPath) {
  if (!fs.existsSync(dirPath)) {
    return [];
  }

  const entries = fs.readdirSync(dirPath, { withFileTypes: true });
  const files = [];

  for (const entry of entries) {
    const fullPath = path.join(dirPath, entry.name);
    if (entry.isDirectory()) {
      files.push(...listFilesRecursive(fullPath));
      continue;
    }
    files.push(fullPath);
  }

  return files;
}

function countLines(filePath) {
  const content = fs.readFileSync(filePath, 'utf8');
  if (content.length === 0) {
    return 0;
  }
  return content.split(/\r?\n/).length;
}

const violations = [];

for (const rule of RULES) {
  const files = listFilesRecursive(rule.root)
    .filter((filePath) => filePath.endsWith(rule.includeExt))
    .filter((filePath) => !filePath.endsWith('.d.ts'));

  for (const filePath of files) {
    const lineCount = countLines(filePath);
    if (lineCount > rule.maxLines) {
      violations.push({
        rule: rule.label,
        filePath: path.relative(REPO_ROOT, filePath),
        lineCount,
        maxLines: rule.maxLines,
      });
    }
  }
}

if (violations.length > 0) {
  console.error('File size guard violations found:');
  for (const violation of violations) {
    console.error(
      `- ${violation.rule} ${violation.filePath}: ${violation.lineCount} LOC (max ${violation.maxLines})`,
    );
  }
  process.exit(1);
}

console.log('File size guards passed for frontend React and TS service files.');
