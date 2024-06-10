package app.bitcake_manager.li;

import app.bitcake_manager.BitcakeManager;
import app.configuration.AppConfig;
import app.snapshot_collector.SnapshotCollector;
import servent.message.Message;
import servent.message.snapshot.li.LiMarkerMessage;
import servent.message.snapshot.li.LiTellMessage;
import servent.message.snapshot.li.Tag;
import servent.message.util.MessageUtil;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class LiBitcakeManager implements BitcakeManager {

    private final AtomicInteger currentAmount = new AtomicInteger(500);
    /**
     * This value is protected by AppConfig.colorLock.
     * Access it only if you have the blessing.
     */
    public int recordedAmount = 0;
    private final Map<Integer, Integer> giveHistory = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> getHistory = new ConcurrentHashMap<>();

    private LiSnapshotResult mySnapshotResult;
    private final Set<Integer> receivedMarkers = ConcurrentHashMap.newKeySet();

    public LiBitcakeManager() {
        for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
            giveHistory.put(neighbor, 0);
            getHistory.put(neighbor, 0);
        }
    }

    public void markerEvent(Tag tag, int senderId, SnapshotCollector snapshotCollector) {
        synchronized (AppConfig.colorLock) {
            if (AppConfig.myServentInfo.id() == AppConfig.master.get()) {
                receivedMarkers.add(senderId);

                if (tag.initId() != AppConfig.master.get()) {
                    AppConfig.idBorderSet.add(tag.initId());
                }

                if (receivedAllMarkers()) {
                    snapshotCollector.setCompletedRegion(true);
                }

                return;
            }

            // We don't want to participate in a snapshot we already participated in
            if (tag.mkno() < AppConfig.initIdMKNOs.getOrDefault(tag.initId(), -1)) {
                return;
            }

            receivedMarkers.add(senderId);

            if (AppConfig.parent.get() == -1) {
                // If process pi executed the “marker sending rule” because it received its first marker
                // from process pj, then process pj is the parent of process pi in the spanning tree.
                AppConfig.parent.set(senderId);

                // When a process executes the “marker sending rule” on the receipt of its first marker,
                // it records the initiator’s identifier carried in the received marker in the master variable
                AppConfig.master.set(tag.initId());

                AppConfig.currentInitId.set(tag.initId());
                AppConfig.initIdMKNOs.put(tag.initId(), tag.mkno());
                AppConfig.snapshotInProgress.set(true);

                recordMySnapshot();

                System.out.println("My parent: " + AppConfig.parent.get() + ", my master: " + AppConfig.master.get());
                System.out.println("PROPAGATING AFTER INITIATION");
                propagateMarker();
            }

            if (tag.initId() != AppConfig.master.get()) {
                // When the initiator’s identifier in a marker received along a channel is
                // different from the value in the master variable, a concurrent initiation of the
                // algorithm is detected and the sender of the marker lies in a different region.
                // The identifier of the concurrent initiator is recorded in a local variable idBorderSet.
                AppConfig.idBorderSet.add(tag.initId());
            }

            // When a leaf process in the spanning tree has recorded the states of all
            // incoming channels, the process sends the locally recorded state
            // (local snapshot, id-border-set) to its parent in the spanning tree.

            // After an intermediate process in a spanning tree has received the recorded states from
            // all its child processes and has recorded the states of all incoming channels, it forwards
            // its locally recorded state and the locally recorded states of all its descendent
            // processes to its parent.
            System.out.println(receivedMarkers + " and should receive from: " + AppConfig.myServentInfo.neighbors());
            if (receivedAllMarkers()) {
                Map<Integer, LiSnapshotResult> childrenResults = snapshotCollector.getLiSnapshotResults();

                childrenResults.put(AppConfig.myServentInfo.id(), mySnapshotResult);

                Message liTell = new LiTellMessage(
                        AppConfig.myServentInfo,
                        AppConfig.getInfoById(AppConfig.parent.get()),
                        childrenResults,
                        AppConfig.idBorderSet
                );
                MessageUtil.sendMessage(liTell);
            }
        }
    }

    public void initSnapshot(SnapshotCollector snapshotCollector) {
        System.out.println("GOT HERE?");
        synchronized (AppConfig.colorLock) {
            int initId = AppConfig.myServentInfo.id();
            int currMKNO = AppConfig.mkno.incrementAndGet();
            AppConfig.currentInitId.set(initId);
            AppConfig.snapshotInProgress.set(true);
            AppConfig.initIdMKNOs.put(initId, currMKNO);
            AppConfig.master.set(initId);
            AppConfig.parent.set(initId);

            recordMySnapshot();

            snapshotCollector.addLiSnapshotInfo(AppConfig.myServentInfo.id(), mySnapshotResult);

            System.out.println("PROPAGATING DURING INITIATION");
            propagateMarker();
        }
    }

    private void propagateMarker() {
        if (AppConfig.myServentInfo.id() == 4) {
            System.out.println("WHY 5");
        }
        synchronized (AppConfig.colorLock) {
            for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
                if (neighbor != AppConfig.parent.get()) {
                    Message liMarker = new LiMarkerMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor));
                    MessageUtil.sendMessage(liMarker);
                }
                try {
                    // This sleep is here to artificially produce some non-tagged node -> tagged node messages
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    AppConfig.timestampedErrorPrint(e);
                }
            }
        }
    }

    private void recordMySnapshot() {
        synchronized (AppConfig.colorLock) {
            recordedAmount = getCurrentBitcakeAmount();
            mySnapshotResult = new LiSnapshotResult(
                    AppConfig.myServentInfo.id(),
                    AppConfig.parent.get(),
                    recordedAmount,
                    giveHistory,
                    getHistory
            );
        }
    }

    @Override
    public void takeSomeBitcakes(int amount) {
        currentAmount.getAndAdd(-amount);
    }

    @Override
    public void addSomeBitcakes(int amount) {
        currentAmount.getAndAdd(amount);
    }

    @Override
    public int getCurrentBitcakeAmount() {
        return currentAmount.get();
    }

    public void recordGiveTransaction(int neighbor, int amount) {
        giveHistory.compute(neighbor, new MapValueUpdater(amount));
    }

    public void recordGetTransaction(int neighbor, int amount) {
        getHistory.compute(neighbor, new MapValueUpdater(amount));
    }

    private boolean receivedAllMarkers() {
        return receivedMarkers.containsAll(AppConfig.myServentInfo.neighbors());
    }

    private record MapValueUpdater(int valueToAdd) implements BiFunction<Integer, Integer, Integer> {
        @Override
        public Integer apply(Integer key, Integer oldValue) {
            return oldValue + valueToAdd;
        }
    }
}
