package interface_adapter.Profile;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ProfileViewModel {
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private State state = new State(false, "", null);

    public State getState() {
        return state;
    }

    public void setState(boolean success, String message, String username) {
        State previousState = state;
        state = new State(success, message, username);
        propertyChangeSupport.firePropertyChange("state", previousState, state);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public static final class State {
        private final boolean success;
        private final String message;
        private final String username;

        private State(boolean success, String message, String username) {
            this.success = success;
            this.message = message;
            this.username = username;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getUsername() {
            return username;
        }
    }
}