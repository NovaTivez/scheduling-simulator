package ui;

import javax.swing.JFrame;

/** Shared integration contract; each module keeps its own scheduling data model. */
public interface SimulatorModule {
    enum Category { CPU, DISK }
    String name();
    Category category();
    JFrame createWindow();
}
