package servent.message.util;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * This worker sends a message asynchronously. Doing this in a separate thread
 * has the added benefit of being able to delay without blocking main or some-such.
 *
 * @author bmilojkovic
 */
public class DelayedMessageSender implements Runnable {

    private final Message messageToSend;

    public DelayedMessageSender(Message messageToSend) {
        this.messageToSend = messageToSend;
    }

    public void run() {
        /*
         * A random sleep before sending.
         * It is important to take regular naps for health reasons.
         */
        try {
            Thread.sleep((long) (Math.random() * 1000) + 500);
        } catch (InterruptedException e) {
            AppConfig.timestampedErrorPrint(e);
        }

        if (MessageUtil.MESSAGE_UTIL_PRINTING) {
            if (messageToSend.getMessageType() != MessageType.PING && messageToSend.getMessageType() != MessageType.PONG) {
                if (messageToSend.getSenderIpAddress().equals(AppConfig.myServentInfo.getIpAddress())
                        && messageToSend.getSenderPort() == AppConfig.myServentInfo.getListenerPort()) {
                    AppConfig.timestampedStandardPrint("Sending message " + messageToSend);
                } else {
                    AppConfig.timestampedStandardPrint("Forwarding message " + messageToSend);
                }

            }
        }

        try {
            Socket sendSocket = new Socket(messageToSend.getReceiverIpAddress(), messageToSend.getReceiverPort());

            ObjectOutputStream oos = new ObjectOutputStream(sendSocket.getOutputStream());
            oos.writeObject(messageToSend);
            oos.flush();

            sendSocket.close();
        } catch (IOException e) {
            AppConfig.timestampedErrorPrint("Couldn't send message: " + messageToSend + " Reason: " + e);
        }
    }

}
