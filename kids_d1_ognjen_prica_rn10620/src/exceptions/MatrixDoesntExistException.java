package exceptions;

public class MatrixDoesntExistException extends AbstractException {

    public MatrixDoesntExistException() {
    }

    public MatrixDoesntExistException(String message) {
        super(message);
    }
}
