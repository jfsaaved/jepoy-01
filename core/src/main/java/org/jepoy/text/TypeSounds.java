package org.jepoy.text;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.Disposable;

public class TypeSounds implements Disposable {
    private final Sound letterA, letterB, punctuation;
    private final RandomXS128 rng = new RandomXS128();
    private float minInterval = 0.018f; // throttle so it’s not machine-gun fast
    private float sinceLast = 0f;

    public TypeSounds() {
        letterA     = Gdx.audio.newSound(Gdx.files.internal("sounds/jepoy_game_sound_text_02.ogg"));
        letterB     = Gdx.audio.newSound(Gdx.files.internal("sounds/jepoy_game_sound_text_02.ogg"));
        punctuation = Gdx.audio.newSound(Gdx.files.internal("sounds/jepoy_game_sound_text_02.ogg"));
    }

    public void update(float dt) { sinceLast += dt; }

    public void playFor(char ch) {
        if (sinceLast < minInterval) return;
        boolean isPunct = ",.!?:;\"'()".indexOf(ch) >= 0;
        float pitch = 0.5f + rng.nextFloat() * 0.08f; // tiny variation
        Sound s = isPunct ? punctuation : (rng.nextBoolean() ? letterA : letterB);
        s.play(0.4f, pitch, 0f);
        sinceLast = 0f;
    }

    @Override public void dispose() {
        letterA.dispose();
        letterB.dispose();
        punctuation.dispose();
    }
}
