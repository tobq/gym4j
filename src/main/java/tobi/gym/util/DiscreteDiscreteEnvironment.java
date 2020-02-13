package tobi.gym.util;

import tobi.gym.Discrete;
import tobi.gym.DiscreteSpace;
import tobi.gym.Environment;

public class DiscreteDiscreteEnvironment extends Environment<Discrete, Discrete> {
    public DiscreteDiscreteEnvironment(String envId) {
        super(envId);
    }

    @Override
    public DiscreteSpace getActionSpace() {
        return (DiscreteSpace) super.getActionSpace();
    }

    @Override
    public DiscreteSpace getObservationSpace() {
        return (DiscreteSpace) super.getObservationSpace();
    }
}
