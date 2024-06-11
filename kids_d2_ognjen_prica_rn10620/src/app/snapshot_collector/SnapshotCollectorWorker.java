package app.snapshot_collector;

import app.bitcake_manager.BitcakeManager;
import app.bitcake_manager.li.LiBitcakeManager;
import app.bitcake_manager.li.LiSnapshotResult;
import app.configuration.AppConfig;
import app.configuration.SnapshotType;
import app.util.Node;
import servent.message.Message;
import servent.message.snapshot.li.BlankMessage;
import servent.message.snapshot.li.ExchangeMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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

    private final Map<Integer, LiSnapshotResult> collectedLiValues = new ConcurrentHashMap<>();
    private final AtomicBoolean completedRegion = new AtomicBoolean(false);
    private final Map<Integer, LiSnapshotResult> receivedRegionResults = new ConcurrentHashMap<>();
    private final Set<Integer> receivedResultsFrom = ConcurrentHashMap.newKeySet();
    private final Set<Integer> receivedBlanks = ConcurrentHashMap.newKeySet();
    private final Set<Integer> gotExchangeMessage = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean changesMade = new AtomicBoolean(false);

    private final SnapshotType snapshotType;

    private BitcakeManager bitcakeManager;

    public SnapshotCollectorWorker(SnapshotType snapshotType) {
        this.snapshotType = snapshotType;

        switch (snapshotType) {
            case LI:
                bitcakeManager = new LiBitcakeManager();
                break;
            case NONE:
                AppConfig.timestampedErrorPrint("Making snapshot collector without specifying type. Exiting...");
                System.exit(0);
        }
    }

    @Override
    public BitcakeManager getBitcakeManager() {
        return bitcakeManager;
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

            // 1 send asks
            ((LiBitcakeManager) bitcakeManager).initSnapshot(this);

            //2 wait for responses or finish
            boolean waiting = true;
            while (waiting) {
                if (completedRegion.get()) {
                    waiting = false;
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


            if (snapshotType == SnapshotType.LI) {
                AppConfig.timestampedStandardPrint("Spanning tree for region: " + AppConfig.myServentInfo.id());
                AppConfig.timestampedStandardPrint("Starting rounds with " + AppConfig.idBorderSet);

                printSpanningTree(collectedLiValues);

                AppConfig.timestampedStandardPrint("Sending my region: " + collectedLiValues);

                // Start rounds
                for (Integer adjacentInitiator : AppConfig.idBorderSet) {
                    Message exchangeMessage = new ExchangeMessage(
                            AppConfig.myServentInfo, AppConfig.getInfoById(adjacentInitiator),
                            collectedLiValues
                    );
                    MessageUtil.sendMessage(exchangeMessage);
                }

                // Start exchanging information
                waiting = true;
                while (waiting) {
                    // Start next round
                    if (gotExchangeMessage.containsAll(AppConfig.idBorderSet)) {
                        if (receivedRegionResults.isEmpty()) {
                            // Nothing changed
                            changesMade.set(false);

                            for (Integer adjacentInitiator : AppConfig.idBorderSet) {
                                Message blankMessage = new BlankMessage(
                                        AppConfig.myServentInfo, AppConfig.getInfoById(adjacentInitiator)
                                );
                                MessageUtil.sendMessage(blankMessage);
                            }
                        } else {
                            // There were changes
                            changesMade.set(true);

                            System.out.println("Sending new information: " + receivedRegionResults);

                            for (Integer adjacentInitiator : AppConfig.idBorderSet) {
                                Message exchangeMessage = new ExchangeMessage(
                                        AppConfig.myServentInfo, AppConfig.getInfoById(adjacentInitiator),
                                        deepCopyReceivedRegionResults()
                                );
                                MessageUtil.sendMessage(exchangeMessage);
                            }

                            collectedLiValues.putAll(receivedRegionResults);
                        }

                        System.out.println("!changesMade.get() = " + !changesMade.get());
                        System.out.println(receivedBlanks + " =? " + AppConfig.idBorderSet);
                        System.out.println("receivedRegionResults = " + receivedRegionResults);

                        if (!changesMade.get()
                                && receivedBlanks.containsAll(AppConfig.idBorderSet)
                                && receivedRegionResults.isEmpty()) {

                            for (Integer adjacentInitiator : AppConfig.idBorderSet) {
                                Message blankMessage = new BlankMessage(
                                        AppConfig.myServentInfo, AppConfig.getInfoById(adjacentInitiator)
                                );
                                MessageUtil.sendMessage(blankMessage);
                            }

                            waiting = false;
                        }

                        gotExchangeMessage.clear();
                        receivedRegionResults.clear();
                        receivedBlanks.clear();
                        changesMade.set(true);
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
                AppConfig.timestampedStandardPrint("Final snapshot: " + collectedLiValues);
            }

            //print
            printLi();

            collecting.set(false);
        }
    }


    @Override
    public void setCompletedRegion(boolean value) {
        completedRegion.set(value);
    }

    @Override
    public void addReceivedRegionResults(int regionMasterId, Map<Integer, LiSnapshotResult> regionResults) {

        AppConfig.timestampedStandardPrint("Got region data from " + regionMasterId + ":");
        for (Map.Entry<Integer, LiSnapshotResult> entry : regionResults.entrySet()) {
            System.out.println(entry.getValue());
        }

        gotExchangeMessage.add(regionMasterId);

        for (Map.Entry<Integer, LiSnapshotResult> entry : regionResults.entrySet()) {
            if (!receivedResultsFrom.contains(entry.getKey())) {
                AppConfig.timestampedStandardPrint("Adding: " + entry.getValue());
                receivedResultsFrom.add(entry.getKey());
                receivedRegionResults.put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void addReceivedBlank(int id) {
        gotExchangeMessage.add(id);
        receivedBlanks.add(id);
    }

    @Override
    public void addLiSnapshotInfo(int id, LiSnapshotResult liSnapshotResult) {
        collectedLiValues.put(id, liSnapshotResult);
    }

    @Override
    public void addLiSnapshotInfos(Map<Integer, LiSnapshotResult> liSnapshotResults) {
        collectedLiValues.putAll(liSnapshotResults);
    }

    @Override
    public Map<Integer, LiSnapshotResult> getLiSnapshotResults() {
        return collectedLiValues;
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

    private void printLi() {
        int sum;

        sum = 0;
        for (Entry<Integer, LiSnapshotResult> nodeResult : collectedLiValues.entrySet()) {
            sum += nodeResult.getValue().recordedAmount();
            AppConfig.timestampedStandardPrint(
                    "Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().recordedAmount());
        }
        for (int i = 0; i < AppConfig.getServentCount(); i++) {
            for (int j = 0; j < AppConfig.getServentCount(); j++) {
                if (i != j) {
                    if (AppConfig.getInfoById(i).neighbors().contains(j) &&
                            AppConfig.getInfoById(j).neighbors().contains(i)) {
                        int ijAmount = collectedLiValues.get(i).giveHistory().get(j);
                        int jiAmount = collectedLiValues.get(j).getHistory().get(i);

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

        collectedLiValues.clear(); //reset for next invocation
    }

    private void printSpanningTree(Map<Integer, LiSnapshotResult> liSnapshotResults) {
        synchronized (AppConfig.colorLock) {
            Node root = generateTree(liSnapshotResults);
            System.out.println();
            System.out.println(root);
        }
    }

    private Node generateTree(Map<Integer, LiSnapshotResult> liSnapshotResults) {
        synchronized (AppConfig.colorLock) {
            Map<Integer, Node> nodeMap = new HashMap<>();
            for (Map.Entry<Integer, LiSnapshotResult> entry : liSnapshotResults.entrySet()) {
                LiSnapshotResult result = entry.getValue();
                nodeMap.put(result.serventId(), new Node(result.serventId(), new ArrayList<>()));
            }
            Node root = null;
            for (Map.Entry<Integer, LiSnapshotResult> entry : liSnapshotResults.entrySet()) {
                LiSnapshotResult result = entry.getValue();
                int id = result.serventId();
                int parentId = result.parentId();

                Node node = nodeMap.get(id);
                if (parentId == id) {
                    root = node;
                } else {
                    Node parentNode = nodeMap.get(parentId);
                    parentNode.children().add(node);
                }
            }
            if (root == null) {
                throw new IllegalStateException("Invalid tree structure. No root node found.");
            }
            return root;
        }
    }

    private Map<Integer, LiSnapshotResult> deepCopyReceivedRegionResults() {
        Map<Integer, LiSnapshotResult> copiedMap = new ConcurrentHashMap<>();
        for (Map.Entry<Integer, LiSnapshotResult> entry : receivedRegionResults.entrySet()) {
            LiSnapshotResult originalResult = entry.getValue();
            LiSnapshotResult copiedResult = new LiSnapshotResult(
                    originalResult.serventId(),
                    originalResult.parentId(),
                    originalResult.recordedAmount(),
                    new ConcurrentHashMap<>(originalResult.giveHistory()),
                    new ConcurrentHashMap<>(originalResult.getHistory())
            );
            copiedMap.put(entry.getKey(), copiedResult);
        }
        return copiedMap;
    }

}
