package io.github.nickid2018.koishibot.monitor;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class ProgramTest {

    public static void main(String[] args) throws IOException {
        SubProgramProcess programProcess = new SubProgramProcess("test", List.of("cmd", "/d", "ss"), ".");
        programProcess.setOutput(System.out);
        Thread thread = new Thread(programProcess);
        thread.start();
        Scanner scanner = new Scanner(System.in);
        while (thread.isAlive()) {
            String line = scanner.nextLine();
            programProcess.writeToInput(line);
        }
    }
}
