package tobi.gym.util;

import tobi.gym.*;

public class DiscreteBoxEnvironment extends Environment<Discrete, Box> {
    public DiscreteBoxEnvironment(String envId, Gym gym) {
        super(envId, gym);
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
