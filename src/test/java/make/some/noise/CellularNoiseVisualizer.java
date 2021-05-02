package make.some.noise;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import static com.badlogic.gdx.Input.Keys.*;
import static com.badlogic.gdx.graphics.GL20.GL_POINTS;

/**
 */
public class CellularNoiseVisualizer extends ApplicationAdapter {

    private int dim = 0; // this can be 0, or 1; these correspond to 2D and 3D
    private int octaves = 1;
    private boolean inverse = false;
    private ImmediateModeRenderer20 renderer;
    private Noise noise = new Noise(1, 1f/32f, Noise.CELLULAR, 1);
    private Noise turbulence = new Noise(1337, 1f/11f, Noise.PERLIN_FRACTAL, 2);
    private int cellType = 0;
    private int cellDistance = 0;
    private Noise cellLookup = new Noise(-1, 1f/32f, Noise.SIMPLEX_FRACTAL, 2);
    private boolean threshold = false;
    
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
        
        noise.setCellularReturnType(cellType);
        noise.setCellularDistanceFunction(cellDistance);
        noise.setCellularNoiseLookup(cellLookup);

        input = new InputAdapter(){
            @Override
            public boolean keyDown(int keycode) {
                switch (keycode) {
                    case MINUS:
                        if(dim <= 1)
                            noise.setNoiseType((noise.getNoiseType() + 11) % 12);
                        else
                            noise.setNoiseType((noise.getNoiseType() + 9) % 10);
                        putMap();
                        break;
                    case EQUALS:
                    case PLUS:
                    case N: // noise type
                        if(dim <= 1) 
                            noise.setNoiseType((noise.getNoiseType() + 1) % 12);
                        else
                            noise.setNoiseType((noise.getNoiseType() + 1) % 10);
                        putMap();
                        break;
                    case U:
                        ctr++;
                        putMap();
                        break;
                    case P: //pause
                        keepGoing = !keepGoing;
                        break;
                    case C:
                        noise.setCellularReturnType(cellType = cellType + 1 & 7);
                        putMap();
                        break;
                    case M:
                        noise.setCellularDistanceFunction(cellDistance = (cellDistance + 1) % 3);
                        putMap();
                        break;
                    case O:
                        cellLookup.setFrequency(Noise.sin((TimeUtils.millis() & 0xFFFFFL) * 0x1p-11f) * 0.11f + 0.17f);
                        putMap();
                        break;
                    case E: //earlier seed
                        noise.setSeed(noise.getSeed() - 1);
                        putMap();
                        break;
                    case S: //seed
                        noise.setSeed(noise.getSeed() + 1);
                        putMap();
                        break;
                    case D: //dimension
                        if(dim == 1 && noise.getNoiseType() >= 10)
                        {
                            noise.setNoiseType(0);
                        }
                        dim = (dim + 1) & 1;
                        putMap();
                        break;
                    case F: // frequency
                        noise.setFrequency(Noise.sin((TimeUtils.millis() & 0xFFFFFL) * 0x1p-11f) * 0.11f + 0.17f);
                        putMap();
                        break;
                    case R: // fRactal type
                        noise.setFractalType((noise.getFractalType() + 1) % 3);
                        putMap();
                        break;
                    case H: // higher octaves
                        noise.setFractalOctaves((octaves = octaves + 1 & 3) + 1);
                        putMap();
                        break;
                    case L: // lower octaves
                        noise.setFractalOctaves((octaves = octaves + 3 & 3) + 1);
                        putMap();
                        break;
                    case I: // inverse mode
                        if (inverse = !inverse) {
                            noise.setFractalLacunarity(0.5f);
                            noise.setFractalGain(2f);
                        } else {
                            noise.setFractalLacunarity(2f);
                            noise.setFractalGain(0.5f);
                        }
                        putMap();
                        break;
                    case T:
                        threshold = !threshold;
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

    private static final float WHITE = Color.WHITE_FLOAT_BITS, BLACK = Color.BLACK.toFloatBits();
    public void putMap() {
//        noise.setFrequency(Noise.sin((TimeUtils.millis() & 0xFFFFFL) * 0x1p-12f) * 0.03125f + 0.0625f);
//        red.setFrequency(noise.getFrequency());
//        green.setFrequency(noise.getFrequency());
//        blue.setFrequency(noise.getFrequency());

        renderer.begin(view.getCamera().combined, GL_POINTS);
        
        if(threshold){
            switch (dim){
                case 0:
                    for (int x = 0; x < 512; x++) {
                        for (int y = 0; y < 512; y++) {
                            renderer.color(noise.getConfiguredNoise(x + ctr * 0.25f, y + ctr * 0.25f) >= 0f ? WHITE : BLACK);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 1:
                    for (int x = 0; x < 512; x++) {
                        for (int y = 0; y < 512; y++) {
                            renderer.color(noise.getConfiguredNoise(x, y, ctr) >= 0f ? WHITE : BLACK);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
//                case 2:
//                    for (int x = 0; x < 512; x++) {
//                        for (int y = 0; y < 512; y++) {
//                            renderer.color(noise.getConfiguredNoise(x, y, ctr, 0x1p-4f * (x + y - ctr)) >= 0f ? WHITE : BLACK);
//                            renderer.vertex(x, y, 0);
//                        }
//                    }
//                    break;
//                case 3:
//                    float xx, yy;
//                    for (int x = 0; x < 512; x++) {
//                        xx = x * 0x1p-4f;
//                        for (int y = 0; y < 512; y++) {
//                            yy = y * 0x1p-4f;
//                            renderer.color(noise.getConfiguredNoise(
//                                    ctr + xx, x + ctr, y - ctr,
//                                    ctr - yy, x +  yy, y - xx) >= 0f ? WHITE : BLACK);
//                            renderer.vertex(x, y, 0);
//                        }
//                    }
//                    break;
//                
            }
        }
        else 
        {
            float bright, t;
            switch (dim) {
                case 0:
                    for (int x = 0; x < 512; x++) {
                        for (int y = 0; y < 512; y++) {
                            t = turbulence.getConfiguredNoise(x, y) * 8f;
                            bright = basicPrepare(Math.min(noise.getConfiguredNoise(x + ctr + t, y + ctr + t)
                                    , cellLookup.getConfiguredNoise(x + ctr, y + ctr)));
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 1:
                    for (int x = 0; x < 512; x++) {
                        for (int y = 0; y < 512; y++) {
                            t = turbulence.getConfiguredNoise(x, y, ctr) * 8f;
                            bright = basicPrepare(Math.min(noise.getConfiguredNoise(x + t, y + t, ctr)
                                    , cellLookup.getConfiguredNoise(x, y, ctr)));
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
//                case 2:
//                    for (int x = 0; x < 512; x++) {
//                        for (int y = 0; y < 512; y++) {
//                            bright = basicPrepare(noise.getConfiguredNoise(x, y, ctr, 0x1p-4f * (x + y - ctr)));
//                            renderer.color(bright, bright, bright, 1f);
//                            renderer.vertex(x, y, 0);
//                        }
//                    }
//                    break;
//                case 3:
//                    float xx, yy;
//                    for (int x = 0; x < 512; x++) {
//                        xx = x * 0x1p-4f;
//                        for (int y = 0; y < 512; y++) {
//                            yy = y * 0x1p-4f;
//                            bright = basicPrepare(noise.getConfiguredNoise(
//                                    ctr + xx, x + ctr, y - ctr,
//                                    ctr - yy, x +  yy, y - xx));
//                            renderer.color(bright, bright, bright, 1f);
//                            renderer.vertex(x, y, 0);
//                        }
//                    }
//                    break;
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
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Make Some Noise");
        config.setWindowedMode(width, height);
        config.setForegroundFPS(0);
        config.useVsync(false);
        config.setResizable(false);
        new Lwjgl3Application(new CellularNoiseVisualizer(), config);
    }
}
