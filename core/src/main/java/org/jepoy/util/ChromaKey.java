package org.jepoy.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;

// ChromaKey.java
public final class ChromaKey {
    private ChromaKey() {}

    /** Remove pixels similar to the color at (keyX,keyY). tol ~ 0.01..0.03 (0..1 range). */
    public static Texture loadKeyTransparent(String internalPath, int keyX, int keyY,
                                             float tol, boolean nearest) {
        Pixmap src = new Pixmap(Gdx.files.internal(internalPath));
        Color key = new Color();
        Color.rgba8888ToColor(key, src.getPixel(Math.max(0, Math.min(keyX, src.getWidth()-1)),
                Math.max(0, Math.min(keyY, src.getHeight()-1))));

        Pixmap out = new Pixmap(src.getWidth(), src.getHeight(), Pixmap.Format.RGBA8888);
        Pixmap.Blending old = src.getBlending();
        src.setBlending(Pixmap.Blending.None);

        Color c = new Color();
        float tol2 = tol * tol; // compare squared distance in RGB
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                Color.rgba8888ToColor(c, src.getPixel(x, y));
                float dr = c.r - key.r, dg = c.g - key.g, db = c.b - key.b;
                if (dr*dr + dg*dg + db*db <= tol2) c.a = 0f; // remove only background color
                out.drawPixel(x, y, Color.rgba8888(c));
            }
        }

        Texture tex = new Texture(out);
        if (nearest) tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        src.setBlending(old);
        src.dispose(); out.dispose();
        return tex;
    }
}
