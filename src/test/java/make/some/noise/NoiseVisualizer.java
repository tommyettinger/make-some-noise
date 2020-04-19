package make.some.noise;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import static com.badlogic.gdx.Input.Keys.*;
import static com.badlogic.gdx.graphics.GL20.GL_POINTS;

/**
 */
public class NoiseVisualizer extends ApplicationAdapter {

    private int dim = 0; // this can be 0, 1, 2, or 3; these correspond to 2D, 3D, 4D, and 6D
    private int octaves = 2;
    private float freq = 0.125f, speed = 1f/6f;
    private boolean inverse = false;
    private ImmediateModeRenderer20 renderer;
    private Noise noise = new Noise(1, 1f/32f, Noise.SIMPLEX_FRACTAL, 3);
    private boolean color = false;
    private boolean threshold = false;
    private Noise red = new Noise(11111, 1f/32f, Noise.SIMPLEX_FRACTAL, 1);
    private Noise green = new Noise(33333, 1f/32f, Noise.SIMPLEX_FRACTAL, 1);
    private Noise blue = new Noise(77777, 1f/32f, Noise.SIMPLEX_FRACTAL, 1);
    
    private static final int width = 256, height = 256;

    private InputAdapter input;
    
    private Viewport view;
    private int ctr = -256;
    private long startTime;
    private boolean keepGoing = true;

    public static float basicPrepare(float n)
    {
        return n * 0.5f + 0.5f;
    }

    @Override
    public void create() {
        startTime = TimeUtils.millis();
        // not sure if this is always needed...
        Gdx.gl.glDisable(GL20.GL_BLEND);
        renderer = new ImmediateModeRenderer20(width * height, false, true, 0);
        view = new ScreenViewport();
        
        //noise.setInterpolation(Noise.QUINTIC);

        input = new InputAdapter(){
            @Override
            public boolean keyDown(int keycode) {
                switch (keycode) {
                case MINUS:
//                        if(dim <= 3)
                    noise.setNoiseType((noise.getNoiseType() + 11) % 12);
//                        else
//                            noise.setNoiseType((noise.getNoiseType() + 9) % 10);
                    red.setNoiseType(noise.getNoiseType());
                    green.setNoiseType(noise.getNoiseType());
                    blue.setNoiseType(noise.getNoiseType());
                    break;
                case EQUALS:
                case PLUS:
                case N: // noise type
//                        if(dim <= 3) 
                    noise.setNoiseType((noise.getNoiseType() + 1) % 12);
//                        else
//                            noise.setNoiseType((noise.getNoiseType() + 1) % 10);
                    red.setNoiseType(noise.getNoiseType());
                    green.setNoiseType(noise.getNoiseType());
                    blue.setNoiseType(noise.getNoiseType());
                    break;
                case U:
                    ctr++;
                    break;
                case P: //pause
                    keepGoing = !keepGoing;
                case C:
                    color = !color;
                    break;
                case E: //earlier seed
                    noise.setSeed(noise.getSeed() - 1);
                    red.setSeed(red.getSeed() - 1);
                    green.setSeed(green.getSeed() - 1);
                    blue.setSeed(blue.getSeed() - 1);
                    break;
                case S: //seed
                    noise.setSeed(noise.getSeed() + 1);
                    red.setSeed(red.getSeed() + 1);
                    green.setSeed(green.getSeed() + 1);
                    blue.setSeed(blue.getSeed() + 1);
                    break;
                case D: //dimension
//                        if(dim >= 2 && noise.getNoiseType() >= 10)
//                        {
//                            noise.setNoiseType(0);
//                            red.setNoiseType(0);
//                            green.setNoiseType(0);
//                            blue.setNoiseType(0);
//                        }
                    dim = (dim + 1) & 3;
                    break;
                case F: // frequency
                    noise.setFrequency(Noise.sin((TimeUtils.timeSinceMillis(startTime) & 0xFFFFFL) * 0x1p-11f) * 0.11f + 0.17f);
                    red.setFrequency(noise.getFrequency());
                    green.setFrequency(noise.getFrequency());
                    blue.setFrequency(noise.getFrequency());
                    break;
                case R: // fRactal type
                    noise.setFractalType((noise.getFractalType() + 1) % 3);
                    red.setFractalType(noise.getFractalType());
                    green.setFractalType(noise.getFractalType());
                    blue.setFractalType(noise.getFractalType());
                    break;
                case H: // higher octaves
                    noise.setFractalOctaves((octaves = octaves + 1 & 3) + 1);
                    red.setFractalOctaves(noise.getFractalOctaves());
                    green.setFractalOctaves(noise.getFractalOctaves());
                    blue.setFractalOctaves(noise.getFractalOctaves());
                    break;
                case L: // lower octaves
                    noise.setFractalOctaves((octaves = octaves + 3 & 3) + 1);
                    red.setFractalOctaves(noise.getFractalOctaves());
                    green.setFractalOctaves(noise.getFractalOctaves());
                    blue.setFractalOctaves(noise.getFractalOctaves());
                    break;
                case I: // inverse mode
                    if (inverse = !inverse) {
                        noise.setFractalLacunarity(0.5f);
                        noise.setFractalGain(2f);
                        red.setFractalLacunarity(0.5f);
                        red.setFractalGain(2f);
                        green.setFractalLacunarity(0.5f);
                        green.setFractalGain(2f);
                        blue.setFractalLacunarity(0.5f);
                        blue.setFractalGain(2f);
                    } else {
                        noise.setFractalLacunarity(2f);
                        noise.setFractalGain(0.5f);
                        red.setFractalLacunarity(2f);
                        red.setFractalGain(0.5f);
                        green.setFractalLacunarity(2f);
                        green.setFractalGain(0.5f);
                        blue.setFractalLacunarity(2f);
                        blue.setFractalGain(0.5f);
                    }
                    break;
                case T:
                    threshold = !threshold;
                    break;
                case K: // sKip
                    ctr += 1000;
                    break;
                case A: // Acceleration
                    speed = (1.5f + MathUtils.cosDeg(TimeUtils.timeSinceMillis(startTime))) * 0.5f;
                    break;
                case Q:
                case ESCAPE: {
                    Gdx.app.exit();
                    return true;
                }
                }
                return true;
            }
        };
        Gdx.input.setInputProcessor(input);
    }

