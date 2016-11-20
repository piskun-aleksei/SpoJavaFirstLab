/**
 * Created by bobrovnik on 11.11.16.
 */
public class SocketCustomException extends Exception {
    public SocketCustomException() {
        super();
    }

    public SocketCustomException(String message) {
        super(message);
    }

    public SocketCustomException(String message, Throwable cause) {
        super(message, cause);
    }

    public SocketCustomException(Throwable cause) {
        super(cause);
    }

    protected SocketCustomException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
