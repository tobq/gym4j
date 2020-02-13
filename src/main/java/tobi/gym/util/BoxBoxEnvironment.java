package tobi.gym.util;

import tobi.gym.Box;
import tobi.gym.BoxSpace;
import tobi.gym.Environment;

public class BoxBoxEnvironment extends Environment<Box, Box> {
    public BoxBoxEnvironment(String envId) {
        super(envId);
    }

    public BoxBoxEnvironment(String envId, boolean render) {
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
