package tobi.gym.util;

import tobi.gym.*;

public class BoxDiscreteEnvironment extends Environment<Box, Discrete> {
    public BoxDiscreteEnvironment(String envId, Gym gym) {
        super(envId, gym);
    }

    public BoxDiscreteEnvironment(String envId, boolean render, Gym gym) {
        super(envId, render, gym);
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
