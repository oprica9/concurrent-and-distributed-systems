package servent;

import app.AppConfig;
import app.Cancellable;
import app.FileManager;
import app.failure_detection.FailureDetector;
import servent.handler.*;
import servent.handler.dht.AskGetHandler;
import servent.handler.dht.PutHandler;
import servent.handler.dht.TellGetHandler;
import servent.handler.files.*;
import servent.handler.friends.AddFriendRequestHandler;
import servent.handler.friends.AddFriendResponseHandler;
import servent.handler.mutex.TokenReplyHandler;
import servent.handler.mutex.TokenRequestHandler;
import servent.handler.mutex.UnlockHandler;
import servent.handler.ping_pong.PingHandler;
import servent.handler.ping_pong.PongHandler;
import servent.handler.ping_pong.RestructureSystemHandler;
import servent.message.Message;
import servent.message.util.MessageUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleServentListener implements Runnable, Cancellable {

    private volatile boolean working = true;

    // Thread pool for executing the handlers. Each client will get its own handler thread.
    private final ExecutorService threadPool = Executors.newWorkStealingPool();

    private final FailureDetector failureDetector;
    private final FileManager fileManager;

    public SimpleServentListener(FailureDetector failureDetector, FileManager fileManager) {
        this.failureDetector = failureDetector;
        this.fileManager = fileManager;
    }

    @Override
    public void run() {
        try (ServerSocket listenerSocket = new ServerSocket(AppConfig.myServentInfo.getListenerPort(), 100)) {
            // If there is no connection after 1s, wake up and see if we should terminate.
            listenerSocket.setSoTimeout(1000);

            while (working) {
                try {
                    Message clientMessage;
                    Socket clientSocket = listenerSocket.accept();
                    clientMessage = MessageUtil.readMessage(clientSocket);

                    MessageHandler messageHandler = new NullHandler(clientMessage);

                    // Each message type has its own handler.
                    // If we can get away with stateless handlers, we will,
                    // because that way is much simpler and less error-prone.

                    switch (clientMessage.getMessageType()) {
                        case NEW_NODE:
                            messageHandler = new NewNodeHandler(clientMessage, fileManager);
                            break;
                        case WELCOME:
                            messageHandler = new WelcomeHandler(clientMessage, fileManager);
                            break;
                        case SORRY:
                            messageHandler = new SorryHandler(clientMessage);
                            break;
                        case UPDATE:
                            messageHandler = new UpdateHandler(clientMessage, failureDetector, fileManager);
                            break;
                        case PUT:
                            messageHandler = new PutHandler(clientMessage);
                            break;
                        case ASK_GET:
                            messageHandler = new AskGetHandler(clientMessage);
                            break;
                        case TELL_GET:
                            messageHandler = new TellGetHandler(clientMessage);
                            break;
                        case POISON:
                            break;
                        case BACKUP_FILE:
                            messageHandler = new BackupFileHandler(clientMessage, fileManager);
                            break;
                        case ASK_VIEW_FILES:
                            messageHandler = new AskViewFilesHandler(clientMessage, fileManager);
                            break;
                        case TELL_VIEW_FILES:
                            messageHandler = new TellViewFilesHandler(clientMessage);
                            break;
                        case ASK_REMOVE_FILE:
                            messageHandler = new AskRemoveFileHandler(clientMessage, fileManager);
                            break;
                        case ASK_REMOVE_ORIGINAL_FILE:
                            messageHandler = new AskRemoveOriginalFileHandler(clientMessage, fileManager);
                            break;
                        case ADD_FRIEND_REQUEST:
                            messageHandler = new AddFriendRequestHandler(clientMessage);
                            break;
                        case ADD_FRIEND_RESPONSE:
                            messageHandler = new AddFriendResponseHandler(clientMessage);
                            break;
                        case PING:
                            messageHandler = new PingHandler(clientMessage, failureDetector);
                            break;
                        case PONG:
                            messageHandler = new PongHandler(clientMessage, failureDetector);
                            break;
                        case RESTRUCTURE:
                            messageHandler = new RestructureSystemHandler(clientMessage, failureDetector);
                            break;
                        case TOKEN_REQUEST:
                            messageHandler = new TokenRequestHandler(clientMessage);
                            break;
                        case TOKEN_REPLY:
                            messageHandler = new TokenReplyHandler(clientMessage);
                            break;
                        case UNLOCK:
                            messageHandler = new UnlockHandler(clientMessage);
                            break;
                    }

                    threadPool.submit(messageHandler);
                } catch (SocketTimeoutException timeoutEx) {
                    // Uncomment the next line to see that we are waking up every second.
                    // AppConfig.timedStandardPrint("Waiting...");
                } catch (IOException e) {
                    AppConfig.timestampedErrorPrint(e);
                }
            }

        } catch (IOException e) {
            AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.getListenerPort());
            System.exit(0);
        }
    }

    @Override
    public void stop() {
        this.working = false;
    }

}