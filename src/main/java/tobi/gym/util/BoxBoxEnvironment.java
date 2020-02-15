package tobi.gym.util;

import tobi.gym.Box;
import tobi.gym.BoxSpace;
import tobi.gym.Environment;
import tobi.gym.Gym;

import java.io.IOException;

public class BoxBoxEnvironment extends Environment<Box, Box> {
    public BoxBoxEnvironment(String envId, Gym gym) throws IOException {
        super(envId, gym);
    }

    public BoxBoxEnvironment(String envId, boolean render, Gym gym) throws IOException {
        super(envId, render, gym);
    }

    @Override
    public BoxSpace getActionSpace() {
        return (BoxSpace) super.getActionSpace();
    }

    @Override
    public BoxSpace getObservationSpace() {
        return (BoxSpace) super.getObservationSpace();
    }
}
