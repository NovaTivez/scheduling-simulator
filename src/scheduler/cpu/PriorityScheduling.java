package scheduler.cpu;

// Preemptive Priority Scheduling practice for Hannah P. Muñoz, 3ITE.
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class PriorityScheduling extends JPanel {
    private final DefaultTableModel input = new DefaultTableModel(
        new String[] {"PID", "Arrival Time", "Burst Time", "Priority"}, 1);
    private final JTable table = new JTable(input);
    private final JTextArea output = new JTextArea();
    private final GanttChart chart = new GanttChart();

    public PriorityScheduling() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel inputPanel = new JPanel(new BorderLayout(0, 6));
        inputPanel.add(new JLabel("Enter whole numbers. A smaller priority number runs first."),
                       BorderLayout.NORTH);
        JScrollPane inputScroll = new JScrollPane(table);
        inputScroll.setPreferredSize(new Dimension(700, 140));
        inputPanel.add(inputScroll, BorderLayout.CENTER);

        JButton add = new JButton("Add Process");
        add.addActionListener(event -> {
            finishEdit();
            input.addRow(new Object[4]);
            int row = input.getRowCount() - 1;
            table.changeSelection(row, 0, false, false);
            table.scrollRectToVisible(table.getCellRect(row, 0, true));
        });
        JButton remove = new JButton("Remove Selected");
        remove.addActionListener(event -> {
            finishEdit();
            int[] rows = table.getSelectedRows();
            for (int i = rows.length - 1; i >= 0; i--) {
                input.removeRow(rows[i]);
            }
            if (input.getRowCount() == 0) {
                input.addRow(new Object[4]);
            }
        });
        JButton sample = new JButton("Load Sample Data");
        sample.setToolTipText("Group demonstration dataset: 10 processes (not a facilitator problem set)");
        sample.addActionListener(event -> setRows(samples.DemoData.cpu(true)));
        JButton clear = new JButton("Clear");
        clear.addActionListener(event -> setRows(new Object[1][4]));
        JButton run = new JButton("Run Simulation");
        run.addActionListener(event -> runSimulation());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttons.add(add);
        buttons.add(remove);
        buttons.add(sample);
        buttons.add(clear);
        buttons.add(run);
        inputPanel.add(buttons, BorderLayout.SOUTH);
        add(inputPanel, BorderLayout.NORTH);

        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        output.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JPanel resultPanel = new JPanel(new BorderLayout(0, 6));
        resultPanel.add(chart, BorderLayout.NORTH);
        resultPanel.add(new JScrollPane(output), BorderLayout.CENTER);
        add(resultPanel, BorderLayout.CENTER);

        input.addTableModelListener(event -> {
            output.setText("");
            chart.showSlots(List.of());
        });
    }

    private void finishEdit() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
    }

    private void setRows(Object[][] rows) {
        finishEdit();
        input.setRowCount(0);
        for (Object[] row : rows) {
            input.addRow(row);
        }
    }

    private int readNumber(int row, int column, int minimum) {
        Object cell = input.getValueAt(row, column);
        String label = "Row " + (row + 1) + " (" + input.getColumnName(column) + ")";
        int number;
        try {
            number = Integer.parseInt(cell == null ? "" : cell.toString().trim());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(label + " needs a whole number from "
                + minimum + " to 2,147,483,647.");
        }
        if (number < minimum) {
            throw new IllegalArgumentException(label + " must be at least " + minimum + ".");
        }
        return number;
    }

    private void runSimulation() {
        finishEdit();
        try {
            List<Job> jobs = new ArrayList<>();
            Set<Integer> pids = new HashSet<>();
            for (int row = 0; row < input.getRowCount(); row++) {
                int pid = readNumber(row, 0, 0);
                if (!pids.add(pid)) {
                    throw new IllegalArgumentException("Duplicate PID " + pid + " on row " + (row + 1) + ".");
                }
                jobs.add(new Job(pid, readNumber(row, 1, 0),
                    readNumber(row, 2, 1), readNumber(row, 3, 0)));
            }
            List<Slot> slots = schedule(jobs);
            output.setText(formatResults(jobs, slots));
            output.setCaretPosition(0);
            chart.showSlots(slots);
        } catch (IllegalArgumentException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(),
                "Check process inputs", JOptionPane.WARNING_MESSAGE);
        }
    }

    // The algorithm is independent of the window. Each run starts with fresh jobs.
    static List<Slot> schedule(List<Job> jobs) {
        List<Job> arrivals = new ArrayList<>(jobs);
        arrivals.sort(Comparator.comparingInt(job -> job.arrival));
        PriorityQueue<Job> ready = new PriorityQueue<>(
            Comparator.comparingInt((Job job) -> job.priority)
                .thenComparingInt(job -> job.arrival)
                .thenComparingInt(job -> job.pid));
        List<Slot> slots = new ArrayList<>();

        Job running = null;
        int next = 0;
        long time = 0;
        long start = 0;

        while (next < arrivals.size() || running != null || !ready.isEmpty()) {
            if (running == null && ready.isEmpty() && arrivals.get(next).arrival > time) {
                long arrival = arrivals.get(next).arrival;
                slots.add(new Slot("Idle", time, arrival));
                time = arrival;
            }

            while (next < arrivals.size() && arrivals.get(next).arrival <= time) {
                ready.add(arrivals.get(next));
                next++;
            }

            // Equal priority lets the current process finish its turn.
            if (running != null && !ready.isEmpty()
                    && ready.peek().priority < running.priority) {
                slots.add(new Slot("P" + running.pid, start, time));
                ready.add(running);
                running = null;
            }

            if (running == null) {
                running = ready.remove();
                start = time;
                if (running.firstStart == -1) {
                    running.firstStart = time;
                }
            }

            // Advance to the current job's completion or the next arrival.
            long end = time + running.remaining;
            if (next < arrivals.size()) {
                end = Math.min(end, arrivals.get(next).arrival);
            }
            running.remaining -= (int) (end - time);
            time = end;

            if (running.remaining == 0) {
                running.completion = time;
                slots.add(new Slot("P" + running.pid, start, time));
                running = null;
            }
        }
        return slots;
    }

    private static String formatResults(List<Job> jobs, List<Slot> slots) {
        StringBuilder text = new StringBuilder();
        text.append(String.format("%-12s %11s %12s %12s %10s %10s%n",
            "PID", "First Start", "Completion", "Turnaround", "Waiting", "Response"));

        long busyTime = 0;
        double totalTurnaround = 0;
        double totalWaiting = 0;
        double totalResponse = 0;
        // Sort only the displayed results; preserve the scheduling and input order.
        List<Job> displayJobs = new ArrayList<>(jobs);
        displayJobs.sort(Comparator.comparingInt(job -> job.pid));
        for (Job job : displayJobs) {
            long turnaround = job.completion - job.arrival;
            long waiting = turnaround - job.burst;
            long response = job.firstStart - job.arrival;
            busyTime += job.burst;
            totalTurnaround += turnaround;
            totalWaiting += waiting;
            totalResponse += response;
            text.append(String.format("%-12s %11d %12d %12d %10d %10d%n",
                "P" + job.pid, job.firstStart, job.completion, turnaround, waiting, response));
        }

        int count = jobs.size();
        long totalTime = slots.get(slots.size() - 1).end;
        text.append(String.format(Locale.ROOT,
            "%nAverage turnaround: %.2f%nAverage waiting:    %.2f%nAverage response:   %.2f%n"
                + "CPU utilization:    %.2f%%%nThroughput:         %.4g processes per time unit%n",
            totalTurnaround / count, totalWaiting / count, totalResponse / count,
            100.0 * busyTime / totalTime, (double) count / totalTime));
        text.append("\nExact Gantt chart intervals\n");
        for (Slot slot : slots) {
            text.append(String.format("%-12s %d to %d%n", slot.label, slot.start, slot.end));
        }
        return text.toString();
    }

    static class Job {
        final int pid, arrival, burst, priority;
        int remaining;
        long firstStart = -1;
        long completion;

        Job(int pid, int arrival, int burst, int priority) {
            this.pid = pid;
            this.arrival = arrival;
            this.burst = burst;
            this.priority = priority;
            this.remaining = burst;
        }
    }

    static class Slot {
        final String label;
        final long start, end;

        Slot(String label, long start, long end) {
            this.label = label;
            this.start = start;
            this.end = end;
        }
    }

    private static class GanttChart extends JPanel {
        private List<Slot> slots = List.of();

        GanttChart() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(700, 100));
        }

        void showSlots(List<Slot> newSlots) {
            slots = newSlots;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.DARK_GRAY);
            g.drawString("Gantt chart", 16, 20);
            if (slots.isEmpty()) {
                g.drawString("Run the simulation to see the CPU schedule.", 16, 50);
                return;
            }

            int left = 24;
            long total = slots.get(slots.size() - 1).end;
            String endLabel = Long.toString(total);
            int endLabelWidth = g.getFontMetrics().stringWidth(endLabel);
            int width = Math.max(1, getWidth() - left - 16 - endLabelWidth / 2);
            int lastTickRight = left - 8;
            for (Slot slot : slots) {
                // Longer time intervals occupy more space on the same scale.
                int x = left + (int) Math.round((double) slot.start / total * width);
                int end = left + (int) Math.round((double) slot.end / total * width);
                g.setColor(slot.label.equals("Idle") ? new Color(0xE5E7EB) : new Color(0xBFDBFE));
                g.fillRect(x, 32, end - x, 30);
                g.setColor(Color.DARK_GRAY);
                g.drawRect(x, 32, end - x, 30);
                int labelWidth = g.getFontMetrics().stringWidth(slot.label);
                if (end - x >= labelWidth + 6) {
                    g.drawString(slot.label, x + (end - x - labelWidth) / 2, 52);
                }
                String tick = Long.toString(slot.start);
                int tickWidth = g.getFontMetrics().stringWidth(tick);
                int tickLeft = x - tickWidth / 2;
                if (tickLeft > lastTickRight
                        && tickLeft + tickWidth + 6 < left + width - endLabelWidth / 2) {
                    g.drawString(tick, tickLeft, 82);
                    lastTickRight = tickLeft + tickWidth + 6;
                }
            }
            g.drawString(endLabel, left + width - endLabelWidth / 2, 82);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Preemptive Priority Scheduling");
            window.setContentPane(new PriorityScheduling());
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setSize(765, 620);
            window.setMinimumSize(new Dimension(660, 525));
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}
