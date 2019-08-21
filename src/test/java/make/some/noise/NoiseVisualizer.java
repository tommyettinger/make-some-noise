package make.some.noise;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import static com.badlogic.gdx.Input.Keys.*;
import static com.badlogic.gdx.graphics.GL20.GL_POINTS;

/**
 */
public class NoiseVisualizer extends ApplicationAdapter {

    private int dim = 0; // this can be 0, 1, 2, or 3; these correspond to 2D, 3D, 4D, and 6D
    private int octaves = 2;
    private float freq = 0.125f;
    private boolean inverse = false;
    private ImmediateModeRenderer20 renderer;
    private Noise noise = new Noise(1, 1f/32f, Noise.SIMPLEX_FRACTAL, 3);
    private boolean color = false;
    private Noise red = new Noise(11111, 1f/32f, Noise.SIMPLEX_FRACTAL, 1);
    private Noise green = new Noise(33333, 1f/32f, Noise.SIMPLEX_FRACTAL, 1);
    private Noise blue = new Noise(77777, 1f/32f, Noise.SIMPLEX_FRACTAL, 1);
    
    private static final int width = 512, height = 512;

    private InputAdapter input;
    
    private Viewport view;
    private int ctr = -256;
    private boolean keepGoing = true;

    public static float basicPrepare(float n)
    {
        return n * 0.5f + 0.5f;
    }

