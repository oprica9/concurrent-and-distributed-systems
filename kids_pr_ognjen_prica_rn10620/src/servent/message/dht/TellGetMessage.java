package servent.message.dht;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class TellGetMessage extends BasicMessage {

	@Serial
	private static final long serialVersionUID = -6213394344524749872L;

	public TellGetMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, int key, int value) {
		super(MessageType.TELL_GET, senderIpAddress, senderPort, receiverIpAddress, receiverPort, key + ":" + value);
	}
}
