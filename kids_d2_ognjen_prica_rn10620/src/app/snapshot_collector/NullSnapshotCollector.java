package app.snapshot_collector;

import app.bitcake_manager.BitcakeManager;
import app.bitcake_manager.li.LiSnapshotResult;

import java.util.Map;

/**
 * This class is used if the user hasn't specified a snapshot type in config.
 *
 * @author bmilojkovic
 */
public class NullSnapshotCollector implements SnapshotCollector {

    @Override
    public void run() {
    }

    @Override
    public void stop() {
    }

    @Override
    public BitcakeManager getBitcakeManager() {
        return null;
    }

    @Override
    public void addLiSnapshotInfo(int id, LiSnapshotResult liSnapshotResult) {

    }

    @Override
    public void addLiSnapshotInfos(Map<Integer, LiSnapshotResult> liSnapshotResults) {

    }

    @Override
    public void setCompletedRegion(boolean completed) {

    }

    @Override
    public void addReceivedRegionResults(int regionMasterId, Map<Integer, LiSnapshotResult> regionResults) {

    }


    @Override
    public void addReceivedBlank(int id) {

    }

    @Override
    public Map<Integer, LiSnapshotResult> getLiSnapshotResults() {
        return null;
    }

    @Override
    public void startCollecting() {
    }

}
