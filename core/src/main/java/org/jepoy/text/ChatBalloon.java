package org.jepoy.text;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import org.jepoy.util.ChromaKey;


public class ChatBalloon implements Disposable {

    public enum Tail { NONE, TOP_LEFT, TOP_RIGHT, BOTTOM }

    private final Texture tex;

    private final int tile;   // 32
    private final int gap;    // 2
    private final int quad;   // 16
    private final int tail;   // 32

    // corners from tile[0]
    private TextureRegion cnTL, cnTR, cnBL, cnBR;

    // edges from tile[1]
    private TextureRegion eTop, eRight, eBottom, eLeft;

    // center fill (sampled from the middle of tile[0], safe margin to avoid borders)
    private TextureRegion fill;

    // tails [2..4]
    private TextureRegion tailTopLeft, tailBottom, tailTopRight;

    // next frames [5..8] (auto-sized)
    private final TextureRegion[] next = new TextureRegion[4];

    // extra content padding (in addition to 16px border)
    private float padL=0, padR=0, padT=0, padB=0;

    /** NEW: build from an existing Texture (already filtered / processed). */
    public ChatBalloon(Texture texture, int tile, int gap) {
        this.tile = tile;
        this.gap  = gap;
        this.quad = tile / 2;
        this.tail = tile;

        this.tex = texture;

        sliceAll();
        validate();
    }

    /**
     * @param path   internal path (e.g., "ui/chat_box.png")
     * @param tile   32
     * @param gap    2
     */
    public ChatBalloon(String path, int tile, int gap) {
        this.tile = tile;
        this.gap = gap;
        this.quad = tile / 2;
        this.tail = tile;

        tex = new Texture(Gdx.files.internal(path));
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        sliceAll();
        validate();
    }

    public ChatBalloon setContentPadding(float left, float right, float top, float bottom) {
        this.padL = left; this.padR = right; this.padT = top; this.padB = bottom;
        return this;
    }

    /** Text area inside the borders (+extra padding). */
    public Rectangle getContentRect(float x, float y, float w, float h, Rectangle out) {
        if (out == null) out = new Rectangle();
        out.set(x + quad + padL,
                y + quad + padB,
                Math.max(0, w - 2*quad - padL - padR),
                Math.max(0, h - 2*quad - padT - padB));
        return out;
    }

    /** Draw the balloon. tailAnchor01 only used for BOTTOM tail. pass nextFrame=-1 to hide the marker. */
    public void draw(Batch batch, float x, float y, float w, float h,
                     Tail tailKind, float tailAnchor01, int nextFrame) {

        // --- center fill: tile the fill region over the inner area ---
        float innerW = Math.max(0, w - 2*quad);
        float innerH = Math.max(0, h - 2*quad);
        tileRegion(batch, fill, x + quad, y + quad, innerW, innerH);

        // --- edges: tile along each side ---
        tileRegion(batch, eTop,    x + quad,       y + h - quad, innerW, quad);     // top (horizontal)
        tileRegion(batch, eBottom, x + quad,       y,            innerW, quad);     // bottom
        tileRegion(batch, eLeft,   x,              y + quad,     quad,   innerH);   // left (vertical)
        tileRegion(batch, eRight,  x + w - quad,   y + quad,     quad,   innerH);   // right

        // --- corners (16×16 blocks) ---
        batch.draw(cnTL, x,             y + h - quad, quad, quad);
        batch.draw(cnTR, x + w - quad,  y + h - quad, quad, quad);
        batch.draw(cnBL, x,             y,            quad, quad);
        batch.draw(cnBR, x + w - quad,  y,            quad, quad);

        // --- tail ---
        switch (tailKind) {
            case TOP_LEFT: {
                float tx = x + quad - tail + 1;  // tuck slightly under border
                float ty = y + h - tail;
                batch.draw(tailTopLeft, tx, ty, tail, tail);
                break;
            }
            case TOP_RIGHT: {
                float tx = x + w - quad - 1;
                float ty = y + h - tail;
                batch.draw(tailTopRight, tx, ty, tail, tail);
                break;
            }
            case BOTTOM: {
                float usable = Math.max(0, w - 2*quad - tail);
                float tx = x + quad + clamp01(tailAnchor01) * usable;
                float ty = y - (tail - quad);
                batch.draw(tailBottom, tx, ty, tail, tail);
                break;
            }
            case NONE: default: break;
        }

        // --- next marker (bottom-right, 8×8 inset) ---
        if (nextFrame >= 0) {
            int idx = Math.min(3, nextFrame);
            TextureRegion r = next[idx];
            if (r != null) {
                float px = x + w - 8 - r.getRegionWidth();
                float py = y + 8;
                batch.draw(r, px, py, r.getRegionWidth(), r.getRegionHeight());
            }
        }
    }

