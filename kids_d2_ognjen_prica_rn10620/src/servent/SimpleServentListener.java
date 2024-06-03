package servent;

import app.AppConfig;
import app.Cancellable;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotType;
import app.snapshot_bitcake.ly.LaiYangBitcakeManager;
import servent.handler.MessageHandler;
import servent.handler.NullHandler;
import servent.handler.TransactionHandler;
import servent.handler.snapshot.ly.LYMarkerHandler;
import servent.handler.snapshot.ly.LYTellHandler;
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

    private volatile boolean working = true;

    private final SnapshotCollector snapshotCollector;

    public SimpleServentListener(SnapshotCollector snapshotCollector) {
        this.snapshotCollector = snapshotCollector;
    }

    /*
     * Thread pool for executing the handlers. Each client will get its own handler thread.
     */
    private final ExecutorService threadPool = Executors.newWorkStealingPool();

    private final List<Message> redMessages = new ArrayList<>();

    @Override
    public void run() {
        try (ServerSocket listenerSocket = new ServerSocket(AppConfig.myServentInfo.listenerPort(), 100)) {
            listenerSocket.setSoTimeout(1000);

            while (working) {
                try {
                    Message clientMessage;

                    /*
                     * Lai-Yang stuff. Process any red messages we got before we got the marker.
                     * The marker contains the collector id, so we need to process that as our first
                     * red message.
                     */
                    if (!AppConfig.isWhite.get() && !redMessages.isEmpty()) {
                        clientMessage = redMessages.remove(0);
                    } else {
                        /*
                         * This blocks for up to 1s, after which SocketTimeoutException is thrown.
                         */
                        Socket clientSocket = listenerSocket.accept();

                        //GOT A MESSAGE! <3
                        clientMessage = MessageUtil.readMessage(clientSocket);
                    }
                    synchronized (AppConfig.colorLock) {
                        if (AppConfig.SNAPSHOT_TYPE == SnapshotType.LAI_YANG) {
                            if (!clientMessage.isWhite() && AppConfig.isWhite.get()) {
                                /*
                                 * If the message is red, we are white, and the message isn't a marker,
                                 * then store it. We will get the marker soon, and then we will process
                                 * this message. The point is, we need the marker to know who to send
                                 * our info to, so this is the simplest way to work around that.
                                 */
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
                        }
                    }

                    MessageHandler messageHandler = new NullHandler(clientMessage);

                    /*
                     * Each message type has its own handler.
                     * If we can get away with stateless handlers, we will,
                     * because that way is much simpler and less error-prone.
                     */
                    switch (clientMessage.getMessageType()) {
                        case TRANSACTION:
                            messageHandler = new TransactionHandler(clientMessage, snapshotCollector.getBitcakeManager());
                            break;
                        case LY_MARKER:
                            messageHandler = new LYMarkerHandler();
                            break;
                        case LY_TELL:
                            messageHandler = new LYTellHandler(clientMessage, snapshotCollector);
                        case POISON:
                            break;
                    }

                    threadPool.submit(messageHandler);
                } catch (SocketTimeoutException timeoutEx) {
                    //Uncomment the next line to see that we are waking up every second.
//				AppConfig.timedStandardPrint("Waiting...");
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

}