    private static final float WHITE = Color.WHITE_FLOAT_BITS, BLACK = Color.BLACK.toFloatBits();
    public void putMap() {
//        noise.setFrequency(Noise.sin((TimeUtils.millis() & 0xFFFFFL) * 0x1p-12f) * 0.03125f + 0.0625f);
//        red.setFrequency(noise.getFrequency());
//        green.setFrequency(noise.getFrequency());
//        blue.setFrequency(noise.getFrequency());

        renderer.begin(view.getCamera().combined, GL_POINTS);
        float c = (TimeUtils.timeSinceMillis(startTime) >>> 3 & 0xFFFFFL) * (speed / (dim * dim + 3)) + ctr;
        if(color)
        {
            switch (dim) {
                case 0:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            renderer.color(basicPrepare(red.getConfiguredNoise(x + c, y + c)),
                                    basicPrepare(green.getConfiguredNoise(x + c, y + c)),
                                    basicPrepare(blue.getConfiguredNoise(x + c, y + c)), 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 1:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            renderer.color(basicPrepare(red.getConfiguredNoise(x, y, c)),
                                    basicPrepare(green.getConfiguredNoise(x, y, c)),
                                    basicPrepare(blue.getConfiguredNoise(x, y, c)), 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 2:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            renderer.color(basicPrepare(red.getConfiguredNoise(x, y, c, 0x1p-4f * (x + y - c))),
                                    basicPrepare(green.getConfiguredNoise(x, y, c, 0x1p-4f * (x + y - c))),
                                    basicPrepare(blue.getConfiguredNoise(x, y, c, 0x1p-4f * (x + y - c))), 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 3:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            renderer.color(basicPrepare(red.getConfiguredNoise(
                                x + c, c - x, y + c,
                                y - c, x + y, x - y)),
                                    basicPrepare(green.getConfiguredNoise(
                                        x + c, c - x, y + c,
                                        y - c, x + y, x - y)),
                                    basicPrepare(blue.getConfiguredNoise(
                                        x + c, c - x, y + c,
                                        y - c, x + y, x - y)), 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
            }
        }
        else if(threshold){
            switch (dim){
                case 0:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            renderer.color(noise.getConfiguredNoise(x + c, y + c) >= 0f ? WHITE : BLACK);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 1:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            renderer.color(noise.getConfiguredNoise(x, y, c) >= 0f ? WHITE : BLACK);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 2:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            renderer.color(noise.getConfiguredNoise(x, y, c, 0x1p-4f * (x + y - c)) >= 0f ? WHITE : BLACK);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 3:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            renderer.color(noise.getConfiguredNoise(
                                x + c, c - x, y + c,
                                y - c, x + y, x - y) >= 0f ? WHITE : BLACK);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                
            }
        }
        else 
        {
            float bright;
            switch (dim) {
                case 0:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            bright = basicPrepare(noise.getConfiguredNoise(x + c, y + c));
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 1:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            bright = basicPrepare(noise.getConfiguredNoise(x, y, c));
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 2:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            bright = basicPrepare(noise.getConfiguredNoise(x, y, c, 0x1p-4f * (x + y - c)));
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 3:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            bright = basicPrepare(noise.getConfiguredNoise(
                                    x + c, c - x, y + c,
                                    y - c, x + y, x - y));
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
            }
        }
        renderer.end();

    }

    @Override
    public void render() {
        Gdx.graphics.setTitle(String.valueOf(Gdx.graphics.getFramesPerSecond()));
        if (keepGoing) {
            // standard clear the background routine for libGDX
            //Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
            //Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            //ctr++;
            putMap();
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        view.update(width, height, true);
        view.apply(true);
    }

    public static void main(String[] arg) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "Make Some Noise";
        config.width = width;
        config.height = height;
        config.foregroundFPS = 0;
        config.vSyncEnabled = false;
        config.resizable = false;
        new LwjglApplication(new NoiseDemo(), config);
    }
}
