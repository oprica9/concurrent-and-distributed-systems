package app;

import app.failure_detection.FailureDetector;
import app.file_manager.FileManager;
import app.friend_manager.FriendManager;
import app.mutex.SuzukiKasamiMutex;
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
        if (args.length != 1) {
            AppConfig.timestampedErrorPrint("Please provide servent property file.");
            System.exit(0);
        }

        String serventPropFile = args[0];

        AppConfig.readServentConfig(serventPropFile);

        int portNumber;
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
        SuzukiKasamiMutex.initialize();

        FailureDetector failureDetector = new FailureDetector();
        Thread failureDetectorThread = new Thread(failureDetector);
        failureDetectorThread.start();

        FriendManager friendManager = new FriendManager();

        FileManager fileManager = new FileManager(friendManager);

        SimpleServentListener simpleListener = new SimpleServentListener(failureDetector, friendManager, fileManager);
        Thread listenerThread = new Thread(simpleListener);
        listenerThread.start();

        CLIParser cliParser = new CLIParser(simpleListener, failureDetector, friendManager, fileManager);
        Thread cliThread = new Thread(cliParser);
        cliThread.start();

        ServentInitializer serventInitializer = new ServentInitializer();
        return new Thread(serventInitializer);
    }
}
