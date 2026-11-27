package scheduler.disk;

import java.util.Arrays;
import java.util.List;

public final class DiskChecks {
    private static int checks;
    private static void equal(Object actual, Object expected) {
        checks++;
        if (!actual.equals(expected)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
    private static void path(int[] actual, int... expected) {
        equal(Arrays.toString(actual), Arrays.toString(expected));
    }
    public static void main(String[] args) {
        int[] textbook = {98, 183, 37, 122, 14, 124, 65, 67};
        var s = new SSTFScheduler();
        var result = s.schedule(textbook, 53);
        path(result.getPath(), 53, 65, 67, 37, 14, 98, 122, 124, 183);
        equal(result.getTotalMovement(), 236); equal(result.getAverageSeek(), 29.5);
        path(result.getMovements(), 12, 2, 30, 23, 84, 24, 2, 59);
        path(s.schedule(new int[]{60, 40, 40}, 50).getPath(), 50, 40, 40, 60);
        path(s.schedule(new int[]{199, 0, 0}, 0).getPath(), 0, 0, 0, 199);
        equal(s.schedule(new int[]{199}, 199).getTotalMovement(), 0);
        int[] queue = Arrays.stream(samples.DemoData.DISK_QUEUE.split(",\\s*")).mapToInt(Integer::parseInt).toArray();
        var twenty = s.schedule(queue, 100);
        path(twenty.getPath(), 100, 95, 90, 88, 70, 60, 45, 25, 10, 5, 0, 110, 115, 130, 130, 140, 150, 160, 175, 185, 199);
        equal(twenty.getTotalMovement(), 299); equal(twenty.getAverageSeek(), 14.95);
        path(s.schedule(queue, 100).getPath(), twenty.getPath());
        var c = new CLookScheduler();
        var up = c.schedule(textbook, 53, 200, DiskScheduler.Direction.UP);
        equal(up.getSeekSequence(), List.of(65, 67, 98, 122, 124, 183, 14, 37));
        equal(up.getTotalMovement(), 322); equal(up.getAverageSeek(), 40.25);
        equal(up.getSteps().get(6).jump, true); equal(up.getSteps().get(6).distance, 169);
        var down = c.schedule(textbook, 53, 200, DiskScheduler.Direction.DOWN);
        equal(down.getSeekSequence(), List.of(37, 14, 183, 124, 122, 98, 67, 65)); equal(down.getTotalMovement(), 326);
        var dup = c.schedule(new int[]{50, 50, 0, 199}, 50, 200, DiskScheduler.Direction.UP);
        equal(dup.getPath(), List.of(50, 50, 50, 199, 0)); equal(dup.getTotalMovement(), 348);
        equal(c.schedule(new int[]{0}, 199, 200, DiskScheduler.Direction.UP).getTotalMovement(), 199);
        equal(c.schedule(new int[]{199}, 0, 200, DiskScheduler.Direction.DOWN).getTotalMovement(), 199);
        var up20 = c.schedule(queue, 100, 200, DiskScheduler.Direction.UP);
        equal(up20.getSeekSequence(), List.of(110,115,130,130,140,150,160,175,185,199,0,5,10,25,45,60,70,88,90,95));
        equal(up20.getTotalMovement(), 393); equal(up20.getAverageSeek(), 19.65);
        equal(c.schedule(queue, 100, 200, DiskScheduler.Direction.DOWN).getTotalMovement(), 388);
        equal(c.schedule(queue, 100, 200, DiskScheduler.Direction.UP).getPath(), up20.getPath());
        path(textbook, 98, 183, 37, 122, 14, 124, 65, 67);
        System.out.println("PASS disk: " + checks + " sequence, movement, wrap, duplicate, boundary and capacity checks");
    }
}
