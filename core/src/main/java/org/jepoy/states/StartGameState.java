package org.jepoy.states;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.jepoy.GameContext;
import org.jepoy.text.CharSheet;

public class StartGameState extends State {
    // pulse
    private float startBlinkT = 0f;
    private final String startMsg = "start game";
    private final float startScale = 4f;     // size
    private final float startPeriod = 1.2f;  // seconds, slower = smoother

    // fade-out
    private boolean fading = false;
    private float fadeT = 0f;
    private final float fadeDuration = 0.6f; // seconds

    public StartGameState(GameContext ctx) { super(ctx); }

    @Override
    void update(float dt) {
        startBlinkT += dt;

        if (fading) {
            fadeT += dt;
            if (fadeT >= fadeDuration) {
                // fade finished -> switch state here
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
        final var sheet = ctx.getCharSheet();
        final float ww = ctx.getViewport().getWorldWidth();
        final float wh = ctx.getViewport().getWorldHeight();
        final float lh = sheet.lineHeight * startScale;

        final int wpx = textWidth(sheet, startMsg, startScale);
        final float x = (ww - wpx) * 0.5f;
        final float y = (wh - lh) * 0.5f;

        // ---- smooth pulse alpha (0.2 .. 1.0) ----
        float phase = (startBlinkT % startPeriod) / startPeriod;     // 0..1
        float raw   = 0.5f + 0.5f * (float)Math.sin(phase * (float)Math.PI * 2f); // 0..1
        float t     = raw * raw * (3f - 2f * raw);                    // smootherstep
        float alphaPulse = 0.2f + 0.8f * t;

        // ---- fade multiplier (1 -> 0) if fading ----
        float fadeMul = fading ? Math.max(0f, 1f - (fadeT / fadeDuration)) : 1f;

        // draw (isolated color so nothing else is affected)
        final float oldPacked = ctx.getBatch().getPackedColor();
        ctx.getBatch().setColor(1f, 1f, 1f, alphaPulse * fadeMul);
        CharSheet.drawText(ctx.getBatch(), sheet, startMsg, x, y, 4f);
        ctx.getBatch().setPackedColor(oldPacked);
    }

    @Override
    void shapeRender() {
        // optional: draw a vignette or dim the background here using ShapeRenderer
        // e.g., a translucent black full-screen rect while on the start screen
    }

    @Override
    void dispose() {
        // nothing to dispose here; shared resources live in GameContext
    }

    private int charW(CharSheet sheet, char ch, float scale) {
        if (ch == ' ') return Math.round(sheet.advance * scale);
        TextureRegion r = sheet.regionFor(ch);
        return (r != null) ? Math.round(r.getRegionWidth() * scale)
                : Math.round(sheet.advance * scale / 2f);
    }

    private int textWidth(CharSheet sheet, String s, float scale) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += charW(sheet, s.charAt(i), scale);
        return w;
    }
}
