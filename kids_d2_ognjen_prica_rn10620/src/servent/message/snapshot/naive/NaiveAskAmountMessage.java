package servent.message.snapshot.naive;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class NaiveAskAmountMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = -2134483210691179901L;

    public NaiveAskAmountMessage(ServentInfo sender, ServentInfo receiver) {
        super(MessageType.NAIVE_ASK_AMOUNT, sender, receiver);
    }
}
