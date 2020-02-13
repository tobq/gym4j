package tobi.gym;

import java.io.IOException;
import java.net.URISyntaxException;

public class GymEnvironment<O extends Action, A extends Action> extends Environment<O, A> {
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
