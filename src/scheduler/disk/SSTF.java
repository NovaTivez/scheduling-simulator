package scheduler.disk;
/*
 * ICS26012 - Operating Systems (Alternative Assessment)
 * Shortest Seek Time First (SSTF) Disk Scheduling Simulator
 * Owner : Nuestro, Aurora Maria Gabrielle
 * Group : 3ITE, Group No. 7
 *
 * This one file has 5 classes:
 *   SSTF           - starts the program (main method)
 *   SSTFScheduler  - the SSTF algorithm (no GUI code)
 *   SSTFResult     - stores the path, movement per step, total and average
 *   SSTFPanel      - the screen: inputs, results table and buttons
 *   HeadPathGraph  - draws the graph of the head's path
 *
 * Rules we follow (from section 4.3 of the guidelines):
 *   - Cylinders are 0 to N-1 (default N = 200).
 *   - If two requests are the same distance away, the LOWER cylinder goes first.
 *   - Duplicate requests are served one after the other (0 movement).
 *   - Total head movement = sum of |next - current|, starting from the head.
 *   - Average seek distance = total head movement / number of requests.
 */

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

// =============================================================================
// SSTF - starts the program
// =============================================================================
public class SSTF {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // if it fails, Java just uses the default look
        }

        // bigger text so the screen is easy to read in the video
        Font font = new Font("SansSerif", Font.PLAIN, 14);
        String[] parts = {"Label.font", "Button.font", "TextField.font", "TextArea.font",
                "ComboBox.font", "Table.font", "TableHeader.font", "OptionPane.messageFont",
                "TitledBorder.font"};
        for (int i = 0; i < parts.length; i++) {
            UIManager.put(parts[i], font);
        }

        JFrame window = new JFrame("SSTF Disk Scheduling Simulator");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.add(new SSTFPanel());
        window.setSize(1200, 720);
        window.setLocationRelativeTo(null);   // center on the screen
        window.setVisible(true);
    }
}

// =============================================================================
// SSTFScheduler - the SSTF algorithm
//
// How SSTF works:
//   From where the head is right now, serve the request that is nearest.
//   Move the head there, mark that request as served, then repeat until
//   every request is served.
// =============================================================================
class SSTFScheduler {

    public SSTFResult schedule(int[] requests, int head) {
        int n = requests.length;
        boolean[] served = new boolean[n];   // true if the request is already done
        int[] path = new int[n + 1];         // path[0] is the starting head

        path[0] = head;
        int current = head;

        for (int step = 1; step <= n; step++) {
            int closest = findClosest(requests, served, current);

            served[closest] = true;
            current = requests[closest];
            path[step] = current;
        }

        return new SSTFResult(path);
    }

    // Returns the index of the request that is nearest to the head
    // and is not served yet. On a tie, the lower cylinder wins.
    // Duplicates are served one after the other because their distance is 0.
    private int findClosest(int[] requests, boolean[] served, int current) {
        int closest = -1;

        for (int i = 0; i < requests.length; i++) {
            if (served[i]) {
                continue;
            }
            if (closest == -1) {
                closest = i;   // first request that is not yet served
                continue;
            }

            int distance = Math.abs(requests[i] - current);
            int bestDistance = Math.abs(requests[closest] - current);

            if (distance < bestDistance) {
                closest = i;
            } else if (distance == bestDistance && requests[i] < requests[closest]) {
                closest = i;   // tie: take the lower cylinder
            }
        }
        return closest;
    }
}

// =============================================================================
// SSTFResult - holds the answer of the SSTF algorithm
//
// path[0] is the starting head position, and path[1] to path[n] are the
// cylinders in the order they were served (the seek sequence).
// movements[i] is how far the head moved from path[i] to path[i + 1].
// =============================================================================
class SSTFResult {

    private int[] path;
    private int[] movements;
    private int totalMovement;
    private double averageSeek;

    public SSTFResult(int[] path) {
        this.path = path;

        int steps = path.length - 1;
        movements = new int[steps];
        totalMovement = 0;

        // head movement of each step = |next position - current position|
        for (int i = 0; i < steps; i++) {
            movements[i] = Math.abs(path[i + 1] - path[i]);
            totalMovement = totalMovement + movements[i];
        }

        // average seek distance = total head movement / number of requests
        if (steps > 0) {
            averageSeek = (double) totalMovement / steps;
        } else {
            averageSeek = 0;
        }
    }

    public int[] getPath() {
        return path;
    }

    public int[] getMovements() {
        return movements;
    }

    public int getTotalMovement() {
        return totalMovement;
    }

    public double getAverageSeek() {
        return averageSeek;
    }

    public int getNumberOfRequests() {
        return path.length - 1;
    }
}

