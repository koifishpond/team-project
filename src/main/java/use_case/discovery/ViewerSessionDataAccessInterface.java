package use_case.discovery;

public interface ViewerSessionDataAccessInterface {
    SessionReadResult resolve(java.util.UUID sessionToken);
}
