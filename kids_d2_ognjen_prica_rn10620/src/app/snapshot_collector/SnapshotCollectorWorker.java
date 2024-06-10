package app.snapshot_collector;

import app.bitcake_manager.BitcakeManager;
import app.bitcake_manager.chandy_lamport.CLSnapshotResult;
import app.bitcake_manager.chandy_lamport.ChandyLamportBitcakeManager;
import app.bitcake_manager.lai_yang.LYSnapshotResult;
import app.bitcake_manager.lai_yang.LaiYangBitcakeManager;
import app.bitcake_manager.li.LiBitcakeManager;
import app.bitcake_manager.li.LiSnapshotResult;
import app.bitcake_manager.naive.NaiveBitcakeManager;
import app.configuration.AppConfig;
import app.configuration.SnapshotType;
import app.util.Node;
import servent.message.Message;
import servent.message.snapshot.li.BlankMessage;
import servent.message.snapshot.li.ExchangeMessage;
import servent.message.snapshot.naive.NaiveAskAmountMessage;
import servent.message.util.MessageUtil;

import java.util.*;
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

    private final Map<String, Integer> collectedNaiveValues = new ConcurrentHashMap<>();
    private final Map<Integer, CLSnapshotResult> collectedCLValues = new ConcurrentHashMap<>();
    private final Map<Integer, LYSnapshotResult> collectedLYValues = new ConcurrentHashMap<>();
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
            case NAIVE:
                bitcakeManager = new NaiveBitcakeManager();
                break;
            case CHANDY_LAMPORT:
                bitcakeManager = new ChandyLamportBitcakeManager();
                break;
            case LAI_YANG:
                bitcakeManager = new LaiYangBitcakeManager();
                break;
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
            switch (snapshotType) {
                case NAIVE:
                    Message askMessage = new NaiveAskAmountMessage(AppConfig.myServentInfo, null);

                    for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
                        askMessage = askMessage.changeReceiver(neighbor);

                        MessageUtil.sendMessage(askMessage);
                    }
                    collectedNaiveValues.put("node" + AppConfig.myServentInfo.id(), bitcakeManager.getCurrentBitcakeAmount());
                    break;


                case CHANDY_LAMPORT:
                    ((ChandyLamportBitcakeManager) bitcakeManager).markerEvent(AppConfig.myServentInfo.id());
                    break;


                case LAI_YANG:
                    ((LaiYangBitcakeManager) bitcakeManager).markerEvent(AppConfig.myServentInfo.id(), this);
                    break;

                case LI:
                    ((LiBitcakeManager) bitcakeManager).initSnapshot(this);
                    break;

                case NONE:
                    //Shouldn't be able to come here. See constructor.
                    break;
            }

            //2 wait for responses or finish
            boolean waiting = true;
            while (waiting) {
                switch (snapshotType) {
                    case NAIVE:
                        if (collectedNaiveValues.size() == AppConfig.getServentCount()) {
                            waiting = false;
                        }
                        break;


                    case CHANDY_LAMPORT:
                        if (collectedCLValues.size() == AppConfig.getServentCount()) {
                            waiting = false;
                        }
                        break;


                    case LAI_YANG:
                        if (collectedLYValues.size() == AppConfig.getServentCount()) {
                            waiting = false;
                        }
                        break;

                    case LI:
                        if (completedRegion.get()) {
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

            if (snapshotType == SnapshotType.LI) {
                System.out.println("STARTING ROUNDS...");

                System.out.println("Spanning tree for region: " + AppConfig.myServentInfo.id());

                printSpanningTree(collectedLiValues);

                System.out.println("Sending: " + collectedLiValues);

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
                    if (!changesMade.get() && receivedBlanks.containsAll(AppConfig.idBorderSet)) {
                        waiting = false;
                    }

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

                            for (Integer adjacentInitiator : AppConfig.idBorderSet) {
                                Message exchangeMessage = new ExchangeMessage(
                                        AppConfig.myServentInfo, AppConfig.getInfoById(adjacentInitiator),
                                        receivedRegionResults
                                );
                                MessageUtil.sendMessage(exchangeMessage);
                            }

                            collectedLiValues.putAll(receivedRegionResults);
                        }
                        receivedRegionResults.clear();
                        receivedBlanks.clear();
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
                System.out.println("Final snapshot: " + collectedLiValues);
            }

            //print
            int sum;
            switch (snapshotType) {
                case NAIVE:
                    sum = 0;
                    for (Entry<String, Integer> itemAmount : collectedNaiveValues.entrySet()) {
                        sum += itemAmount.getValue();
                        AppConfig.timestampedStandardPrint(
                                "Info for " + itemAmount.getKey() + " = " + itemAmount.getValue() + " bitcake");
                    }

                    AppConfig.timestampedStandardPrint("System bitcake count: " + sum);

                    collectedNaiveValues.clear(); //reset for next invocation
                    break;

                case CHANDY_LAMPORT:
                    sum = 0;
                    for (Entry<Integer, CLSnapshotResult> nodeResult : collectedCLValues.entrySet()) {
                        sum += nodeResult.getValue().recordedAmount();
                        AppConfig.timestampedStandardPrint(
                                "Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().recordedAmount());
                        if (nodeResult.getValue().allChannelMessages().isEmpty()) {
                            AppConfig.timestampedStandardPrint("No channel bitcake for " + nodeResult.getKey());
                        } else {
                            for (Entry<String, List<Integer>> channelMessages : nodeResult.getValue().allChannelMessages().entrySet()) {
                                int channelSum = 0;
                                for (Integer val : channelMessages.getValue()) {
                                    channelSum += val;
                                }
                                AppConfig.timestampedStandardPrint("Channel bitcake for " + channelMessages.getKey() +
                                        ": " + channelMessages.getValue() + " with channel bitcake sum: " + channelSum);

                                sum += channelSum;
                            }
                        }
                    }

                    AppConfig.timestampedStandardPrint("System bitcake count: " + sum);

                    collectedCLValues.clear();
                    break;

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

                    collectedLYValues.clear();
                    break;

                case LI:
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
                    break;
            }
            collecting.set(false);
        }
    }


    @Override
    public void setCompletedRegion(boolean value) {
        completedRegion.set(value);
    }

    @Override
    public void addReceivedRegionResults(int regionMasterId, Map<Integer, LiSnapshotResult> regionResults) {
        gotExchangeMessage.add(regionMasterId);

        for (Map.Entry<Integer, LiSnapshotResult> entry : regionResults.entrySet()) {
            if (!receivedResultsFrom.contains(entry.getKey())) {
                System.out.println("Adding: " + entry.getValue());
                receivedResultsFrom.add(entry.getKey());
                receivedRegionResults.put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void addReceivedBlank(int id) {
        receivedBlanks.add(id);
    }

    @Override
    public void addNaiveSnapshotInfo(String snapshotSubject, int amount) {
        collectedNaiveValues.put(snapshotSubject, amount);
    }

    @Override
    public void addCLSnapshotInfo(int id, CLSnapshotResult clSnapshotResult) {
        collectedCLValues.put(id, clSnapshotResult);
    }

    @Override
    public void addLYSnapshotInfo(int id, LYSnapshotResult lySnapshotResult) {
        collectedLYValues.put(id, lySnapshotResult);
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

}
