package org.jepoy.text;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class CharSheet {
    private final Texture texture;
    private final TextureRegion[] glyphs; // ASCII 32..126
    private final int first = 32, last = 126;
    public final float advance, lineHeight;

    public CharSheet(String path, int cellW, int cellH, int cols, int rows) {
        texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        TextureRegion[][] split = TextureRegion.split(texture, cellW, cellH);
        glyphs = new TextureRegion[last - first + 1];
        int idx = 0;
        outer:
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int code = first + idx;
                if (code > last) break outer;
                glyphs[idx++] = split[r][c];
            }
        }
        lineHeight = cellH;
        advance = cellW;  // monospaced; tweak if needed
    }

    public TextureRegion regionFor(char ch) {
        if (ch < first || ch > last) return null;
        return glyphs[ch - first];
    }

    public void dispose() { texture.dispose(); }
    public static void drawText(SpriteBatch batch, CharSheet sheet,
                                String text, float x, float y, float scale) {
        float cursorX = x;
        float cursorY = y; // baseline (LibGDX uses bottom-left origin)

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            // newline support
            if (ch == '\n') {
                cursorX = x;
                cursorY -= sheet.lineHeight * scale;
                continue;
            }

            if (ch == ' ') { // space advance
                cursorX += sheet.advance * scale;
                continue;
            }

            TextureRegion r = sheet.regionFor(ch);
            if (r != null) {
                float w = r.getRegionWidth() * scale;
                float h = r.getRegionHeight() * scale;
                batch.draw(r, cursorX, cursorY, w, h);
                cursorX += sheet.advance * scale;
            } else {
                // unknown char → small gap
                cursorX += sheet.advance * 0.5f * scale;
            }
        }
    }
}