// =============================================================================
// SSTFPanel - the screen (inputs, results table and graph)
//
// This class is only for the user interface. The computing is done by
// SSTFScheduler, so the algorithm can be tested without the GUI.
// =============================================================================
class SSTFPanel extends JPanel implements ActionListener {

    private static final int DEFAULT_DISK_SIZE = 200;
    private static final int MAX_DISK_SIZE = 100000;
    private static final int MAX_REQUESTS = 100;

    // Sample problems for the "Load Sample Data" button.
    // Only the inputs are saved here; the answers are computed by the algorithm.
    // TODO: add the facilitator's practice problem sets here.
    private String[] sampleNames = {
        "1 - Textbook example",
        "2 - 20 requests",
        "3 - Tie (40 and 60)",
        "4 - Head at the end"
    };
    private String[] sampleQueues = {
        "98, 183, 37, 122, 14, 124, 65, 67",
        "45, 130, 10, 199, 88, 110, 150, 60, 175, 25, 130, 95, 70, 140, 5, 160, 115, 0, 185, 90",
        "60, 40, 90, 10",
        "0, 198, 100, 100, 150"
    };
    private int[] sampleHeads = {53, 100, 50, 199};

    private SSTFScheduler scheduler = new SSTFScheduler();

    // input fields
    private JTextArea queueField = new JTextArea(4, 18);
    private JTextField headField = new JTextField(8);
    private JTextField diskSizeField = new JTextField("200", 8);
    private JComboBox<String> sampleBox = new JComboBox<String>(sampleNames);

    // buttons
    private JButton loadButton = new JButton("Load Sample Data");
    private JButton runButton = new JButton("Run SSTF");
    private JButton resetButton = new JButton("Reset / Clear");

    // outputs
    private JTextArea resultArea = new JTextArea(6, 20);
    private DefaultTableModel tableModel;
    private HeadPathGraph graph = new HeadPathGraph();

