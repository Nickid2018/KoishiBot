package io.github.nickid2018.koishibot.monitor;

import io.github.nickid2018.koishibot.util.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SubProgramProcess implements Runnable {

    public static final Logger LOGGER = LoggerFactory.getLogger("Sub-Program Process");

    private final String name;
    private final BufferedReader programOutput;
    private final BufferedWriter programInput;
    private final Process process;
    private BufferedWriter output;

    public SubProgramProcess(String name, List<String> cmd, String runDir) throws IOException {
        this.name = name;
        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.directory(new File(runDir));
        builder.redirectErrorStream(true);
        process = builder.start();
        programInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        programOutput = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        setOutputNull();
    }

    public String getName() {
        return name;
    }

    public void setOutput(OutputStream output) {
        this.output = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
    }

    public void setOutputNull() {
        output = new BufferedWriter(Writer.nullWriter());
    }

    public Process getProcess() {
        return process;
    }

    public void writeToInput(String line) throws IOException {
        programInput.write(line);
        programInput.newLine();
        programInput.flush();
    }

    @Override
    public void run() {
        while (process.isAlive()) {
            try {
                String line = programOutput.readLine();
                if (line == null)
                    continue;
                output.write("\33[0;32m<%s>\33[0m ".formatted(name));
                output.write(line);
                output.newLine();
                output.flush();
            } catch (IOException e) {
                LogUtils.error(LOGGER, "Error when writing to output", e);
            }
        }
        LogUtils.info(LogUtils.FontColor.YELLOW, LOGGER, "Sub-process {} exited with code {}", name, process.exitValue());
    }
}
