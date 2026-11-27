package ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** A single lifecycle for all modules: Back and the window X both return to the menu. */
public final class ModuleWindow {
    private ModuleWindow() { }

    public static JFrame open(SimulatorModule module, Runnable onReturn) {
        JFrame frame = module.createWindow();
        Container original = frame.getContentPane();
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT));
        navigation.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 8));
        JButton back = new JButton("Back to Menu");
        back.addActionListener(event -> frame.dispose());
        navigation.add(back);
        navigation.add(new JLabel(module.name()));
        wrapper.add(navigation, BorderLayout.NORTH);
        wrapper.add(original, BorderLayout.CENTER);
        frame.setContentPane(wrapper);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) { onReturn.run(); }
        });
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        return frame;
    }
}
