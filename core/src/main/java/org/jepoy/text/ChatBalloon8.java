package org.jepoy.text;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import org.jepoy.util.ChromaKey;

import static java.util.Objects.requireNonNull;

public class ChatBalloon8 implements Disposable {
    public enum Tail { NONE, TOP_LEFT, TOP_RIGHT, BOTTOM }

    private final Texture tex;
    private final boolean ownsTex;

    private final int tile;   // 32
    private final int gap;    // 2
    private final int thick;  // 8 (chip size)
    private final int tailSz; // 32

    // corners (8x8) from tile #0
    private TextureRegion cTL, cTR, cBL, cBR;
    // edges (8x8) from tile #1
    private TextureRegion eTop, eRight, eBottom, eLeft;
    // center (8x8) filler from tile #0 (middle)
    private TextureRegion fill8;

    // tails #2..#4 (32x32)
    private TextureRegion tailTopLeft, tailBottom, tailTopRight;

    // next frames #5..#8 (auto-sized)
    private final TextureRegion[] next = new TextureRegion[4];

    // extra padding for the text area (added after the border)
    private float padL=0, padR=0, padT=0, padB=0;

    // tail config
    private boolean tailsEnabled = true;           // you can toggle this at runtime
    private Tail defaultTail = Tail.NONE;          // used by the overload below
    private float defaultTailAnchor01 = 0.5f;      // used for BOTTOM tail
    private boolean hasTails = true;               // auto-detected from the sheet

    public ChatBalloon8 setTailsEnabled(boolean on) {
        this.tailsEnabled = on; return this;
    }
    public ChatBalloon8 setDefaultTail(Tail t, float anchor01) {
        this.defaultTail = t; this.defaultTailAnchor01 = clamp01(anchor01); return this;
    }

    /** Draw using stored tail settings (or none if disabled). */
    public void draw(Batch batch, float x, float y, float w, float h, float scale, int nextFrame) {
        Tail t = (tailsEnabled && hasTails) ? defaultTail : Tail.NONE;
        draw(batch, x, y, w, h, scale, t, defaultTailAnchor01, nextFrame);
    }


    public ChatBalloon8(Texture preprocessed, int tile, int gap, boolean ownsTexture) {
        this.tex = requireNonNull(preprocessed, "Texture is null");
        this.ownsTex = ownsTexture;
        this.tile = tile;
        this.gap  = gap;
        this.thick = 8;
        this.tailSz = tile;
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        sliceAll();
        validate();
    }

    public ChatBalloon8(String internalPath, int tile, int gap) {
        this(new Texture(Gdx.files.internal(internalPath)), tile, gap, true);
    }

    public static ChatBalloon8 fromChromaKey(String internalPath, int tile, int gap, float threshold01) {
        Texture t = ChromaKey.loadKeyTransparent(internalPath, 0,0,0f,true);
        return new ChatBalloon8(t, tile, gap, true);
    }

    public ChatBalloon8 setContentPadding(float left, float right, float top, float bottom) {
        this.padL = left; this.padR = right; this.padT = top; this.padB = bottom; return this;
    }

    /** Inner text rect for a given scale (border & padding are scaled). */
    public Rectangle getContentRectScaled(float x, float y, float w, float h, float scale, Rectangle out) {
        int b = Math.round(thick * scale);
        float pl = padL * scale, pr = padR * scale, pt = padT * scale, pb = padB * scale;
        if (out == null) out = new Rectangle();
        out.set(x + b + pl,
                y + b + pb,
                Math.max(0, w - 2*b - pl - pr),
                Math.max(0, h - 2*b - pt - pb));
        return out;
    }

