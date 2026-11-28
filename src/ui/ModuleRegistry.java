package ui;

import java.util.List;
import java.util.function.Supplier;
import javax.swing.JFrame;
import scheduler.cpu.PriorityScheduling;
import scheduler.cpu.RoundRobinSimulator;
import scheduler.disk.CLook;
import scheduler.disk.SSTF;

/** Adapts existing panels and frames without changing their algorithms. */
public final class ModuleRegistry {
    private ModuleRegistry() { }

    public static List<SimulatorModule> modules() {
        return List.of(
            module("Preemptive Priority", SimulatorModule.Category.CPU, () -> {
                JFrame frame = new JFrame("Preemptive Priority Scheduling");
                frame.setContentPane(new PriorityScheduling());
                frame.setSize(1000, 720);
                return frame;
            }),
            module("Round Robin", SimulatorModule.Category.CPU, RoundRobinSimulator::new),
            module("Shortest Seek Time First (SSTF)", SimulatorModule.Category.DISK, () -> {
                JFrame frame = new JFrame("SSTF Disk Scheduling Simulator");
                frame.setContentPane(SSTF.createPanel());
                frame.setSize(1200, 720);
                return frame;
            }),
            module("C-LOOK", SimulatorModule.Category.DISK, CLook::new)
        );
    }

    private static SimulatorModule module(String name, SimulatorModule.Category category,
                                           Supplier<JFrame> factory) {
        return new SimulatorModule() {
            public String name() { return name; }
            public Category category() { return category; }
            public JFrame createWindow() { return factory.get(); }
        };
    }
}
