package app.snapshot_bitcake;

import app.AppConfig;
import app.snapshot_bitcake.ly.LYSnapshotResult;
import app.snapshot_bitcake.ly.LaiYangBitcakeManager;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport
 * and Lai-Yang snapshot algorithms.
 *
 * @author bmilojkovic
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

    private volatile boolean working = true;

    private final AtomicBoolean collecting = new AtomicBoolean(false);
    private final Map<Integer, LYSnapshotResult> collectedLYValues = new ConcurrentHashMap<>();

    private final SnapshotType snapshotType;

    private BitcakeManager bitcakeManager;

    public SnapshotCollectorWorker(SnapshotType snapshotType) {
        this.snapshotType = snapshotType;

        switch (snapshotType) {
            case LAI_YANG:
                bitcakeManager = new LaiYangBitcakeManager();
                break;
            case NONE:
                AppConfig.timestampedErrorPrint("Making snapshot collector without specifying type. Exiting...");
                System.exit(0);
        }
    }

    @Override
    public void run() {
        while (working) {
            /*
             * Not collecting yet - just sleep until we start actual work, or finish
             */
            while (!collecting.get()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    AppConfig.timestampedErrorPrint(e);
                }

                if (!working) {
                    return;
                }
            }

            /*
             * Collecting is done in three stages:
             * 1. Send messages asking for values
             * 2. Wait for all the responses
             * 3. Print result
             */

            //1 send asks
            switch (snapshotType) {
                case LAI_YANG:
                    ((LaiYangBitcakeManager) bitcakeManager).markerEvent(AppConfig.myServentInfo.id(), this);
                    break;
                case NONE:
                    //Shouldn't be able to come here. See constructor.
                    break;
            }

            //2 wait for responses or finish
            boolean waiting = true;
            while (waiting) {
                switch (snapshotType) {
                    case LAI_YANG:
                        if (collectedLYValues.size() == AppConfig.getServentCount()) {
                            waiting = false;
                        }
                        break;
                    case NONE:
                        //Shouldn't be able to come here. See constructor.
                        break;
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    AppConfig.timestampedErrorPrint(e);
                }

                if (!working) {
                    return;
                }
            }

            //print
            int sum;
            switch (snapshotType) {
                case LAI_YANG:
                    sum = 0;
                    for (Entry<Integer, LYSnapshotResult> nodeResult : collectedLYValues.entrySet()) {
                        sum += nodeResult.getValue().recordedAmount();
                        AppConfig.timestampedStandardPrint(
                                "Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().recordedAmount());
                    }
                    for (int i = 0; i < AppConfig.getServentCount(); i++) {
                        for (int j = 0; j < AppConfig.getServentCount(); j++) {
                            if (i != j) {
                                if (AppConfig.getInfoById(i).neighbors().contains(j) &&
                                        AppConfig.getInfoById(j).neighbors().contains(i)) {
                                    int ijAmount = collectedLYValues.get(i).giveHistory().get(j);
                                    int jiAmount = collectedLYValues.get(j).getHistory().get(i);

                                    if (ijAmount != jiAmount) {
                                        String outputString = String.format(
                                                "Unreceived bitcake amount: %d from servent %d to servent %d",
                                                ijAmount - jiAmount, i, j);
                                        AppConfig.timestampedStandardPrint(outputString);
                                        sum += ijAmount - jiAmount;
                                    }
                                }
                            }
                        }
                    }

                    AppConfig.timestampedStandardPrint("System bitcake count: " + sum);

                    collectedLYValues.clear(); //reset for next invocation
                    break;
                //Shouldn't be able to come here. See constructor.
            }
            collecting.set(false);
        }

    }

    @Override
    public BitcakeManager getBitcakeManager() {
        return bitcakeManager;
    }

    @Override
    public void addLYSnapshotInfo(int id, LYSnapshotResult lySnapshotResult) {
        collectedLYValues.put(id, lySnapshotResult);
    }

    @Override
    public void startCollecting() {
        boolean oldValue = this.collecting.getAndSet(true);

        if (oldValue) {
            AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
        }
    }

    @Override
    public void stop() {
        working = false;
    }

}
