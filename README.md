# gym4j
Library for running Open AI gym in Java
---
## Usage

I've setup a variety of ways to create any environment for full flexibility 
```Java
try (Gym gym = new Gym()) {
    try (BoxBoxEnvironment env = new BoxBoxEnvironment("BipedalWalker-v2", gym)) {
//            try (GymEnvironment<Box, Box> env = new GymEnvironment<Box,Box>("BipedalWalker-v2")) {
//            try (BoxBoxEnvironment env = Gym.makeBoxBox("BipedalWalker-v2", gym)) {
        final BoxSpace actionSpace = (BoxSpace) env.getActionSpace();
        final BoxSpace observationSpace = (BoxSpace) env.getObservationSpace();
        final Box initialState = env.reset();
        for (int i = 0; i < 1; i++) {
            final ActionResult<Box> result = env.step(new Box(1, 1, 1, 1));
            final Box observation = result.getObservation();
            final double reward = result.getReward();
            final boolean done = result.isDone();
        }
    }
}
```

Note: The `GymEnvironment` class is for utility, and is tightly coupled with its own `Gym` instance (a python subprocess).
