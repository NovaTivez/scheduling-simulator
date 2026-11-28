package scheduler.cpu;

import java.util.ArrayList;
import java.util.List;

/** Fixed, hand-checkable expectations independent of the UI. */
public final class CpuChecks {
    private static int checks;
    private static void equal(Object actual, Object expected) {
        checks++;
        if (!actual.equals(expected)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
    private static void near(double actual, double expected) {
        checks++;
        if (Math.abs(actual - expected) > 0.000001) throw new AssertionError(actual + " != " + expected);
    }
    private static RoundRobinSimulator.Process process(int pid, int at, int bt) {
        return new RoundRobinSimulator.Process(pid, at, bt);
    }
    private static String rrPath(RoundRobinSimulator.SchedulerResult result) {
        return result.ganttChart.stream().map(s -> s.pid + ":" + s.startTime + "-" + s.endTime).toList().toString();
    }
    public static void main(String[] args) {
        var jobs = new ArrayList<PriorityScheduling.Job>();
        jobs.add(new PriorityScheduling.Job(1, 2, 5, 3));
        jobs.add(new PriorityScheduling.Job(2, 4, 3, 1));
        jobs.add(new PriorityScheduling.Job(3, 4, 2, 1));
        jobs.add(new PriorityScheduling.Job(4, 5, 1, 1));
        jobs.add(new PriorityScheduling.Job(5, 14, 2, 2));
        var slots = PriorityScheduling.schedule(jobs);
        equal(slots.stream().map(s -> s.label + ":" + s.start + "-" + s.end).toList().toString(),
            "[Idle:0-2, P1:2-4, P2:4-7, P3:7-9, P4:9-10, P1:10-13, Idle:13-14, P5:14-16]");
        long[] completion = {13, 7, 9, 10, 16}, first = {2, 4, 7, 9, 14};
        for (int i = 0; i < jobs.size(); i++) {
            equal(jobs.get(i).completion, completion[i]); equal(jobs.get(i).firstStart, first[i]);
        }
        var ties = new ArrayList<PriorityScheduling.Job>();
        ties.add(new PriorityScheduling.Job(9, 0, 2, 1));
        ties.add(new PriorityScheduling.Job(2, 0, 1, 1));
        equal(PriorityScheduling.schedule(ties).get(0).label, "P2");
        var equalPriority = new ArrayList<PriorityScheduling.Job>();
        equalPriority.add(new PriorityScheduling.Job(5, 0, 3, 1));
        equalPriority.add(new PriorityScheduling.Job(1, 1, 1, 1));
        equal(PriorityScheduling.schedule(equalPriority).get(0).end, 3L);
        var ten = new ArrayList<PriorityScheduling.Job>();
        for (int i = 10; i >= 1; i--) ten.add(new PriorityScheduling.Job(i, 0, 1, 1));
        equal(PriorityScheduling.schedule(ten).size(), 10);
        for (var job : ten) equal(job.completion, (long) job.pid);
        var single = new ArrayList<PriorityScheduling.Job>();
        single.add(new PriorityScheduling.Job(1, 3, 2, 0));
        equal(PriorityScheduling.schedule(single).get(0).label, "Idle"); equal(single.get(0).completion, 5L);

        var scheduler = new RoundRobinSimulator.RoundRobinScheduler();
        var input = new ArrayList<>(List.of(process(1, 0, 4), process(2, 2, 1)));
        var result = scheduler.schedule(input, 2);
        equal(rrPath(result), "[1:0-2, 2:2-3, 1:3-5]");
        equal(result.completedProcesses.get(0).completionTime, 5);
        equal(result.completedProcesses.get(1).completionTime, 3);
        near(result.avgWaiting, 0.5); near(result.avgTurnaround, 3); near(result.avgResponse, 0);
        near(result.cpuUtilization, 100); near(result.throughput, 0.4);
        equal(rrPath(scheduler.schedule(input, 2)), rrPath(result));
        var idle = scheduler.schedule(new ArrayList<>(List.of(process(1, 3, 2))), 2);
        equal(rrPath(idle), "[-1:0-3, 1:3-5]"); near(idle.cpuUtilization, 40); near(idle.throughput, 0.2);
        var simultaneous = scheduler.schedule(new ArrayList<>(List.of(process(9, 0, 1), process(2, 0, 1))), 2);
        equal(rrPath(simultaneous), "[2:0-1, 9:1-2]");
        var tenRr = new ArrayList<RoundRobinSimulator.Process>();
        for (int i = 10; i >= 1; i--) tenRr.add(process(i, 0, 1));
        var tenResult = scheduler.schedule(tenRr, 1);
        equal(tenResult.completedProcesses.size(), 10);
        for (var p : tenResult.completedProcesses) equal(p.completionTime, p.pid);
        near(tenResult.avgWaiting, 4.5); near(tenResult.avgTurnaround, 5.5); near(tenResult.avgResponse, 4.5);
        near(tenResult.cpuUtilization, 100); near(tenResult.throughput, 1);
        System.out.println("PASS CPU: " + checks + " sequence, metric, tie, capacity and repeat-run checks");
    }
}
