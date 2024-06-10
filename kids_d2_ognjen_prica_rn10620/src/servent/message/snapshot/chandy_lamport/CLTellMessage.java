package servent.message.snapshot.chandy_lamport;

import app.ServentInfo;
import app.bitcake_manager.chandy_lamport.CLSnapshotResult;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class CLTellMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 8224274653159843559L;

    private final CLSnapshotResult clSnapshotResult;

    public CLTellMessage(ServentInfo sender, ServentInfo receiver, CLSnapshotResult clSnapshotResult) {
        super(MessageType.CL_TELL, sender, receiver);
        this.clSnapshotResult = clSnapshotResult;
    }

    public CLSnapshotResult getCLSnapshotResult() {
        return clSnapshotResult;
    }

}
