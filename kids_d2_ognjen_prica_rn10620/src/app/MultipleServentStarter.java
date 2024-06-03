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
        List<Process> serventProcesses = new ArrayList<>();

        AppConfig.readConfig(testName + "/servent_list.properties");

        AppConfig.timestampedStandardPrint("Starting multiple servent runner. "
                + "If servents do not finish on their own, type \"stop\" to finish them");

        int serventCount = AppConfig.getServentCount();

        for (int i = 0; i < serventCount; i++) {
            try {
                Process p = getProcess(testName, i);
                serventProcesses.add(p);

            } catch (IOException e) {
                AppConfig.timestampedErrorPrint(e);
            }
        }

        Thread t = new Thread(new ServentCLI(serventProcesses));

        t.start(); //CLI thread waiting for user to type "stop".

        for (Process process : serventProcesses) {
            try {
                process.waitFor(); //Wait for graceful process finish.
            } catch (InterruptedException e) {
                AppConfig.timestampedErrorPrint(e);
            }
        }

        AppConfig.timestampedStandardPrint("All servent processes finished. Type \"stop\" to exit.");
    }

    private static Process getProcess(String testName, int i) throws IOException {
        ProcessBuilder builder = new ProcessBuilder("java", "-cp", "out\\production\\kids_d2_ognjen_prica_rn10620", "app.ServentMain",
                testName + "/servent_list.properties", String.valueOf(i));
        //We use files to read and write.
        //System.out, System.err and System.in will point to these files.
        builder.redirectOutput(new File(testName + "/output/servent" + i + "_out.txt"));
        builder.redirectError(new File(testName + "/error/servent" + i + "_err.txt"));
        builder.redirectInput(new File(testName + "/input/servent" + i + "_in.txt"));

        //Starts the servent as a completely separate process.
        return builder.start();
    }

    /**
     * We will wait for user stop in a separate thread.
     * The main thread is waiting for processes to end naturally.
     */
    private record ServentCLI(List<Process> serventProcesses) implements Runnable {

        @Override
        public void run() {
            Scanner sc = new Scanner(System.in);

            while (true) {
                String line = sc.nextLine();

                if (line.equals("stop")) {
                    for (Process process : serventProcesses) {
                        process.destroy();
                    }
                    break;
                }
            }

            sc.close();
        }
    }
}
