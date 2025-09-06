package org.jepoy.text;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.ObjectFloatMap;
import com.badlogic.gdx.utils.viewport.Viewport;

public class TypewriterText {
    private final CharSheet sheet;
    private String text = "";
    private float scale = 1f;
    private float cps = 20f;
    private float elapsed = 0f;
    private int shown = 0;
    private boolean done = false;

    // wrapping box (bottom-left origin)
    private final Rectangle box = new Rectangle();
    private float padding = 0f;
    private float lineSpacing = 0f; // extra pixels between lines

    // optional pauses on punctuation
    private final ObjectFloatMap<Character> pause = new ObjectFloatMap<>();

    private CharRevealListener listener;
    public void setListener(CharRevealListener l) { this.listener = l; }


    public TypewriterText(CharSheet sheet, Rectangle boundsBL, float padding, float scale) {
        this.sheet = sheet;
        this.scale = scale;
        this.box.set(boundsBL);
        this.padding = padding;
        pause.put('.', 0.20f);
        pause.put(',', 0.12f);
        pause.put('!', 0.18f);
        pause.put('?', 0.18f);
    }

    public void setText(String t) {
        text = (t != null) ? t : "";
        elapsed = 0f; shown = 0; done = false;
    }
    public void setCharsPerSecond(float charsPerSec) { this.cps = charsPerSec; }
    public void setLineSpacing(float px) { this.lineSpacing = px; }
    public void skipAll() { shown = text.length(); done = true; }
    public boolean isDone() { return done; }

    public void update(float dt) {
        if (done) return;
        elapsed += dt;

        int target = Math.min(text.length(), (int)(elapsed * cps));
        while (shown < target) {
            char ch = text.charAt(shown++);
            float extra = pause.get(ch, 0f);
            // fire event for non-space/newline
            if (listener != null && ch != ' ' && ch != '\n') {
                listener.onCharRevealed(ch, shown - 1);
            }
            if (extra > 0f) { elapsed -= extra; break; }
        }
        if (shown >= text.length()) done = true;
    }


