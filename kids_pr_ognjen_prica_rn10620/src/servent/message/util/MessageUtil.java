package servent.message.util;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

/**
 * For now, just the read and send implementation, based on Java serializing.
 * Not too smart. Doesn't even check the neighbor list, so it actually allows cheating.
 * <p>
 * Depending on the configuration it delegates sending either to a {@link DelayedMessageSender}
 * in a new thread (non-FIFO).
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
    public static final boolean PRINT_FAILURE = true;

    public static Message readMessage(Socket socket) {

        Message clientMessage = null;

        try {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            clientMessage = (Message) ois.readObject();

            socket.close();
        } catch (IOException e) {
            AppConfig.timestampedErrorPrint("Error in reading socket on " +
                    socket.getInetAddress() + ":" + socket.getPort());
        } catch (ClassNotFoundException e) {
            AppConfig.timestampedErrorPrint(e);
        }

        if (MESSAGE_UTIL_PRINTING) {
            if (clientMessage != null && (clientMessage.getMessageType() != MessageType.PING && clientMessage.getMessageType() != MessageType.PONG)) {
                AppConfig.timestampedStandardPrint("Got message " + clientMessage);
            }
        }

        return clientMessage;
    }

    public static void sendMessage(Message message) {
        Thread delayedSender = new Thread(new DelayedMessageSender(message));
        delayedSender.start();
    }
}
