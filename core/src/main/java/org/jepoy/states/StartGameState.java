package org.jepoy.states;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.math.Rectangle;
import org.jepoy.GameContext;
import org.jepoy.text.StaticSpriteText;

public class StartGameState extends State {
    // pulse
    private float startBlinkT = 0f;
    private final String startMsg = "start game";
    private final float startScale = 4f;     // size
    private final float startPeriod = 1.2f;  // seconds, slower = smoother

    // fade-out
    private boolean fading = false;
    private float fadeT = 0f;

    Rectangle box;
    StaticSpriteText staticSpriteText;

    private float pulseT = 0f;

    public StartGameState(GameContext ctx) {
        super(ctx);
        box = new Rectangle((ctx.getViewport().getWorldWidth() - 300) / 2f, (ctx.getViewport().getWorldHeight() - 100) / 2f, 500, 100);
        staticSpriteText = new StaticSpriteText(ctx.getCharSheet(), box, 1f, 4f);
        staticSpriteText.setText("START GAME");
    }

    @Override
    void update(float dt) {
        startBlinkT += dt;
        pulseT += dt;
        if (fading) {
            fadeT += dt;
            // seconds
            float fadeDuration = 0.6f;
            if (fadeT >= fadeDuration) {
                ctx.getGsm().set(new IntroState(ctx));
                return;
            }
        }
    }

    @Override
    void handleInput() {
        if (!fading && Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
            fading = true;
            fadeT = 0f;
            // optional: play a start sfx here
            Music sfx = Gdx.audio.newMusic(Gdx.files.internal("sounds/jepoy_game_sound_okay_01.ogg"));// Optional: loop it// Volume: 0.0 to 1.0
            sfx.play();
        }
    }

    @Override
    void render() {
        staticSpriteText.drawBlinking(ctx, pulseT, 1f);
    }

    @Override
    void shapeRender() {
        //ctx.getShapes().rect(box.x, box.y, box.width, box.height);
    }

    @Override
    void dispose() {
        // nothing to dispose here; shared resources live in GameContext
    }
}
