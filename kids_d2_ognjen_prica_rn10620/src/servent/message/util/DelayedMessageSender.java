package servent.message.util;

import app.ServentInfo;
import app.configuration.AppConfig;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.li.ExchangeMessage;
import servent.message.snapshot.li.Tag;

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

    private Message messageToSend;

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

        ServentInfo receiverInfo = messageToSend.getReceiverInfo();

        if (MessageUtil.MESSAGE_UTIL_PRINTING) {
            AppConfig.timestampedStandardPrint("Sending message " + messageToSend);
        }

        try {
            // Similar sync block to the one in FifoSenderWorker, except this one is
            // related to Lai-Yang. We want to be sure that message color is red if we
            // are red. Just setting the attribute when we were making the message may
            // have been too early.
            // All messages that declare their own stuff (e.g. LYTellMessage) will have
            // to override setRedColor() because of this.
            synchronized (AppConfig.colorLock) {
                if (!AppConfig.isWhite.get()) {
                    messageToSend = messageToSend.setRedColor();
                }

                if (AppConfig.snapshotInProgress.get()) {
                    int currentInitId = AppConfig.currentInitId.get();
                    int currentMKNO = AppConfig.initIdMKNOs.get(currentInitId);

                    Tag tag = new Tag(currentInitId, currentMKNO);

                    messageToSend = messageToSend.setTag(tag);
                }

                Socket sendSocket = new Socket(receiverInfo.ipAddress(), receiverInfo.listenerPort());

                ObjectOutputStream oos = new ObjectOutputStream(sendSocket.getOutputStream());
                oos.writeObject(messageToSend);
                oos.flush();

                sendSocket.close();

                if (messageToSend.getMessageType() == MessageType.EXCHANGE) {
                    ExchangeMessage message = (ExchangeMessage) messageToSend;
                    System.out.println("Sending: " + message.getCollectedRegionalValues());
                }

                messageToSend.sendEffect();
            }
        } catch (IOException e) {
            AppConfig.timestampedErrorPrint("Couldn't send message: " + messageToSend.toString());
        }
    }

}
