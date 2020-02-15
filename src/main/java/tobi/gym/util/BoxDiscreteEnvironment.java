package tobi.gym.util;

import tobi.gym.*;

import java.io.IOException;

public class BoxDiscreteEnvironment extends Environment<Box, Discrete> {
    public BoxDiscreteEnvironment(String envId, Gym gym) throws IOException {
        super(envId, gym);
    }

    public BoxDiscreteEnvironment(String envId, boolean render, Gym gym) throws IOException {
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
