package servent.handler;

import app.AppConfig;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.ly.LaiYangBitcakeManager;
import servent.message.Message;
import servent.message.MessageType;

public class TransactionHandler implements MessageHandler {

    private final Message clientMessage;
    private final BitcakeManager bitcakeManager;

    public TransactionHandler(Message clientMessage, BitcakeManager bitcakeManager) {
        this.clientMessage = clientMessage;
        this.bitcakeManager = bitcakeManager;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.TRANSACTION) {
            AppConfig.timestampedErrorPrint("Transaction handler got: " + clientMessage);
            return;
        }

        String amountString = clientMessage.getMessageText();

        int amountNumber;
        try {
            amountNumber = Integer.parseInt(amountString);
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
            return;
        }

        bitcakeManager.addSomeBitcakes(amountNumber);

        synchronized (AppConfig.colorLock) {
            if (bitcakeManager instanceof LaiYangBitcakeManager lyBitcakeManager && clientMessage.isWhite()) {
                lyBitcakeManager.recordGetTransaction(clientMessage.getOriginalSenderInfo().id(), amountNumber);
            }
        }
    }

}