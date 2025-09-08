package org.jepoy.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import org.jepoy.GameContext;
import org.jepoy.text.*;
import org.jepoy.util.ChromaKey;

import java.util.Arrays;
import java.util.HashMap;

public class IntroState extends State {
    TypeWriterText typewriterText;
    float x = 100, y = 100;
    Rectangle box;
    TypeSounds sfx;

    // fields
    private float qPulseT = 0f;
    private float qPulsePeriod = 1.2f; // slower, smoother
    private float qMinAlpha = 0.25f;   // never fully disappear      // visible portion of the cycle

    private HashMap<Integer, String> textMap = new HashMap<>();
    private Integer currentTextMapIndex = 0;

    // timing
    private float animT = 0f, fadeInT = 0f;
    private final float fadeInDuration = 5f;

    // fade-out to next state
    private boolean firstTransitionActive = false;
    private float firstTransitionT = 0f;
    private final float firstTransitionDuration = 1f;

    // sprite constants
    private static final int KNIGHT_W = 77, KNIGHT_H = 86;
    private static final int HEAD_W   = 34, HEAD_H   = 49;
    private static final float SCALE  = 2f;
    private static final float FPS    = 10f;

    private static final float HEAD_ATTACH_X = 0f; // body pixels (pre-scale)
    private static final float HEAD_ATTACH_Y = 6f;

    // knight
    private Texture knightTex, headsTex;
    private TextureRegion[][] knightGrid, headsGrid;
    private Animation<TextureRegion> walkDown;
    private TextureRegion headOverlay;

    // fire sheet (your image: 6 frames, 36x48)
    private Texture fireTex;
    private Animation<TextureRegion> fireAnim;
    private float fireAnimT = 0f;
    private static final int FIRE_COLS = 6, FIRE_W = 36, FIRE_H = 48;
    private static final float FIRE_FPS = 12f;

    // shader
    private ShaderProgram fireShader;
    private float fireT = 0f;
    private float fireSpeed = 1f;

    // background
    private Texture bgTex;
    private TextureRegion bgRegion;
    private float bgScale = 1.6f;   // 1.0 = native size; tweak if you want
    private float bgYOffset = 0f;   // nudge bg up/down if needed

    private Texture shadowTex;  // soft radial used as blob shadow

    private boolean finishedFadeIn;

    JepoyChatBox chatBalloon;

