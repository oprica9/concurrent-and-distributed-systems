package app;

import app.snapshot_bitcake.NullSnapshotCollector;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotCollectorWorker;
import app.snapshot_bitcake.SnapshotType;
import cli.CLIParser;
import servent.SimpleServentListener;
import servent.message.util.MessageUtil;

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
        if (args.length != 2) {
            AppConfig.timestampedErrorPrint("Please provide servent list file and id of this servent.");
        }

        int serventId = -1;
        int portNumber;

        String serventListFile = args[0];

        AppConfig.readConfig(serventListFile);

        try {
            serventId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Second argument should be an int. Exiting...");
            System.exit(0);
        }

        if (serventId >= AppConfig.getServentCount()) {
            AppConfig.timestampedErrorPrint("Invalid servent id provided");
            System.exit(0);
        }

        AppConfig.myServentInfo = AppConfig.getInfoById(serventId);

        try {
            portNumber = AppConfig.myServentInfo.listenerPort();

            if (portNumber < 1000 || portNumber > 2000) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Port number should be in range 1000-2000. Exiting...");
            System.exit(0);
        }

        MessageUtil.initializePendingMessages();

        AppConfig.timestampedStandardPrint("Starting servent " + AppConfig.myServentInfo);

        Thread cliThread = getCLIThread();
        cliThread.start();
    }

    private static Thread getCLIThread() {
        SnapshotCollector snapshotCollector;

        if (AppConfig.SNAPSHOT_TYPE == SnapshotType.NONE) {
            snapshotCollector = new NullSnapshotCollector();
        } else {
            snapshotCollector = new SnapshotCollectorWorker(AppConfig.SNAPSHOT_TYPE);
        }
        Thread snapshotCollectorThread = new Thread(snapshotCollector);
        snapshotCollectorThread.start();

        CLIParser cliParser = getCliParser(snapshotCollector);
        return new Thread(cliParser);
    }

    private static CLIParser getCliParser(SnapshotCollector snapshotCollector) {
        SimpleServentListener simpleListener = new SimpleServentListener(snapshotCollector);
        Thread listenerThread = new Thread(simpleListener);
        listenerThread.start();

        return new CLIParser(simpleListener, snapshotCollector);
    }
}
