package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Group 7 application entry point and shared navigation. */
public final class MainMenu extends JFrame {
    public MainMenu() {
        super("Group 7 - Scheduling Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(16, 20));
        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 20, 24));
        JLabel title = new JLabel("CPU and Disk Scheduling Simulator", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 23f));
        root.add(title, BorderLayout.NORTH);
        JPanel groups = new JPanel(new GridLayout(1, 2, 18, 0));
        for (SimulatorModule.Category category : SimulatorModule.Category.values()) {
            JPanel group = new JPanel(new GridLayout(2, 1, 10, 14));
            group.setBorder(BorderFactory.createTitledBorder(
                category == SimulatorModule.Category.CPU ? "CPU Scheduling" : "Disk Scheduling"));
            for (SimulatorModule module : ModuleRegistry.modules()) {
                if (module.category() != category) continue;
                JButton button = new JButton(module.name());
                button.addActionListener(event -> openModule(module));
                group.add(button);
            }
            groups.add(group);
        }
        root.add(groups, BorderLayout.CENTER);
        JPanel footer = new JPanel();
        JButton about = new JButton("About");
        about.addActionListener(event -> JOptionPane.showMessageDialog(this,
            "ICS26012 Operating Systems - 3ITE, Group 7\n\n"
            + "Aileen Shein V. Banzuela - C-LOOK\n"
            + "Hannah P. Mu\u00f1oz - Preemptive Priority\n"
            + "Aurora Maria Gabrielle Nuestro - SSTF\n"
            + "Roz Alexi A. Tengco - Round Robin\n\n"
            + "Demonstration samples are included. All outputs are computed.\n"
            + "No internet connection or external libraries required.",
            "About Group 7", JOptionPane.INFORMATION_MESSAGE));
        JButton exit = new JButton("Exit");
        exit.addActionListener(event -> dispose());
        footer.add(about);
        footer.add(exit);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
        setMinimumSize(new Dimension(760, 350));
        setSize(850, 410);
        setLocationRelativeTo(null);
    }

    private void openModule(SimulatorModule module) {
        try {
            ModuleWindow.open(module, () -> { setVisible(true); toFront(); });
            setVisible(false);
        } catch (RuntimeException error) {
            JOptionPane.showMessageDialog(this, "Unable to open " + module.name()
                + ": " + error.getMessage(), "Module error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { }
            new MainMenu().setVisible(true);
        });
    }
}
