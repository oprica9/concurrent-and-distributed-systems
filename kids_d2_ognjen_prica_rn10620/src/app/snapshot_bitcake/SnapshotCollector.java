package app.snapshot_bitcake;

import app.Cancellable;
import app.snapshot_bitcake.ly.LYSnapshotResult;

/**
 * Describes a snapshot collector. Made not-so-flexibly for readability.
 *
 * @author bmilojkovic
 */
public interface SnapshotCollector extends Runnable, Cancellable {

    BitcakeManager getBitcakeManager();

    void addLYSnapshotInfo(int id, LYSnapshotResult lySnapshotResult);

    void startCollecting();

}