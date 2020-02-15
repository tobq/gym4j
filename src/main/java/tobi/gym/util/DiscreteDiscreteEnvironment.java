package tobi.gym.util;

import tobi.gym.Discrete;
import tobi.gym.DiscreteSpace;
import tobi.gym.Environment;
import tobi.gym.Gym;

import java.io.IOException;

public class DiscreteDiscreteEnvironment extends Environment<Discrete, Discrete> {
    public DiscreteDiscreteEnvironment(String envId, Gym gym) throws IOException {
        super(envId, gym);
    }

    public DiscreteDiscreteEnvironment(String envId, boolean render, Gym gym) throws IOException {
        super(envId, render, gym);
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
