# gym4j
Library for running Open AI gym in Java
---
```Java
Environment.init();
    try (BoxBoxEnvironment env = new BoxBoxEnvironment("BipedalWalker-v2")) {
        final BoxSpace actionSpace = env.getActionSpace();
        final BoxSpace observationSpace = env.getObservationSpace();
        final Box initialState = env.reset();
        for (int i = 0; i < 1000; i++) {
            env.step(new Box(1, 1, 1, 1));
        }
    }
```
