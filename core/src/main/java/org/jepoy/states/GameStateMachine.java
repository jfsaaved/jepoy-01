package org.jepoy.states;

import java.util.Stack;

public class GameStateMachine {
    private final Stack<State> states;

    public GameStateMachine() {
        states = new Stack<>();
    }

    public void update(float dt){
        states.peek().handleInput();
        states.peek().update(dt);
    }

    public void render(){
        states.peek().render();
    }

    public void shapeRender(){
        states.peek().shapeRender();
    }

    public void push(State state) {
        states.push(state);
    }

    public void pop(){
        states.pop();
    }

    public void set(State state){
        states.pop();
        states.push(state);
    }

}
