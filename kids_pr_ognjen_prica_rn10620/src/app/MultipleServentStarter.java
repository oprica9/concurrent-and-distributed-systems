package app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * This class implements the logic for starting multiple servent instances.
 * <p>
 * To use it, invoke startServentTest with a directory name as parameter.
 * This directory should include:
 * <ul>
 * <li>A <code>servent_list.properties</code> file (explained in {@link AppConfig} class</li>
 * <li>A directory called <code>output</code> </li>
 * <li>A directory called <code>error</code> </li>
 * <li>A directory called <code>input</code> with text files called
 * <code> servent0_in.txt </code>, <code>servent1_in.txt</code>, ... and so on for each servent.
 * These files should contain the commands for each servent, as they would be entered in console.</li>
 * </ul>
 *
 * @author bmilojkovic
 */
public class MultipleServentStarter {

    public static void main(String[] args) {
        startServentTest(args[0]);
    }

    /**
     * The parameter for this function should be the name of a directory that
     * contains a servent_list.properties file which will describe our distributed system.
     */
    private static void startServentTest(String testName) {
        AppConfig.readBootstrapConfig(testName + "/bootstrap.properties");

        AppConfig.timestampedStandardPrint("Starting multiple servent runner. "
                + "If servents do not finish on their own, type \"stop\" to finish them");

        Process bsProcess;
        ProcessBuilder bsBuilder = new ProcessBuilder("java", "-cp", "out\\production\\kids_pr_ognjen_prica_rn10620", "app.BootstrapServer", String.valueOf(AppConfig.BOOTSTRAP_PORT));
        try {
            bsBuilder.redirectOutput(new File(testName + "/output/bootstrap" + "_out.txt"));
            bsBuilder.redirectError(new File(testName + "/error/bootstrap" + "_err.txt"));

            bsProcess = bsBuilder.start();
        } catch (IOException e) {
            AppConfig.timestampedErrorPrint(e);
            return;
        }

        // Wait for Bootstrap to start
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            AppConfig.timestampedErrorPrint(e);
        }

        int serventCount = AppConfig.SERVENT_COUNT;
        List<Process> serventProcesses = new ArrayList<>(); // Track processes

        for (int i = 0; i < serventCount; i++) {
            try {
                ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "start",
                        "java", "-cp", "out/production/kids_pr_ognjen_prica_rn10620", "app.ServentMain",
                        testName + "/servent" + "/servent" + i + ".properties", String.valueOf(i));

                // We'll let the system handle the output for each process in its own terminal window
                serventProcesses.add(builder.start());

            } catch (IOException e) {
                AppConfig.timestampedErrorPrint(e);
            }

//            try { // Give each node 10s to start up
//                Thread.sleep(10000);
//            } catch (InterruptedException e) {
//                AppConfig.timestampedErrorPrint(e);
//            }
        }

        Thread t = new Thread(new ServentCLI(serventProcesses, bsProcess));

        t.start(); // CLI thread waiting for user to type "stop".

        for (Process process : serventProcesses) {
            try {
                process.waitFor(); //Wait for graceful process finish.
            } catch (InterruptedException e) {
                AppConfig.timestampedErrorPrint(e);
            }
        }

        AppConfig.timestampedStandardPrint("All servent processes finished. Type \"stop\" to halt bootstrap.");
        try {
            bsProcess.waitFor();
        } catch (InterruptedException e) {
            AppConfig.timestampedErrorPrint(e);
        }
    }

    /**
     * We will wait for user stop in a separate thread.
     * The main thread is waiting for processes to end naturally.
     */
    private record ServentCLI(List<Process> serventProcesses, Process bsProcess) implements Runnable {

        @Override
        public void run() {
            Scanner sc = new Scanner(System.in);

            while (true) {
                String line = sc.nextLine();

                if (line.equals("stop")) {
                    for (Process process : serventProcesses) {
                        process.destroy();
                    }
                    bsProcess.destroy();
                    break;
                }
            }

            sc.close();
        }
    }

}
