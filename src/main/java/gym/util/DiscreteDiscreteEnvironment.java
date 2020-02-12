package gym.util;

import gym.Discrete;
import gym.DiscreteSpace;
import gym.Environment;

import java.io.IOException;

public class DiscreteDiscreteEnvironment extends Environment<Discrete, Discrete> {
    public DiscreteDiscreteEnvironment(String envId) throws IOException, InterruptedException {
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