    @Override
    public void create() {
        // not sure if this is always needed...
        Gdx.gl.glDisable(GL20.GL_BLEND);
        renderer = new ImmediateModeRenderer20(width * height, false, true, 0);
        view = new ScreenViewport();

        input = new InputAdapter(){
            @Override
            public boolean keyDown(int keycode) {
                switch (keycode) {
                    case MINUS:
                        noise.setNoiseType((noise.getNoiseType() + 9) % 10);
                        red.setNoiseType(noise.getNoiseType());
                        green.setNoiseType(noise.getNoiseType());
                        blue.setNoiseType(noise.getNoiseType());
                        putMap();
                        break;
                    case EQUALS:
                    case PLUS:
                    case N: // noise type
                        noise.setNoiseType((noise.getNoiseType() + 1) % 10);
                        red.setNoiseType(noise.getNoiseType());
                        green.setNoiseType(noise.getNoiseType());
                        blue.setNoiseType(noise.getNoiseType());
                        putMap();
                        break;
                    case U:
                        ctr++;
                        putMap();
                        break;
                    case P: //pause
                        keepGoing = !keepGoing;
                    case C:
                        color = !color;
                        putMap();
                        break;
                    case E: //earlier seed
                        noise.setSeed(noise.getSeed() - 1);
                        red.setSeed(red.getSeed() - 1);
                        green.setSeed(green.getSeed() - 1);
                        blue.setSeed(blue.getSeed() - 1);
                        putMap();
                        break;
                    case S: //seed
                        noise.setSeed(noise.getSeed() + 1);
                        red.setSeed(red.getSeed() + 1);
                        green.setSeed(green.getSeed() + 1);
                        blue.setSeed(blue.getSeed() + 1);
                        putMap();
                        break;
                    case D: //dimension
                        dim = (dim + 1) & 3;
                        putMap();
                        break;
                    case F: // frequency
                        noise.setFrequency(Noise.sin(freq += 0.125f) * 0.15f + 0.17f);
                        red.setFrequency(noise.getFrequency());
                        green.setFrequency(noise.getFrequency());
                        blue.setFrequency(noise.getFrequency());
                        putMap();
                        break;
                    case R: // fRactal type
                        noise.setFractalType((noise.getFractalType() + 1) % 3);
                        red.setFractalType(noise.getFractalType());
                        green.setFractalType(noise.getFractalType());
                        blue.setFractalType(noise.getFractalType());
                        putMap();
                        break;
                    case H: // higher octaves
                        noise.setFractalOctaves((octaves = octaves + 1 & 3) + 1);
                        red.setFractalOctaves(noise.getFractalOctaves());
                        green.setFractalOctaves(noise.getFractalOctaves());
                        blue.setFractalOctaves(noise.getFractalOctaves());
                        putMap();
                        break;
                    case L: // lower octaves
                        noise.setFractalOctaves((octaves = octaves + 3 & 3) + 1);
                        red.setFractalOctaves(noise.getFractalOctaves());
                        green.setFractalOctaves(noise.getFractalOctaves());
                        blue.setFractalOctaves(noise.getFractalOctaves());
                        putMap();
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
                        putMap();
                        break;
                    case K: // sKip
                        ctr += 1000;
                        putMap();
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

    public void putMap() {
        renderer.begin(view.getCamera().combined, GL_POINTS);
        if(color)
        {
            switch (dim) {
                case 0:
                    for (int x = 0; x < 512; x++) {
                        for (int y = 0; y < 512; y++) {
                            renderer.color(basicPrepare(red.getConfiguredNoise(x + ctr, y + ctr)),
                                    basicPrepare(green.getConfiguredNoise(x + ctr, y + ctr)),
                                    basicPrepare(blue.getConfiguredNoise(x + ctr, y + ctr)), 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 1:
                    for (int x = 0; x < 512; x++) {
                        for (int y = 0; y < 512; y++) {
                            renderer.color(basicPrepare(red.getConfiguredNoise(x, y, ctr)),
                                    basicPrepare(green.getConfiguredNoise(x, y, ctr)),
                                    basicPrepare(blue.getConfiguredNoise(x, y, ctr)), 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 2:
                    for (int x = 0; x < 512; x++) {
                        for (int y = 0; y < 512; y++) {
                            renderer.color(basicPrepare(red.getConfiguredNoise(x, y, ctr, 0x1p-4f * (x + y - ctr))),
                                    basicPrepare(green.getConfiguredNoise(x, y, ctr, 0x1p-4f * (x + y - ctr))),
                                    basicPrepare(blue.getConfiguredNoise(x, y, ctr, 0x1p-4f * (x + y - ctr))), 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 3:
                    float xx, yy;
                    for (int x = 0; x < 512; x++) {
                        xx = x * 0x1p-4f;
                        for (int y = 0; y < 512; y++) {
                            yy = y * 0x1p-4f;
                            renderer.color(basicPrepare(red.getConfiguredNoise(
                                    ctr + xx, x + ctr, y - ctr,
                                    ctr - yy, x +  yy, y - xx)),
                                    basicPrepare(green.getConfiguredNoise(
                                            ctr + xx, x + ctr, y - ctr,
                                            ctr - yy, x +  yy, y - xx)),
                                    basicPrepare(blue.getConfiguredNoise(
                                            ctr + xx, x + ctr, y - ctr,
                                            ctr - yy, x +  yy, y - xx)), 1f);
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
                    for (int x = 0; x < 512; x++) {
                        for (int y = 0; y < 512; y++) {
                            bright = basicPrepare(noise.getConfiguredNoise(x + ctr, y + ctr));
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 1:
                    for (int x = 0; x < 512; x++) {
                        for (int y = 0; y < 512; y++) {
                            bright = basicPrepare(noise.getConfiguredNoise(x, y, ctr));
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 2:
                    for (int x = 0; x < 512; x++) {
                        for (int y = 0; y < 512; y++) {
                            bright = basicPrepare(noise.getConfiguredNoise(x, y, ctr, 0x1p-4f * (x + y - ctr)));
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 3:
                    float xx, yy;
                    for (int x = 0; x < 512; x++) {
                        xx = x * 0x1p-4f;
                        for (int y = 0; y < 512; y++) {
                            yy = y * 0x1p-4f;
                            bright = basicPrepare(noise.getConfiguredNoise(
                                    ctr + xx, x + ctr, y - ctr,
                                    ctr - yy, x +  yy, y - xx));
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
            ctr++;
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
        new LwjglApplication(new NoiseVisualizer(), config);
    }
}
