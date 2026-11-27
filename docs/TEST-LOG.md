# Integration and correctness test log

Date: November 28, 2026 (Asia/Manila). Windows, JDK 23.0.2, compiled for Java 17; no external libraries.

No facilitator datasets/answer keys were provided. Expected results below are independently traced demonstration cases. The tests do not certify unseen facilitator cases.

Verified result: **55 CPU checks, 28 disk checks, 77 GUI checks, all 5 preservation hashes, and the executable JAR launch/exit check passed.**

## Fixed expectations

CPU inputs below use (PID, arrival, burst, priority) for Priority and (PID, arrival, burst) for RR.

| Case | Inputs | Expected | Actual |
| --- | --- | --- | --- |
| Priority preemption/ties/idle | (1,2,5,3), (2,4,3,1), (3,4,2,1), (4,5,1,1), (5,14,2,2) | Idle0–2; P1 2–4; P2 4–7; P3 7–9; P4 9–10; P1 10–13; Idle13–14; P5 14–16. Completion by PID:13,7,9,10,16; first execution:2,4,7,9,14 | Match |
| Priority PID tie | (9,0,2,1), (2,0,1,1) | P2 first | Match |
| Priority strict preemption | (5,0,3,1), (1,1,1,1) | P5 continues until3 | Match |
| Priority capacity | 10 processes, arrival0/burst1/priority1, reverse PID input | Completion of Pi=i | Match |
| Priority single/idle | (1,3,2,0) | Idle0–3; completion5 | Match |
| RR arrival at quantum expiry | (1,0,4), (2,2,1), quantum2 | P1 0–2; P2 2–3; P1 3–5. AWT=.5; ATT=3; ART=0; utilization100%; throughput.4 | Match |
| RR single/idle | (1,3,2), quantum2 | Idle0–3; P1 3–5; utilization40%; throughput.2 | Match |
| RR simultaneous arrival | (9,0,1), (2,0,1), quantum2 | P2 then P9 | Match |
| RR capacity | 10 processes, arrival0/burst1, reverse PID input, quantum1 | Completion Pi=i; AWT4.5; ATT5.5; ART4.5; utilization100%; throughput1 | Match |
| SSTF standard | Queue98,183,37,122,14,124,65,67; head53 | Path53→65→67→37→14→98→122→124→183; movements12,2,30,23,84,24,2,59; total236; average29.5 | Match |
| SSTF ties/duplicates | Queue60,40,40; head50 | 50→40→40→60 | Match |
| SSTF boundary | Queue199,0,0; head0 | 0→0→0→199 | Match |
| SSTF single | Queue199; head199 | Movement0 | Match |
| C-LOOK Up | Standard queue above; head53 | 65,67,98,122,124,183,14,37; jump183→14=169; total322; average40.25 | Match |
| C-LOOK Down | Standard queue above; head53 | 37,14,183,124,122,98,67,65; total326 | Match |
| C-LOOK duplicates/head | Queue50,50,0,199; head50; Up | Path50→50→50→199→0; total348 | Match |
| C-LOOK single/opposite direction | Queue0,head199,Up; queue199,head0,Down | Movement199 in each case | Match |
| SSTF capacity | Shared20-request sample; head100 | Total299; average14.95 | Match |
| C-LOOK capacity Up | Same20-request sample | Total393; average19.65 | Match |
| C-LOOK capacity Down | Same20-request sample | Total388 | Match |
| Repeated runs | Reuse RR input; rerun disk queues and all GUI samples | Identical results; no state accumulation | Match |

Shared 20-request input, disk size 200, head 100:

```text
45,130,10,199,88,110,150,60,175,25,130,95,70,140,5,160,115,0,185,90
```

Expected SSTF path:

```text
100→95→90→88→70→60→45→25→10→5→0→110→115→130→130→140→150→160→175→185→199
```

Expected C-LOOK Up path (199→0 is the counted return jump):

```text
100→110→115→130→130→140→150→160→175→185→199→0→5→10→25→45→60→70→88→90→95
```

## GUI and packaging

`test.ps1 -Ui` loads production classes from the executable JAR first on its classpath. It checks:

- All four menu routes; CPU/Disk grouping; About with group/member names; Exit.
- Back and window-X return, using DISPOSE_ON_CLOSE for each module.
- Sample, run, repeat run, clear, and successful run after validation errors.
- Samples with 10 CPU processes or 20 disk requests (SSTF sample 2).
- CPU validation dialogs: blank, negative, nonnumeric, and integer-out-of-range arrival; zero burst; duplicate PID; zero RR quantum; empty inputs after Clear.
- Disk validation dialogs: blank, negative, nonnumeric, integer-out-of-range and head outside disk; empty/separator-only, negative, nonnumeric and out-of-disk queue; zero disk size; too many requests.
- Actual manifest launch of the JAR, main-window detection, clean exit, and empty error log.

GUI captures in `build/test-screenshots/` are reviewed for visible controls/results and usable scrollable regions. The source ZIP is inspected to confirm source, JAR and README are present.

Original scheduling/model regions are SHA-256 checked against main `fdd99be`, normalizing only CRLF/LF. The baseline file is `tests/algorithm-baselines.tsv`.

## Limits

- RR large-number overflow is intentionally unchanged at the user's request. Two burst times of 1,500,000,000 exceed its integer range. Huge idle intervals or many quantum slices can also block its UI. Verified workloads use classroom-scale times.
- No actual Java 17 laboratory runtime or unseen facilitator answer keys were available.
- Documentation.pdf and Presentation.mp4 remain separate deliverables. This log is supporting evidence, not the complete assessment documentation.