    /** Draw balloon at (x,y) of size (w,h) with uniform pixel scale. */
    public void draw(Batch batch, float x, float y, float w, float h,
                     float scale, Tail tail, float tailAnchor01, int nextFrame) {

        final int b  = Math.round(thick * scale);     // scaled border thickness
        final int ts = Math.round(tailSz * scale);    // scaled tail size
        final float innerW = Math.max(0, w - 2*b);
        final float innerH = Math.max(0, h - 2*b);

        // center fill (tile 8x8 at 'scale')
        tileRegionScaled(batch, fill8, x + b, y + b, innerW, innerH, scale);

        // edges
        tileRegionScaled(batch, eTop,    x + b,     y + h - b, innerW, b, scale);
        tileRegionScaled(batch, eBottom, x + b,     y,         innerW, b, scale);
        tileRegionScaled(batch, eLeft,   x,         y + b,     b,      innerH, scale);
        tileRegionScaled(batch, eRight,  x + w - b, y + b,     b,      innerH, scale);

        // corners
        batch.draw(cTL, x,           y + h - b, b, b);
        batch.draw(cTR, x + w - b,   y + h - b, b, b);
        batch.draw(cBL, x,           y,         b, b);
        batch.draw(cBR, x + w - b,   y,         b, b);

        // tails
// tails
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


        // next marker (bottom-right, inset 8*scale)
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

    public void draw(Batch batch, float x, float y, float w, float h) {
        draw(batch, x, y, w, h, 1f, Tail.NONE, 0.5f, -1);
    }

    @Override public void dispose() { if (ownsTex && tex != null) tex.dispose(); }

    // ------------------ slicing ------------------
    private void sliceAll() {
        int baseX = 0;

        // Tile #0 (32x32): 8x8 corners + center chip
        int t0x = baseX, t0y = 0; baseX += tile + gap;
        cTL = sub(t0x + 0,               t0y + 0,               thick, thick);                 // top-left
        cTR = sub(t0x + (tile - thick),  t0y + 0,               thick, thick);                 // top-right
        cBL = sub(t0x + 0,               t0y + (tile - thick),  thick, thick);                 // bottom-left
        cBR = sub(t0x + (tile - thick),  t0y + (tile - thick),  thick, thick);                 // bottom-right


        int cx = t0x + (tile/2 - thick/2); // 12
        int cy = t0y + (tile/2 - thick/2); // 12
        fill8 = sub(cx, cy, thick, thick);

        // Tile #1 (32x32): edge chips — FIXED mapping (top is top, bottom is bottom)
        int t1x = baseX, t1y = 0; baseX += tile + gap;
        int mid = (tile/2 - thick/2); // 12
        eTop    = sub(t1x + mid, t1y + 0,              thick, thick);
        eBottom = sub(t1x + mid, t1y + (tile - thick), thick, thick);
        eLeft   = sub(t1x + 0,   t1y + mid,            thick, thick);
        eRight  = sub(t1x + (tile - thick), t1y + mid, thick, thick);

// ---- Tails #2..#4 ----
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

        // Next frames #5..#8
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

    // scaled tiling of a region (keeps pixel art crisp when 'scale' is integer)
    private void tileRegionScaled(Batch batch, TextureRegion r,
                                  float x, float y, float w, float h, float scale) {
        int rw = r.getRegionWidth();
        int rh = r.getRegionHeight();
        if (rw <= 0 || rh <= 0) return;

        float stepW = rw * scale;
        float stepH = rh * scale;

        for (float yy = y; yy < y + h - 0.0001f; yy += stepH) {
            float drawH = Math.min(stepH, y + h - yy);
            float srcH  = rh * (drawH / stepH);
            for (float xx = x; xx < x + w - 0.0001f; xx += stepW) {
                float drawW = Math.min(stepW, x + w - xx);
                float srcW  = rw * (drawW / stepW);
                batch.draw(r.getTexture(),
                        xx, yy,
                        drawW, drawH,
                        r.getRegionX(), r.getRegionY(),
                        Math.round(srcW), Math.round(srcH),
                        false, false);
            }
        }
    }

    private void validate() {
        if (tex.getHeight() < tile)
            throw new IllegalArgumentException("Strip height < tile size: " + tex.getHeight() + " vs " + tile);
        if (tex.getWidth() < tile + gap + tile)
            throw new IllegalArgumentException("Strip width too small for first two tiles.");
    }

    private static float clamp01(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
}
