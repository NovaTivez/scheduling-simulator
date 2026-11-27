// C-LOOK Algorithm
// Aileen Shein V. Banzuela - Group 7

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

interface DiskScheduler {

    enum Direction { UP, DOWN }

    String getName();

    DiskResult schedule(int[] requests, int head, int diskSize, Direction dir);
}

// Stores the seek sequence and head movements
class DiskResult {
    public static class Step {
        public final int from, to, distance;
        public final boolean jump;
        Step(int from, int to, boolean jump) {
            this.from = from;
            this.to = to;
            this.distance = Math.abs(to - from);
            this.jump = jump;
        }
    }

    private final int startHead;
    private int current;
    private int total;
    private final List<Step> steps = new ArrayList<>();

    public DiskResult(int startHead) {
        this.startHead = startHead;
        this.current = startHead;
    }

    public void addStep(int to, boolean jump) {
        // Include the return jump in total movement
        Step s = new Step(current, to, jump);
        steps.add(s);
        total += s.distance;
        current = to;
    }

    public int getStartHead() {

        return startHead;

    }
    public List<Step> getSteps() {
        return steps;
    }
    public int getTotalMovement() {
        return total;
    }

    public double getAverageSeek() {
        // Divide total movement by the number of requests
        return steps.isEmpty() ? 0 : (double) total / steps.size();
    }

    public boolean hasJump() {
        for (Step s : steps) if (s.jump) return true;
        return false;
    }

    public List<Integer> getSeekSequence() {
        List<Integer> seq = new ArrayList<>();
        for (Step s : steps) seq.add(s.to);
        return seq;
    }

    public List<Integer> getPath() {
        List<Integer> p = new ArrayList<>();
        p.add(startHead);
        p.addAll(getSeekSequence());
        return p;
    }
}

class CLookScheduler implements DiskScheduler {

    @Override
    public String getName() {
        return "C-LOOK";
    }

    @Override
    public DiskResult schedule(int[] requests, int head, int diskSize, Direction dir) {
        // Sort the requests without changing the original array
        int[] sorted = requests.clone();
        Arrays.sort(sorted);

        // Split the requests into two groups
        List<Integer> first = new ArrayList<>();
        List<Integer> second = new ArrayList<>();

        // Serve requests at the current head position first
        if (dir == Direction.UP) {
            // Move from lower to higher cylinders
            for (int v : sorted) {
                if (v >= head) first.add(v); else second.add(v);
            }
        } else {
            // Move from higher to lower cylinders
            for (int i = sorted.length - 1; i >= 0; i--) {
                int v = sorted[i];
                if (v <= head) first.add(v); else second.add(v);
            }
        }

        DiskResult result = new DiskResult(head);
        // Serve requests in the chosen direction first
        for (int v : first) result.addStep(v, false);

        // Jump to the first remaining request, then continue
        boolean jump = true;
        for (int v : second) {
            result.addStep(v, jump);
            jump = false; 
        }
        return result;
    }
}

class PathPanel extends JPanel {
    private static final int LEFT = 40, RIGHT = 40, TOP = 55, ROW = 30, BOTTOM = 25;
    private static final Color SEEK = new Color(0x1F6FEB);
    private static final Color JUMP = new Color(0xD1242F);

    private DiskResult result;
    private int diskSize = 1;

    PathPanel() {
        setBackground(Color.WHITE);
    }

    void setResult(DiskResult r, int diskSize) {
        this.result = r;
        this.diskSize = diskSize;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int rows = result == null ? 1 : result.getPath().size();
        return new Dimension(600, TOP + ROW * rows + BOTTOM);
    }

