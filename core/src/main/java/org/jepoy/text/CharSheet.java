package org.jepoy.text;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectMap;

public final class CharSheet {
    private final Texture tex;
    public final int lineHeight;        // base line height in pixels (pre-scale)
    private int defaultAdvance;         // fallback advance if none set
    private int spaceAdvance;           // width of ' '

    private final ObjectMap<Character, TextureRegion> region = new ObjectMap<>();
    private final ObjectIntMap<Character> advance = new ObjectIntMap<>(); // optional per-glyph advance

    private char fallbackChar = '?';

    private CharSheet(Texture tex, int lineHeight, int defaultAdvance, int spaceAdvance) {
        this.tex = tex;
        this.lineHeight = lineHeight;
        this.defaultAdvance = defaultAdvance;
        this.spaceAdvance = spaceAdvance;
    }

    public int getDefaultAdvance() {
        return defaultAdvance;
    }

    /** Quick builder when your sheet is a uniform grid and layout is arbitrary. */
    public static CharSheet fromGrid(Texture tex, int cellW, int cellH,
                                     String[] layoutRows, int lineHeight,
                                     int defaultAdvance, int spaceAdvance) {
        CharSheet cs = new CharSheet(tex, lineHeight, defaultAdvance, spaceAdvance);
        for (int row = 0; row < layoutRows.length; row++) {
            String rowStr = layoutRows[row];
            for (int col = 0; col < rowStr.length(); col++) {
                char ch = rowStr.charAt(col);
                if (ch == '\0') continue;     // ignore nulls
                // If you want a visual placeholder for "empty", use '·' and skip it:
                if (ch == '·') continue;
                TextureRegion r = new TextureRegion(tex, col * cellW, row * cellH, cellW, cellH);
                cs.region.put(ch, r);
                cs.advance.put(ch, cellW); // start with cellW; you can refine per glyph later
            }
        }
        return cs;
    }

    /** Map a single character that lives on the grid at (col,row). */
    public CharSheet mapAtGrid(char ch, int col, int row, int cellW, int cellH) {
        TextureRegion r = new TextureRegion(tex, col * cellW, row * cellH, cellW, cellH);
        region.put(ch, r);
        advance.put(ch, cellW);
        return this;
    }

    /** Map a single character to an arbitrary pixel rectangle. */
    public CharSheet mapRect(char ch, int x, int y, int w, int h, Integer advPx) {
        TextureRegion r = new TextureRegion(tex, x, y, w, h);
        region.put(ch, r);
        if (advPx != null) advance.put(ch, advPx);
        return this;
    }

    /** Use an existing glyph's art for another character. */
    public CharSheet alias(char to, char from) {
        TextureRegion r = region.get(from);
        if (r != null) region.put(to, r);
        int adv = advance.get(from, 0);
        if (adv != 0) advance.put(to, adv);
        return this;
    }

    public CharSheet setAdvance(char ch, int px) { advance.put(ch, px); return this; }
    public CharSheet setSpaceAdvance(int px) { this.spaceAdvance = px; return this; }
    public CharSheet setDefaultAdvance(int px) { this.defaultAdvance = px; return this; }
    public CharSheet setFallbackChar(char c) { this.fallbackChar = c; return this; }

    /** Region for drawing (returns fallback if missing). */
    public TextureRegion regionFor(char ch) {
        TextureRegion r = region.get(ch);
        if (r == null) r = region.get(fallbackChar);
        return r;
    }

    /** Pixel advance for layout (space handled here). */
    public int advanceOf(char ch) {
        if (ch == ' ') return spaceAdvance;
        int adv = advance.get(ch, 0);
        if (adv != 0) return adv;
        TextureRegion r = region.get(ch);
        if (r != null) return r.getRegionWidth();
        // fallback advance
        TextureRegion fb = region.get(fallbackChar);
        return fb != null ? fb.getRegionWidth() : defaultAdvance;
    }

    public static Texture loadFontAsAlpha(String path) {
        Pixmap src = new Pixmap(Gdx.files.internal(path));
        Pixmap dst = new Pixmap(src.getWidth(), src.getHeight(), Pixmap.Format.RGBA8888);

        Color c = new Color();
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int rgba = src.getPixel(x, y);
                Color.rgba8888ToColor(c, rgba);

                // luminance of the source pixel (tweak if you like)
                float lum = 0.299f * c.r + 0.587f * c.g + 0.114f * c.b;

                // Optional threshold/soft edge:
                // lum = MathUtils.clamp((lum - 0.05f) / 0.15f, 0f, 1f);

                // write white with alpha = luminance
                dst.drawPixel(x, y, Color.rgba8888(0f, 0f, 0f, lum));
            }
        }
        Texture t = new Texture(dst);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        src.dispose();
        dst.dispose();
        return t;
    }
}
