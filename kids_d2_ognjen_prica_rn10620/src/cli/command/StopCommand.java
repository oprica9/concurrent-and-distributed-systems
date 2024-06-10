package cli.command;

import app.configuration.AppConfig;
import app.snapshot_collector.SnapshotCollector;
import cli.CLIParser;
import servent.SimpleServentListener;
import servent.message.util.FifoSendWorker;

import java.util.List;

public class StopCommand implements CLICommand {

    private final CLIParser parser;
    private final SimpleServentListener listener;
    private final List<FifoSendWorker> senderWorkers;
    private final SnapshotCollector snapshotCollector;

    public StopCommand(CLIParser parser, SimpleServentListener listener,
                       List<FifoSendWorker> senderWorkers, SnapshotCollector snapshotCollector) {
        this.parser = parser;
        this.listener = listener;
        this.senderWorkers = senderWorkers;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public String commandName() {
        return "stop";
    }

    @Override
    public void execute(String args) {
        AppConfig.timestampedStandardPrint("Stopping...");
        parser.stop();
        listener.stop();
        for (FifoSendWorker senderWorker : senderWorkers) {
            senderWorker.stop();
        }
        snapshotCollector.stop();
    }

}
