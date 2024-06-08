package app;

import app.model.ServentInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

/**
 * This class contains all the global application configuration stuff.
 *
 * @author bmilojkovic
 */
public class AppConfig {

    public static final Object failLock = new Object();
    /**
     * Convenience access for this servent's information
     */
    public static ServentInfo myServentInfo;
    public static boolean INITIALIZED = false;
    public static String BOOTSTRAP_IP;
    public static String SERVENT_IP;
    public static int BOOTSTRAP_PORT;
    public static int SERVENT_COUNT = 6; // TODO
    public static ChordState chordState;
    public static ServentInfo temp;
    public static String ROOT;

    /**
     * Print a message to stdout with a timestamp
     *
     * @param message message to print
     */
    public static void timestampedStandardPrint(String message) {
        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        Date now = new Date();

        System.out.println(timeFormat.format(now) + " - " + message);
    }

    /**
     * Print a message to stderr with a timestamp
     *
     * @param message message to print
     */
    public static void timestampedErrorPrint(String message) {
        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        Date now = new Date();

        System.err.println(timeFormat.format(now) + " - " + message);
    }

    /**
     * Print a message to stderr with a timestamp
     *
     * @param e exception to print
     */
    public static void timestampedErrorPrint(Exception e) {
        String err = e.getMessage();
        StackTraceElement[] stackTrace = e.getStackTrace();
        timestampedErrorPrint("Error: " + err + ", StackTrace: " + Arrays.toString(stackTrace));
    }

    public static void readBootstrapConfig(String configName) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(configName));
        } catch (IOException e) {
            timestampedErrorPrint("Couldn't open properties file. Exiting...");
            System.exit(0);
        }

        BOOTSTRAP_IP = properties.getProperty("bootstrap.ip");
        try {
            BOOTSTRAP_PORT = Integer.parseInt(properties.getProperty("bootstrap.port"));
        } catch (NumberFormatException e) {
            timestampedErrorPrint("Problem reading bootstrap_port. Exiting...");
            System.exit(0);
        }

        try {
            ChordState.CHORD_SIZE = Integer.parseInt(properties.getProperty("chord_size"));
        } catch (NumberFormatException e) {
            timestampedErrorPrint("Problem reading Chord size. Exiting...");
            System.exit(0);
        }

        myServentInfo = new ServentInfo(BOOTSTRAP_IP, BOOTSTRAP_PORT);
    }

    /**
     * Reads a servent config file. Should be called once at start of app.
     * The config file should be of the following format:
     * <br/>
     * <code><br/>
     * bs.port=2000				- bootstrap server listener port <br/>
     * servent0.port=1100 		- listener ports for each servent <br/>
     * servent1.port=1200 <br/>
     * servent2.port=1300 <br/>
     *
     * </code>
     * <br/>
     * So in this case, we would have three servents, listening on ports:
     * 1100, 1200, and 1300. A bootstrap server listening on port 2000, and Chord system with
     * max 64 keys and 64 nodes.<br/>
     *
     * @param configName name of configuration file
     */
    public static void readServentConfig(String configName) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(configName));

        } catch (IOException e) {
            timestampedErrorPrint("Couldn't open properties file. Exiting...");
            System.exit(0);
        }

        try {
            ChordState.CHORD_SIZE = Integer.parseInt(properties.getProperty("chord_size"));
        } catch (NumberFormatException e) {
            timestampedErrorPrint("Problem reading Chord size. Exiting...");
            System.exit(0);
        }

        chordState = new ChordState();

        ROOT = properties.getProperty("root");
        File rootDir = new File(ROOT);
        if (!Files.exists(rootDir.toPath()) || !rootDir.isDirectory()) {
            timestampedErrorPrint("Root directory cannot be found at " + ROOT + ". Exiting...");
            System.exit(0);
        }

        int listenerPort = -1;
        SERVENT_IP = properties.getProperty("ip");
        try {
            listenerPort = Integer.parseInt(properties.getProperty("port"));
        } catch (NumberFormatException e) {
            timestampedErrorPrint("Problem reading servent port. Exiting...");
            System.exit(0);
        }

        BOOTSTRAP_IP = properties.getProperty("bootstrap.ip");
        try {
            BOOTSTRAP_PORT = Integer.parseInt(properties.getProperty("bootstrap.port"));
        } catch (NumberFormatException e) {
            timestampedErrorPrint("Problem reading bootstrap_port. Exiting...");
            System.exit(0);
        }

        int weakFailureConsistency = -1;
        int strongFailureConsistency = -1;
        try {
            weakFailureConsistency = Integer.parseInt(properties.getProperty("weak_failure_consistency"));
            strongFailureConsistency = Integer.parseInt(properties.getProperty("strong_failure_consistency"));
        } catch (NumberFormatException e) {
            timestampedErrorPrint("Problem reading failure consistency. Exiting...");
            System.exit(0);
        }

        myServentInfo = new ServentInfo(SERVENT_IP, listenerPort, weakFailureConsistency, strongFailureConsistency);
    }

}
