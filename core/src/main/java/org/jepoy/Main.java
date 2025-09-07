package org.jepoy;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import org.jepoy.states.GameStateMachine;
import org.jepoy.states.StartGameState;
import org.jepoy.text.CharSheet;

public class Main extends ApplicationAdapter {

    private GameContext ctx;

    @Override
    public void create() {
        OrthographicCamera camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        FitViewport viewport = new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), camera);
        camera.setToOrtho(false);
        ShapeRenderer shapeRenderer = new ShapeRenderer();
        shapeRenderer.setAutoShapeType(true);
        SpriteBatch spriteBatch = new SpriteBatch();
        GameStateMachine gameStateMachine = new GameStateMachine();

        Texture fontTex = CharSheet.loadFontAsAlpha("dungeon-mode.png");
        String[] LAYOUT = {
                "················", // 0  (icons)
                "················",
                ".,!?-+/\\()*:[_]^",// 1  (icons)
                "0123456789·;<=>%", // 2  digits + punctuation
                "@ABCDEFGHIJKLMNO", // 4  uppercase (starts with '@')
                "PQRSTUVWXYZ`?~$&",// 5  uppercase tail + brackets
                "#abcdefghijklmno", // 6  lowercase
                "pqrstuvwxyz\"?···", // 7  lowercase tail + braces/tilde
                "················", // 8  (tiles/icons)
                "················", // 9
                "················", // 10
                "················", // 11
                "················", // 12
                "················", // 13
                "················", // 14
                "················"  // 15
        };

        CharSheet charSheet = CharSheet.fromGrid(
                fontTex,
                8, 8,        // cellW, cellH
                LAYOUT,
                8,           // lineHeight
                7,           // defaultAdvance
                4            // spaceAdvance
        );



        // optional tidy-ups
        charSheet.alias('“','"').alias('”','"').alias('‘','\'').alias('’','\'').alias('—','-').alias('…','.');
        charSheet.setAdvance('.', 5).setAdvance(',', 5).setAdvance('!', 6).setAdvance('i', 6);


        Music emptyMusic = Gdx.audio.newMusic(Gdx.files.internal("sounds/heavenly_night.mp3"));
        ctx = new GameContext(gameStateMachine, camera, viewport, spriteBatch, shapeRenderer, charSheet, emptyMusic);
        gameStateMachine.push(new StartGameState(ctx));
    }

    @Override
    public void render () {
        ctx.getCamera().update();
        ctx.getGsm().update(Gdx.graphics.getDeltaTime());

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        ctx.getBatch().setProjectionMatrix(ctx.getCamera().combined);
        ctx.getBatch().begin();
        ctx.getGsm().render();
        ctx.getBatch().end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        ctx.getShapes().begin();
        ctx.getGsm().shapeRender();
        ctx.getShapes().end();
    }

    @Override
    public void dispose() {
        ctx.getBatch().dispose();
        ctx.getShapes().dispose();
    }
}
