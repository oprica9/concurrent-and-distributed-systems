package app.snapshot_bitcake.ly;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import app.AppConfig;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.message.Message;
import servent.message.snapshot.ly.LYMarkerMessage;
import servent.message.snapshot.ly.LYTellMessage;
import servent.message.util.MessageUtil;

public class LaiYangBitcakeManager implements BitcakeManager {

    private final AtomicInteger currentAmount = new AtomicInteger(1000);

    public void takeSomeBitcakes(int amount) {
        currentAmount.getAndAdd(-amount);
    }

    public void addSomeBitcakes(int amount) {
        currentAmount.getAndAdd(amount);
    }

    public int getCurrentBitcakeAmount() {
        return currentAmount.get();
    }

    private final Map<Integer, Integer> giveHistory = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> getHistory = new ConcurrentHashMap<>();

    public LaiYangBitcakeManager() {
        for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
            giveHistory.put(neighbor, 0);
            getHistory.put(neighbor, 0);
        }
    }

    /*
     * This value is protected by AppConfig.colorLock.
     * Access it only if you have the blessing.
     */
    public int recordedAmount = 0;

    public void markerEvent(int collectorId, SnapshotCollector snapshotCollector) {
        synchronized (AppConfig.colorLock) {
            AppConfig.isWhite.set(false);
            recordedAmount = getCurrentBitcakeAmount();

            LYSnapshotResult snapshotResult = new LYSnapshotResult(
                    AppConfig.myServentInfo.id(), recordedAmount, giveHistory, getHistory);

            if (collectorId == AppConfig.myServentInfo.id()) {
                snapshotCollector.addLYSnapshotInfo(
                        AppConfig.myServentInfo.id(),
                        snapshotResult);
            } else {

                Message tellMessage = new LYTellMessage(
                        AppConfig.myServentInfo, AppConfig.getInfoById(collectorId), snapshotResult);

                MessageUtil.sendMessage(tellMessage);
            }

            for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
                Message clMarker = new LYMarkerMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor), collectorId);
                MessageUtil.sendMessage(clMarker);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    AppConfig.timestampedErrorPrint(e);
                }
            }
        }
    }

    private record MapValueUpdater(int valueToAdd) implements BiFunction<Integer, Integer, Integer> {
        @Override
        public Integer apply(Integer key, Integer oldValue) {
            return oldValue + valueToAdd;
        }
    }

    public void recordGiveTransaction(int neighbor, int amount) {
        giveHistory.compute(neighbor, new MapValueUpdater(amount));
    }

    public void recordGetTransaction(int neighbor, int amount) {
        getHistory.compute(neighbor, new MapValueUpdater(amount));
    }
}
