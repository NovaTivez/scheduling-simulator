# Scheduling Simulator

ICS26012 Operating Systems - Group 7, 3ITE. An offline Java Swing application with four scheduling modules, a shared menu, About, Exit, and return navigation.

Repository: https://github.com/NovaTivez/scheduling-simulator

## Requirements and running

- Build: JDK **17 or later**, with `java`, `javac`, and `jar` on PATH.
- Run: Java 17 or later with a graphical desktop. No database, internet connection, or external dependencies.
- Verified with JDK 23.0.2, compiled using `--release 17`. Test on the actual laboratory runtime before submission.

From the repository root in PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File build.ps1
java -jar build/SchedulingSimulator.jar
```

The execution-policy option applies only to that process; it does not change machine policy. If local scripts are already allowed, `./build.ps1` also works.

To generate the source submission ZIP:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File build.ps1 -Package
```

Outputs:

- `build/SchedulingSimulator.jar`: executable application; main class `ui.MainMenu`.
- `build/SourceCode.zip`: source, JAR, README, build/test scripts, tests and project notes. Documentation.pdf and Presentation.mp4 are separate deliverables.

Generated files stay in ignored `build/`. Fresh staging directories prevent stale classes entering the JAR; remove old staging directories manually when no longer needed.

## Modules and ownership

| Module | Owner | Source | Status |
| --- | --- | --- | --- |
| Preemptive Priority | Hannah P. Muñoz | `src/scheduler/cpu/PriorityScheduling.java` | Integrated and tested |
| Round Robin | Roz Alexi A. Tengco | `src/scheduler/cpu/RoundRobinSimulator.java` | Integrated and tested; large-time limitation below |
| SSTF | Aurora Maria Gabrielle Nuestro | `src/scheduler/disk/SSTF.java` | Integrated and tested |
| C-LOOK | Aileen Shein V. Banzuela | `src/scheduler/disk/CLook.java` | Integrated and tested |

## Using the application

1. Choose a module under **CPU Scheduling** or **Disk Scheduling**.
2. Click **Load Sample Data**, or enter your own inputs. For SSTF, select **2 - Group demonstration (20 requests)** to use the same disk data as C-LOOK.
3. Click the module's Run button. CPU results include a time-scaled Gantt chart with idle time, completion/turnaround/waiting/response per process, averages, utilization and throughput. Disk results include service order, each movement, total, average seek distance and head-path graph.
4. Use scrollbars when tables, metrics or graphs exceed the window. C-LOOK's graph scrolls vertically and horizontally; its dashed red segment marks the counted return jump.
5. Clear/Reset clears the module. **Back to Menu** or the module's X returns to the menu. C-LOOK's existing **Close** button also returns. Reopening a module starts fresh.
6. **About** lists Group 7 and all four members. **Exit** or the menu's X ends the application.

CPU input tables support at least 10 processes. **Add Process** adds a row; the row count determines the number of processes. Priority also has **Remove Selected**. Enter PID, nonnegative arrival, positive burst, and nonnegative priority for Priority. RR requires a positive quantum. PIDs must be unique. Smaller priority values run first.

Disk queues support at least 20 requests. Use commas or spaces; enter disk size N and head/cylinders from 0 to N-1. C-LOOK accepts Up/Down. SSTF supports 100 requests and disk sizes 2-100,000; C-LOOK supports 1,000 requests and sizes 1-100,000.

## Demonstration samples

No facilitator practice datasets or answer keys were supplied. Included samples are **group demonstration inputs**. Outputs are computed at runtime, never stored as answers.

`src/samples/DemoData.java` provides a shared 10-process CPU dataset (RR quantum 3) and a shared 20-request disk dataset (head 100, size 200). Both CPU modules use identical PID/arrival/burst inputs; Priority adds its priority column. SSTF retains additional original examples.

## Scheduling conventions

- CPU starts at 0, shows idle intervals, and assumes zero context-switch time.
- Priority: lower number first, then earlier arrival, then lower PID; equal priority does not preempt; no aging.
- RR: FIFO ready queue; simultaneous arrivals ordered by PID; new arrivals queued before the process whose quantum expired.
- Turnaround = completion - arrival; waiting = turnaround - burst; response = first execution - arrival.
- Utilization = busy time / final completion time x 100; throughput = completed processes / final completion time. Initial idle time is included.
- SSTF: nearest request first; distance ties choose lower cylinder; duplicates have zero movement between them.
- C-LOOK: service in the chosen direction, jump at pending requests, and count the return jump in total movement and the graph.
- Disk total = sum of absolute differences starting from the initial head; average seek = total / requests.

## Integration and preservation

`ui.SimulatorModule` is the shared integration contract: name, category and window factory. `ModuleRegistry` adapts existing panels/frames; `ModuleWindow` supplies Back and safe close behavior. Existing algorithm classes keep their own scheduling/result models. This interface unifies module lifecycle rather than replacing owners' scheduling APIs.

Original standalone entry points remain available. C-LOOK now uses `package scheduler.disk;`. SSTF exposes its existing panel through a factory, and its standalone UI starts on Swing's event thread.

Scheduling/model regions remain unchanged from main commit `fdd99be`, protected by SHA-256 baseline tests. Changes to the original files concern packaging, sample loading and UI. SSTF now rejects a separator-only queue. RR metrics scroll horizontally, and loading a sample clears old results.

## Verification

```powershell
# Algorithms, capacities, and original-code preservation
powershell -NoProfile -ExecutionPolicy Bypass -File test.ps1

# Also opens/closes Swing windows and launches the executable JAR
powershell -NoProfile -ExecutionPolicy Bypass -File test.ps1 -Ui
```

The UI suite checks every menu route, About/Exit, Back/window X, sample loading, repeated runs, clearing, validation dialogs and recovery, 10-process inputs and 20-request inputs. It loads production classes from the JAR, then also launches the manifest entry point and checks clean exit. Screenshots go to `build/test-screenshots/`. Fixed expected/actual results are in `docs/TEST-LOG.md`.

## Known limits and remaining submission work

- **RR retains 32-bit integer times.** Very large accumulated times can overflow; huge idle gaps or very many quantum slices can block the UI. Use classroom-scale numbers. The group chose to preserve this original behavior.
- Dense Gantt charts may need a wider window for readable labels. Exact Priority intervals are also listed in its output text.
- Java 17 compatibility is targeted at compile time; actual runtime checks used JDK 23.0.2. Test the JAR on the laboratory machine.
- Generic guidelines specify one non-preemptive and one preemptive CPU algorithm, but the group's draft lists Preemptive Priority and RR. Confirm the assigned pair with the facilitator; the listed algorithms are preserved here.
- Complete Documentation.pdf and Presentation.mp4 separately, including hand solutions, comparisons, screenshots, signed contributions, test log and tool declaration. Declare AI assistance in Appendix D.

## Troubleshooting

- **Command not found:** install a full JDK, put its `bin` on PATH, and reopen the terminal.
- **UnsupportedClassVersionError:** use Java 17 or later and rebuild with the provided script.
- **Unable to access jarfile:** build first, run from the repository root, or supply the absolute JAR path.
- **Validation dialog:** correct the indicated field; check unique PIDs, positive burst/quantum and positions within the disk range.
- **Partly visible output:** enlarge the window and use the result/graph/metrics scrollbars.

Keep reports, algorithm explanations, screenshots and other documentation under `docs/`. Review any future algorithm changes with its owner and rerun the checks.