    public void draw(SpriteBatch batch, Viewport viewport) {
        if (text == null || text.isEmpty() || shown <= 0) return;

        final float startX = box.x + padding;
        final float rightX = box.x + box.width - padding;
        final float topY   = box.y + box.height - padding;
        final float lh     = (sheet.lineHeight * scale) + lineSpacing;
        final float visibleH = box.height - 2f * padding;

        final int n = Math.min(shown, text.length());

        // helpers
        java.util.function.IntUnaryOperator charW = idx -> {
            char ch = text.charAt(idx);
            if (ch == ' ') return Math.round(sheet.advance * scale);
            TextureRegion r = sheet.regionFor(ch);
            if (r == null) return Math.round(sheet.advance * scale / 2f);
            return Math.round(r.getRegionWidth() * scale);
        };
        java.util.function.IntBinaryOperator spanW = (i, j) -> {
            int w = 0;
            for (int k = i; k < j; k++) w += charW.applyAsInt(k);
            return w;
        };

        // ---------- 1) PRE-PASS: count how many visual lines current text occupies ----------
        int totalLines = 0;
        {
            float cx = startX;
            int i = 0;
            boolean placedSomethingOnLine = false;

            while (i < n) {
                char ch = text.charAt(i);

                if (ch == '\n') {
                    totalLines++;
                    cx = startX;
                    placedSomethingOnLine = false;
                    i++;
                    continue;
                }

                if (ch == ' ') {
                    int w = charW.applyAsInt(i);
                    if (cx + w > rightX && cx > startX) { // wrap before space
                        totalLines++;
                        cx = startX;
                        placedSomethingOnLine = false;
                    } else {
                        cx += w;
                        placedSomethingOnLine = true;
                    }
                    i++;
                    continue;
                }

                // word token [i, wordEnd)
                int wordEnd = i;
                while (wordEnd < text.length()) {
                    char c = text.charAt(wordEnd);
                    if (c == ' ' || c == '\n') break;
                    wordEnd++;
                }

                int fullWordW = spanW.applyAsInt(i, wordEnd);

                if (cx > startX && cx + fullWordW > rightX) {
                    totalLines++;
                    cx = startX;
                    placedSomethingOnLine = false;
                }

                if (cx == startX && fullWordW > (rightX - startX)) {
                    // hard-break extremely long word
                    int k = i;
                    while (k < Math.min(wordEnd, n)) {
                        int w = charW.applyAsInt(k);
                        if (cx + w > rightX && cx > startX) {
                            totalLines++;
                            cx = startX;
                            placedSomethingOnLine = false;
                        }
                        cx += w;
                        placedSomethingOnLine = true;
                        k++;
                    }
                    if (n < wordEnd) break;   // partial word revealed
                    i = wordEnd;
                } else {
                    int drawEnd = Math.min(wordEnd, n);
                    for (int k = i; k < drawEnd; k++) {
                        int w = charW.applyAsInt(k);
                        if (cx + w > rightX && cx > startX) {
                            totalLines++;
                            cx = startX;
                            placedSomethingOnLine = false;
                        }
                        cx += w;
                        placedSomethingOnLine = true;
                    }
                    if (n < wordEnd) break;   // partial word revealed
                    i = wordEnd;
                }
            }
            if (placedSomethingOnLine) totalLines++;
            if (totalLines == 0) totalLines = 1; // safety
        }

        // visible line capacity & scroll offset
        int maxLines = Math.max(1, (int)Math.floor(visibleH / lh));
        final float bottomY = box.y + padding;

        float cx = startX;
        final boolean overflow = totalLines > maxLines;
        float cy = overflow
                ? bottomY + (totalLines - 1) * lh   // latest line lands on bottom
                : topY - lh;

        // ---------- 2) SCISSOR: clip to your box using Scene2D utils ----------
        Rectangle areaWorld = new Rectangle(box); // box is already in world coords (bottom-left origin)
        Rectangle scissors = new Rectangle();

        // IMPORTANT: calculate with the same camera/viewport you render with
        ScissorStack.calculateScissors(
                viewport.getCamera(),
                batch.getTransformMatrix(),
                areaWorld,
                scissors
        );

        // tighten to integer pixels to avoid 1px bleed (optional)
        scissors.set(
                (float)Math.floor(scissors.x),
                (float)Math.floor(scissors.y),
                (float)Math.floor(scissors.width),
                (float)Math.floor(scissors.height)
        );

        batch.flush();
        ScissorStack.pushScissors(scissors);// if push fails, just draw without clipping to avoid a hard fail

        // ---------- 3) DRAW PASS (no bottom abort; scissor clips) ----------
        int i = 0;
        while (i < n) {
            char ch = text.charAt(i);

            if (ch == '\n') {
                cx = startX;
                cy -= lh;
                i++;
                continue;
            }

            if (ch == ' ') {
                int w = charW.applyAsInt(i);
                if (cx + w > rightX && cx > startX) {
                    cx = startX;
                    cy -= lh;
                } else {
                    cx += w;
                }
                i++;
                continue;
            }

            int wordEnd = i;
            while (wordEnd < text.length()) {
                char c = text.charAt(wordEnd);
                if (c == ' ' || c == '\n') break;
                wordEnd++;
            }

            int fullWordW = spanW.applyAsInt(i, wordEnd);

            if (cx > startX && cx + fullWordW > rightX) {
                cx = startX;
                cy -= lh;
            }

            if (cx == startX && fullWordW > (rightX - startX)) {
                int k = i;
                while (k < Math.min(wordEnd, n)) {
                    int w = charW.applyAsInt(k);
                    if (cx + w > rightX && cx > startX) {
                        cx = startX;
                        cy -= lh;
                    }
                    TextureRegion r = sheet.regionFor(text.charAt(k));
                    if (r != null) batch.draw(r, cx, cy, r.getRegionWidth() * scale, r.getRegionHeight() * scale);
                    cx += w;
                    k++;
                }
                if (n < wordEnd) break; // partial word not fully revealed yet
                i = wordEnd;
                continue;
            }

            int drawEnd = Math.min(wordEnd, n);
            for (int k = i; k < drawEnd; k++) {
                int w = charW.applyAsInt(k);
                if (cx + w > rightX && cx > startX) {
                    cx = startX;
                    cy -= lh;
                }
                TextureRegion r = sheet.regionFor(text.charAt(k));
                if (r != null) batch.draw(r, cx, cy, r.getRegionWidth() * scale, r.getRegionHeight() * scale);
                cx += w;
            }

            if (n < wordEnd) break; // partial word revealed; stop this frame
            i = wordEnd;
        }

        batch.flush();
        if (ScissorStack.peekScissors() == scissors) {
            ScissorStack.popScissors();
        }
    }
}