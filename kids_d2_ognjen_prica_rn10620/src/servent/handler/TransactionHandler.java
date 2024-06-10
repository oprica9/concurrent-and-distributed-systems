package servent.handler;

import app.bitcake_manager.BitcakeManager;
import app.bitcake_manager.lai_yang.LaiYangBitcakeManager;
import app.bitcake_manager.li.LiBitcakeManager;
import app.configuration.AppConfig;
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
        if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
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
                } else if (bitcakeManager instanceof LiBitcakeManager liBitcakeManager && !clientMessage.isTagged()) {
                    liBitcakeManager.recordGetTransaction(clientMessage.getOriginalSenderInfo().id(), amountNumber);
                }
            }
        } else {
            AppConfig.timestampedErrorPrint("Transaction handler got: " + clientMessage);
        }
    }

}
