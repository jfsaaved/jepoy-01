package org.jepoy.text;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;

import java.util.Objects;

public class JepoyChatBox implements Disposable {

    public enum Tail { NONE, TOP_LEFT, TOP_RIGHT, BOTTOM }

    private final Texture tex;
    private final boolean ownsTex;

    private final int tile;   // 32
    private final int gap;    // 2
    private final int chip;   // 8 (tile/4)
    private final int tailSz; // 32

    // 8×8 corners
    private TextureRegion cTL, cTR, cBL, cBR;

    // 8×8 edges (two chips per edge → ABAB… pattern)
    private TextureRegion topA, topB, botA, botB, leftA, leftB, rightA, rightB;

    // 8×8 body pattern (center 2×2 chips: c00,c10,c01,c11)
    private TextureRegion f00, f10, f01, f11;

    // optional tails (32×32)
    private TextureRegion tailTopLeft, tailBottom, tailTopRight;
    private boolean hasTails = false;
    private boolean tailsEnabled = true;
    private Tail defaultTail = Tail.NONE;
    private float defaultTailAnchor01 = 0.5f;

    // optional “next” frames
    private final TextureRegion[] next = new TextureRegion[4];

    // extra padding inside the 8px border (scaled later)
    private float padL=0, padR=0, padT=0, padB=0;

    public JepoyChatBox(String internalPath, int tile, int gap) {
        this(new Texture(Gdx.files.internal(internalPath)), tile, gap, true);
    }
    public static JepoyChatBox fromTexture(Texture texture, int tile, int gap, boolean ownsTexture) {
        return new JepoyChatBox(texture, tile, gap, ownsTexture);
    }
    private JepoyChatBox(Texture texture, int tile, int gap, boolean ownsTexture) {
        this.tex = Objects.requireNonNull(texture);
        this.ownsTex = ownsTexture;
        this.tile = tile;
        this.gap = gap;
        this.chip = tile/4;
        this.tailSz = tile;
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        sliceAll();
        validate();
    }

    public JepoyChatBox setContentPadding(float left, float right, float top, float bottom) {
        this.padL = left; this.padR = right; this.padT = top; this.padB = bottom; return this;
    }
    public JepoyChatBox setTailsEnabled(boolean on) { this.tailsEnabled = on; return this; }
    public JepoyChatBox setDefaultTail(Tail t, float anchor01) {
        this.defaultTail = t; this.defaultTailAnchor01 = clamp01(anchor01); return this;
    }

    /** Text area after border + extra padding (all scaled). */
    public Rectangle getContentRectScaled(float x, float y, float w, float h, float scale, Rectangle out) {
        float b = chip * scale; // 8*scale
        float pl = padL * scale, pr = padR * scale, pt = padT * scale, pb = padB * scale;
        if (out == null) out = new Rectangle();
        out.set(x + b + pl, y + b + pb,
                Math.max(0, w - 2*b - pl - pr),
                Math.max(0, h - 2*b - pt - pb));
        return out;
    }

    /** Convenience overload using the stored tail settings. */
    public void draw(Batch batch, float x, float y, float w, float h, float scale, int nextFrame) {
        Tail t = (tailsEnabled && hasTails) ? defaultTail : Tail.NONE;
        draw(batch, x, y, w, h, scale, t, defaultTailAnchor01, nextFrame);
    }

