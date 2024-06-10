package app.snapshot_collector;

import app.Cancellable;
import app.bitcake_manager.BitcakeManager;
import app.bitcake_manager.chandy_lamport.CLSnapshotResult;
import app.bitcake_manager.lai_yang.LYSnapshotResult;

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

    void startCollecting();

}