package scheduler.cpu;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Round Robin
 * Roz Alexi A. Tengco - Group 7
 */
public class RoundRobinSimulator extends JFrame {

    // DATA MODELS & ALGORITHM LOGIC

    public static class Process {
        int pid;
        int arrivalTime;
        int burstTime;

        int remainingTime;
        int completionTime;
        int turnaroundTime;
        int waitingTime;
        int responseTime;
        int firstRunTime = -1;
        boolean hasArrived = false;

        public Process(int pid, int arrivalTime, int burstTime) {
            this.pid = pid;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            this.remainingTime = burstTime;
        }
    }

    public static class GanttRecord {
        int pid; // -1 indicates Idle time
        int startTime;
        int endTime;

        public GanttRecord(int pid, int startTime, int endTime) {
            this.pid = pid;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    public static class SchedulerResult {
        List<Process> completedProcesses;
        List<GanttRecord> ganttChart;
        double avgWaiting;
        double avgTurnaround;
        double avgResponse;
        double cpuUtilization;
        double throughput;

        public SchedulerResult() {
            completedProcesses = new ArrayList<>();
            ganttChart = new ArrayList<>();
        }
    }

    public static class RoundRobinScheduler {

        public SchedulerResult schedule(List<Process> processes, int quantum) {
            SchedulerResult result = new SchedulerResult();
            if (processes == null || processes.isEmpty() || quantum <= 0) return result;

            // Reset process states for a fresh run
            int totalBurst = 0;
            for (Process p : processes) {
                totalBurst += p.burstTime;
                p.remainingTime = p.burstTime;
                p.firstRunTime = -1;
                p.hasArrived = false;
            }

            // Sort by Arrival Time initially; ties broken by PID
            processes.sort((p1, p2) -> {
                if (p1.arrivalTime != p2.arrivalTime) {
                    return Integer.compare(p1.arrivalTime, p2.arrivalTime);
                }
                return Integer.compare(p1.pid, p2.pid);
            });

            int currentTime = 0;
            int completedCount = 0;
            Queue<Process> readyQueue = new LinkedList<>();

            // Initial check for time 0
            checkArrivals(processes, readyQueue, currentTime);

            int currentIdleStart = -1;

            while (completedCount < processes.size()) {
                if (readyQueue.isEmpty()) {
                    // CPU is idle
                    if (currentIdleStart == -1) {
                        currentIdleStart = currentTime;
                    }
                    currentTime++;
                    checkArrivals(processes, readyQueue, currentTime);
                    continue;
                }

                // If we are exiting an idle period, record it
                if (currentIdleStart != -1) {
                    result.ganttChart.add(new GanttRecord(-1, currentIdleStart, currentTime));
                    currentIdleStart = -1;
                }

                Process currentProcess = readyQueue.poll();

                // Record first response time
                if (currentProcess.firstRunTime == -1) {
                    currentProcess.firstRunTime = currentTime;
                }

                // Determine execution slice
                int runTime = Math.min(quantum, currentProcess.remainingTime);
                int startTime = currentTime;

                currentTime += runTime;
                currentProcess.remainingTime -= runTime;

                // Record execution on Gantt chart
                result.ganttChart.add(new GanttRecord(currentProcess.pid, startTime, currentTime));

                // Add new arrivals to the queue FIRST, before re-queuing the preempted process
                checkArrivals(processes, readyQueue, currentTime);

                // Check if process finished or needs to be re-queued
                if (currentProcess.remainingTime == 0) {
                    currentProcess.completionTime = currentTime;
                    currentProcess.turnaroundTime = currentProcess.completionTime - currentProcess.arrivalTime;
                    currentProcess.waitingTime = currentProcess.turnaroundTime - currentProcess.burstTime;
                    currentProcess.responseTime = currentProcess.firstRunTime - currentProcess.arrivalTime;

                    result.completedProcesses.add(currentProcess);
                    completedCount++;
                } else {
                    // Preempted process goes back to the queue AFTER new arrivals
                    readyQueue.add(currentProcess);
                }
            }

            // Calculate final averages and metrics
            double totalWait = 0, totalTurnaround = 0, totalResponse = 0;
            for (Process p : result.completedProcesses) {
                totalWait += p.waitingTime;
                totalTurnaround += p.turnaroundTime;
                totalResponse += p.responseTime;
            }

            int totalTime = currentTime;
            result.avgWaiting = totalWait / processes.size();
            result.avgTurnaround = totalTurnaround / processes.size();
            result.avgResponse = totalResponse / processes.size();
            result.cpuUtilization = ((double) totalBurst / totalTime) * 100;
            result.throughput = (double) processes.size() / totalTime;

            // Sort by PID for standard UI display
            result.completedProcesses.sort(Comparator.comparingInt(p -> p.pid));

            return result;
        }

        // Helper method to handle arrivals
        private void checkArrivals(List<Process> processes, Queue<Process> readyQueue, int currentTime) {
            for (Process p : processes) {
                if (!p.hasArrived && p.arrivalTime <= currentTime) {
                    readyQueue.add(p);
                    p.hasArrived = true;
                }
            }
        }
    }


    // GRAPHICAL USER INTERFACE (Swing)

    private DefaultTableModel inputTableModel;
    private JTable inputTable;
    private DefaultTableModel outputTableModel;
    private JTable outputTable;
    private GanttPanel ganttPanel;
    private JLabel metricsLabel;
    private JTextField quantumField;

    public RoundRobinSimulator() {
        setTitle("Round Robin CPU Scheduling");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top Panel: Inputs
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("Process Inputs"));
        topPanel.setPreferredSize(new Dimension(950, 260));

        String[] inputColumns = {"PID", "Arrival Time", "Burst Time"};
        inputTableModel = new DefaultTableModel(inputColumns, 0);
        inputTable = new JTable(inputTableModel);
        topPanel.add(new JScrollPane(inputTable), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();

        buttonPanel.add(new JLabel("Time Quantum:"));
        quantumField = new JTextField(5);
        buttonPanel.add(quantumField);

        JButton btnAddRow = new JButton("Add Process");
        JButton btnLoadSample = new JButton("Load Sample Data");
        JButton btnRun = new JButton("Run Simulator");
        JButton btnClear = new JButton("Clear / Reset");

        buttonPanel.add(btnAddRow);
        buttonPanel.add(btnLoadSample);
        buttonPanel.add(btnRun);
        buttonPanel.add(btnClear);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Center Panel: Outputs
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Simulation Results"));

        String[] outputColumns = {"PID", "Arrival", "Burst", "Completion", "Turnaround", "Waiting", "Response"};
        outputTableModel = new DefaultTableModel(outputColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make output table read-only
            }
        };
        outputTable = new JTable(outputTableModel);
        centerPanel.add(new JScrollPane(outputTable), BorderLayout.CENTER);

        metricsLabel = new JLabel(" Averages and System Metrics will appear here.");
        metricsLabel.setFont(new Font("Arial", Font.BOLD, 13));
        metricsLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centerPanel.add(metricsLabel, BorderLayout.SOUTH);

        // Bottom Panel: Gantt Chart
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Gantt Chart"));
        bottomPanel.setPreferredSize(new Dimension(950, 120));
        ganttPanel = new GanttPanel();
        bottomPanel.add(ganttPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Action Listeners
        btnAddRow.addActionListener(e -> inputTableModel.addRow(new Object[]{"", "", ""}));

        btnClear.addActionListener(e -> {
            if (inputTable.isEditing()) {
                inputTable.getCellEditor().stopCellEditing();
            }
            inputTableModel.setRowCount(0);
            outputTableModel.setRowCount(0);
            quantumField.setText("");
            ganttPanel.setChart(new ArrayList<>());
            metricsLabel.setText(" Averages and System Metrics will appear here.");
        });

        btnLoadSample.addActionListener(e -> {
            if (inputTable.isEditing()) {
                inputTable.getCellEditor().stopCellEditing();
            }
            inputTableModel.setRowCount(0);
            quantumField.setText("3");
            // Sample Data: PID, Arrival, Burst
            inputTableModel.addRow(new Object[]{1, 0, 5});
            inputTableModel.addRow(new Object[]{2, 1, 4});
            inputTableModel.addRow(new Object[]{3, 3, 2});
            inputTableModel.addRow(new Object[]{4, 4, 4});
            inputTableModel.addRow(new Object[]{5, 6, 2});
        });

        btnRun.addActionListener(e -> runSimulation());
    }

    private void runSimulation() {
        // Commit any currently active cell edit before reading table values
        if (inputTable.isEditing()) {
            inputTable.getCellEditor().stopCellEditing();
        }

        try {
            int quantum = 0;
            try {
                quantum = Integer.parseInt(quantumField.getText().trim());
                if (quantum <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid positive integer (> 0) for Time Quantum.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (inputTableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "Please add at least one process.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            List<Process> processes = new ArrayList<>();
            Set<Integer> usedPids = new HashSet<>();

            for (int i = 0; i < inputTableModel.getRowCount(); i++) {
                Object pidObj = inputTableModel.getValueAt(i, 0);
                Object arrObj = inputTableModel.getValueAt(i, 1);
                Object burstObj = inputTableModel.getValueAt(i, 2);

                if (pidObj == null || arrObj == null || burstObj == null ||
                    pidObj.toString().trim().isEmpty() ||
                    arrObj.toString().trim().isEmpty() ||
                    burstObj.toString().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Row " + (i + 1) + " has empty fields. Please fill in all values.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int pid = Integer.parseInt(pidObj.toString().trim());
                int arrival = Integer.parseInt(arrObj.toString().trim());
                int burst = Integer.parseInt(burstObj.toString().trim());

                if (pid < 0 || arrival < 0) {
                    JOptionPane.showMessageDialog(this, "Row " + (i + 1) + ": PID and Arrival Time cannot be negative.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (burst <= 0) {
                    JOptionPane.showMessageDialog(this, "Row " + (i + 1) + ": Burst Time must be greater than 0.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (!usedPids.add(pid)) {
                    JOptionPane.showMessageDialog(this, "Row " + (i + 1) + ": Duplicate PID (" + pid + ") found. Each process must have a unique PID.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                processes.add(new Process(pid, arrival, burst));
            }

            RoundRobinScheduler scheduler = new RoundRobinScheduler();
            SchedulerResult result = scheduler.schedule(processes, quantum);

            // Populate Output Table
            outputTableModel.setRowCount(0);
            for (Process p : result.completedProcesses) {
                outputTableModel.addRow(new Object[]{
                        p.pid, p.arrivalTime, p.burstTime,
                        p.completionTime, p.turnaroundTime, p.waitingTime, p.responseTime
                });
            }

            // Update Metrics Label
            String metricsText = String.format(
                " Average Waiting Time: %.2f  |  Average Turnaround Time: %.2f  |  Average Response Time: %.2f  ||  CPU Utilization: %.2f%%  |  Throughput: %.2f proc/unit",
                result.avgWaiting, result.avgTurnaround, result.avgResponse, result.cpuUtilization, result.throughput
            );
            metricsLabel.setText(metricsText);

            // Update Gantt Chart
            ganttPanel.setChart(result.ganttChart);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please ensure all table inputs are valid whole numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    // GANTT CHART CREATOR

    private static class GanttPanel extends JPanel {
        private List<GanttRecord> chart = new ArrayList<>();

        public void setChart(List<GanttRecord> chart) {
            // Merge contiguous blocks of the same process ID
            this.chart.clear();
            for (GanttRecord record : chart) {
                if (!this.chart.isEmpty() && this.chart.get(this.chart.size() - 1).pid == record.pid) {
                    this.chart.get(this.chart.size() - 1).endTime = record.endTime;
                } else {
                    this.chart.add(new GanttRecord(record.pid, record.startTime, record.endTime));
                }
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (chart == null || chart.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int totalTime = chart.get(chart.size() - 1).endTime;
            int panelWidth = getWidth() - 40;
            int height = 40;
            int y = 20;
            int startX = 20;

            for (GanttRecord record : chart) {
                int blockWidth = (int) (((double) (record.endTime - record.startTime) / totalTime) * panelWidth);

                // Draw Box
                if (record.pid == -1) {
                    g2.setColor(Color.LIGHT_GRAY); // Idle time
                } else {
                    g2.setColor(new Color(144, 238, 144)); // Process block (Light Green)
                }
                g2.fillRect(startX, y, blockWidth, height);

                // Draw Border
                g2.setColor(Color.BLACK);
                g2.drawRect(startX, y, blockWidth, height);

                // Draw Text
                String label = record.pid == -1 ? "Idle" : "P" + record.pid;
                FontMetrics fm = g2.getFontMetrics();
                int textX = startX + (blockWidth - fm.stringWidth(label)) / 2;
                int textY = y + ((height - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(label, textX, textY);

                // Draw Time Markers
                g2.drawString(String.valueOf(record.startTime), startX - (fm.stringWidth(String.valueOf(record.startTime)) / 2), y + height + 15);

                startX += blockWidth;
            }

            // Draw Final Time Marker
            g2.drawString(String.valueOf(totalTime), startX - 5, y + height + 15);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new RoundRobinSimulator().setVisible(true);
        });
    }
}