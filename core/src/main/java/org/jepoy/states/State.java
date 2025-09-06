package org.jepoy.states;

import org.jepoy.GameContext;

public abstract class State {
    protected final GameContext ctx;
    abstract void update(float dt);
    abstract void render();
    abstract void shapeRender();
    abstract void handleInput();
    abstract void dispose();

    protected State(GameContext ctx) {;
        this.ctx = ctx;
    }
}
