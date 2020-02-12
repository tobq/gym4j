package gym.util;

import gym.Box;
import gym.BoxSpace;
import gym.Environment;

import java.io.IOException;

public class BoxBoxEnvironment extends Environment<Box, Box> {
    public BoxBoxEnvironment(String envId) throws IOException, InterruptedException {
        super(envId);
    }

    public BoxBoxEnvironment(String envId, boolean render) throws IOException, InterruptedException {
        super(envId, render);
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
