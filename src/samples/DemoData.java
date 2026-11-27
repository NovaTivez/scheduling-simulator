package samples;

/** Shared inputs only. These are group demonstration cases, not facilitator answer keys. */
public final class DemoData {
    private DemoData() { }

    public static Object[][] cpu(boolean includePriority) {
        int[][] rows = {
            {1, 2, 5, 3}, {2, 4, 3, 1}, {3, 4, 2, 1}, {4, 5, 1, 1}, {5, 14, 2, 2},
            {6, 15, 4, 0}, {7, 16, 2, 2}, {8, 18, 3, 1}, {9, 20, 1, 3}, {10, 22, 2, 2}
        };
        Object[][] result = new Object[rows.length][includePriority ? 4 : 3];
        for (int i = 0; i < rows.length; i++)
            for (int j = 0; j < result[i].length; j++) result[i][j] = rows[i][j];
        return result;
    }

    public static final int QUANTUM = 3;
    public static final int DISK_SIZE = 200;
    public static final int HEAD = 100;
    public static final String DISK_QUEUE =
        "45, 130, 10, 199, 88, 110, 150, 60, 175, 25, 130, 95, 70, 140, 5, 160, 115, 0, 185, 90";
}
