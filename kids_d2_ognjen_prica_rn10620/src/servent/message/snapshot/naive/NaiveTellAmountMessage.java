package servent.message.snapshot.naive;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class NaiveTellAmountMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = -296602475465394852L;

    public NaiveTellAmountMessage(ServentInfo sender, ServentInfo receiver, int amount) {
        super(MessageType.NAIVE_TELL_AMOUNT, sender, receiver, String.valueOf(amount));
    }
}
