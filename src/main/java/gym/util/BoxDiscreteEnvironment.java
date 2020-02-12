package gym.util;

import gym.*;

import java.io.IOException;

public class BoxDiscreteEnvironment extends Environment<Box, Discrete> {
    public BoxDiscreteEnvironment(String envId) throws IOException, InterruptedException {
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