    public SSTFPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createTitle(), BorderLayout.NORTH);
        add(createInputPanel(), BorderLayout.WEST);
        add(createOutputPanel(), BorderLayout.CENTER);

        loadButton.addActionListener(this);
        runButton.addActionListener(this);
        resetButton.addActionListener(this);
    }

    private JPanel createTitle() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        JLabel title = new JLabel("Disk Scheduling - Shortest Seek Time First (SSTF)");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        JLabel info = new JLabel("Serves the request nearest to the head first. "
                + "If two requests are tied, the lower cylinder goes first.");
        info.setFont(new Font("SansSerif", Font.PLAIN, 14));
        panel.add(title);
        panel.add(info);
        return panel;
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Input"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;

        panel.add(new JLabel("<html>Request queue<br>(separate with commas or spaces):</html>"), c);
        c.gridy++;
        queueField.setLineWrap(true);
        queueField.setWrapStyleWord(true);
        panel.add(new JScrollPane(queueField), c);

        c.gridy++;
        panel.add(new JLabel("Initial head position:"), c);
        c.gridy++;
        panel.add(headField, c);

        c.gridy++;
        panel.add(new JLabel("Disk size N (cylinders 0 to N-1):"), c);
        c.gridy++;
        panel.add(diskSizeField, c);

        c.gridy++;
        panel.add(new JLabel("Sample data:"), c);
        c.gridy++;
        panel.add(sampleBox, c);

        c.gridy++;
        JPanel buttons = new JPanel(new GridLayout(3, 1, 5, 5));
        buttons.add(loadButton);
        buttons.add(runButton);
        buttons.add(resetButton);
        panel.add(buttons, c);

        // empty space at the bottom so everything stays on top
        c.gridy++;
        c.weighty = 1;
        panel.add(new JLabel(""), c);

        return panel;
    }

    private JPanel createOutputPanel() {
        // left side: result text and the table
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JScrollPane resultScroll = new JScrollPane(resultArea);
        resultScroll.setBorder(BorderFactory.createTitledBorder("Result"));

        String[] columns = {"Step", "From", "To", "Computation", "Movement"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;   // user cannot edit the results
            }
        };
        JTable table = new JTable(tableModel);
        table.setRowHeight(22);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(45);
        table.getColumnModel().getColumn(2).setPreferredWidth(45);
        table.getColumnModel().getColumn(3).setPreferredWidth(130);
        table.getColumnModel().getColumn(4).setPreferredWidth(70);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Head movement per step"));

        JPanel leftSide = new JPanel(new BorderLayout(5, 5));
        leftSide.add(resultScroll, BorderLayout.NORTH);
        leftSide.add(tableScroll, BorderLayout.CENTER);

        // right side: the graph
        JPanel rightSide = new JPanel(new BorderLayout());
        rightSide.setBorder(BorderFactory.createTitledBorder("Graph of the head's path"));
        rightSide.add(graph, BorderLayout.CENTER);

        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.add(leftSide);
        panel.add(rightSide);
        return panel;
    }

    // Called when any button is clicked
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == loadButton) {
            loadSample();
        } else if (e.getSource() == runButton) {
            runSSTF();
        } else if (e.getSource() == resetButton) {
            resetAll();
        }
    }

    private void runSSTF() {
        // 1. read and check the disk size
        int diskSize = readNumber(diskSizeField.getText(), "Disk size");
        if (diskSize == -1) {
            return;
        }
        if (diskSize < 2 || diskSize > MAX_DISK_SIZE) {
            showError("Disk size must be between 2 and " + MAX_DISK_SIZE + " cylinders.");
            return;
        }

        // 2. read and check the head position
        int head = readNumber(headField.getText(), "Initial head position");
        if (head == -1) {
            return;
        }
        if (head >= diskSize) {
            showError("Initial head position " + head + " is out of range. "
                    + "It must be between 0 and " + (diskSize - 1) + ".");
            return;
        }

        // 3. read and check the request queue
        int[] requests = readQueue(queueField.getText(), diskSize);
        if (requests == null) {
            return;
        }

        // 4. run the algorithm and show the answer
        SSTFResult result = scheduler.schedule(requests, head);
        showResult(result, diskSize);
    }

    private void showResult(SSTFResult result, int diskSize) {
        int[] path = result.getPath();
        int[] movements = result.getMovements();

        // seek sequence, total and average
        String text = "Order of service (seek sequence):\n";
        for (int i = 0; i < path.length; i++) {
            if (i > 0) {
                text = text + " -> ";
            }
            text = text + path[i];
        }
        text = text + "\n\nTotal head movement = " + result.getTotalMovement() + " cylinders";
        text = text + "\nAverage seek distance = " + result.getTotalMovement() + " / "
                + result.getNumberOfRequests() + " = "
                + String.format("%.2f", result.getAverageSeek()) + " cylinders";
        resultArea.setText(text);
        resultArea.setCaretPosition(0);

        // table of each step
        tableModel.setRowCount(0);
        for (int i = 0; i < movements.length; i++) {
            String computation = "|" + path[i + 1] + " - " + path[i] + "| = " + movements[i];
            Object[] row = {i + 1, path[i], path[i + 1], computation, movements[i]};
            tableModel.addRow(row);
        }
        Object[] totalRow = {"", "", "", "Total", result.getTotalMovement()};
        tableModel.addRow(totalRow);

        graph.setData(path, diskSize);
    }

    // Reads a whole number that is 0 or more.
    // Shows an error and returns -1 if the input is wrong.
    private int readNumber(String text, String fieldName) {
        text = text.trim();
        if (text.isEmpty()) {
            showError(fieldName + " is empty. Please enter a whole number.");
            return -1;
        }

        int number;
        try {
            number = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            showError(fieldName + " must be a whole number, but \"" + text + "\" was entered.");
            return -1;
        }

        if (number < 0) {
            showError(fieldName + " cannot be negative.");
            return -1;
        }
        return number;
    }

    // Reads the request queue. Returns null if there is an error.
    private int[] readQueue(String text, int diskSize) {
        text = text.trim();
        if (text.isEmpty()) {
            showError("Request queue is empty. Enter at least one cylinder number.");
            return null;
        }

        String[] parts = text.split("[,\\s]+");   // split by commas and/or spaces
        if (parts.length > MAX_REQUESTS) {
            showError("Too many requests (" + parts.length + "). The maximum is " + MAX_REQUESTS + ".");
            return null;
        }

        int[] requests = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            int cylinder = readNumber(parts[i], "Request #" + (i + 1));
            if (cylinder == -1) {
                return null;
            }
            if (cylinder >= diskSize) {
                showError("Request #" + (i + 1) + " (" + cylinder + ") is out of range. "
                        + "Cylinders must be between 0 and " + (diskSize - 1) + ".");
                return null;
            }
            requests[i] = cylinder;
        }
        return requests;
    }

    private void loadSample() {
        int index = sampleBox.getSelectedIndex();
        queueField.setText(sampleQueues[index]);
        headField.setText(String.valueOf(sampleHeads[index]));
        diskSizeField.setText(String.valueOf(DEFAULT_DISK_SIZE));
        clearResults();
    }

    private void resetAll() {
        queueField.setText("");
        headField.setText("");
        diskSizeField.setText(String.valueOf(DEFAULT_DISK_SIZE));
        sampleBox.setSelectedIndex(0);
        clearResults();
    }

    private void clearResults() {
        resultArea.setText("");
        tableModel.setRowCount(0);
        graph.clear();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Invalid input", JOptionPane.WARNING_MESSAGE);
    }
}

// =============================================================================
// HeadPathGraph - draws the graph of the head's path
//
// The cylinder numbers are on top (0 to diskSize - 1) and each step goes
// one row down, like the seek diagrams in the textbook.
// =============================================================================
class HeadPathGraph extends JPanel {

