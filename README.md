# Scheduling Simulator

ICS26012 Operating Systems — Group 7, 3ITE. Java Swing application for CPU and disk scheduling.

## Current status

The workspace initially contained only Git metadata, with no commits or source files. There were no existing modules, shared interfaces, result classes, documentation, or entry points to reorganize or verify. The folders below are ready for incoming code; no scheduling behavior or runnable application has been implemented yet.

| Module | Owner | Destination / package | Status |
| --- | --- | --- | --- |
| Preemptive Priority Scheduling | Requesting group member; name pending | `src/scheduler/cpu/` / `scheduler.cpu` | Pending |
| Round Robin | Member assignment pending | `src/scheduler/cpu/` / `scheduler.cpu` | Pending |
| Shortest Seek Time First (SSTF) | Member assignment pending | `src/scheduler/disk/` / `scheduler.disk` | Pending |
| C-LOOK | Member assignment pending | `src/scheduler/disk/` / `scheduler.disk` | Pending |
| Shared Swing menu and screens | Group integration | `src/ui/` / `ui` | Pending |

## Repository layout

```text
src/
  scheduler/
    cpu/
    disk/
  ui/
docs/
README.md
.gitignore
```

Keep the root README as the project entry guide. Put reports, algorithm explanations, diagrams, screenshots, and other documentation in `docs/`. `.gitkeep` files preserve empty folders in Git and may be removed once those folders contain real files.

## JDK and build

Install a full JDK, including `javac`, and make both `java` and `javac` available on PATH. JDK 23.0.2 is installed in the current workspace environment. The group has not yet chosen a minimum supported JDK; use JDK 23 for initial local checks and confirm compatibility when modules arrive. There are currently no external dependencies or build-tool configuration.

From the repository root in PowerShell, compile all incoming Java sources with:

```powershell
$sources = @(Get-ChildItem -LiteralPath src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)
if ($sources.Count -eq 0) {
    Write-Host 'No Java modules have been added yet.'
} else {
    New-Item -ItemType Directory -Path build/classes -Force | Out-Null
    & javac -d build/classes $sources
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
}
```

This command currently reports that no modules are present. Compilation and entry-point verification remain pending until source code arrives.

After successful compilation, run a module using the fully qualified name of its class containing `public static void main(String[] args)`:

```text
java -cp build/classes <package.ClassName>
```

For example, if a teammate supplies `src/scheduler/cpu/RoundRobin.java` with `package scheduler.cpu;` and a `main` method, run `java -cp build/classes scheduler.cpu.RoundRobin`. This is an example, not an existing entry point. Document each actual entry point here when added. A shared Swing menu is still pending and has no run command yet.

## Adding a teammate's module

1. Add CPU files under `src/scheduler/cpu/` with `package scheduler.cpu;`, and disk files under `src/scheduler/disk/` with `package scheduler.disk;`. Public class names must match their filenames.
2. Keep each algorithm and its helper files together while reviewing dependencies. For larger modules, use matching subfolders and packages, such as `src/scheduler/cpu/roundrobin/` and `scheduler.cpu.roundrobin`.
3. If a standalone file combines Swing UI and scheduling logic, keep it together in its algorithm package and retain its existing `main()` method. Update package declarations, imports, resource paths, and references as needed, then compile and run it before extracting shared code.
4. Put genuinely shared Swing screens and the eventual main menu under `src/ui/` with `package ui;`. Keep algorithm-specific screens with their module until dependencies are understood.
5. Before adding shared interfaces or result classes, inspect all incoming modules. Compare same-named classes by purpose and behavior before merging them. Use module subpackages to keep unrelated helpers distinct if needed.
6. Add required assets and document any libraries or JDK requirements. Update the module table with the owner's name, actual file location, entry point, and verification status.

## Decisions before group integration

- Confirm member names, module assignments, and the shared JDK version.
- Review the four implementations before choosing shared input and result interfaces. CPU timing results and disk seek results may require different models.
- Agree on input validation, priority-number ordering and tie-breaking, Round Robin quantum handling, disk bounds, and C-LOOK direction/wrap accounting. Preserve and document incoming behavior before proposing changes.
- Decide how the shared menu opens each module and returns to the menu. Review standalone close behavior such as `EXIT_ON_CLOSE` or `System.exit` before embedding screens, and use Swing's event dispatch thread for UI creation.
- Compile and launch every supplied entry point, check representative scheduling results, and then verify the integrated menu.

No shared interfaces have been introduced in advance of the incoming implementations.