    private int xOf(int cyl, int plotW) {
        return LEFT + (int) Math.round((double) cyl / Math.max(1, diskSize - 1) * plotW);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (result == null) {
            g.setColor(Color.GRAY);
            g.drawString("Run the scheduler to see the head path.", 20, 30);
            g.dispose();
            return;
        }

        int plotW = Math.max(100, getWidth() - LEFT - RIGHT);
        FontMetrics fm = g.getFontMetrics();

        g.setColor(Color.DARK_GRAY);
        g.drawLine(LEFT, 30, LEFT + plotW, 30);
        String zero = "0", last = String.valueOf(diskSize - 1);
        g.drawLine(LEFT, 26, LEFT, 34);
        g.drawLine(LEFT + plotW, 26, LEFT + plotW, 34);
        g.drawString(zero, LEFT - fm.stringWidth(zero) / 2, 20);
        g.drawString(last, LEFT + plotW - fm.stringWidth(last) / 2, 20);

        List<Integer> path = result.getPath();
        List<DiskResult.Step> steps = result.getSteps();

        Stroke solid = new BasicStroke(2f);
        Stroke dashed = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{6f, 4f}, 0f);
        for (int i = 0; i < steps.size(); i++) {
            DiskResult.Step s = steps.get(i);
            int x1 = xOf(path.get(i), plotW), y1 = TOP + i * ROW;
            int x2 = xOf(path.get(i + 1), plotW), y2 = TOP + (i + 1) * ROW;
            g.setStroke(s.jump ? dashed : solid);
            g.setColor(s.jump ? JUMP : SEEK);
            g.drawLine(x1, y1, x2, y2);
        }

        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i < path.size(); i++) {
            int x = xOf(path.get(i), plotW), y = TOP + i * ROW;
            g.setColor(i == 0 ? new Color(0x1A7F37) : Color.BLACK);
            g.fillOval(x - 4, y - 4, 8, 8);
            String label = path.get(i) + (i == 0 ? " (start)" : "");
            int lx = x + 8;
            if (lx + fm.stringWidth(label) > getWidth() - 4) lx = x - 8 - fm.stringWidth(label);
            g.drawString(label, lx, y + 4);
        }

        int ly = TOP + ROW * path.size() - 8;
        g.setColor(SEEK);
        g.setStroke(solid);
        g.drawLine(LEFT, ly, LEFT + 24, ly);
        g.setColor(Color.DARK_GRAY);
        g.drawString("Seek", LEFT + 30, ly + 4);
        g.setColor(JUMP);
        g.setStroke(dashed);
        g.drawLine(LEFT + 80, ly, LEFT + 104, ly);
        g.setColor(Color.DARK_GRAY);
        g.drawString("Return jump", LEFT + 110, ly + 4);
        g.dispose();
    }
}

public class CLook extends JFrame {

    private static final int DEFAULT_DISK_SIZE = 200;
    private final JTextField diskSizeField = new JTextField(String.valueOf(DEFAULT_DISK_SIZE), 6);
    private final JTextField headField = new JTextField(6);
    private final JTextField requestsField = new JTextField(28);
    private final JRadioButton upBtn = new JRadioButton("Up (toward higher)", true);
    private final JRadioButton downBtn = new JRadioButton("Down (toward lower)");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Step", "From", "To", "Movement", "Type"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTextField sequenceField = new JTextField();
    private final JLabel totalLabel = new JLabel("Total head movement: -");
    private final JLabel avgLabel = new JLabel("Average seek distance: -");
    private final PathPanel pathPanel = new PathPanel();

    public CLook() {
        this(null);
    }

