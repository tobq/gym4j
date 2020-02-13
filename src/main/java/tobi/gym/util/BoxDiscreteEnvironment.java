package tobi.gym.util;

import tobi.gym.*;

public class BoxDiscreteEnvironment extends Environment<Box, Discrete> {
    public BoxDiscreteEnvironment(String envId) {
        super(envId);
    }

    @Override
    public DiscreteSpace getActionSpace() {
        return (DiscreteSpace) super.getActionSpace();
    }

    @Override
    public BoxSpace getObservationSpace() {
        return (BoxSpace) super.getObservationSpace();
    }
}