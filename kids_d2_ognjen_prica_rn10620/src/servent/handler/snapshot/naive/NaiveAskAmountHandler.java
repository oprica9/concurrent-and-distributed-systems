package servent.handler.snapshot.naive;

import app.bitcake_manager.BitcakeManager;
import app.configuration.AppConfig;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.naive.NaiveTellAmountMessage;
import servent.message.util.MessageUtil;

public class NaiveAskAmountHandler implements MessageHandler {

    private final Message clientMessage;
    private final BitcakeManager bitcakeManager;

    public NaiveAskAmountHandler(Message clientMessage, BitcakeManager bitcakeManager) {
        this.clientMessage = clientMessage;
        this.bitcakeManager = bitcakeManager;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.NAIVE_ASK_AMOUNT) {
            AppConfig.timestampedErrorPrint("Ask amount handler got: " + clientMessage);
        }

        int currentAmount = bitcakeManager.getCurrentBitcakeAmount();

        Message tellMessage = new NaiveTellAmountMessage(
                clientMessage.getReceiverInfo(), clientMessage.getOriginalSenderInfo(), currentAmount);

        MessageUtil.sendMessage(tellMessage);
    }

}
