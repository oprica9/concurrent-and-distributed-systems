package app;

import app.failure_detection.FailureDetector;
import cli.CLIParser;
import servent.SimpleServentListener;

import java.util.Arrays;

/**
 * Describes the procedure for starting a single Servent
 *
 * @author bmilojkovic
 */
public class ServentMain {

    /**
     * Command line arguments are:
     * 0 - path to servent list file
     * 1 - this servent's id
     */
    public static void main(String[] args) {
        AppConfig.timestampedStandardPrint(Arrays.toString(args));
        if (args.length != 2) {
            AppConfig.timestampedErrorPrint("Please provide servent list file and id of this servent.");
        }

        int serventId = -1;
        int portNumber;

        String serventListFile = args[0];

        try {
            serventId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Second argument should be an int. Exiting...");
            System.exit(0);
        }

        AppConfig.readServentConfig(serventListFile);

        try {
            portNumber = AppConfig.myServentInfo.getListenerPort();

            if (portNumber < 1000 || portNumber > 2000) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Port number should be in range 1000-2000. Exiting...");
            System.exit(0);
        }

        AppConfig.timestampedStandardPrint("Starting servent " + AppConfig.myServentInfo);

        Thread initializerThread = getInitializerThread();
        initializerThread.start();
    }

    private static Thread getInitializerThread() {
        FailureDetector failureDetector = new FailureDetector();
        Thread failureDetectorThread = new Thread(failureDetector);
        failureDetectorThread.start();

        FileManager fileManager = new FileManager();

        SimpleServentListener simpleListener = new SimpleServentListener(failureDetector, fileManager);
        Thread listenerThread = new Thread(simpleListener);
        listenerThread.start();

        CLIParser cliParser = new CLIParser(simpleListener, failureDetector, fileManager);
        Thread cliThread = new Thread(cliParser);
        cliThread.start();

        ServentInitializer serventInitializer = new ServentInitializer();
        return new Thread(serventInitializer);
    }
}
