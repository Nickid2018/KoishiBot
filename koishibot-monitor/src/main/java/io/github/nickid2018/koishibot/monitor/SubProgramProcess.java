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

    public SubProgramProcess(String name, List<String> cmd) throws IOException {
        this.name = name;
        ProcessBuilder builder = new ProcessBuilder(cmd);
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
        String line;
        try {
            while (process.isAlive() && (line = programOutput.readLine()) != null) {
                try {
                    output.write(line);
                    output.newLine();
                    output.flush();
                } catch (IOException e) {
                    LogUtils.error(LOGGER, "Error when writing to output", e);
                }
            }
        } catch (IOException e) {
            LogUtils.error(LOGGER, "Error when reading from program output", e);
        }
        LogUtils.info(LogUtils.FontColor.YELLOW, LOGGER, "Sub-process {} exited with code {}", name, process.exitValue());
    }
}
