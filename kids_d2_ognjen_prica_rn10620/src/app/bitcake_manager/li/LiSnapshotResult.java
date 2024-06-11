package app.bitcake_manager.li;

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
public record LiSnapshotResult(int serventId, int parentId, int recordedAmount, Map<Integer, Integer> giveHistory,
                               Map<Integer, Integer> getHistory) implements Serializable {

    @Serial
    private static final long serialVersionUID = 8939516333227254439L;

    public LiSnapshotResult(int serventId, int parentId, int recordedAmount,
                            Map<Integer, Integer> giveHistory, Map<Integer, Integer> getHistory) {
        this.serventId = serventId;
        this.parentId = parentId;
        this.recordedAmount = recordedAmount;
        this.giveHistory = new ConcurrentHashMap<>(giveHistory);
        this.getHistory = new ConcurrentHashMap<>(getHistory);
    }

}