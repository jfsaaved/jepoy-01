package org.jepoy.text;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import org.jepoy.GameContext;

public class StaticSpriteText {
    protected final CharSheet sheet;
    protected final Rectangle box = new Rectangle();
    protected float padding = 0f;
    protected float scale = 1f;
    protected float lineSpacing = 0f;
    protected String text = "";

    public StaticSpriteText(CharSheet sheet, Rectangle boundsBL, float padding, float scale) {
        this.sheet = sheet;
        this.box.set(boundsBL);
        this.padding = padding;
        this.scale = scale;
    }

    public void setText(String t) { this.text = (t != null) ? t : ""; }
    public void setBox(Rectangle r) { this.box.set(r); }
    public void setPadding(float p) { this.padding = p; }
    public void setScale(float s) { this.scale = s; }
    public void setLineSpacing(float px) { this.lineSpacing = px; }

    // No-op for API parity with animated classes
    public void update(float dt) {}

    public void drawBlinking(GameContext ctx, float pulseT, float pulsePeriod) {
        Color old = ctx.getBatch().getColor();
        float alpha = 0.2f + 0.8f * 0.5f * (0.5f + MathUtils.sin(MathUtils.PI2 * (pulseT / pulsePeriod)));
        ctx.getBatch().setColor(old.r, old.g, old.b, alpha);
        this.draw(ctx.getBatch());
        ctx.getBatch().setColor(old);
    }

    public void draw(SpriteBatch batch) {
        drawWrapped(batch, text, sheet, box, padding, scale, lineSpacing, null);
    }

    /** Renders full text with predictive wrap; if cursorOut != null, writes caret position. */
    protected static void drawWrapped(SpriteBatch batch, String text, CharSheet sheet,
                                      Rectangle box, float padding, float scale, float lineSpacing,
                                      CursorOut cursorOut) {
        if (text == null || text.isEmpty()) {
            if (cursorOut != null) cursorOut.set(box.x + padding, box.y + box.height - padding - sheet.lineHeight * scale);
            return;
        }

        final float startX = box.x + padding;
        final float rightX = box.x + box.width - padding;
        final float topY   = box.y + box.height - padding;
        final float lh     = sheet.lineHeight * scale + lineSpacing;

        float cx = startX;
        float cy = topY - (sheet.lineHeight * scale); // baseline for first line

        // variable-width safe width function
        java.util.function.IntUnaryOperator charW = idx -> {
            char ch = text.charAt(idx);
            if (ch == ' ') return Math.round(sheet.getDefaultAdvance() * scale);
            if (ch == '\n') return 0;
            TextureRegion r = sheet.regionFor(ch);
            return (r != null) ? Math.round(r.getRegionWidth() * scale)
                    : Math.round(sheet.getDefaultAdvance() * scale * 0.5f);
        };

        java.util.function.IntBinaryOperator spanW = (i, j) -> {
            int w = 0;
            for (int k = i; k < j; k++) w += charW.applyAsInt(k);
            return w;
        };

        int i = 0, n = text.length();
        while (i < n) {
            char ch = text.charAt(i);

            if (ch == '\n') {
                cx = startX; cy -= lh;
                if (cy + sheet.lineHeight * scale < box.y + padding) break;
                i++; continue;
            }

            if (ch == ' ') {
                int w = charW.applyAsInt(i);
                if (cx + w > rightX && cx > startX) {
                    cx = startX; cy -= lh;
                    if (cy + sheet.lineHeight * scale < box.y + padding) break;
                } else {
                    cx += w;
                }
                i++; continue;
            }

            // word [i, wordEnd)
            int wordEnd = i;
            while (wordEnd < n) {
                char c = text.charAt(wordEnd);
                if (c == ' ' || c == '\n') break;
                wordEnd++;
            }
            int wordW = spanW.applyAsInt(i, wordEnd);

            // wrap before drawing if needed
            if (cx > startX && cx + wordW > rightX) {
                cx = startX; cy -= lh;
                if (cy + sheet.lineHeight * scale < box.y + padding) break;
            }

            // very long word: hard-break at char boundaries
            if (cx == startX && wordW > (rightX - startX)) {
                int k = i;
                while (k < wordEnd) {
                    int w = charW.applyAsInt(k);
                    if (cx + w > rightX && cx > startX) {
                        cx = startX; cy -= lh;
                        if (cy + sheet.lineHeight * scale < box.y + padding) { k = wordEnd; break; }
                    }
                    TextureRegion r = sheet.regionFor(text.charAt(k));
                    if (r != null) batch.draw(r, cx, cy, r.getRegionWidth() * scale, r.getRegionHeight() * scale);
                    cx += w; k++;
                }
                i = wordEnd;
                continue;
            }

            // draw normal word
            for (int k = i; k < wordEnd; k++) {
                int w = charW.applyAsInt(k);
                TextureRegion r = sheet.regionFor(text.charAt(k));
                if (r != null) batch.draw(r, cx, cy, r.getRegionWidth() * scale, r.getRegionHeight() * scale);
                cx += w;
            }
            i = wordEnd;
        }

        if (cursorOut != null) cursorOut.set(cx, cy);
    }

    /** Simple holder for caret position after drawing. */
    protected static class CursorOut {
        public float x, y;
        public void set(float x, float y) { this.x = x; this.y = y; }
    }
}
