package app.snapshot_collector;

import app.Cancellable;
import app.bitcake_manager.BitcakeManager;
import app.bitcake_manager.chandy_lamport.CLSnapshotResult;
import app.bitcake_manager.lai_yang.LYSnapshotResult;
import app.bitcake_manager.li.LiSnapshotResult;

import java.util.Map;

/**
 * Describes a snapshot collector. Made not-so-flexibly for readability.
 *
 * @author bmilojkovic
 */
public interface SnapshotCollector extends Runnable, Cancellable {

    BitcakeManager getBitcakeManager();

    void addNaiveSnapshotInfo(String snapshotSubject, int amount);

    void addCLSnapshotInfo(int id, CLSnapshotResult clSnapshotResult);

    void addLYSnapshotInfo(int id, LYSnapshotResult lySnapshotResult);

    void addLiSnapshotInfo(int id, LiSnapshotResult liSnapshotResult);
    void addLiSnapshotInfos(Map<Integer, LiSnapshotResult> liSnapshotResults);

    Map<Integer, LiSnapshotResult> getLiSnapshotResults();

    void startCollecting();
}