    public void draw(Batch batch, float x, float y, float w, float h) {
        draw(batch, x, y, w, h, Tail.NONE, 0.5f, -1);
    }

    @Override public void dispose() { tex.dispose(); }

    // --------------------- helpers ---------------------

    private void sliceAll() {
        int W = tex.getWidth();

        int x = 0;

        // [0] 32×32 corners source
        TextureRegion t0 = sub(x, 0, tile, tile); x += tile + gap;
        // quads order: BL, BR, TL, TR  (because LibGDX y=0 is bottom)
        TextureRegion[] q0 = quads(t0);
        cnBL = q0[0]; cnBR = q0[1]; cnTL = q0[2]; cnTR = q0[3];

        // build a fill region from the CENTER of t0 (avoid borders by insetting a few px)
        int inset = Math.max(2, quad/4); // 4px for 32×32
        fill = sub(t0.getRegionX() + inset, t0.getRegionY() + inset,
                tile - 2*inset, tile - 2*inset);

        // [1] 32×32 edges source
        TextureRegion t1 = sub(x, 0, tile, tile); x += tile + gap;
        TextureRegion[] q1 = quads(t1);
        // Default mapping that matches your sheet (see image): TL=top, TR=right, BR=bottom, BL=left
        eTop    = q1[2];
        eRight  = q1[3];
        eBottom = q1[1];
        eLeft   = q1[0];

        // [2..4] tails (32×32)
        tailTopLeft = sub(x, 0, tail, tail); x += tail + gap;
        tailBottom  = sub(x, 0, tail, tail); x += tail + gap;
        tailTopRight= sub(x, 0, tail, tail); x += tail + gap;

        // [5..8] next frames — auto width (whatever fits four frames + 3 gaps)
        int remaining = Math.max(0, W - x);
        int frameGapCount = 3;
        int fw = (remaining - frameGapCount*gap) / 4;
        fw = Math.max(1, fw);                    // never 0
        int fh = Math.min(fw, tex.getHeight());  // keep square-ish and within strip height

        for (int i = 0; i < 4; i++) {
            next[i] = sub(x, 0, fw, fh);
            x += fw + (i < 3 ? gap : 0);
        }
    }

    private TextureRegion[] quads(TextureRegion r32) {
        TextureRegion[] out = new TextureRegion[4];
        int bx = r32.getRegionX(), by = r32.getRegionY();
        out[0] = sub(bx,           by,           quad, quad); // BL
        out[1] = sub(bx + quad,    by,           quad, quad); // BR
        out[2] = sub(bx,           by + quad,    quad, quad); // TL
        out[3] = sub(bx + quad,    by + quad,    quad, quad); // TR
        return out;
    }

    private TextureRegion sub(int x, int y, int w, int h) {
        // clamp so we never request outside the texture (prevents null-ish states)
        int W = tex.getWidth(), H = tex.getHeight();
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x + w > W) w = Math.max(0, W - x);
        if (y + h > H) h = Math.max(0, H - y);
        if (w <= 0 || h <= 0) return new TextureRegion(tex, 0, 0, 1, 1); // safe 1x1 fallback
        return new TextureRegion(tex, x, y, w, h);
    }

    private void tileRegion(Batch batch, TextureRegion r, float x, float y, float w, float h) {
        int rw = r.getRegionWidth();
        int rh = r.getRegionHeight();
        if (rw <= 0 || rh <= 0) return;
        // Draw in a grid of region-sized tiles (no stretching)
        for (float yy = y; yy < y + h; yy += rh) {
            float hh = Math.min(rh, y + h - yy);
            for (float xx = x; xx < x + w; xx += rw) {
                float ww = Math.min(rw, x + w - xx);
                batch.draw(r, xx, yy, 0, 0, ww, hh, 1f, 1f, 0f);
            }
        }
    }

    private void validate() {
        if (tex == null) throw new IllegalStateException("Texture failed to load.");
        if (tex.getHeight() < tile)
            throw new IllegalArgumentException("Strip height < tile size. Got " + tex.getHeight() + " vs tile " + tile);
        // quick sanity on first 2 tiles presence
        int minWidth = tile + gap + tile;
        if (tex.getWidth() < minWidth)
            throw new IllegalArgumentException("Strip width too small for first two tiles. Need >= " + minWidth + "px.");
    }

    private static float clamp01(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
}