    /** Main draw. */
    public void draw(Batch batch, float x, float y, float w, float h,
                     float scale, Tail tail, float tailAnchor01, int nextFrame) {

        // scaled sizes
        final float b  = chip   * scale; // border thickness
        final float ts = tailSz * scale; // tail size
        final float innerW = Math.max(0, w - 2*b);
        final float innerH = Math.max(0, h - 2*b);

        // ----- body fill: tile the 2×2 pattern (f00,f10 / f01,f11) -----
        tileBody2x2(batch, x + b, y + b, innerW, innerH, scale);

        // ----- edges (two-chip AB tiling) -----
        tileH(batch, topA, topB,   x + b,       y + h - b, innerW, b, scale); // top
        tileH(batch, botA, botB,   x + b,       y,         innerW, b, scale); // bottom
        tileV(batch, leftA, leftB, x,           y + b,     b,      innerH, scale); // left
        tileV(batch, rightA,rightB,x + w - b,   y + b,     b,      innerH, scale); // right

        // ----- corners -----
        batch.draw(cTL, x,           y + h - b, b, b);
        batch.draw(cTR, x + w - b,   y + h - b, b, b);
        batch.draw(cBL, x,           y,         b, b);
        batch.draw(cBR, x + w - b,   y,         b, b);

        // ----- tail (optional) -----
        if (tailsEnabled && hasTails && tail != Tail.NONE) {
            switch (tail) {
                case TOP_LEFT: {
                    float tx = x + b - ts + 1;
                    float ty = y + h - ts;
                    batch.draw(tailTopLeft, tx, ty, ts, ts);
                    break;
                }
                case TOP_RIGHT: {
                    float tx = x + w - b - 1;
                    float ty = y + h - ts;
                    batch.draw(tailTopRight, tx, ty, ts, ts);
                    break;
                }
                case BOTTOM: {
                    float usable = Math.max(0, w - 2*b - ts);
                    float tx = x + b + clamp01(tailAnchor01) * usable;
                    float ty = y - (ts - b);
                    batch.draw(tailBottom, tx, ty, ts, ts);
                    break;
                }
                default: break;
            }
        }

        // ----- next marker (optional; 8*scale inset) -----

    }

    public void drawNextMarker(Batch batch, float x, float y, float w,
                               float scale, int nextFrame) {
        if (nextFrame >= 0) {
            int idx = Math.min(3, nextFrame);
            TextureRegion r = next[idx];
            if (r != null) {
                float inset = 8f * scale;
                float nw = r.getRegionWidth()  * scale;
                float nh = r.getRegionHeight() * scale;
                float px = x + w - inset - nw;
                float py = y + inset;
                batch.draw(r, px, py, nw, nh);
            }
        }
    }

    @Override public void dispose() { if (ownsTex && tex != null) tex.dispose(); }

    // =================== slicing ===================

    private void sliceAll() {
        int baseX = 0;
        int t0x = baseX, t0y = 0; baseX += tile + gap; // first 32×32

        // corners (top-left origin in PNG)
        cTL = sub(0, 0, chip, chip);
        cTR = sub(t0x + 3*chip, 0, chip, chip);
        cBL = sub(0, t0y + 3*chip, chip, chip);
        cBR = sub(t0x + 3*chip, t0y + 3*chip, chip, chip);

        // edges: middle two along each side
        topA = sub(t0x + chip, 0, chip, chip);
        topB = sub(t0x + 2*chip, 0, chip, chip);
        botA = sub(t0x + chip, t0y + 3*chip, chip, chip);
        botB = sub(t0x + 2*chip, t0y + 3*chip, chip, chip);

        leftA  = sub(0, t0y + chip, chip, chip);
        leftB  = sub(0, t0y + 2*chip, chip, chip);
        rightA = sub(t0x + 3*chip, t0y + chip, chip, chip);
        rightB = sub(t0x + 3*chip, t0y + 2*chip, chip, chip);

        // body 2×2 center
        f00 = sub(t0x + chip, t0y + chip, chip, chip);
        f10 = sub(t0x + 2*chip, t0y + chip, chip, chip);
        f01 = sub(t0x + chip, t0y + 2*chip, chip, chip);
        f11 = sub(t0x + 2*chip, t0y + 2*chip, chip, chip);

        baseX += tile + gap;

        // Optional tails (tiles #1..#3)
        int tailsWidthNeeded = 3 * tailSz + 2 * gap;
        if (tex.getWidth() - baseX >= tailsWidthNeeded) {
            hasTails = true;
            tailTopLeft = sub(baseX, 0, tailSz, tailSz); baseX += tailSz + gap;
            tailBottom  = sub(baseX, 0, tailSz, tailSz); baseX += tailSz + gap;
            tailTopRight= sub(baseX, 0, tailSz, tailSz); baseX += tailSz + gap;
        } else {
            hasTails = false;
            tailTopLeft = tailBottom = tailTopRight = null; // nothing to draw
        }

        // Optional “next” 4 frames (#4..#7)
        int remaining = Math.max(0, tex.getWidth() - baseX);
        int fw = remaining > 0 ? (remaining - 3*gap)/4 : 0;
        fw = Math.max(1, fw);
        int fh = Math.min(fw, tex.getHeight());
        for (int i=0;i<4;i++) {
            next[i] = sub(baseX, 0, fw, fh);
            baseX += fw + (i<3 ? gap : 0);
        }
    }

