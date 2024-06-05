package servent.message;

public enum MessageType {
    NEW_NODE, WELCOME, SORRY, UPDATE, PUT, ASK_GET, TELL_GET, POISON,
    BACKUP_FILE, ASK_VIEW_FILES, TELL_VIEW_FILES, ASK_REMOVE_FILE, ASK_REMOVE_ORIGINAL_FILE,
    ADD_FRIEND_REQUEST, ADD_FRIEND_RESPONSE,
    PING, PONG, RESTRUCTURE,
    TOKEN_REQUEST, TOKEN_REPLY, UNLOCK

}