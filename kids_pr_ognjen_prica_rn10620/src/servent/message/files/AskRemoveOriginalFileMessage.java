package servent.message.files;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;
import java.util.List;

public class AskRemoveOriginalFileMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = 662551018842811949L;
    private final String filePath;
    private final int ownerId;
    private final List<Integer> visited;

    public AskRemoveOriginalFileMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, String filePath, int ownerId, List<Integer> visited) {
        super(MessageType.ASK_REMOVE_ORIGINAL_FILE, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
        this.filePath = filePath;
        this.ownerId = ownerId;
        this.visited = visited;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public boolean hasVisited(int chordId) {
        return visited.contains(chordId);
    }

    public List<Integer> getVisited() {
        return visited;
    }
}
