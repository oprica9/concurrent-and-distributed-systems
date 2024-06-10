package servent.message.util;

import app.configuration.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * For now, just the read and send implementation, based on Java serializing.
 * Not too smart. Doesn't even check the neighbor list, so it actually allows cheating.
 * <p>
 * Depending on the configuration it delegates sending either to a {@link DelayedMessageSender}
 * in a new thread (non-FIFO) or stores the message in a queue for the {@link FifoSendWorker} (FIFO).
 * <p>
 * When reading, if we are FIFO, we send an ACK message on the same socket, so the other side
 * knows they can send the next message.
 *
 * @author bmilojkovic
 */
public class MessageUtil {

    /**
     * Normally this should be true, because it helps with debugging.
     * Flip this to false in order to disable printing every message send / receive.
     */
    public static final boolean MESSAGE_UTIL_PRINTING = true;

    public static Map<Integer, BlockingQueue<Message>> pendingMessages = new ConcurrentHashMap<>();
    public static Map<Integer, BlockingQueue<Message>> pendingMarkers = new ConcurrentHashMap<>();

    public static void initializePendingMessages() {
        for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
            pendingMarkers.put(neighbor, new LinkedBlockingQueue<>());
            pendingMessages.put(neighbor, new LinkedBlockingQueue<>());
        }
    }

    public static Message readMessage(Socket socket) {

        Message clientMessage = null;

        try {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            clientMessage = (Message) ois.readObject();

            if (AppConfig.IS_FIFO) {
                String response = "ACK";
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(response);
                oos.flush();
            }

            socket.close();
        } catch (IOException e) {
            AppConfig.timestampedErrorPrint("Error in reading socket on " +
                    socket.getInetAddress() + ":" + socket.getPort());
        } catch (ClassNotFoundException e) {
            AppConfig.timestampedErrorPrint(e);
        }

        if (MESSAGE_UTIL_PRINTING) {
            AppConfig.timestampedStandardPrint("Got message " + clientMessage);
        }

        return clientMessage;
    }

    public static void sendMessage(Message message) {

        if (AppConfig.IS_FIFO) {
            try {
                if (message.getMessageType() == MessageType.CL_MARKER) {
                    pendingMarkers.get(message.getReceiverInfo().id()).put(message);
                } else {
                    pendingMessages.get(message.getReceiverInfo().id()).put(message);
                }
            } catch (InterruptedException e) {
                AppConfig.timestampedErrorPrint(e);
            }
        } else {
            Thread delayedSender = new Thread(new DelayedMessageSender(message));

            delayedSender.start();
        }
    }
}