    private TextureRegion sub(int x, int y, int w, int h) {
        int W = tex.getWidth(), H = tex.getHeight();
        if (x < 0) x = 0; if (y < 0) y = 0;
        if (x + w > W) w = Math.max(0, W - x);
        if (y + h > H) h = Math.max(0, H - y);
        if (w <= 0 || h <= 0) return new TextureRegion(tex, 0, 0, 1, 1);
        return new TextureRegion(tex, x, y, w, h);
    }

    // =================== tiling helpers ===================

    private void tileBody2x2(Batch batch, float x, float y, float w, float h, float scale) {
        float step = chip * scale; // 8*scale
        for (float yy = y, row = 0; yy < y + h; yy += step, row++) {
            for (float xx = x, col = 0; xx < x + w; xx += step, col++) {
                TextureRegion r = ((int)row % 2 == 0)
                        ? (((int)col % 2 == 0) ? f00 : f10)
                        : (((int)col % 2 == 0) ? f01 : f11);
                float drawW = Math.min(step, x + w - xx);
                float drawH = Math.min(step, y + h - yy);
                // partial tile at edges: crop source proportionally
                float srcW = r.getRegionWidth()  * (drawW / step);
                float srcH = r.getRegionHeight() * (drawH / step);
                batch.draw(r.getTexture(), xx, yy, drawW, drawH,
                        r.getRegionX(), r.getRegionY(),
                        Math.round(srcW), Math.round(srcH), false, false);
            }
        }
    }

    private void tileH(Batch batch, TextureRegion a, TextureRegion b,
                       float x, float y, float w, float h, float scale) {
        float step = chip * scale;
        boolean useA = true;
        for (float xx = x; xx < x + w; xx += step, useA = !useA) {
            TextureRegion r = useA ? a : b;
            float drawW = Math.min(step, x + w - xx);
            float srcW  = r.getRegionWidth() * (drawW / step);
            batch.draw(r.getTexture(), xx, y, drawW, h,
                    r.getRegionX(), r.getRegionY(),
                    Math.round(srcW), r.getRegionHeight(), false, false);
        }
    }

    private void tileV(Batch batch, TextureRegion a, TextureRegion b,
                       float x, float y, float w, float h, float scale) {
        float step = chip * scale;
        boolean useA = true;
        for (float yy = y; yy < y + h; yy += step, useA = !useA) {
            TextureRegion r = useA ? a : b;
            float drawH = Math.min(step, y + h - yy);
            float srcH  = r.getRegionHeight() * (drawH / step);
            batch.draw(r.getTexture(), x, yy, w, drawH,
                    r.getRegionX(), r.getRegionY(),
                    r.getRegionWidth(), Math.round(srcH), false, false);
        }
    }

    private static float clamp01(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    private void validate() {
        if (tex.getHeight() < tile) throw new IllegalArgumentException("Strip height < tile.");
        if (tex.getWidth() < tile)  throw new IllegalArgumentException("Strip too narrow for first 32×32.");
    }
}
