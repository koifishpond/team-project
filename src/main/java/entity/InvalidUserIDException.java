package entity;

public class InvalidUserIDException extends RuntimeException {
    public InvalidUserIDException() {
        super("Invalid or Occupied UserID");
    }
    
}
