package io.github.nickid2018.koishibot.monitor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ProcessManager {

    private static final ExecutorService PROCESS_EXECUTOR = Executors.newCachedThreadPool();

    private static final Map<String, SubProgramProcess> PROCESSES = new HashMap<>();
    private static SubProgramProcess currentProcess;

    private static void checkProcessesAlive() {
        Set<String> dead = PROCESSES.values().stream()
                .filter(p -> !p.getProcess().isAlive())
                .map(SubProgramProcess::getName)
                .collect(Collectors.toSet());
        dead.forEach(PROCESSES::remove);
        if (currentProcess != null && !currentProcess.getProcess().isAlive())
            setNoInteract();
    }

    public static boolean sendCommand(String command) throws IOException {
        checkProcessesAlive();
        if (currentProcess == null)
            return false;
        currentProcess.writeToInput(command);
        return true;
    }

    public static boolean setInteract(String name) {
        checkProcessesAlive();
        if (currentProcess != null)
            currentProcess.setOutputNull();
        if (!PROCESSES.containsKey(name))
            return false;
        currentProcess = PROCESSES.get(name);
        currentProcess.setOutput(System.out);
        return true;
    }

    public static boolean nowInteract() {
        checkProcessesAlive();
        return currentProcess != null && currentProcess.getProcess().isAlive();
    }

    public static void setNoInteract() {
        if (currentProcess != null && currentProcess.getProcess().isAlive())
            currentProcess.setOutputNull();
        currentProcess = null;
    }

    public static Set<String> nowRunning() {
        checkProcessesAlive();
        return PROCESSES.keySet();
    }

    public static void startCore() throws IOException {
        if (PROCESSES.containsKey("core"))
            return;
        SubProgramProcess process = new SubProgramProcess("core", List.of("java", "-jar", "koishibot-core.jar"), ".");
        PROCESS_EXECUTOR.execute(process);
        PROCESSES.put("core", process);
    }

    public static void startBackend(String name) throws IOException {
        if (PROCESSES.containsKey(name))
            return;
        SubProgramProcess process = new SubProgramProcess(
                name, List.of("java", "-jar", new File("%s/koishibot-%s.jar".formatted(name, name)).getAbsolutePath()), name);
        PROCESS_EXECUTOR.execute(process);
        PROCESSES.put(name, process);
    }
}