    private int[] path;          // null means nothing to draw yet
    private int diskSize = 200;

    public HeadPathGraph() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(400, 400));
    }

    public void setData(int[] path, int diskSize) {
        this.path = path;
        this.diskSize = diskSize;
        repaint();
    }

    public void clear() {
        path = null;
        repaint();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));

        if (path == null) {
            g2.setColor(Color.GRAY);
            g2.drawString("Click Run SSTF to see the graph.", 20, 40);
            return;
        }

        // space around the graph
        int left = 55;
        int right = 35;
        int top = 60;
        int bottom = 20;
        int width = getWidth() - left - right;

        // make each row fit inside the panel (max 40 pixels per row)
        int steps = path.length - 1;
        int rowHeight = 40;
        if (steps > 0) {
            rowHeight = Math.min(40, (getHeight() - top - bottom) / steps);
        }

        drawAxis(g2, left, top, width, rowHeight);

        // lines between the points
        g2.setColor(new Color(30, 90, 200));
        g2.setStroke(new BasicStroke(2));
        for (int i = 0; i < steps; i++) {
            int x1 = toX(path[i], left, width);
            int x2 = toX(path[i + 1], left, width);
            g2.drawLine(x1, top + i * rowHeight, x2, top + (i + 1) * rowHeight);
        }

        // the points and their cylinder numbers
        for (int i = 0; i < path.length; i++) {
            int x = toX(path[i], left, width);
            int y = top + i * rowHeight;

            if (i == 0) {
                g2.setColor(new Color(0, 150, 70));   // green = start
            } else {
                g2.setColor(new Color(200, 50, 50));  // red = served request
            }
            g2.fillOval(x - 5, y - 5, 10, 10);

            if (rowHeight >= 14) {   // skip labels if the rows are too close
                String label = String.valueOf(path[i]);
                if (i == 0) {
                    label = "Start " + label;
                }
                int labelWidth = g2.getFontMetrics().stringWidth(label);
                int labelX = x + 8;
                if (labelX + labelWidth > getWidth()) {
                    labelX = x - 8 - labelWidth;   // put it on the left side instead
                }
                g2.setColor(Color.BLACK);
                g2.drawString(label, labelX, y + 4);
            }
        }
    }

    // Draws the cylinder scale on top, the grid lines and the step numbers.
    private void drawAxis(Graphics2D g2, int left, int top, int width, int rowHeight) {
        int axisY = top - 25;
        int bottomY = top + (path.length - 1) * rowHeight;
        int tickStep = pickTickStep(width);

        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Cylinder", left, axisY - 20);
        g2.drawLine(left, axisY, left + width, axisY);

        int lastLabelX = 0;
        for (int cylinder = 0; cylinder < diskSize; cylinder = cylinder + tickStep) {
            drawTick(g2, cylinder, left, width, axisY, bottomY);
            lastLabelX = toX(cylinder, left, width);
        }

        // also label the last cylinder if there is space for it
        int endX = toX(diskSize - 1, left, width);
        if (endX - lastLabelX >= 35) {
            drawTick(g2, diskSize - 1, left, width, axisY, bottomY);
        }

        // step numbers on the left side
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Step", 10, axisY + 15);
        if (rowHeight >= 14) {
            for (int i = 0; i < path.length; i++) {
                g2.drawString(String.valueOf(i), 20, top + i * rowHeight + 4);
            }
        }
    }

    private void drawTick(Graphics2D g2, int cylinder, int left, int width, int axisY, int bottomY) {
        int x = toX(cylinder, left, width);
        String text = String.valueOf(cylinder);
        int textWidth = g2.getFontMetrics().stringWidth(text);

        g2.setColor(new Color(225, 225, 225));
        g2.drawLine(x, axisY, x, bottomY);   // light grid line

        g2.setColor(Color.DARK_GRAY);
        g2.drawLine(x, axisY - 4, x, axisY + 4);
        g2.drawString(text, x - textWidth / 2, axisY - 7);
    }

    // Picks a tick spacing so the numbers on top do not overlap
    // (about 45 pixels for each number).
    private int pickTickStep(int width) {
        int[] niceSteps = {5, 10, 20, 25, 50, 100, 200, 250, 500, 1000, 2000, 2500, 5000, 10000, 25000, 50000};

        for (int i = 0; i < niceSteps.length; i++) {
            int numberOfTicks = diskSize / niceSteps[i] + 1;
            if (numberOfTicks * 45 <= width) {
                return niceSteps[i];
            }
        }
        return diskSize;
    }

    // Changes a cylinder number into an x position on the panel.
    private int toX(int cylinder, int left, int width) {
        if (diskSize <= 1) {
            return left;
        }
        return left + (int) ((double) cylinder / (diskSize - 1) * width);
    }
}

