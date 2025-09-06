package org.jepoy;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import org.jepoy.states.GameStateMachine;
import org.jepoy.text.CharSheet;

import java.util.HashMap;

public class GameContext {
    private final GameStateMachine gsm;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final CharSheet charSheet;
    private Music mainMusic;

    public GameContext(GameStateMachine gsm,
                       OrthographicCamera camera,
                       FitViewport viewport,
                       SpriteBatch batch,
                       ShapeRenderer shapes,
                       CharSheet charSheet,
                       Music mainMusic) {
        this.gsm = gsm;
        this.camera = camera;
        this.viewport = viewport;
        this.batch = batch;
        this.shapes = shapes;
        this.charSheet = charSheet;
        this.mainMusic = mainMusic;
    }

    public GameStateMachine getGsm() {
        return gsm;
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public FitViewport getViewport() {
        return viewport;
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public ShapeRenderer getShapes() {
        return shapes;
    }

    public CharSheet getCharSheet() {
        return charSheet;
    }

    public Music getMainMusic() {
        return mainMusic;
    }

    public void setMainMusic(Music newMusic) {
        this.mainMusic = newMusic;
    }

}
