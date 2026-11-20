package entity.matchmaking;

import java.util.*;
public final class ProfileValidationException extends IllegalArgumentException {
    private final Map<String, FieldError> errors;
    public ProfileValidationException(Map<String, FieldError> errors) {
        super("Invalid profile fields: " + errors);
        this.errors = Map.copyOf(errors);
    }
    public Map<String, FieldError> getErrors() { return errors; }
}
