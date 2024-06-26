package app.configuration;

import app.ServentInfo;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class contains all the global application configuration stuff.
 *
 * @author bmilojkovic
 */
public class AppConfig {

    /**
     * Convenience access for this servent's information
     */
    public static ServentInfo myServentInfo;
    /**
     * If this is true, the system is a clique - all nodes are each other's
     * neighbors.
     */
    public static boolean IS_CLIQUE;
    /**
     * If this is true, messages will be sent purely via FIFO -
     * if message M1 is sent before message M2 from one node to another,
     * M1 will also be received before M2.
     */
    public static boolean IS_FIFO;
    /**
     * Snapshot algorithm selection. Possible values are:
     * <ul>
     * <li>naive - No algorithm</li>
     * <li>cl - Chandy-Lamport - works on FIFO systems</li>
     * <li>ly - Lai-Yang - works on non-FIFO systems</li>
     * </ul>
     */
    public static SnapshotType SNAPSHOT_TYPE;
    public static AtomicBoolean isWhite = new AtomicBoolean(true);
    public static final Object colorLock = new Object();
    private static final List<ServentInfo> serventInfoList = new ArrayList<>();

    // Li et al.
    private static final List<Integer> initiators = new ArrayList<>();
    /**
     * Snapshots initiated by an initiator are assigned a sequence number.
     * MKNO is the sequence number of the algorithm’s most recent invocation by an initiator.
     */
    public static final AtomicInteger mkno = new AtomicInteger(-1);
    public static final Map<Integer, Integer> initIdMKNOs = new ConcurrentHashMap<>();
    public static final AtomicInteger currentInitId = new AtomicInteger(-1);
    public static final AtomicBoolean snapshotInProgress = new AtomicBoolean(false);

    // Spezialetti-Kearns
    /**
     * If process pi executed the “marker sending rule” because it received its first marker
     * from process pj, then process pj is the parent of process pi in the spanning tree.
     */
    public static final AtomicInteger parent = new AtomicInteger(-1);
    /**
     * When a process executes the “marker sending rule” on the receipt of its first marker,
     * it records the initiator’s identifier carried in the received marker in the master variable.
     * A process that initiates the algorithm records its own identifier in the master variable.
     */
    public static final AtomicInteger master = new AtomicInteger(-1);
    /**
     * The variable idBorderSet at a process contains the identifiers of the neighboring regions.
     */
    public static final Set<Integer> idBorderSet = ConcurrentHashMap.newKeySet();

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

    public static void timestampedErrorPrint(Exception e) {
        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        Date now = new Date();

        System.err.println(timeFormat.format(now) + " - Exception: " + e);
    }

    /**
     * Reads a config file. Should be called once at start of app.
     * The config file should be of the following format:
     * <br/>
     * <code><br/>
     * servent_count=3 			- number of servents in the system <br/>
     * clique=false 			- is it a clique or not <br/>
     * fifo=false				- should sending be fifo
     * servent0.port=1100 		- listener ports for each servent <br/>
     * servent1.port=1200 <br/>
     * servent2.port=1300 <br/>
     * servent0.neighbors=1,2 	- if not a clique, who are the neighbors <br/>
     * servent1.neighbors=0 <br/>
     * servent2.neighbors=0 <br/>
     *
     * </code>
     * <br/>
     * So in this case, we would have three servents, listening on ports:
     * 1100, 1200, and 1300. This is not a clique, and:<br/>
     * servent 0 sees servent 1 and 2<br/>
     * servent 1 sees servent 0<br/>
     * servent 2 sees servent 0<br/>
     *
     * @param configName name of configuration file
     */
    public static void readConfig(String configName) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(configName));

        } catch (IOException e) {
            timestampedErrorPrint("Couldn't open properties file. Exiting...");
            System.exit(0);
        }

        int serventCount = -1;
        try {
            serventCount = Integer.parseInt(properties.getProperty("servent_count"));
        } catch (NumberFormatException e) {
            timestampedErrorPrint("Problem reading servent_count. Exiting...");
            System.exit(0);
        }

        IS_CLIQUE = Boolean.parseBoolean(properties.getProperty("clique", "false"));
        IS_FIFO = Boolean.parseBoolean(properties.getProperty("fifo", "false"));
        String snapshotType = properties.getProperty("snapshot");
        if (snapshotType == null) {
            snapshotType = "none";
        }
        if (snapshotType.equals("li")) {
            SNAPSHOT_TYPE = SnapshotType.LI;
        } else {
            timestampedErrorPrint("Problem reading snapshot algorithm. Defaulting to NONE.");
            SNAPSHOT_TYPE = SnapshotType.NONE;
        }

        for (int i = 0; i < serventCount; i++) {
            String portProperty = "servent" + i + ".port";

            int serventPort = -1;

            try {
                serventPort = Integer.parseInt(properties.getProperty(portProperty));
            } catch (NumberFormatException e) {
                timestampedErrorPrint("Problem reading " + portProperty + ". Exiting...");
                System.exit(0);
            }

            List<Integer> neighborList = new ArrayList<>();
            if (IS_CLIQUE) {
                for (int j = 0; j < serventCount; j++) {
                    if (j == i) {
                        continue;
                    }

                    neighborList.add(j);
                }
            } else {
                String neighborListProp = properties.getProperty("servent" + i + ".neighbors");

                if (neighborListProp == null) {
                    timestampedErrorPrint("Warning: graph is not clique, and node " + i + " doesnt have neighbors");
                } else {
                    String[] neighborListArr = neighborListProp.split(",");

                    try {
                        for (String neighbor : neighborListArr) {
                            neighborList.add(Integer.parseInt(neighbor));
                        }
                    } catch (NumberFormatException e) {
                        timestampedErrorPrint("Bad neighbor list for node " + i + ": " + neighborListProp);
                    }
                }
            }

            String initiatorsListProp = properties.getProperty("initiators");

            if (initiatorsListProp == null) {
                timestampedErrorPrint("Warning: no initiators specified");
            } else {
                String[] initiatorListArr = initiatorsListProp.split(",");

                try {
                    for (String initiator : initiatorListArr) {
                        initiators.add(Integer.parseInt(initiator));
                    }
                } catch (NumberFormatException e) {
                    timestampedErrorPrint("Could not parse initiator values.");
                    System.exit(0);
                }
            }

            ServentInfo newInfo = new ServentInfo("localhost", i, serventPort, neighborList);
            serventInfoList.add(newInfo);
        }
    }

    /**
     * Get info for a servent selected by a given id.
     *
     * @param id id of servent to get info for
     * @return {@link ServentInfo} object for this id
     */
    public static ServentInfo getInfoById(int id) {
        if (id >= getServentCount()) {
            throw new IllegalArgumentException(
                    "Trying to get info for servent " + id + " when there are " + getServentCount() + " servents.");
        }
        return serventInfoList.get(id);
    }

    /**
     * Get number of servents in this system.
     */
    public static int getServentCount() {
        return serventInfoList.size();
    }

    public static List<Integer> getInitiators() {
        return initiators;
    }

}