    public CLook(Runnable onReturn) {
        super("C-LOOK Disk Scheduling");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        if (onReturn != null) {
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    onReturn.run();
                }
            });
        }
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildInputPanel(onReturn != null), BorderLayout.NORTH);
        add(buildResultPanel(), BorderLayout.CENTER);

        setMinimumSize(new Dimension(820, 520));
        setSize(1000, 640);
        setLocationRelativeTo(null);
    }

    // Input fields and buttons
    private JPanel buildInputPanel(boolean hasMenu) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Input"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("Disk size (cylinders):"), c);
        c.gridx = 1;
        p.add(diskSizeField, c);
        c.gridx = 2;
        p.add(new JLabel("Initial head position:"), c);
        c.gridx = 3;
        p.add(headField, c);

        c.gridx = 0;
        c.gridy = 1;
        p.add(new JLabel("Requests (comma/space separated):"), c);
        c.gridx = 1;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        p.add(requestsField, c);
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;

        ButtonGroup bg = new ButtonGroup();
        bg.add(upBtn);
        bg.add(downBtn);
        JPanel dirPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dirPanel.add(upBtn);
        dirPanel.add(downBtn);
        c.gridx = 0;
        c.gridy = 2;
        p.add(new JLabel("Initial direction:"), c);
        c.gridx = 1;
        c.gridwidth = 3;
        p.add(dirPanel, c);
        c.gridwidth = 1;

        JButton run = new JButton("Run");
        JButton clear = new JButton("Clear");
        JButton close = new JButton(hasMenu ? "Back to Menu" : "Close");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.add(close);
        btns.add(clear);
        btns.add(run);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 4;
        c.anchor = GridBagConstraints.EAST;
        p.add(btns, c);

        run.addActionListener(e -> onRun());
        clear.addActionListener(e -> onClear());
        close.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(run);
        return p;
    }

    // Results
    private JComponent buildResultPanel() {
        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(22);

        sequenceField.setEditable(false);
        JPanel seqPanel = new JPanel(new BorderLayout(6, 0));
        seqPanel.add(new JLabel("Seek sequence:"), BorderLayout.WEST);
        seqPanel.add(sequenceField, BorderLayout.CENTER);

        JPanel summary = new JPanel(new GridLayout(2, 1, 0, 2));
        summary.add(totalLabel);
        summary.add(avgLabel);

        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.add(seqPanel, BorderLayout.NORTH);
        south.add(summary, BorderLayout.SOUTH);

        JPanel left = new JPanel(new BorderLayout(0, 6));
        left.setBorder(BorderFactory.createTitledBorder("Result"));
        left.add(new JScrollPane(table), BorderLayout.CENTER);
        left.add(south, BorderLayout.SOUTH);

        JScrollPane graph = new JScrollPane(pathPanel);
        graph.setBorder(BorderFactory.createTitledBorder("Head path"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, graph);
        split.setResizeWeight(0.42);
        return split;
    }

    // Reset inputs and clear previous results
    private void onClear() {
        diskSizeField.setText(String.valueOf(DEFAULT_DISK_SIZE));
        headField.setText("");
        requestsField.setText("");
        upBtn.setSelected(true);
        clearResults();
        requestsField.requestFocusInWindow();
    }

    private void clearResults() {
        tableModel.setRowCount(0);
        sequenceField.setText("");
        totalLabel.setText("Total head movement: -");
        avgLabel.setText("Average seek distance: -");
        pathPanel.setResult(null, DEFAULT_DISK_SIZE);
    }

    // Validation
    private void onRun() {
        try {
            int diskSize = parseInt(diskSizeField.getText(), "Disk size", 1, 100000);
            int head = parseInt(headField.getText(), "Initial head position", 0, diskSize - 1);

            String text = requestsField.getText().trim();
            if (text.isEmpty()) throw new IllegalArgumentException("Enter at least one request.");
            String[] parts = text.split("[,\\s]+");
            if (parts.length == 0) throw new IllegalArgumentException("Enter at least one request.");
            if (parts.length > 1000) throw new IllegalArgumentException("At most 1000 requests allowed.");
            int[] requests = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                requests[i] = parseInt(parts[i], "Request " + (i + 1), 0, diskSize - 1);
            }

            DiskScheduler.Direction dir = upBtn.isSelected()
                    ? DiskScheduler.Direction.UP : DiskScheduler.Direction.DOWN;
            DiskResult r = new CLookScheduler().schedule(requests, head, diskSize, dir);
            showResult(r, requests.length, diskSize);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid input",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static int parseInt(String s, String name, int min, int max) {
        String t = s.trim();
        if (t.isEmpty()) throw new IllegalArgumentException(name + " is required.");
        int v;
        try {
            v = Integer.parseInt(t);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + ": \"" + t + "\" is not a whole number.");
        }
        if (v < min || v > max) {
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max + ".");
        }
        return v;
    }

    // Display 
    private void showResult(DiskResult r, int n, int diskSize) {
        tableModel.setRowCount(0);
        int i = 1;
        for (DiskResult.Step s : r.getSteps()) {
            tableModel.addRow(new Object[]{i++, s.from, s.to, s.distance,
                    s.jump ? "Return jump" : "Seek"});
        }
        sequenceField.setText(r.getSeekSequence().toString());
        totalLabel.setText("Total head movement: " + r.getTotalMovement() + " cylinders"
                + (r.hasJump() ? " (includes return jump)" : ""));
        avgLabel.setText(String.format("Average seek distance: %.2f (%d / %d requests)",
                r.getAverageSeek(), r.getTotalMovement(), n));
        pathPanel.setResult(r, diskSize);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new CLook().setVisible(true));
    }
}