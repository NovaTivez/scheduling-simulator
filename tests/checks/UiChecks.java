package checks;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import ui.MainMenu;
import ui.ModuleRegistry;
import ui.SimulatorModule;

/** Real Swing workflows, run against production classes loaded from the executable JAR. */
public final class UiChecks {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static List<Component> components(Component root) {
        List<Component> result = new ArrayList<>(); result.add(root);
        if (root instanceof Container container)
            for (Component child : container.getComponents()) result.addAll(components(child));
        return result;
    }
    private static JButton button(Component root, String text) {
        return components(root).stream().filter(c -> c instanceof JButton b && text.equals(b.getText()))
            .map(c -> (JButton)c).findFirst().orElseThrow(() -> new AssertionError("Missing button " + text));
    }
    @SuppressWarnings("unchecked")
    private static <T> T field(Object owner, String name) {
        try { Field f = owner.getClass().getDeclaredField(name); f.setAccessible(true); return (T)f.get(owner); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void capture(Window window, String name) {
        try {
            Files.createDirectories(Path.of("build/test-screenshots"));
            BufferedImage image = new BufferedImage(window.getWidth(), window.getHeight(), BufferedImage.TYPE_INT_RGB);
            var g = image.createGraphics(); window.printAll(g); g.dispose();
            ImageIO.write(image, "png", Path.of("build/test-screenshots", name + ".png").toFile());
        } catch (Exception e) { throw new AssertionError(e); }
    }
    private static void reject(JButton run, Runnable change) {
        change.run();
        AtomicBoolean found = new AtomicBoolean();
        Timer closer = new Timer(100, event -> {
            for (Window w : Window.getWindows()) if (w instanceof JDialog dialog && dialog.isShowing()) {
                found.set(true); dialog.dispose();
            }
        });
        closer.start();
        try { run.doClick(); } finally { closer.stop(); }
        check(found.get(), "Invalid input was not rejected with a dialog");
    }
    private static String output(Component root) {
        StringBuilder result = new StringBuilder();
        for (Component c : components(root)) {
            if (c instanceof JTable table && !table.getModel().isCellEditable(0, 0)) {
                for (int r = 0; r < table.getRowCount(); r++) for (int col = 0; col < table.getColumnCount(); col++)
                    result.append(table.getValueAt(r, col)).append('|');
            }
            if (c instanceof JTextArea text && !text.isEditable()) result.append(text.getText());
        }
        return result.toString();
    }
    private static void exercise(JFrame window, SimulatorModule module, int index) {
        JButton sample = button(window, "Load Sample Data");
        JButton run = button(window, index == 0 ? "Run Simulation" : index == 1 ? "Run Simulator" : index == 2 ? "Run SSTF" : "Run");
        JButton clear = button(window, index == 0 || index == 3 ? "Clear" : index == 1 ? "Clear / Reset" : "Reset / Clear");
        if (index == 2) {
            components(window).stream().filter(c -> c instanceof JComboBox).map(c -> (JComboBox<?>)c)
                .findFirst().orElseThrow().setSelectedIndex(1);
        }
        sample.doClick(); run.doClick();
        String first = output(window);
        check(!first.isBlank(), "No computed results for " + module.name());
        run.doClick(); check(first.equals(output(window)), "Repeated run changed results: " + module.name());
        capture(window, "module-" + index);
        if (index < 2) {
            JTable table = components(window).stream().filter(c -> c instanceof JTable t && t.isCellEditable(0, 0))
                .map(c -> (JTable)c).findFirst().orElseThrow();
            DefaultTableModel input = (DefaultTableModel)table.getModel();
            check(input.getRowCount() == 10, "CPU sample must contain 10 processes");
            for (String bad : new String[]{"", "-1", "abc", "2147483648"}) {
                sample.doClick(); reject(run, () -> input.setValueAt(bad, 0, 1));
            }
            sample.doClick(); reject(run, () -> input.setValueAt(0, 0, 2));
            sample.doClick(); reject(run, () -> input.setValueAt(input.getValueAt(0, 0), 1, 0));
            if (index == 1) {
                sample.doClick(); JTextField quantum = field(window, "quantumField");
                reject(run, () -> quantum.setText("0"));
            }
            sample.doClick(); run.doClick(); clear.doClick();
            check(output(window).isBlank(), "Clear left CPU results");
            reject(run, () -> { });
        } else {
            Object owner = index == 3 ? window : components(window).stream()
                .filter(c -> c.getClass().getSimpleName().equals("SSTFPanel")).findFirst().orElseThrow();
            JTextField head = field(owner, "headField");
            JTextField size = field(owner, "diskSizeField");
            javax.swing.text.JTextComponent queue = field(owner, index == 2 ? "queueField" : "requestsField");
            DefaultTableModel result = field(owner, "tableModel");
            check(result.getRowCount() == (index == 2 ? 21 : 20), "Disk sample must serve 20 requests");
            for (String bad : new String[]{"", "-1", "abc", "200", "2147483648"}) {
                sample.doClick(); reject(run, () -> head.setText(bad));
            }
            for (String bad : new String[]{"", ",,,", "-1", "abc", "200"}) {
                sample.doClick(); reject(run, () -> queue.setText(bad));
            }
            sample.doClick(); reject(run, () -> size.setText("0"));
            sample.doClick(); reject(run, () -> queue.setText("0,".repeat(index == 2 ? 101 : 1001)));
            sample.doClick(); run.doClick(); clear.doClick();
            check(result.getRowCount() == 0 && head.getText().isEmpty() && queue.getText().isEmpty(), "Clear left disk inputs/results");
        }
        sample.doClick(); run.doClick();
        check(!output(window).isBlank(), "Cannot run after clear/validation");
    }
    public static void main(String[] args) {
        int status = 0;
        try {
            MainMenu[] menu = new MainMenu[1];
            SwingUtilities.invokeAndWait(() -> {
                try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
                menu[0] = new MainMenu(); menu[0].setVisible(true); capture(menu[0], "menu");
                Timer timer = new Timer(100, event -> {
                    for (Window w : Window.getWindows()) if (w instanceof JDialog d && d.isShowing()) {
                        String text = components(d).stream().filter(c -> c instanceof JLabel)
                            .map(c -> ((JLabel)c).getText()).reduce("", (a,b) -> a + b);
                        check(text.contains("Group 7") && text.contains("Banzuela") && text.contains("Nuestro")
                            && text.contains("Tengco") && text.contains("Mu"), "About missing members");
                        capture(d, "about"); d.dispose();
                    }
                });
                timer.start(); try { button(menu[0], "About").doClick(); } finally { timer.stop(); }
            });
            var modules = ModuleRegistry.modules();
            for (int i = 0; i < modules.size(); i++) {
                int index = i; JFrame[] child = new JFrame[1];
                SwingUtilities.invokeAndWait(() -> {
                    button(menu[0], modules.get(index).name()).doClick();
                    check(!menu[0].isVisible(), "Menu was not hidden");
                    child[0] = java.util.Arrays.stream(Window.getWindows())
                        .filter(w -> w instanceof JFrame && w != menu[0] && w.isVisible())
                        .map(w -> (JFrame)w).findFirst().orElseThrow();
                    check(child[0].getDefaultCloseOperation() == JFrame.DISPOSE_ON_CLOSE, "Unsafe close action");
                    exercise(child[0], modules.get(index), index);
                    button(child[0], "Back to Menu").doClick();
                });
                SwingUtilities.invokeAndWait(() -> check(menu[0].isVisible(), "Back did not restore menu"));
                SwingUtilities.invokeAndWait(() -> {
                    button(menu[0], modules.get(index).name()).doClick();
                    for (Window w : Window.getWindows()) if (w != menu[0] && w.isVisible())
                        w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
                });
                SwingUtilities.invokeAndWait(() -> check(menu[0].isVisible(), "Window X did not restore menu"));
            }
            SwingUtilities.invokeAndWait(() -> { button(menu[0], "Exit").doClick(); check(!menu[0].isDisplayable(), "Exit did not close menu"); });
            System.out.println("PASS packaged-JAR GUI: " + checks + " checks; screenshots in build/test-screenshots");
        } catch (Throwable error) { error.printStackTrace(); status = 1; }
        finally { System.exit(status); }
    }
}
