package tobi.gym.util;

import tobi.gym.SpaceInstance;
import tobi.gym.Environment;
import tobi.gym.Gym;

import java.io.IOException;
import java.net.URISyntaxException;

public class GymEnvironment<O extends SpaceInstance, A extends SpaceInstance> extends Environment<O, A> {
    public GymEnvironment(String envId) throws InterruptedException, IOException, URISyntaxException {
        super(envId, new Gym());
    }

    public GymEnvironment(String envId, boolean render) throws InterruptedException, IOException, URISyntaxException {
        super(envId, render, new Gym());
    }

    @Override
    public void close() throws InterruptedException {
        gym.close();
    }
}
