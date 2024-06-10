package app.bitcake_manager.chandy_lamport;

import app.configuration.AppConfig;
import app.bitcake_manager.BitcakeManager;
import app.snapshot_collector.SnapshotCollector;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.chandy_lamport.CLMarkerMessage;
import servent.message.snapshot.chandy_lamport.CLTellMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ChandyLamportBitcakeManager implements BitcakeManager {

    private final AtomicInteger currentAmount = new AtomicInteger(1000);
    /*
     * This value is protected by AppConfig.colorLock.
     * Access it only if you have the blessing.
     */
    public int recordedAmount = 0;
    private final Map<Integer, Boolean> closedChannels = new ConcurrentHashMap<>();
    private final Map<String, List<Integer>> allChannelTransactions = new ConcurrentHashMap<>();
    private final Object allChannelTransactionsLock = new Object();

    /**
     * This is invoked when we are white and get a marker. Basically,
     * we or someone else has started recording a snapshot.
     * This method does the following:
     * <ul>
     * <li>Makes us red</li>
     * <li>Records our bitcakes</li>
     * <li>Sets all channels to not closed</li>
     * <li>Sends markers to all neighbors</li>
     * </ul>
     *
     * @param collectorId - id of collector node, to be put into marker messages for others.
     */
    public void markerEvent(int collectorId) {
        synchronized (AppConfig.colorLock) {
            AppConfig.timestampedStandardPrint("Going red");
            AppConfig.isWhite.set(false);
            recordedAmount = getCurrentBitcakeAmount();

            for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
                closedChannels.put(neighbor, false);
                Message clMarker = new CLMarkerMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor), collectorId);
                MessageUtil.sendMessage(clMarker);
                try {
                    // This sleep is here to artificially produce some white node -> red node messages
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    AppConfig.timestampedErrorPrint(e);
                }
            }
        }

    }

    /**
     * This is invoked whenever we get a marker from another node. We do the following:
     * <ul>
     * <li>If we are white, we do markerEvent()</li>
     * <li>We mark the channel of the person that sent the marker as closed</li>
     * <li>If we are done, we report our snapshot result to the collector</li>
     * </ul>
     */
    public void handleMarker(Message clientMessage, SnapshotCollector snapshotCollector) {
        synchronized (AppConfig.colorLock) {
            int collectorId = Integer.parseInt(clientMessage.getMessageText());

            if (AppConfig.isWhite.get()) {
                markerEvent(collectorId);
            }

            closedChannels.put(clientMessage.getOriginalSenderInfo().id(), true);

            if (isDone()) {
                CLSnapshotResult snapshotResult = new CLSnapshotResult(
                        AppConfig.myServentInfo.id(), recordedAmount, allChannelTransactions);

                if (AppConfig.myServentInfo.id() == collectorId) {
                    snapshotCollector.addCLSnapshotInfo(collectorId, snapshotResult);
                } else {
                    Message clTellMessage = new CLTellMessage(
                            AppConfig.myServentInfo, AppConfig.getInfoById(collectorId),
                            snapshotResult);

                    MessageUtil.sendMessage(clTellMessage);
                }

                recordedAmount = 0;
                allChannelTransactions.clear();
                AppConfig.timestampedStandardPrint("Going white");
                AppConfig.isWhite.set(true);
            }
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

    /**
     * Checks if we are done being red. This happens when all channels are closed.
     *
     * @return if we are done being red
     */
    private boolean isDone() {
        if (AppConfig.isWhite.get()) {
            return false;
        }

        AppConfig.timestampedStandardPrint(closedChannels.toString());

        for (Entry<Integer, Boolean> closedChannel : closedChannels.entrySet()) {
            if (!closedChannel.getValue()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Records a channel message. This will be invoked if we are red and
     * get a message that is not a marker.
     */
    public void addChannelMessage(Message clientMessage) {
        if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
            synchronized (allChannelTransactionsLock) {
                String channelName = "channel " + AppConfig.myServentInfo.id() + "<-" + clientMessage.getOriginalSenderInfo().id();

                List<Integer> channelMessages = allChannelTransactions.getOrDefault(channelName, new ArrayList<>());
                channelMessages.add(Integer.parseInt(clientMessage.getMessageText()));
                allChannelTransactions.put(channelName, channelMessages);
            }
        }
    }
}
