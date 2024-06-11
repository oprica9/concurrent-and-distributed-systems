package app.snapshot_collector;

import app.Cancellable;
import app.bitcake_manager.BitcakeManager;
import app.bitcake_manager.li.LiSnapshotResult;

import java.util.Map;

/**
 * Describes a snapshot collector. Made not-so-flexibly for readability.
 *
 * @author bmilojkovic
 */
public interface SnapshotCollector extends Runnable, Cancellable {

    BitcakeManager getBitcakeManager();

    void addLiSnapshotInfo(int id, LiSnapshotResult liSnapshotResult);

    void addLiSnapshotInfos(Map<Integer, LiSnapshotResult> liSnapshotResults);

    void setCompletedRegion(boolean completed);

    void addReceivedRegionResults(int regionMasterId, Map<Integer, LiSnapshotResult> regionResults);

    void addReceivedBlank(int id);

    Map<Integer, LiSnapshotResult> getLiSnapshotResults();

    void startCollecting();
}