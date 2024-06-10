package servent;

import app.Cancellable;
import app.bitcake_manager.chandy_lamport.ChandyLamportBitcakeManager;
import app.bitcake_manager.lai_yang.LaiYangBitcakeManager;
import app.bitcake_manager.li.LiBitcakeManager;
import app.configuration.AppConfig;
import app.configuration.SnapshotType;
import app.snapshot_collector.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.handler.NullHandler;
import servent.handler.TransactionHandler;
import servent.handler.ping_pong.PingHandler;
import servent.handler.ping_pong.PongHandler;
import servent.handler.snapshot.chandy_lamport.CLMarkerHandler;
import servent.handler.snapshot.chandy_lamport.CLTellHandler;
import servent.handler.snapshot.lai_yang.LYMarkerHandler;
import servent.handler.snapshot.lai_yang.LYTellHandler;
import servent.handler.snapshot.li.LiMarkerHandler;
import servent.handler.snapshot.li.LiTellHandler;
import servent.handler.snapshot.li.TagMiddlewareHandler;
import servent.handler.snapshot.naive.NaiveAskAmountHandler;
import servent.handler.snapshot.naive.NaiveTellAmountHandler;
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
    private final List<Message> redMessages = new ArrayList<>();
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

                    // Lai-Yang stuff. Process any red messages we got before we got the marker.
                    // The marker contains the collector id, so we need to process that as our
                    // first red message.
//                    if (!AppConfig.isWhite.get() && !redMessages.isEmpty()) {
//                        clientMessage = redMessages.remove(0);
//                    } else {
//                        // This blocks for up to 1s, after which SocketTimeoutException is thrown.
//                        Socket clientSocket = listenerSocket.accept();
//                        clientMessage = MessageUtil.readMessage(clientSocket);
//                    }

                    // Li et al. stuff. Process any tagged messages we got before we got the marker.
                    // The marker contains the tag, so we need to process that as our
                    // first tagged message.
                    if (AppConfig.snapshotInProgress.get() && !buffer.isEmpty()) {
                        clientMessage = buffer.remove(0);
                    } else {
                        Socket clientSocket = listenerSocket.accept();
                        clientMessage = MessageUtil.readMessage(clientSocket);
                    }

                    if (clientMessage.getMessageType() == MessageType.LI_MARKER) {
                        System.out.println("IS LI_MARKER AND TAG IS: " + clientMessage.getTag());
                    }

                    synchronized (AppConfig.colorLock) {
                        if (AppConfig.SNAPSHOT_TYPE == SnapshotType.CHANDY_LAMPORT) {
                            if (!AppConfig.isWhite.get() &&
                                    clientMessage.getMessageType() != MessageType.CL_MARKER) {
                                ChandyLamportBitcakeManager clBitcakeManager =
                                        (ChandyLamportBitcakeManager) snapshotCollector.getBitcakeManager();
                                clBitcakeManager.addChannelMessage(clientMessage);
                            }
                        } else if (AppConfig.SNAPSHOT_TYPE == SnapshotType.LAI_YANG) {
                            if (!clientMessage.isWhite() && AppConfig.isWhite.get()) {
                                // If the message is red, we are white, and the message isn't a marker,
                                // then store it. We will get the marker soon, and then we will process
                                // this message. The point is, we need the marker to know who to send
                                // our info to, so this is the simplest way to work around that.
                                if (clientMessage.getMessageType() != MessageType.LY_MARKER) {
                                    redMessages.add(clientMessage);
                                    continue;
                                } else {
                                    LaiYangBitcakeManager lyBitcakeManager =
                                            (LaiYangBitcakeManager) snapshotCollector.getBitcakeManager();
                                    lyBitcakeManager.markerEvent(
                                            Integer.parseInt(clientMessage.getMessageText()), snapshotCollector);
                                }
                            }
                        } else if (AppConfig.SNAPSHOT_TYPE == SnapshotType.LI) {
                            System.out.println("GOT 1");
                            System.out.println("Snapshot in progress: " + AppConfig.snapshotInProgress.get() + ", is message tagged: " + clientMessage.isTagged());
                            if (clientMessage.isTagged() && !AppConfig.snapshotInProgress.get()) {
                                System.out.println("GOT 2");
                                // If the message is tagged, we haven't yet taken a snapshot, and the
                                // message isn't a marker, then store it. We will get the marker soon,
                                // and then we will process this message. The point is, we need the
                                // marker to know who to send our info to, so this is the simplest
                                // way to work around that.
                                if (clientMessage.getMessageType() != MessageType.LI_MARKER) {
                                    System.out.println("GOT 3");
                                    buffer.add(clientMessage);
                                    continue;
                                } else {
                                    System.out.println("GOT 4");
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
        MessageHandler messageHandler = new NullHandler(clientMessage);

        // Each message type has its own handler.
        // If we can get away with stateless handlers, we will,
        // because that way is much simpler and less error-prone.

        switch (clientMessage.getMessageType()) {
            case PING:
                messageHandler = new PingHandler(clientMessage);
                break;
            case PONG:
                messageHandler = new PongHandler(clientMessage);
                break;
            case TRANSACTION:
                messageHandler = new TransactionHandler(clientMessage, snapshotCollector.getBitcakeManager());
                break;
            case NAIVE_ASK_AMOUNT:
                messageHandler = new NaiveAskAmountHandler(clientMessage, snapshotCollector.getBitcakeManager());
                break;
            case NAIVE_TELL_AMOUNT:
                messageHandler = new NaiveTellAmountHandler(clientMessage, snapshotCollector);
                break;
            case CL_MARKER:
                messageHandler = new CLMarkerHandler(clientMessage, snapshotCollector);
                break;
            case CL_TELL:
                messageHandler = new CLTellHandler(clientMessage, snapshotCollector);
                break;
            case LY_MARKER:
                messageHandler = new LYMarkerHandler();
                break;
            case LY_TELL:
                messageHandler = new LYTellHandler(clientMessage, snapshotCollector);
                break;
            case LI_MARKER:
                messageHandler = new LiMarkerHandler();
                break;
            case LI_TELL:
                messageHandler = new LiTellHandler(clientMessage, snapshotCollector);
                break;
            case POISON:
                break;
        }

        return new TagMiddlewareHandler(clientMessage, snapshotCollector, messageHandler);
    }

}
