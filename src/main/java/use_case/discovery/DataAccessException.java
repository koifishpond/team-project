package use_case.discovery;

/** An expected storage failure; programming errors are deliberately not swallowed. */
public final class DataAccessException extends RuntimeException {
    public DataAccessException(String message) { super(message); }
}
