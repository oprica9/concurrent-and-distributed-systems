package app.snapshot_bitcake.ly;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Snapshot result for servent with id serventId.
 * The amount of bitcakes on that servent is written in recordedAmount.
 * The channel messages are recorded in giveHistory and getHistory.
 * In Lai-Yang, the initiator has to reconcile the differences between
 * individual nodes, so we just let him know what we got and what we gave
 * and let him do the rest.
 *
 * @author bmilojkovic
 */
public record LYSnapshotResult(int serventId, int recordedAmount, Map<Integer, Integer> giveHistory,
                               Map<Integer, Integer> getHistory) implements Serializable {

    @Serial
    private static final long serialVersionUID = 8939516333227254439L;

    public LYSnapshotResult(int serventId, int recordedAmount,
                            Map<Integer, Integer> giveHistory, Map<Integer, Integer> getHistory) {
        this.serventId = serventId;
        this.recordedAmount = recordedAmount;
        this.giveHistory = new ConcurrentHashMap<>(giveHistory);
        this.getHistory = new ConcurrentHashMap<>(getHistory);
    }


}
