package entity;

public class InvalidUserIDException extends Exception{
    public InvalidUserIDException() {
        super("Invalid or Occupied UserID");
    }
}
