package app.bitcake_manager.chandy_lamport;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Snapshot result for servent with id serventId.
 * The amount of bitcakes on that servent is recordedAmount,
 * and messages from others to that servent are recorded in
 * allChannelMessages.
 *
 * @author bmilojkovic
 */
public record CLSnapshotResult(int serventId, int recordedAmount,
                               Map<String, List<Integer>> allChannelMessages) implements Serializable {

    @Serial
    private static final long serialVersionUID = -1443515806440079979L;

    public CLSnapshotResult(int serventId, int recordedAmount, Map<String, List<Integer>> allChannelMessages) {
        this.serventId = serventId;
        this.recordedAmount = recordedAmount;
        this.allChannelMessages = new ConcurrentHashMap<>(allChannelMessages);
    }

}
