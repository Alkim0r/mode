# Regnum Git workflow

This folder is the shared, local Git repository for the canonical project. `main` is the common baseline; each Codex task gets its own branch and worktree. Claude stays on this folder and continues communicating through `coord/INBOX_*` until it is explicitly moved to Git.

Do not commit generated Minecraft worlds, build caches, logs, the downloaded reference-mod JARs, or local credentials. The ignored `_refs` binaries remain available in the canonical workspace for offline reading; worktree agents must read them through `../regnum/_refs` and must not copy their assets into Regnum.

Before implementation, reserve files in `coord/LOCKS.md`; before any build, reserve `coord/BUILD.lock`. In a worktree, run Gradle with `-p <worktree>` and the project Java 21 runtime. A task branch is ready for review only after build and the server selftest pass. Merge reviewed task branches to `main`; do not merge concurrent changes automatically. `origin` points to `https://github.com/Alkim0r/mode.git`; push only with ordinary non-force `git push origin main`, after confirming the worktree and build lock state.

## Current bootstrap status
The initial `main` commit is a snapshot of the shared working tree during active development; it is a recovery/baseline point, not a release and does not certify that this exact snapshot passed tests. Claude may keep working in the canonical checkout while Codex tasks use separate worktrees. The `coord/BUILD.lock` is ignored by Git: each worktree has its own copy, so concurrent builds are allowed only when they use separate worktree paths and runtime directories.

Portable Git is stored at `.codex-tools/mingit/cmd/git.exe` and intentionally ignored. On this Windows sandbox, invoke it with `-c safe.directory=<exact checkout path>` because checkout owner metadata differs. Never use `safe.directory=*`. Reference binaries under `_refs/` stay local and are excluded wholesale.
