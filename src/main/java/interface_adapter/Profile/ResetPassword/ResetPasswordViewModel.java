package interface_adapter.Profile.ResetPassword;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ResetPasswordViewModel {
    public enum Step {
        VERIFY_ACCOUNT,
        NEW_PASSWORD
    }

    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private State state = new State(Step.VERIFY_ACCOUNT, "", false);

    public State getState() {
        return state;
    }

    public void showNewPasswordStep() {
        setState(Step.NEW_PASSWORD, "", false);
    }

    public void showMessage(String message) {
        setState(state.getStep(), message, false);
    }

    public void complete(String message) {
        setState(Step.VERIFY_ACCOUNT, message, true);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    private void setState(Step step, String message, boolean completed) {
        State previousState = state;
        state = new State(step, message, completed);
        propertyChangeSupport.firePropertyChange("state", previousState, state);
    }

    public static final class State {
        private final Step step;
        private final String message;
        private final boolean completed;

        private State(Step step, String message, boolean completed) {
            this.step = step;
            this.message = message;
            this.completed = completed;
        }

        public Step getStep() {
            return step;
        }

        public String getMessage() {
            return message;
        }

        public boolean isCompleted() {
            return completed;
        }
    }
}