    public IntroState(GameContext ctx) {
        super(ctx);

        chatBalloon = new JepoyChatBox("chat_box_transparent.png", 32, 2)
                .setContentPadding(24, 24, 24, 24);

        finishedFadeIn = false;

        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        float bw = sw * 0.6f, bh = sh * 0.25f;
        float bx = (sw - bw) / 2f, by = sh * 0.10f;

        box = new Rectangle(bx - 100, by, bw + 200, bh);
        sfx = new TypeSounds();

        typewriterText = new TypeWriterText(ctx.getCharSheet(), box, 8f, 2f);

        typewriterText.setListener((ch, idx) -> {
            //sfx.playFor(ch);
        });

        typewriterText.setCharsPerSecond(18f);
        typewriterText.setLineSpacing(1f);
        textMap.put(0,  "We have not met before, traveler. I keep watch on this road when the nights grow cold and the wind forgets our names. The fire is small, but it is honest; it keeps wolves—and worse—at the edge of the dark.");

        textMap.put(1,  "You carry yourself like one with questions. Ask, and I will answer what I can, but know this: truth is heavier than steel and cuts deeper when bared. Some truths you must cradle, or they will unmake your sleep.");

        textMap.put(2,  "This land is not what it once was. The roads are quiet, yet the stones remember every oath and every footfall; fields lie stitched with old wards, and maps lie because the world refuses to stay still.");

        textMap.put(3,  "Long ago, kingdoms rose and fell chasing a power none could keep. Banners changed, oaths were traded like coin, and generals promised dawns that tasted of ash. Hunger stayed when every harvest failed.");

        textMap.put(4,  "Power always asks a price. We paid in oaths, in blood, and in names that no one speaks anymore because names are doors and doors swing both ways. The debt comes late, but it always knows the road to your fire.");

        textMap.put(5,  "The stones and the old magicks were never meant for hands like ours. We bent them to our will until they bent us back; some wounds do not bleed—they echo. You can hide a scar from others, never from night.");

        textMap.put(6,  "Many sought the relics; few returned with more than stories that shook in their throats. Those who came back brought riddles, and a light behind the eyes that ruined their sleep. I was one of them, once.");

        textMap.put(7,  "You remind me of a comrade from the border war—brave, stubborn, certain that destiny listened when he spoke. He ran toward thunder and laughed so the line would not break. I still hear that laugh when rain begins.");

        textMap.put(8,  "If you wish, I will tell you what became of us: the ford at Keld, the night the river turned against our feet, and the bargain we tried to refuse. I will tell you why I sit here where no patrol rides anymore.");

        textMap.put(9,  "But once you know the truth, sleep will come slower. Stories take their due and never give it back; faces will live in the coals, and the rain will learn your name. You will not be the same traveler at dawn.");

        textMap.put(10, "Will you hear the story, traveler? Nod, and I will begin at the milestone where the road bends east and fate learned our names. If you would turn away, do it now, while the fire is kind and the dark still merciful.");

        typewriterText.setText("We have not met before, traveler. I keep watch on this road when the nights grow cold and the wind forgets our names. The fire is small, but it is honest; it keeps wolves—and worse—at the edge of the dark.");



        ctx.getMainMusic().setLooping(true);
        ctx.getMainMusic().setVolume(0.2f);
        ctx.getMainMusic().play();

        Music crickets = Gdx.audio.newMusic(Gdx.files.internal("sounds/crickets.mp3"));
        crickets.setLooping(true);
        crickets.setVolume(0.3f);
        crickets.play();

        Music campfire = Gdx.audio.newMusic(Gdx.files.internal("sounds/campfire.mp3"));
        campfire.setLooping(true);
        campfire.setVolume(0.6f);
        campfire.play();

        bgTex = new Texture(Gdx.files.internal("sitting_spot2.png"));
        bgTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        bgRegion = new TextureRegion(bgTex);

        knightTex = new Texture(Gdx.files.internal("knight_fixed_atlas.png"));
        knightTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        knightGrid = TextureRegion.split(knightTex, KNIGHT_W, KNIGHT_H);

        headsTex = new Texture(Gdx.files.internal("heads_fixed_atlas.png"));
        headsTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        headsGrid = TextureRegion.split(headsTex, HEAD_W, HEAD_H);

        int row = 0, fromCol = 7, toCol = 7; // pick your body frame(s)
        TextureRegion[] frames = Arrays.copyOfRange(knightGrid[row], fromCol, toCol + 1);
        walkDown = new Animation<>(1f / FPS, frames);
        walkDown.setPlayMode(Animation.PlayMode.LOOP);

        int headRow = 0, headCol = 8;
        headOverlay = headsGrid[headRow][headCol];

        fireTex = new Texture(Gdx.files.internal("fire_sheet.png"));
        fireTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        TextureRegion[] fireFrames = new TextureRegion[FIRE_COLS];
        for (int i = 0; i < FIRE_COLS; i++) {
            fireFrames[i] = new TextureRegion(fireTex, i * FIRE_W, 0, FIRE_W, FIRE_H);
        }
        fireAnim = new Animation<>(1f / FIRE_FPS, fireFrames);
        fireAnim.setPlayMode(Animation.PlayMode.LOOP);

        ShaderProgram.pedantic = false;
        fireShader = new ShaderProgram(
                Gdx.files.internal("vertex.glsl"),
                Gdx.files.internal("fragment.glsl")
        );
        if (!fireShader.isCompiled()) {
            Gdx.app.error("Shader", fireShader.getLog());
            fireShader = null;
        }

        // --- build a small radial gradient texture for the shadow ---
        Pixmap pm = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        int w = pm.getWidth(), h = pm.getHeight();
        float cx = w * 0.5f, cy = h * 0.5f;
        float radius = Math.min(cx, cy) - 1f;
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                float dx = (px + 0.5f) - cx;
                float dy = (py + 0.5f) - cy;
                float r  = (float)Math.sqrt(dx*dx + dy*dy) / radius; // 0..~1
                float a  = Math.max(0f, 1f - r);                     // 1 at center -> 0 at edge
                // smooth falloff
                a = a*a*(3f - 2f*a);
                pm.drawPixel(px, py, Color.rgba8888(0f, 0f, 0f, a));
            }
        }
        shadowTex = new Texture(pm);
        shadowTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();

    }

    @Override
    void update(float dt) {

//        if (firstTransitionActive) {
//            firstTransitionT += dt;
//            if (firstTransitionT >= firstTransitionDuration) {
//                ctx.getGsm().set(new IntroState(ctx));
//                return;
//            }
//        }

        animT += dt;
        fireAnimT += dt;
        fireT += dt * fireSpeed;
        if(fadeInT < fadeInDuration) {
            fadeInT += dt;
        }
        if(finishedFadeIn) {
            sfx.update(dt);
            typewriterText.update(dt);
            if (typewriterText.isDone()) {
                qPulseT += dt;
            }
        }
    }

    private com.badlogic.gdx.math.Rectangle computeBgRect(Texture tex, float scale, float yOffset) {
        float vw = ctx.getViewport().getWorldWidth();
        float vh = ctx.getViewport().getWorldHeight();
        float dw = tex.getWidth() * scale;
        float dh = tex.getHeight() * scale;
        float dx = (vw - dw) * 0.5f;
        float dy = (vh - dh) * 0.5f + yOffset;
        return new com.badlogic.gdx.math.Rectangle(dx, dy, dw, dh);
    }

    private final Vector2 camOffsetStart = new Vector2(0f, -0.2f); // relative to world size (−8% X, +12% Y)
    private void applyIntroCameraTransition() {

        // Progress based on your fade-in timer
        float aLin = Math.min(fadeInT / fadeInDuration, 1f);
        float a = aLin * aLin * (3f - 2f * aLin); // smoothstep

        // World center (target)
        float ww = ctx.getViewport().getWorldWidth();
        float wh = ctx.getViewport().getWorldHeight();
        float targetX = ww * 0.5f;
        float targetY = wh * 0.5f;

        // Start position is offset from target by a % of world size
        float startX = targetX + camOffsetStart.x * ww;
        float startY = targetY + camOffsetStart.y * wh;

        // Lerp pos + zoom
        float cx = startX + (targetX - startX) * a;
        float cy = startY + (targetY - startY) * a;

        OrthographicCamera cam = (OrthographicCamera) ctx.getViewport().getCamera();
        cam.position.set(cx, cy, 0f);
        cam.update();

        // Make sure batch uses the camera
        ctx.getBatch().setProjectionMatrix(cam.combined);
    }

    @Override
    void render() {
        applyIntroCameraTransition();
        // ----- compute rects first (no draw yet) -----
        com.badlogic.gdx.math.Rectangle bgRect = computeBgRect(bgTex, bgScale, bgYOffset);

        float aLin = Math.min(fadeInT / fadeInDuration, 1f);
        float alpha = aLin * aLin * (3f - 2f * aLin);
        ctx.getBatch().setColor(1f, 1f, 1f, alpha);



        float w  = KNIGHT_W * SCALE, h = KNIGHT_H * SCALE;
        float ww = ctx.getViewport().getWorldWidth();
        float wh = ctx.getViewport().getWorldHeight();
        float x  = (ww - w) * 0.5f;
        float y  = (wh - h) * 0.5f + 50;

        TextureRegion frame = walkDown.getKeyFrame(animT, true);

        // fire sprite rect (in front of feet)
        TextureRegion flame = fireAnim.getKeyFrame(fireAnimT, true);
        float fw = FIRE_W * SCALE * 1.2f, fh = FIRE_H * SCALE * 1.2f;
        float fx = x + (w - fw) * 0.5f;
        float fy = y - fh * 0.10f - 25f;

// fire "light" point (near the base of the sprite)
        float lightX = fx + fw * 0.5f;
        float lightY = fy + fh * 0.12f;

// light position in BG local [0..1]
        float bgFireX = (lightX - bgRect.x) / bgRect.width;
        float bgFireY = (lightY - bgRect.y) / bgRect.height;

// ----- 0) BACKGROUND *with shader* (radial light, darker far away) -----
        if (fireShader != null) {
            ctx.getBatch().setShader(fireShader);

            fireShader.setUniformf("u_time", fireT);

            // Kill vertical gradient; use ambient as the global darkness
            fireShader.setUniformf("u_darkStart", 0.0f);
            fireShader.setUniformf("u_darkEnd", 0.0f);
            fireShader.setUniformf("u_ambient", 0.0f);  // baseline darkness of the whole BG

            // Big radius centered at the real fire; brighten only near it
            fireShader.setUniformf("u_firePos", bgFireX, bgFireY + 0.06f);
            fireShader.setUniformf("u_radius", 0.3f);  // 0.45–0.65 depending on taste
            fireShader.setUniformf("u_flickerAmp", 0f);
            fireShader.setUniformf("u_flickerHz", 0.1f);

            // Very subtle warm tint; no shimmer on BG
            fireShader.setUniformf("u_tintColor", 1.00f, 0.92f, 0.55f);
            fireShader.setUniformf("u_tintStrength", 0.2f);
            fireShader.setUniformf("u_tintHeight", 0.70f); // extend up the BG a bit
            fireShader.setUniformf("u_tintPulseAmp", 0.10f);
            fireShader.setUniformf("u_tintPulseHz", 4.0f);

            fireShader.setUniformf("u_waveAmpPx", 0.4f);   // no heat shimmer on BG
            fireShader.setUniformf("u_waveFreqX", 10.0f);
            fireShader.setUniformf("u_waveFreqY", 14.0f);
            fireShader.setUniformf("u_waveSpeed", 1.0f);
            fireShader.setUniformf("u_waveFalloff", 0.5f);
            fireShader.setUniformf("u_waveTurbulence", 0.0f);


            setSpriteRectAndTexel(bgRegion, bgRect.x, bgRect.y, bgRect.width, bgRect.height);
            ctx.getBatch().draw(bgRegion, bgRect.x, bgRect.y, bgRect.width, bgRect.height);

            ctx.getBatch().setShader(null);
        }

        // --------- SHADOW (world-space, no shader) ---------
        ctx.getBatch().setShader(null);

// size/placement under feet
        float shadowW = w * 0.90f;       // width relative to body
        float shadowH = w * 0.5f;       // make it oval (flatter)
        float footX   = x + w * 0.5f;
        float footY   = y + 30f;

// (optional) cast slightly away from the fire:
        float vx = (footX - lightX), vy = (footY - lightY);
        float vlen = (float)Math.sqrt(vx*vx + vy*vy);
        if (vlen > 0.0001f) { vx /= vlen; vy /= vlen; }
        float castOffset = fw * 0.10f;  // tweak how far it’s pushed
        float sxCenter = footX + vx * castOffset;
        float syCenter = footY + vy * castOffset - 2f;

// final rect
        float sx = sxCenter - shadowW * 0.5f;
        float sy = syCenter - shadowH * 0.5f;

        float fade = aLin * aLin * (3f - 2f * aLin);

        final float oldPacked = ctx.getBatch().getPackedColor();
        ctx.getBatch().setColor(1f, 1f, 1f, 0.65f * fade); // overall shadow opacity
        ctx.getBatch().draw(shadowTex, sx, sy, shadowW, shadowH);
        ctx.getBatch().setPackedColor(oldPacked);


        // --- KNIGHT (shader + fade-in) ---

        if (fireShader != null) {
            ctx.getBatch().setShader(fireShader);

            // shared shader params (once per frame)
            fireShader.setUniformf("u_time", fireT);
            fireShader.setUniformf("u_darkStart", 0.12f);
            fireShader.setUniformf("u_darkEnd", 0.2f);
            fireShader.setUniformf("u_ambient", 0.8f);


            fireShader.setUniformf("u_firePos", 0.50f, 0.12f);
            fireShader.setUniformf("u_radius", 0.42f);
            fireShader.setUniformf("u_flickerAmp", 0.45f);
            fireShader.setUniformf("u_flickerHz", 13.0f);

            fireShader.setUniformf("u_tintColor", 1.00f, 0.92f, 0.55f);
            fireShader.setUniformf("u_tintStrength", 0.55f);
            fireShader.setUniformf("u_tintHeight", 0.42f);
            fireShader.setUniformf("u_tintPulseAmp", 0.35f);
            fireShader.setUniformf("u_tintPulseHz", 7.0f);

            fireShader.setUniformf("u_waveAmpPx", 0.3f);  // pixels of wobble
            fireShader.setUniformf("u_waveFreqX", 10.0f);
            fireShader.setUniformf("u_waveFreqY", 14.0f);
            fireShader.setUniformf("u_waveSpeed", 4.0f);
            fireShader.setUniformf("u_waveFalloff", 0.45f);
            fireShader.setUniformf("u_waveTurbulence", 0.55f);

            // BODY: per-draw rect + texel
            setSpriteRectAndTexel(frame, x, y, w, h);
            ctx.getBatch().draw(frame, x, y, w, h);

            // HEAD: same shader, its own rect + texel
            if (headOverlay != null) {
                float hw = HEAD_W * SCALE, hh = HEAD_H * SCALE;
                float hx = x + (w - hw) * 0.5f + HEAD_ATTACH_X * SCALE;
                float hy = y + (h - hh) + (HEAD_ATTACH_Y * SCALE);
                setSpriteRectAndTexel(headOverlay, hx, hy, hw, hh);
                ctx.getBatch().draw(headOverlay, hx - 3, hy + 25, hw, hh);
            }

            ctx.getBatch().setShader(null);
        } else {
            // fallback: still fade-in
            ctx.getBatch().draw(frame, x, y, w, h);
        }



// use the same shader + params, but tweak a few for the fire sprite:
        ctx.getBatch().setShader(fireShader);

// time / shared already set above for the knight — ok to set again:
        fireShader.setUniformf("u_time", fireT);
        fireShader.setUniformf("u_darkStart", 0.20f);
        fireShader.setUniformf("u_darkEnd", 0.95f);
        fireShader.setUniformf("u_ambient", 0.10f); // a bit brighter so flame isn’t muted

        fireShader.setUniformf("u_firePos", 0.50f, 0.12f); // centered, near base
        fireShader.setUniformf("u_radius", 0.32f);
        fireShader.setUniformf("u_flickerAmp", 0.40f);
        fireShader.setUniformf("u_flickerHz", 12.0f);

        fireShader.setUniformf("u_tintColor", 1.00f, 0.92f, 0.55f);
        fireShader.setUniformf("u_tintStrength", 0.35f);  // lighter tint on the fire art
        fireShader.setUniformf("u_tintHeight", 0.36f);
        fireShader.setUniformf("u_tintPulseAmp", 0.25f);
        fireShader.setUniformf("u_tintPulseHz", 6.0f);

        fireShader.setUniformf("u_waveAmpPx", 0.4f);  // tiny shimmer on rocks
        fireShader.setUniformf("u_waveFreqX", 10.0f);
        fireShader.setUniformf("u_waveFreqY", 14.0f);
        fireShader.setUniformf("u_waveSpeed", 4.0f);
        fireShader.setUniformf("u_waveFalloff", 0.35f);
        fireShader.setUniformf("u_waveTurbulence", 0.45f);

// NEW: apply more effect to rocks than flame
        fireShader.setUniformf("u_flameCutoff", 0.65f); // pixels brighter than this are "flame"
        fireShader.setUniformf("u_flameFeather", 0.15f);
        fireShader.setUniformf("u_effectRock", 1.00f); // full effect on rocks
        fireShader.setUniformf("u_effectFlame", 0.20f); // light touch on bright flame

// per-draw rect + texel for the fire sprite
        setSpriteRectAndTexel(flame, fx, fy, fw, fh);
        ctx.getBatch().draw(flame, fx, fy - 20, fw, fh);

        // restore color
        //ctx.getBatch().setColor(1,1,1,1);

        // reset batch state for UI
        ctx.getBatch().setShader(null);

        if(finishedFadeIn) {
            int f = (int)((System.currentTimeMillis()/120) % 4);
            chatBalloon.draw(ctx.getBatch(), box.getX(), box.getY(), box.getWidth(), box.getHeight(), 2f, JepoyChatBox.Tail.NONE, 0.40f, f);
            typewriterText.draw(ctx.getBatch(), ctx.getViewport());
            if (typewriterText.isDone() &&  currentTextMapIndex < 10) {
                chatBalloon.drawNextMarker(ctx.getBatch(), box.getX(), box.getY(), box.getWidth(), 2f, f);
            }
        }

        ctx.getBatch().setColor(1, 1, 1, 1);
    }

    /** Pass the sprite rect (for gl_FragCoord mapping) and the texture's texel size. */
    private void setSpriteRectAndTexel(TextureRegion r, float x, float y, float w, float h) {
        // sprite rect in pixels
        fireShader.setUniformf("u_spritePos",  x, y);
        fireShader.setUniformf("u_spriteSize", w, h);
        // texel size for the *underlying* texture (not region size)
        Texture t = r.getTexture();
        fireShader.setUniformf("u_texel", 1f / t.getWidth(), 1f / t.getHeight());
    }

    @Override
    void shapeRender() {}

    @Override
    void handleInput() {
        if(finishedFadeIn) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.Z) && !typewriterText.isDone()) {
                typewriterText.skipAll();
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.Z) && currentTextMapIndex < 10) {
                Sound sfx = Gdx.audio.newSound(Gdx.files.internal("sounds/knight_02.mp3"));// Optional: loop it// Volume: 0.0 to 1.0
                sfx.play(0.7f, 1f, 0f);
                currentTextMapIndex++;
                typewriterText.setText(textMap.get(currentTextMapIndex));
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.Z) && currentTextMapIndex >= 10) {
                Sound sfx = Gdx.audio.newSound(Gdx.files.internal("sounds/cursor.mp3"));// Optional: loop it// Volume: 0.0 to 1.0
                sfx.play(0.7f, 1f, 0f);
                typewriterText.skipAll();
                typewriterText.setText(null);
            }
        } else {
            if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
                if(fadeInT > fadeInDuration) {
                    Sound sfx = Gdx.audio.newSound(Gdx.files.internal("sounds/hmm.mp3"));// Optional: loop it// Volume: 0.0 to 1.0
                    sfx.play(0.6f, 0.6f, 0f);
                    finishedFadeIn = true;
                }
            }
        }
    }

    @Override
    void dispose() {
        if (knightTex != null) knightTex.dispose();
        if (headsTex  != null) headsTex.dispose();
        if (fireTex   != null) fireTex.dispose();
        if (fireShader!= null) fireShader.dispose();
    }
}
