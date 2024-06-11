package servent;

import app.Cancellable;
import app.bitcake_manager.li.LiBitcakeManager;
import app.configuration.AppConfig;
import app.configuration.SnapshotType;
import app.snapshot_collector.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.handler.NullHandler;
import servent.handler.TransactionHandler;
import servent.handler.snapshot.li.*;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleServentListener implements Runnable, Cancellable {

    // Thread pool for executing the handlers. Each client will get its own handler thread.
    private final ExecutorService threadPool = Executors.newWorkStealingPool();
    private volatile boolean working = true;
    private final SnapshotCollector snapshotCollector;
    private final List<Message> buffer = new ArrayList<>();

    public SimpleServentListener(SnapshotCollector snapshotCollector) {
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        try (ServerSocket listenerSocket = new ServerSocket(AppConfig.myServentInfo.listenerPort(), 100)) {
            // If there is no connection after 1s, wake up and see if we should terminate.
            listenerSocket.setSoTimeout(1000);

            while (working) {
                try {
                    Message clientMessage;
                    // Li et al. stuff. Process any tagged messages we got before we got the marker.
                    // The marker contains the tag, so we need to process that as our
                    // first tagged message.
                    if (AppConfig.snapshotInProgress.get() && !buffer.isEmpty()) {
                        clientMessage = buffer.remove(0);
                    } else {
                        Socket clientSocket = listenerSocket.accept();
                        clientMessage = MessageUtil.readMessage(clientSocket);
                    }

                    synchronized (AppConfig.colorLock) {
                        if (AppConfig.SNAPSHOT_TYPE == SnapshotType.LI) {
                            if (clientMessage.isTagged() && !AppConfig.snapshotInProgress.get()) {
                                // If the message is tagged, we haven't yet taken a snapshot, and the
                                // message isn't a marker, then store it. We will get the marker soon,
                                // and then we will process this message. The point is, we need the
                                // marker to know who to send our info to, so this is the simplest
                                // way to work around that.
                                if (clientMessage.getMessageType() != MessageType.LI_MARKER) {
                                    buffer.add(clientMessage);
                                    continue;
                                } else {
                                    LiBitcakeManager liBitcakeManager = (LiBitcakeManager) snapshotCollector.getBitcakeManager();
                                    liBitcakeManager.markerEvent(clientMessage.getTag(), clientMessage.getOriginalSenderInfo().id(), snapshotCollector);
                                }
                            }
                        }
                    }

                    MessageHandler messageHandler = getMessageHandler(clientMessage);

                    threadPool.submit(messageHandler);
                } catch (SocketTimeoutException timeoutEx) {
                    // Uncomment the next line to see that we are waking up every second.
                    // AppConfig.timedStandardPrint("Waiting...");
                } catch (IOException e) {
                    AppConfig.timestampedErrorPrint(e);
                }
            }
        } catch (IOException e) {
            AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.listenerPort());
            System.exit(0);
        }
    }

    @Override
    public void stop() {
        this.working = false;
    }

    private MessageHandler getMessageHandler(Message clientMessage) {
        return switch (clientMessage.getMessageType()) {
            case TRANSACTION -> new TagMiddlewareHandler(clientMessage, snapshotCollector, new TransactionHandler(clientMessage, snapshotCollector.getBitcakeManager()));
            case LI_MARKER -> new TagMiddlewareHandler(clientMessage, snapshotCollector, new LiMarkerHandler());
            case LI_TELL -> new TagMiddlewareHandler(clientMessage, snapshotCollector, new LiTellHandler(clientMessage, snapshotCollector));
            case EXCHANGE -> new TagMiddlewareHandler(clientMessage, snapshotCollector, new ExchangeHandler(clientMessage, snapshotCollector));
            case BLANK -> new TagMiddlewareHandler(clientMessage, snapshotCollector, new BlankHandler(clientMessage, snapshotCollector));
        };
    }

}
