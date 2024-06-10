package servent.message;

public enum MessageType {
    PING, PONG, POISON, TRANSACTION,
    NAIVE_ASK_AMOUNT, NAIVE_TELL_AMOUNT,
    CL_MARKER, CL_TELL,
    LY_MARKER, LY_TELL,
    LI_MARKER, LI_TELL
}
