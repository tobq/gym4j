package tobi.gym.util;

import tobi.gym.*;

import java.io.IOException;

public class DiscreteBoxEnvironment extends Environment<Discrete, Box> {
    public DiscreteBoxEnvironment(String envId, Gym gym) throws IOException {
        super(envId, gym);
    }

    public DiscreteBoxEnvironment(String envId, boolean render, Gym gym) throws IOException {
        super(envId, render, gym);
    }

    @Override
    public BoxSpace getActionSpace() {
        return (BoxSpace) super.getActionSpace();
    }

    @Override
    public DiscreteSpace getObservationSpace() {
        return (DiscreteSpace) super.getObservationSpace();
    }
}
