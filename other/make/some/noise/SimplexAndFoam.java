// This file was originally from https://github.com/Auburns/FastNoise_Java as:
// FastNoise.java
//
// MIT License
//
// Copyright(c) 2017 Jordan Peck
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
// The developer's email is jorzixdan.me2@gzixmail.com (for great email, take
// off every 'zix'.)
//

package make.some.noise;

import java.io.Serializable;

/**
 * Simplex and Foam Noise, with options, that can be called from one configurable object. Originally from Jordan Peck's
 * FastNoise library, the implementation here is meant to be fast without sacrificing quality. Usage requires a Noise
 * object, which can be {@link #instance} in simple cases and otherwise should be constructed with whatever adjustment
 * is needed. You can choose Simplex (Ken Perlin's later creation, which is a reasonable default), Value (a simple noise
 * with lots of artifacts), or Foam (a slower but higher-detail kind of noise built using Value noise). All of these
 * have fractal variants that allow layering multiple frequencies of noise. After construction you can set how a fractal
 * variant is layered using {@link #setFractalType(int)}, with {@link #FBM} as the normal mode and {@link #RIDGED_MULTI}
 * as a not-uncommon way of altering the form noise takes. This supports 2D, 3D, and 4D fully.
 */
public class SimplexAndFoam implements Serializable {
    private static final long serialVersionUID = 2L;
    public static final int SIMPLEX = 0, SIMPLEX_FRACTAL = 1, 
            FOAM = 2, FOAM_FRACTAL = 3,
            VALUE = 4, VALUE_FRACTAL = 5;
    public static final int LINEAR = 0, HERMITE = 1, QUINTIC = 2;

    public static final int FBM = 0, BILLOW = 1, RIDGED_MULTI = 2;
    
    private int seed;
    private float frequency = 0.03125f;
    private int interpolation = HERMITE;
    private int noiseType = SIMPLEX_FRACTAL;

    private int octaves = 1;
    private float lacunarity = 2f;
    private float gain = 0.5f;
    private int fractalType = FBM;

    private float fractalBounding;
    
    // bunch of constants, mostly used by Simplex
    public static final float F2f = 0.3660254f;
    public static final float G2f = 0.21132487f;
    public static final float H2f = 0.42264974f;

    private static final float F3 = (1f / 3f);
    private static final float G3 = (1f / 6f);
    private static final float G33 = -0.5f;

    private static final float F4 = (float) ((2.23606797 - 1.0) / 4.0);
    private static final float G4 = (float) ((5.0 - 2.23606797) / 20.0);
    /**
     * This allows some slightly better lookup of the properties of the 4D simplex shape.
     */
    private static final int[] SIMPLEX_4D = {0, 1, 3, 7, 0, 1, 7, 3,
            0, 0, 0, 0, 0, 3, 7, 1, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 3, 7, 0, 0, 3, 1, 7, 0, 0, 0, 0,
            0, 7, 1, 3, 0, 7, 3, 1, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 7, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 1, 3, 0, 7, 0, 0, 0, 0,
            1, 7, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            3, 7, 0, 1, 3, 7, 1, 0, 1, 0, 3, 7, 1, 0, 7, 3,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 7, 1,
            0, 0, 0, 0, 3, 1, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 1, 7, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 1, 3, 7, 0, 3, 1,
            0, 0, 0, 0, 7, 1, 3, 0, 3, 1, 0, 7, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 7, 1, 0, 3, 0, 0, 0, 0,
            7, 3, 0, 1, 7, 3, 1, 0};
    
    /**
     * A publicly available Noise object with seed 1337, frequency 1.0f/32.0f, 1 octave of Simplex noise using
     * SIMPLEX_FRACTAL noiseType, 2f lacunarity and 0.5f gain. It's encouraged to use methods that temporarily configure
     * this variable, like {@link #getNoiseWithSeed(float, float, int)} rather than changing its settings and using a
     * method that needs that lasting configuration, like {@link #getConfiguredNoise(float, float)}.
     */
    public static final SimplexAndFoam instance = new SimplexAndFoam();
    /**
     * A constructor that takes no parameters, and uses all default settings with a seed of 1337. An example call to
     * this would be {@code new Noise()}, which makes noise with the seed 1337, a default frequency of 1.0f/32.0f, 1
     * octave of Simplex noise (since this doesn't specify octave count, it always uses 1 even for the 
     * SIMPLEX_FRACTAL noiseType this uses, but you can call {@link #setFractalOctaves(int)} later to benefit from the
     * fractal noiseType), and normal lacunarity and gain (when unspecified, they are 2f and 0.5f).
     */
    public SimplexAndFoam() {
        this(1337);
    }

    /**
     * A constructor that takes only a parameter for the Noise's seed, which should produce different results for
     * any different seeds. An example call to this would be {@code new Noise(1337)}, which makes noise with the
     * seed 1337, a default frequency of 1.0f/32.0f, 1 octave of Simplex noise (since this doesn't specify octave count,
     * it always uses 1 even for the SIMPLEX_FRACTAL noiseType this uses, but you can call
     * {@link #setFractalOctaves(int)} later to benefit from the fractal noiseType), and normal lacunarity and gain
     * (when unspecified, they are 2f and 0.5f).
     * @param seed the int seed for the noise, which should significantly affect the produced noise
     */
    public SimplexAndFoam(int seed) {
        this.seed = seed;
        calculateFractalBounding();
    }
    /**
     * A constructor that takes two parameters to specify the Noise from the start. An example call to this
     * would be {@code new Noise(1337, 0.02f)}, which makes noise with the seed 1337, a lower
     * frequency, 1 octave of Simplex noise (since this doesn't specify octave count, it always uses 1 even for the 
     * SIMPLEX_FRACTAL noiseType this uses, but you can call {@link #setFractalOctaves(int)} later to benefit from the
     * fractal noiseType), and normal lacunarity and gain (when unspecified, they are 2f and 0.5f).
     * @param seed the int seed for the noise, which should significantly affect the produced noise
     * @param frequency the multiplier for all dimensions, which is usually fairly small (1.0f/32.0f is the default)
     */
    public SimplexAndFoam(int seed, float frequency)
    {
        this(seed, frequency, SIMPLEX_FRACTAL, 1, 2f, 0.5f);
    }
    /**
     * A constructor that takes a few parameters to specify the Noise from the start. An example call to this
     * would be {@code new Noise(1337, 0.02f, Noise.SIMPLEX)}, which makes noise with the seed 1337, a lower
     * frequency, 1 octave of Simplex noise (since this doesn't specify octave count, it always uses 1 even for 
     * noiseTypes like SIMPLEX_FRACTAL, but using a fractal noiseType can make sense if you call
     * {@link #setFractalOctaves(int)} later), and normal lacunarity and gain (when unspecified, they are 2f and 0.5f).
     * @param seed the int seed for the noise, which should significantly affect the produced noise
     * @param frequency the multiplier for all dimensions, which is usually fairly small (1.0f/32.0f is the default)
     * @param noiseType the noiseType, which should be a constant from this class (see {@link #setNoiseType(int)})
     */
    public SimplexAndFoam(int seed, float frequency, int noiseType)
    {
        this(seed, frequency, noiseType, 1, 2f, 0.5f);
    }

    /**
     * A constructor that takes several parameters to specify the Noise from the start. An example call to this
     * would be {@code new Noise(1337, 0.02f, Noise.SIMPLEX_FRACTAL, 4)}, which makes noise with the seed 1337, a lower
     * frequency, 4 octaves of Simplex noise, and normal lacunarity and gain (when unspecified, they are 2f and 0.5f).
     * @param seed the int seed for the noise, which should significantly affect the produced noise
     * @param frequency the multiplier for all dimensions, which is usually fairly small (1.0f/32.0f is the default)
     * @param noiseType the noiseType, which should be a constant from this class (see {@link #setNoiseType(int)})
     * @param octaves how many octaves of noise to use when the noiseType is one of the _FRACTAL types
     */
    public SimplexAndFoam(int seed, float frequency, int noiseType, int octaves)
    {
        this(seed, frequency, noiseType, octaves, 2f, 0.5f);
    }

    /**
     * A constructor that takes a lot of parameters to specify the Noise from the start. An example call to this
     * would be {@code new Noise(1337, 0.02f, Noise.SIMPLEX_FRACTAL, 4, 0.5f, 2f)}, which makes noise with a
     * lower frequency, 4 octaves of Simplex noise, and the "inverse" effect on how those octaves work (which makes
     * the extra added octaves be more significant to the final result and also have a lower frequency, while normally
     * added octaves have a higher frequency and tend to have a minor effect on the large-scale shape of the noise).
     * @param seed the int seed for the noise, which should significantly affect the produced noise
     * @param frequency the multiplier for all dimensions, which is usually fairly small (1.0f/32.0f is the default)
     * @param noiseType the noiseType, which should be a constant from this class (see {@link #setNoiseType(int)})
     * @param octaves how many octaves of noise to use when the noiseType is one of the _FRACTAL types
     * @param lacunarity typically 2.0, or 0.5 to change how extra octaves work (inverse mode) 
     * @param gain typically 0.5, or 2.0 to change how extra octaves work (inverse mode)
     */
    public SimplexAndFoam(int seed, float frequency, int noiseType, int octaves, float lacunarity, float gain)
    {
        this.seed = seed;
        this.frequency = frequency;
        this.noiseType = noiseType;
        this.octaves = octaves;
        this.lacunarity = lacunarity;
        this.gain = gain;
        calculateFractalBounding();
    }
    
    /**
     * @return Returns the seed used by this object
     */
    public int getSeed() {
        return seed;
    }

    // Sets
    // Default: 1337L

    /**
     * Sets the seed used for all noise types, as a long.
     * If this is not called, defaults to 1337L.
     * @param seed a seed as a long
     */
    public void setSeed(int seed) {
        this.seed = seed;
    }

    /**
     * Sets the frequency for all noise types. If this is not called, it defaults to 0.03125f (or 1f/32f).
     * @param frequency the frequency for all noise types, as a positive non-zero float
     */
    public void setFrequency(float frequency) {
        this.frequency = frequency;
    }

    /**
     * Gets the frequency for all noise types. The default is 0.03125f, or 1f/32f.
     * @return the frequency for all noise types, which should be a positive non-zero float
     */
    public float getFrequency()
    {
        return frequency;
    }

    /**
     * Changes the interpolation method used to smooth between noise values, using on of the following constants from
     * this class (lowest to highest quality): {@link #LINEAR} (0), {@link #HERMITE} (1), or {@link #QUINTIC} (2). If
     * this is not called, it defaults to HERMITE. This is used in Value Noise and Foam Noise.
     * @param interpolation an int (0, 1, or 2) corresponding  to a constant from this class for an interpolation level
     */
    public void setInterpolation(int interpolation) {
        this.interpolation = interpolation;
    }

    /**
     * Sets the default type of noise returned by {@link #getConfiguredNoise(float, float)}, using one of the following constants
     * in this class:
     * {@link #SIMPLEX} (0), {@link #SIMPLEX_FRACTAL} (1), {@link #FOAM} (2), {@link #FOAM_FRACTAL} (3), {@link #VALUE}
     * (4), or {@link #VALUE_FRACTAL} (5). If this isn't called, getConfiguredNoise() will default to SIMPLEX_FRACTAL.
     * @param noiseType an int from 0 to 5 corresponding to a constant from this class for a noise type
     */
    public void setNoiseType(int noiseType) {
        this.noiseType = noiseType;
    }

    /**
     * Gets the default type of noise returned by {@link #getConfiguredNoise(float, float)}, using one of the following constants
     * in this class:
     * {@link #SIMPLEX} (0), {@link #SIMPLEX_FRACTAL} (1), {@link #FOAM} (2), {@link #FOAM_FRACTAL} (3), {@link #VALUE}
     * (4), or {@link #VALUE_FRACTAL} (5). The default is SIMPLEX_FRACTAL.
     * @return the noise type as a code, from 0 to 5 inclusive
     */
    public int getNoiseType()
    {
        return noiseType;
    }

    /**
     * Sets the octave count for all fractal noise types.
     * If this isn't called, it will default to 3.
     * @param octaves the number of octaves to use for fractal noise types, as a positive non-zero int
     */
    public void setFractalOctaves(int octaves) {
        this.octaves = octaves;
        calculateFractalBounding();
    }

    /**
     * Gets the octave count for all fractal noise types. The default is 3.
     * @return the number of octaves to use for fractal noise types, as a positive non-zero int
     */
    public int getFractalOctaves()
    {
        return octaves;
    }

    /**
     * Sets the octave lacunarity for all fractal noise types.
     * Lacunarity is a multiplicative change to frequency between octaves. If this isn't called, it defaults to 2.
     * @param lacunarity a non-0 float that will be used for the lacunarity of fractal noise types; commonly 2.0 or 0.5
     */
    public void setFractalLacunarity(float lacunarity) {
        this.lacunarity = lacunarity;
    }

    /**
     * Sets the octave gain for all fractal noise types.
     * If this isn't called, it defaults to 0.5.
     * @param gain the gain between octaves, as a float
     */
    public void setFractalGain(float gain) {
        this.gain = gain;
        calculateFractalBounding();
    }

    /**
     * Sets the method for combining octaves in all fractal noise types, allowing an int argument corresponding to one
     * of the following constants from this class: {@link #FBM} (0), {@link #BILLOW} (1), or {@link #RIDGED_MULTI} (2).
     * If this hasn't been called, it will use FBM.
     * @param fractalType an int (0, 1, or 2) that corresponds to a constant like {@link #FBM} or {@link #RIDGED_MULTI}
     */
    public void setFractalType(int fractalType) {
        this.fractalType = fractalType;
    }

    /**
     * Gets the method for combining octaves in all fractal noise types, allowing an int argument corresponding to one
     * of the following constants from this class: {@link #FBM} (0), {@link #BILLOW} (1), or {@link #RIDGED_MULTI} (2).
     * The default is FBM.     
     * @return the fractal type as a code; 0, 1, or 2
     */
    public int getFractalType()
    {
        return fractalType;
    }
    
    public float getNoiseWithSeed(float x, float y, int seed) {
        final int s = this.seed;
        this.seed = seed;
        float r = getConfiguredNoise(x, y);
        this.seed = s;
        return r;
    }
    public float getNoiseWithSeed(float x, float y, float z, int seed) {
        final int s = this.seed;
        this.seed = seed;
        float r = getConfiguredNoise(x, y, z);
        this.seed = s;
        return r;
    }
    public float getNoiseWithSeed(float x, float y, float z, float w, int seed) {
        final int s = this.seed;
        this.seed = seed;
        float r = getConfiguredNoise(x, y, z, w);
        this.seed = s;
        return r;
    }

    private static int fastFloor(float f) {
        return (f >= 0 ? (int) f : (int) f - 1);
    }
    
    
    private static float hermiteInterpolator(float t) {
        return t * t * (3 - 2 * t);
    }


    private static float quinticInterpolator(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    
    private void calculateFractalBounding() {
        float amp = gain;
        float ampFractal = 1;
        for (int i = 1; i < octaves; i++) {
            ampFractal += amp;
            amp *= gain;
        }
        fractalBounding = 1 / ampFractal;
    }

    protected static float gradCoord2D(int seed, int x, int y, float xd, float yd) {
        final Float2 g = phiGrad2f[hash256(x, y, seed)];
        return xd * g.x + yd * g.y;
    }

    protected static float gradCoord3D(int seed, int x, int y, int z, float xd, float yd, float zd) {
        final Float3 g = GRAD_3D[hash32(x, y, z, seed)];
        return xd * g.x + yd * g.y + zd * g.z;
    }

    protected static float gradCoord4D(int seed, int x, int y, int z, int w, float xd, float yd, float zd, float wd) {
        final int hash = hash256(x, y, z, w, seed) & 0xFC;
        return xd * grad4f[hash] + yd * grad4f[hash + 1] + zd * grad4f[hash + 2] + wd * grad4f[hash + 3];
    }

    /**
     * After being configured with the setters in this class, such as {@link #setNoiseType(int)},
     * {@link #setFrequency(float)}, {@link #setFractalOctaves(int)}, and {@link #setFractalType(int)}, among others,
     * you can call this method to get the particular variety of noise you specified, in 2D.
     *
     * @param x
     * @param y
     * @return noise as a float from -1f to 1f
     */
    public float getConfiguredNoise (float x, float y) {
        x *= frequency;
        y *= frequency;

        switch (noiseType) {
        case VALUE:
            return singleValue(seed, x, y);
        case VALUE_FRACTAL:
            switch (fractalType) {
            case BILLOW:
                return singleValueFractalBillow(x, y);
            case RIDGED_MULTI:
                return singleValueFractalRidgedMulti(x, y);
            default:
                return singleValueFractalFBM(x, y);
            }
        case FOAM:
            return singleFoam(seed, x, y);
        case FOAM_FRACTAL:
            switch (fractalType) {
            case BILLOW:
                return singleFoamFractalBillow(x, y);
            case RIDGED_MULTI:
                return singleFoamFractalRidgedMulti(x, y);
            default:
                return singleFoamFractalFBM(x, y);
            }
        case SIMPLEX_FRACTAL:
            switch (fractalType) {
            case BILLOW:
                return singleSimplexFractalBillow(x, y);
            case RIDGED_MULTI:
                return singleSimplexFractalRidgedMulti(x, y);
            default:
                return singleSimplexFractalFBM(x, y);
            }
        default:
            return singleSimplex(seed, x, y);
        }
    }

    /**
     * After being configured with the setters in this class, such as {@link #setNoiseType(int)},
     * {@link #setFrequency(float)}, {@link #setFractalOctaves(int)}, and {@link #setFractalType(int)}, among others,
     * you can call this method to get the particular variety of noise you specified, in 3D.
     *
     * @param x
     * @param y
     * @param z
     * @return noise as a float from -1f to 1f
     */
    public float getConfiguredNoise (float x, float y, float z) {
        x *= frequency;
        y *= frequency;
        z *= frequency;

        switch (noiseType) {
        case VALUE:
            return singleValue(seed, x, y, z);
        case VALUE_FRACTAL:
            switch (fractalType) {
            case BILLOW:
                return singleValueFractalBillow(x, y, z);
            case RIDGED_MULTI:
                return singleValueFractalRidgedMulti(x, y, z);
            default:
                return singleValueFractalFBM(x, y, z);
            }
        case FOAM:
            return singleFoam(seed, x, y, z);
        case FOAM_FRACTAL:
            switch (fractalType) {
            case BILLOW:
                return singleFoamFractalBillow(x, y, z);
            case RIDGED_MULTI:
                return singleFoamFractalRidgedMulti(x, y, z);
            default:
                return singleFoamFractalFBM(x, y, z);
            }
        case SIMPLEX_FRACTAL:
            switch (fractalType) {
            case BILLOW:
                return singleSimplexFractalBillow(x, y, z);
            case RIDGED_MULTI:
                return singleSimplexFractalRidgedMulti(x, y, z);
            default:
                return singleSimplexFractalFBM(x, y, z);
            }
        default:
            return singleSimplex(seed, x, y, z);
        }
    }

    /**
     * After being configured with the setters in this class, such as {@link #setNoiseType(int)},
     * {@link #setFrequency(float)}, {@link #setFractalOctaves(int)}, and {@link #setFractalType(int)}, among others,
     * you can call this method to get the particular variety of noise you specified, in 3D.
     *
     * @param x
     * @param y
     * @param z
     * @param w
     * @return noise as a float from -1f to 1f
     */
    public float getConfiguredNoise (float x, float y, float z, float w) {
        x *= frequency;
        y *= frequency;
        z *= frequency;
        w *= frequency;

        switch (noiseType) {
        case VALUE:
            return singleValue(seed, x, y, z, w);
        case VALUE_FRACTAL:
            switch (fractalType) {
            case BILLOW:
                return singleValueFractalBillow(x, y, z, w);
            case RIDGED_MULTI:
                return singleValueFractalRidgedMulti(x, y, z, w);
            default:
                return singleValueFractalFBM(x, y, z, w);
            }
        case FOAM:
            return singleFoam(seed, x, y, z, w);
        case FOAM_FRACTAL:
            switch (fractalType) {
            case BILLOW:
                return singleFoamFractalBillow(x, y, z, w);
            case RIDGED_MULTI:
                return singleFoamFractalRidgedMulti(x, y, z, w);
            default:
                return singleFoamFractalFBM(x, y, z, w);
            }
        case SIMPLEX_FRACTAL:
            switch (fractalType) {
            case BILLOW:
                return singleSimplexFractalBillow(x, y, z, w);
            case RIDGED_MULTI:
                return singleSimplexFractalRidgedMulti(x, y, z, w);
            default:
                return singleSimplexFractalFBM(x, y, z, w);
            }
        default:
            return singleSimplex(seed, x, y, z, w);
        }
    }


    // Value Noise
    //x should be premultiplied by 0xD1B55
    //y should be premultiplied by 0xABC99
    private static int hashPart1024(final int x, final int y, int s) {
        s += x ^ y;
        return (s ^ (s << 19 | s >>> 13) ^ (s << 5 | s >>> 27) ^ 0xD1B54A35) * 0x125493 >> 22;
    }
    //x should be premultiplied by 0xDB4F1
    //y should be premultiplied by 0xBBE05
    //z should be premultiplied by 0xA0F2F
    private static int hashPart1024(final int x, final int y, final int z, int s) {
        s += x ^ y ^ z;
        return (s ^ (s << 19 | s >>> 13) ^ (s << 5 | s >>> 27) ^ 0xD1B54A35) * 0x125493 >> 22;
    }
    //x should be premultiplied by 0xE19B1
    //y should be premultiplied by 0xC6D1D
    //z should be premultiplied by 0xAF36D
    //w should be premultiplied by 0x9A695
    private static int hashPart1024(final int x, final int y, final int z, final int w, int s) {
        s += x ^ y ^ z ^ w;
        return (s ^ (s << 19 | s >>> 13) ^ (s << 5 | s >>> 27) ^ 0xD1B54A35) * 0x125493 >> 22;
    }


    public float getValueFractal(float x, float y) {
        x *= frequency;
        y *= frequency;

        switch (fractalType) {
        case FBM:
            return singleValueFractalFBM(x, y);
        case BILLOW:
            return singleValueFractalBillow(x, y);
        case RIDGED_MULTI:
            return singleValueFractalRidgedMulti(x, y);
        default:
            return 0;
        }
    }

    private float singleValueFractalFBM(float x, float y) {
        int seed = this.seed;
        float sum = singleValue(seed, x, y);
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;

            amp *= gain;
            sum += singleValue(++seed, x, y) * amp;
        }

        return sum * fractalBounding;
    }

    private float singleValueFractalBillow(float x, float y) {
        int seed = this.seed;
        float sum = Math.abs(singleValue(seed, x, y)) * 2 - 1;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;
            amp *= gain;
            sum += (Math.abs(singleValue(++seed, x, y)) * 2 - 1) * amp;
        }

        return sum * fractalBounding;
    }

    private float singleValueFractalRidgedMulti(float x, float y) {
        int seed = this.seed;
        float sum = 0, amp = 1, ampBias = 1f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(singleValue(seed + i, x, y));
            spike *= spike * amp;
            amp = Math.max(0f, Math.min(1f, spike * 2f));
            sum += (spike * ampBias);
            ampBias *= 2f;
            x *= lacunarity;
            y *= lacunarity;
        }
        return sum / ((ampBias - 1f) * 0.5f) - 1f;
    }

    public float getValue(float x, float y) {
        return singleValue(seed, x * frequency, y * frequency);
    }


    public float singleValue (int seed, float x, float y) {
        int xFloor = x >= 0 ? (int) x : (int) x - 1;
        x -= xFloor;
        int yFloor = y >= 0 ? (int) y : (int) y - 1;
        y -= yFloor;
        switch (interpolation) {
        case HERMITE:
            x = hermiteInterpolator(x);
            y = hermiteInterpolator(y);
            break;
        case QUINTIC:
            x = quinticInterpolator(x);
            y = quinticInterpolator(y);
            break;
        }
        xFloor *= 0xD1B55;
        yFloor *= 0xABC99;
        return ((1 - y) * ((1 - x) * hashPart1024(xFloor, yFloor, seed) + x * hashPart1024(xFloor + 0xD1B55, yFloor, seed))
            + y * ((1 - x) * hashPart1024(xFloor, yFloor + 0xABC99, seed) + x * hashPart1024(xFloor + 0xD1B55, yFloor + 0xABC99, seed)))
            * 0x1p-9f;
    }

    /**
     * Produces noise from 0 to 1, instead of the normal -1 to 1.
     * @param seed
     * @param x
     * @param y
     * @return noise from 0 to 1.
     */
    private static float valueNoise (int seed, float x, float y) {
        int xFloor = x >= 0 ? (int) x : (int) x - 1;
        x -= xFloor;
        x *= x * (3 - 2 * x);
        int yFloor = y >= 0 ? (int) y : (int) y - 1;
        y -= yFloor;
        y *= y * (3 - 2 * y);
        xFloor *= 0xD1B55;
        yFloor *= 0xABC99;
        return ((1 - y) * ((1 - x) * hashPart1024(xFloor, yFloor, seed) + x * hashPart1024(xFloor + 0xD1B55, yFloor, seed))
            + y * ((1 - x) * hashPart1024(xFloor, yFloor + 0xABC99, seed) + x * hashPart1024(xFloor + 0xD1B55, yFloor + 0xABC99, seed)))
            * 0x1p-10f + 0.5f;
    }
    public float getValueFractal(float x, float y, float z) {
        x *= frequency;
        y *= frequency;
        z *= frequency;

        switch (fractalType) {
        case BILLOW:
            return singleValueFractalBillow(x, y, z);
        case RIDGED_MULTI:
            return singleValueFractalRidgedMulti(x, y, z);
        default:
            return singleValueFractalFBM(x, y, z);
        }
    }

    private float singleValueFractalFBM(float x, float y, float z) {
        int seed = this.seed;
        float sum = singleValue(seed, x, y, z);
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;

            amp *= gain;
            sum += singleValue(++seed, x, y, z) * amp;
        }

        return sum * fractalBounding;
    }

    private float singleValueFractalBillow(float x, float y, float z) {
        int seed = this.seed;
        float sum = Math.abs(singleValue(seed, x, y, z)) * 2 - 1;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;

            amp *= gain;
            sum += (Math.abs(singleValue(++seed, x, y, z)) * 2 - 1) * amp;
        }

        return sum * fractalBounding;
    }

    private float singleValueFractalRidgedMulti(float x, float y, float z) {
        int seed = this.seed;
        float sum = 0, amp = 1, ampBias = 1f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(singleValue(seed + i, x, y, z));
            spike *= spike * amp;
            amp = Math.max(0f, Math.min(1f, spike * 2f));
            sum += (spike * ampBias);
            ampBias *= 2f;
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;
        }
        return sum / ((ampBias - 1f) * 0.5f) - 1f;
    }

    public float getValue(float x, float y, float z) {
        return singleValue(seed, x * frequency, y * frequency, z * frequency);
    }

    public float singleValue(int seed, float x, float y, float z) {
        int xFloor = x >= 0 ? (int) x : (int) x - 1;
        x -= xFloor;
        int yFloor = y >= 0 ? (int) y : (int) y - 1;
        y -= yFloor;
        int zFloor = z >= 0 ? (int) z : (int) z - 1;
        z -= zFloor;
        switch (interpolation) { 
        case HERMITE:
            x = hermiteInterpolator(x);
            y = hermiteInterpolator(y);
            z = hermiteInterpolator(z);
            break;
        case QUINTIC:
            x = quinticInterpolator(x);
            y = quinticInterpolator(y);
            z = quinticInterpolator(z);
            break;
        }
        //0xDB4F1, 0xBBE05, 0xA0F2F
        xFloor *= 0xDB4F1;
        yFloor *= 0xBBE05;
        zFloor *= 0xA0F2F;
        return ((1 - z) *
            ((1 - y) * ((1 - x) * hashPart1024(xFloor, yFloor, zFloor, seed) + x * hashPart1024(xFloor + 0xDB4F1, yFloor, zFloor, seed))
                + y * ((1 - x) * hashPart1024(xFloor, yFloor + 0xBBE05, zFloor, seed) + x * hashPart1024(xFloor + 0xDB4F1, yFloor + 0xBBE05, zFloor, seed)))
            + z *
            ((1 - y) * ((1 - x) * hashPart1024(xFloor, yFloor, zFloor + 0xA0F2F, seed) + x * hashPart1024(xFloor + 0xDB4F1, yFloor, zFloor + 0xA0F2F, seed))
                + y * ((1 - x) * hashPart1024(xFloor, yFloor + 0xBBE05, zFloor + 0xA0F2F, seed) + x * hashPart1024(xFloor + 0xDB4F1, yFloor + 0xBBE05, zFloor + 0xA0F2F, seed)))
        ) * 0x1p-9f;
    }
    /**
     * Produces noise from 0 to 1, instead of the normal -1 to 1.
     * @param seed
     * @param x
     * @param y
     * @param z 
     * @return noise from 0 to 1.
     */
    private static float valueNoise (int seed, float x, float y, float z)
    {
        int xFloor = x >= 0 ? (int) x : (int) x - 1;
        x -= xFloor;
        x *= x * (3 - 2 * x);
        int yFloor = y >= 0 ? (int) y : (int) y - 1;
        y -= yFloor;
        y *= y * (3 - 2 * y);
        int zFloor = z >= 0 ? (int) z : (int) z - 1;
        z -= zFloor;
        z *= z * (3 - 2 * z);
        //0xDB4F1, 0xBBE05, 0xA0F2F
        xFloor *= 0xDB4F1;
        yFloor *= 0xBBE05;
        zFloor *= 0xA0F2F;
        return ((1 - z) *
            ((1 - y) * ((1 - x) * hashPart1024(xFloor, yFloor, zFloor, seed) + x * hashPart1024(xFloor + 0xDB4F1, yFloor, zFloor, seed))
                + y * ((1 - x) * hashPart1024(xFloor, yFloor + 0xBBE05, zFloor, seed) + x * hashPart1024(xFloor + 0xDB4F1, yFloor + 0xBBE05, zFloor, seed)))
            + z *
            ((1 - y) * ((1 - x) * hashPart1024(xFloor, yFloor, zFloor + 0xA0F2F, seed) + x * hashPart1024(xFloor + 0xDB4F1, yFloor, zFloor + 0xA0F2F, seed))
                + y * ((1 - x) * hashPart1024(xFloor, yFloor + 0xBBE05, zFloor + 0xA0F2F, seed) + x * hashPart1024(xFloor + 0xDB4F1, yFloor + 0xBBE05, zFloor + 0xA0F2F, seed)))
        ) * 0x1p-10f + 0.5f;

    }
    public float getValueFractal(float x, float y, float z, float w) {
        x *= frequency;
        y *= frequency;
        z *= frequency;
        w *= frequency;

        switch (fractalType) {
        case BILLOW:
            return singleValueFractalBillow(x, y, z, w);
        case RIDGED_MULTI:
            return singleValueFractalRidgedMulti(x, y, z, w);
        default:
            return singleValueFractalFBM(x, y, z, w);
        }
    }

    private float singleValueFractalFBM(float x, float y, float z, float w) {
        int seed = this.seed;
        float sum = singleValue(seed, x, y, z, w);
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;
            w *= lacunarity;

            amp *= gain;
            sum += singleValue(++seed, x, y, z, w) * amp;
        }

        return sum * fractalBounding;
    }

    private float singleValueFractalBillow(float x, float y, float z, float w) {
        int seed = this.seed;
        float sum = Math.abs(singleValue(seed, x, y, z, w)) * 2 - 1;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;
            w *= lacunarity;

            amp *= gain;
            sum += (Math.abs(singleValue(++seed, x, y, z, w)) * 2 - 1) * amp;
        }

        return sum * fractalBounding;
    }

    private float singleValueFractalRidgedMulti(float x, float y, float z, float w) {
        int seed = this.seed;
        float sum = 0, amp = 1, ampBias = 1f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(singleValue(seed + i, x, y, z, w));
            spike *= spike * amp;
            amp = Math.max(0f, Math.min(1f, spike * 2f));
            sum += (spike * ampBias);
            ampBias *= 2f;
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;
            w *= lacunarity;
        }
        return sum / ((ampBias - 1f) * 0.5f) - 1f;
    }

    public float getValue(float x, float y, float z, float w) {
        return singleValue(seed, x * frequency, y * frequency, z * frequency, w * frequency);
    }

    private float singleValue(int seed, float x, float y, float z, float w) {
        int xFloor = x >= 0 ? (int) x : (int) x - 1;
        x -= xFloor;
        int yFloor = y >= 0 ? (int) y : (int) y - 1;
        y -= yFloor;
        int zFloor = z >= 0 ? (int) z : (int) z - 1;
        z -= zFloor;
        int wFloor = w >= 0 ? (int) w : (int) w - 1;
        w -= wFloor;
        switch (interpolation) {
        case HERMITE:
            x = hermiteInterpolator(x);
            y = hermiteInterpolator(y);
            z = hermiteInterpolator(z);
            w = hermiteInterpolator(w);
            break;
        case QUINTIC:
            x = quinticInterpolator(x);
            y = quinticInterpolator(y);
            z = quinticInterpolator(z);
            w = quinticInterpolator(w);
            break;
        }
        //0xE19B1, 0xC6D1D, 0xAF36D, 0x9A695
        xFloor *= 0xE19B1;
        yFloor *= 0xC6D1D;
        zFloor *= 0xAF36D;
        wFloor *= 0x9A695;
        return ((1 - w) *
            ((1 - z) *
                ((1 - y) * ((1 - x) * hashPart1024(xFloor, yFloor, zFloor, wFloor, seed) + x * hashPart1024(xFloor + 0xE19B1, yFloor, zFloor, wFloor, seed))
                    + y * ((1 - x) * hashPart1024(xFloor, yFloor + 0xC6D1D, zFloor, wFloor, seed) + x * hashPart1024(xFloor + 0xE19B1, yFloor + 0xC6D1D, zFloor, wFloor, seed)))
                + z *
                ((1 - y) * ((1 - x) * hashPart1024(xFloor, yFloor, zFloor + 0xAF36D, wFloor, seed) + x * hashPart1024(xFloor + 0xE19B1, yFloor, zFloor + 0xAF36D, wFloor, seed))
                    + y * ((1 - x) * hashPart1024(xFloor, yFloor + 0xC6D1D, zFloor + 0xAF36D, wFloor, seed) + x * hashPart1024(xFloor + 0xE19B1, yFloor + 0xC6D1D, zFloor + 0xAF36D, wFloor, seed))))
            + (w *
            ((1 - z) *
                ((1 - y) * ((1 - x) * hashPart1024(xFloor, yFloor, zFloor, wFloor + 0x9A695, seed) + x * hashPart1024(xFloor + 0xE19B1, yFloor, zFloor, wFloor + 0x9A695, seed))
                    + y * ((1 - x) * hashPart1024(xFloor, yFloor + 0xC6D1D, zFloor, wFloor + 0x9A695, seed) + x * hashPart1024(xFloor + 0xE19B1, yFloor + 0xC6D1D, zFloor, wFloor + 0x9A695, seed)))
                + z *
                ((1 - y) * ((1 - x) * hashPart1024(xFloor, yFloor, zFloor + 0xAF36D, wFloor + 0x9A695, seed) + x * hashPart1024(xFloor + 0xE19B1, yFloor, zFloor + 0xAF36D, wFloor + 0x9A695, seed))
                    + y * ((1 - x) * hashPart1024(xFloor, yFloor + 0xC6D1D, zFloor + 0xAF36D, wFloor + 0x9A695, seed) + x * hashPart1024(xFloor + 0xE19B1, yFloor + 0xC6D1D, zFloor + 0xAF36D, wFloor + 0x9A695, seed)))
            ))) * 0x1p-9f;
    }
    private static float valueNoise(int seed, float x, float y, float z, float w)
    {
        int xFloor = x >= 0 ? (int) x : (int) x - 1;
        x -= xFloor;
        x *= x * (3 - 2 * x);
        int yFloor = y >= 0 ? (int) y : (int) y - 1;
        y -= yFloor;
        y *= y * (3 - 2 * y);
        int zFloor = z >= 0 ? (int) z : (int) z - 1;
        z -= zFloor;
        z *= z * (3 - 2 * z);
        int wFloor = w >= 0 ? (int) w : (int) w - 1;
        w -= wFloor;
        w *= w * (3 - 2 * w);
        //0xE19B1, 0xC6D1D, 0xAF36D, 0x9A695
        xFloor *= 0xE19B1;
        yFloor *= 0xC6D1D;
        zFloor *= 0xAF36D;
        wFloor *= 0x9A695;
        return ((1 - w) *
            ((1 - z) *
                ((1 - y) * ((1 - x) * hashPart1024(xFloor, yFloor, zFloor, wFloor, seed) + x * hashPart1024(xFloor + 0xE19B1, yFloor, zFloor, wFloor, seed))
                    + y * ((1 - x) * hashPart1024(xFloor, yFloor + 0xC6D1D, zFloor, wFloor, seed) + x * hashPart1024(xFloor + 0xE19B1, yFloor + 0xC6D1D, zFloor, wFloor, seed)))
                + z *
                ((1 - y) * ((1 - x) * hashPart1024(xFloor, yFloor, zFloor + 0xAF36D, wFloor, seed) + x * hashPart1024(xFloor + 0xE19B1, yFloor, zFloor + 0xAF36D, wFloor, seed))
                    + y * ((1 - x) * hashPart1024(xFloor, yFloor + 0xC6D1D, zFloor + 0xAF36D, wFloor, seed) + x * hashPart1024(xFloor + 0xE19B1, yFloor + 0xC6D1D, zFloor + 0xAF36D, wFloor, seed))))
            + (w *
            ((1 - z) *
                ((1 - y) * ((1 - x) * hashPart1024(xFloor, yFloor, zFloor, wFloor + 0x9A695, seed) + x * hashPart1024(xFloor + 0xE19B1, yFloor, zFloor, wFloor + 0x9A695, seed))
                    + y * ((1 - x) * hashPart1024(xFloor, yFloor + 0xC6D1D, zFloor, wFloor + 0x9A695, seed) + x * hashPart1024(xFloor + 0xE19B1, yFloor + 0xC6D1D, zFloor, wFloor + 0x9A695, seed)))
                + z *
                ((1 - y) * ((1 - x) * hashPart1024(xFloor, yFloor, zFloor + 0xAF36D, wFloor + 0x9A695, seed) + x * hashPart1024(xFloor + 0xE19B1, yFloor, zFloor + 0xAF36D, wFloor + 0x9A695, seed))
                    + y * ((1 - x) * hashPart1024(xFloor, yFloor + 0xC6D1D, zFloor + 0xAF36D, wFloor + 0x9A695, seed) + x * hashPart1024(xFloor + 0xE19B1, yFloor + 0xC6D1D, zFloor + 0xAF36D, wFloor + 0x9A695, seed)))
            ))) * 0x1p-10f + 0.5f;
    }


    public float getFoamFractal(float x, float y) {
        x *= frequency;
        y *= frequency;

        switch (fractalType) {
        case FBM:
            return singleFoamFractalFBM(x, y);
        case BILLOW:
            return singleFoamFractalBillow(x, y);
        case RIDGED_MULTI:
            return singleFoamFractalRidgedMulti(x, y);
        default:
            return 0;
        }
    }
    
    private float singleFoamFractalFBM(float x, float y) {
        int seed = this.seed;
        float sum = singleFoam(seed, x, y);
        float amp = 1, t;

        for (int i = 1; i < octaves; i++) {
            t = x;
            x = y * lacunarity;
            y = t * lacunarity;

            amp *= gain;
            sum += singleFoam(seed + i, x, y) * amp;
        }

        return sum * fractalBounding;
    }

    private float singleFoamFractalBillow(float x, float y) {
        int seed = this.seed;
        float sum = Math.abs(singleFoam(seed, x, y)) * 2 - 1;
        float amp = 1, t;

        for (int i = 1; i < octaves; i++) {
            t = x;
            x = y * lacunarity;
            y = t * lacunarity;

            amp *= gain;
            sum += (Math.abs(singleFoam(++seed, x, y)) * 2 - 1) * amp;
        }

        return sum * fractalBounding;
    }

    private float singleFoamFractalRidgedMulti(float x, float y) {
        int seed = this.seed;
        float sum = 0, amp = 1, ampBias = 1f, spike, t;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(singleFoam(seed + i, x, y));
            spike *= spike * amp;
            amp = Math.max(0f, Math.min(1f, spike * 2f));
            sum += (spike * ampBias);
            ampBias *= 2f;
            t = x;
            x = y * lacunarity;
            y = t * lacunarity;
        }
        return sum / ((ampBias - 1f) * 0.5f) - 1f;
    }

    public float getFoam(float x, float y) {
        return singleFoam(seed, x * frequency, y * frequency);
    }


    // Foam Noise
    public float singleFoam(int seed, float x, float y) {
        final float p0 = x;
        final float p1 = x * -0.5f + y * 0.8660254037844386f;
        final float p2 = x * -0.5f + y * -0.8660254037844387f;

        float xin = p2;
        float yin = p0;
        final float a = valueNoise(seed, xin, yin);
        seed += 0x9E3779BD;
        seed ^= seed >>> 14;
        xin = p1;
        yin = p2;
        final float b = valueNoise(seed, xin + a, yin);
        seed += 0x9E3779BD;
        seed ^= seed >>> 14;
        xin = p0;
        yin = p1;
        final float c = valueNoise(seed, xin + b, yin);
        final float result = (a + b + c) * F3;
        return (result <= 0.5f)
            ? (result * result * 4) - 1
            : 1 - ((result - 1) * (result - 1) * 4);
    }
    public float getFoamFractal(float x, float y, float z) {
        x *= frequency;
        y *= frequency;
        z *= frequency;

        switch (fractalType) {
        case FBM:
            return singleFoamFractalFBM(x, y, z);
        case BILLOW:
            return singleFoamFractalBillow(x, y, z);
        case RIDGED_MULTI:
            return singleFoamFractalRidgedMulti(x, y, z);
        default:
            return 0;
        }
    }

    private float singleFoamFractalFBM(float x, float y, float z) {
        int seed = this.seed;
        float sum = singleFoam(seed, x, y, z);
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;

            amp *= gain;
            sum += singleFoam(++seed, x, y, z) * amp;
        }

        return sum * fractalBounding;
    }

    private float singleFoamFractalBillow(float x, float y, float z) {
        int seed = this.seed;
        float sum = Math.abs(singleFoam(seed, x, y, z)) * 2 - 1;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;

            amp *= gain;
            sum += (Math.abs(singleFoam(++seed, x, y, z)) * 2 - 1) * amp;
        }

        return sum * fractalBounding;
    }

    private float singleFoamFractalRidgedMulti(float x, float y, float z) {
        int seed = this.seed;
        float sum = 0, amp = 1, ampBias = 1f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(singleFoam(seed + i, x, y, z));
            spike *= spike * amp;
            amp = Math.max(0f, Math.min(1f, spike * 2f));
            sum += (spike * ampBias);
            ampBias *= 2f;
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;
        }
        return sum / ((ampBias - 1f) * 0.5f) - 1f;
    }

    public float getFoam(float x, float y, float z) {
        return singleFoam(seed, x * frequency, y * frequency, z * frequency);
    }

    public float singleFoam(int seed, float x, float y, float z){
        final float p0 = x;
        final float p1 = x * -0.3333333333333333f + y * 0.9428090415820634f;
        final float p2 = x * -0.3333333333333333f + y * -0.4714045207910317f + z * 0.816496580927726f;
        final float p3 = x * -0.3333333333333333f + y * -0.4714045207910317f + z * -0.816496580927726f;

        float xin = p3;
        float yin = p2;
        float zin = p0;
        final float a = valueNoise(seed, xin, yin, zin);
        seed += 0x9E3779BD;
        seed ^= seed >>> 14;
        xin = p0;
        yin = p1;
        zin = p3;
        final float b = valueNoise(seed, xin + a, yin, zin);
        seed += 0x9E3779BD;
        seed ^= seed >>> 14;
        xin = p1;
        yin = p2;
        zin = p3;
        final float c = valueNoise(seed, xin + b, yin, zin);
        seed += 0x9E3779BD;
        seed ^= seed >>> 14;
        xin = p0;
        yin = p1;
        zin = p2;
        final float d = valueNoise(seed, xin + c, yin, zin);

        float result = (a + b + c + d) * 0.25f;
        if(result <= 0.5){
            result *= 2;
            return result * result * result - 1;
        }
        else {
            result = (result - 1) * 2;
            return result * result * result + 1;
        }
    }


    private float singleFoamFractalFBM(float x, float y, float z, float w) {
        int seed = this.seed;
        float sum = singleFoam(seed, x, y, z, w);
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;
            w *= lacunarity;

            amp *= gain;
            sum += singleFoam(++seed, x, y, z, w) * amp;
        }

        return sum * fractalBounding;
    }

    private float singleFoamFractalBillow(float x, float y, float z, float w) {
        int seed = this.seed;
        float sum = Math.abs(singleFoam(seed, x, y, z, w)) * 2 - 1;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;
            w *= lacunarity;

            amp *= gain;
            sum += (Math.abs(singleFoam(++seed, x, y, z, w)) * 2 - 1) * amp;
        }

        return sum * fractalBounding;
    }

    private float singleFoamFractalRidgedMulti(float x, float y, float z, float w) {
        int seed = this.seed;
        float sum = 0, amp = 1, ampBias = 1f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(singleFoam(seed + i, x, y,  z, w));
            spike *= spike * amp;
            amp = Math.max(0f, Math.min(1f, spike * 2f));
            sum += (spike * ampBias);
            ampBias *= 2f;
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;
            w *= lacunarity;
        }
        return sum / ((ampBias - 1f) * 0.5f) - 1f;
    }

    public float getFoam(float x, float y, float z, float w) {
        return singleFoam(seed, x * frequency, y * frequency, z * frequency, w * frequency);
    }

    public float singleFoam(int seed, float x, float y, float z, float w) {
        final float p0 = x;
        final float p1 = x * -0.25f + y *  0.9682458365518543f;
        final float p2 = x * -0.25f + y * -0.3227486121839514f + z *  0.91287092917527690f;
        final float p3 = x * -0.25f + y * -0.3227486121839514f + z * -0.45643546458763834f + w *  0.7905694150420949f;
        final float p4 = x * -0.25f + y * -0.3227486121839514f + z * -0.45643546458763834f + w * -0.7905694150420947f;

        float xin = p1;
        float yin = p2;
        float zin = p3;
        float win = p4;
        final float a = valueNoise(seed, xin, yin, zin, win);
        seed += 0x9E3779BD;
        seed ^= seed >>> 14;
        xin = p0;
        yin = p2;
        zin = p3;
        win = p4;
        final float b = valueNoise(seed, xin + a, yin, zin, win);
        seed += 0x9E3779BD;
        seed ^= seed >>> 14;
        xin = p0;
        yin = p1;
        zin = p3;
        win = p4;
        final float c = valueNoise(seed, xin + b, yin, zin, win);
        seed += 0x9E3779BD;
        seed ^= seed >>> 14;
        xin = p0;
        yin = p1;
        zin = p2;
        win = p4;
        final float d = valueNoise(seed, xin + c, yin, zin, win);
        seed += 0x9E3779BD;
        seed ^= seed >>> 14;
        xin = p0;
        yin = p1;
        zin = p2;
        win = p3;
        final float e = valueNoise(seed, xin + d, yin, zin, win);

        float result = (a + b + c + d + e) * 0.2f;
        if(result <= 0.5f){
            result *= 2;
            result *= result;
            return result * result - 1;
        }
        else{
            result = (result - 1) * 2;
            result *= result;
            return 1 - result * result;
        }
    }
    

    
    //2D Simplex
    
    public float getSimplexFractal(float x, float y) {
        x *= frequency;
        y *= frequency;

        switch (fractalType) {
            case FBM:
                return singleSimplexFractalFBM(x, y);
            case BILLOW:
                return singleSimplexFractalBillow(x, y);
            case RIDGED_MULTI:
                return singleSimplexFractalRidgedMulti(x, y);
            default:
                return 0;
        }
    }

    /**
     * Generates ridged-multi simplex noise with the given amount of octaves and default frequency (0.03125), lacunarity
     * (2) and gain (0.5) in 2D.
     * @param x
     * @param y
     * @param seed
     * @param octaves
     * @return noise as a float between -1f and 1f
     */
    public float layered2D(float x, float y, int seed, int octaves)
    {
        x *= 0.03125f;
        y *= 0.03125f;

        float sum = 1 - Math.abs(singleSimplex(seed, x, y));
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;

            amp *= 0.5f;
            sum -= (1 - Math.abs(singleSimplex(seed + i, x, y))) * amp;
        }
        amp = gain;
        float ampFractal = 1;
        for (int i = 1; i < octaves; i++) {
            ampFractal += amp;
            amp *= gain;
        }
        return sum / ampFractal;
    }
    /**
     * Generates ridged-multi simplex noise with the given amount of octaves and default frequency (0.03125), lacunarity
     * (2) and gain (0.5) in 2D.
     * @param x
     * @param y
     * @param seed
     * @param octaves
     * @return noise as a float between -1f and 1f
     */
    public float layered2D(float x, float y, int seed, int octaves, float frequency)
    {
        x *= frequency;
        y *= frequency;

        float sum = singleSimplex(seed, x, y);
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;

            amp *= 0.5f;
            sum += singleSimplex(seed + i, x, y) * amp;
        }
        amp = gain;
        float ampFractal = 1;
        for (int i = 1; i < octaves; i++) {
            ampFractal += amp;
            amp *= gain;
        }
        return sum / ampFractal;
    }
    /**
     * Generates layered simplex noise with the given amount of octaves and specified lacunarity (the amount of
     * frequency change between octaves) and gain (0.5) in D.
     * @param x
     * @param y
     * @param seed
     * @param octaves
     * @return noise as a float between -1f and 1f
     */
    public float layered2D(float x, float y, int seed, int octaves, float frequency, float lacunarity)
    {
        x *= frequency;
        y *= frequency;

        float sum = singleSimplex(seed, x, y);
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;

            amp *= 0.5f;
            sum += singleSimplex(seed + i, x, y) * amp;
        }
        amp = gain;
        float ampFractal = 1;
        for (int i = 1; i < octaves; i++) {
            ampFractal += amp;
            amp *= gain;
        }
        return sum / ampFractal;
    }

    /**
     * Generates layered simplex noise with the given amount of octaves and specified lacunarity (the amount of
     * frequency change between octaves) and gain (loosely, how much to emphasize lower-frequency octaves) in 2D.
     * @param x
     * @param y
     * @param seed
     * @param octaves
     * @return noise as a float between -1f and 1f
     */
    public float layered2D(float x, float y, int seed, int octaves, float frequency, float lacunarity, float gain)
    {
        x *= frequency;
        y *= frequency;

        float sum = singleSimplex(seed, x, y);
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;

            amp *= gain;
            sum += singleSimplex(seed + i, x, y) * amp;
        }
        amp = gain;
        float ampFractal = 1;
        for (int i = 1; i < octaves; i++) {
            ampFractal += amp;
            amp *= gain;
        }
        return sum / ampFractal;
    }

    private float singleSimplexFractalFBM(float x, float y) {
        int seed = this.seed;
        float sum = singleSimplex(seed, x, y);
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;

            amp *= gain;
            sum += singleSimplex(seed + i, x, y) * amp;
        }

        return sum * fractalBounding;
    }

    private float singleSimplexFractalBillow(float x, float y) {
        int seed = this.seed;
        float sum = Math.abs(singleSimplex(seed, x, y)) * 2 - 1;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;

            amp *= gain;
            sum += (Math.abs(singleSimplex(++seed, x, y)) * 2 - 1) * amp;
        }

        return sum * fractalBounding;
    }

    /**
     * Generates ridged-multi simplex noise with the given amount of octaves and default frequency (0.03125), lacunarity
     * (2) and gain (0.5).
     * @param x
     * @param y
     * @param seed
     * @param octaves
     * @return noise as a float between -1f and 1f
     */
    public float ridged2D(float x, float y, int seed, int octaves)
    {
        return ridged2D(x, y, seed, octaves, 0.03125f, 2f);
    }
    /**
     * Generates ridged-multi simplex noise with the given amount of octaves and default frequency (0.03125), lacunarity
     * (2) and gain (0.5).
     * @param x
     * @param y
     * @param seed
     * @param octaves
     * @return noise as a float between -1f and 1f
     */
    public float ridged2D(float x, float y, int seed, int octaves, float frequency)
    {
        return ridged2D(x, y, seed, octaves, frequency, 2f);
    }
    /**
     * Generates ridged-multi simplex noise with the given amount of octaves and specified lacunarity (the amount of
     * frequency change between octaves); gain is not used.
     * @param x
     * @param y
     * @param seed any int
     * @param octaves how many "layers of detail" to generate; at least 1, but note this slows down with many octaves
     * @param frequency often about {@code 1f / 32f}, but generally adjusted for the use case
     * @param lacunarity when {@code octaves} is 2 or more, this affects the change between layers
     * @return noise as a float between -1f and 1f
     */
    public float ridged2D(float x, float y, int seed, int octaves, float frequency, float lacunarity)
    {
        x *= frequency;
        y *= frequency;
        float sum = 0, amp = 1, ampBias = 1f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(singleSimplex(seed + i, x, y));
            spike *= spike * amp;
            amp = Math.max(0f, Math.min(1f, spike * 2f));
            sum += (spike * ampBias);
            ampBias *= 2f;
            x *= lacunarity;
            y *= lacunarity;
        }
        return sum / ((ampBias - 1f) * 0.5f) - 1f;
    }

    private float singleSimplexFractalRidgedMulti(float x, float y) {
        int seed = this.seed;
        float sum = 0, amp = 1, ampBias = 1f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(singleSimplex(seed + i, x, y));
            spike *= spike * amp;
            amp = Math.max(0f, Math.min(1f, spike * 2f));
            sum += (spike * ampBias);
            ampBias *= 2f;
            x *= lacunarity;
            y *= lacunarity;
        }
        return sum / ((ampBias - 1f) * 0.5f) - 1f;
    }

    public float getSimplex(float x, float y) {
        return singleSimplex(seed, x * frequency, y * frequency);
    }

    public float singleSimplex(int seed, float x, float y) {
        float t = (x + y) * F2f;
        int i = fastFloor(x + t);
        int j = fastFloor(y + t);

        t = (i + j) * G2f;
        float X0 = i - t;
        float Y0 = j - t;

        float x0 = x - X0;
        float y0 = y - Y0;

        int i1, j1;
        if (x0 > y0) {
            i1 = 1;
            j1 = 0;
        } else {
            i1 = 0;
            j1 = 1;
        }

        float x1 = x0 - i1 + G2f;
        float y1 = y0 - j1 + G2f;
        float x2 = x0 - 1 + H2f;
        float y2 = y0 - 1 + H2f;

        float n = 0f;

        t = 0.75f - x0 * x0 - y0 * y0;
        if (t >= 0) {
            t *= t;
            n += t * t * gradCoord2D(seed, i, j, x0, y0);
        }

        t = 0.75f - x1 * x1 - y1 * y1;
        if (t > 0) {
            t *= t;
            n += t * t * gradCoord2D(seed, i + i1, j + j1, x1, y1);
        }

        t = 0.75f - x2 * x2 - y2 * y2;
        if (t > 0)  {
            t *= t;
            n += t * t * gradCoord2D(seed, i + 1, j + 1, x2, y2);
        }

        return 9.11f * n;
    }

    // 3D Simplex Noise
    public float getSimplexFractal(float x, float y, float z) {
        x *= frequency;
        y *= frequency;
        z *= frequency;

        switch (fractalType) {
            case FBM:
                return singleSimplexFractalFBM(x, y, z);
            case BILLOW:
                return singleSimplexFractalBillow(x, y, z);
            case RIDGED_MULTI:
                return singleSimplexFractalRidgedMulti(x, y, z);
            default:
                return 0;
        }
    }

    /**
     * Generates ridged-multi simplex noise with the given amount of octaves and default frequency (0.03125), lacunarity
     * (2) and gain (0.5) in 3D.
     * @param x
     * @param y
     * @param z
     * @param seed
     * @param octaves
     * @return noise as a float between -1f and 1f
     */
    public float layered3D(float x, float y, float z, int seed, int octaves)
    {
        x *= 0.03125f;
        y *= 0.03125f;
        z *= 0.03125f;

        float sum = 1 - Math.abs(singleSimplex(seed, x, y, z));
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;

            amp *= 0.5f;
            sum -= (1 - Math.abs(singleSimplex(seed + i, x, y, z))) * amp;
        }
        amp = gain;
        float ampFractal = 1;
        for (int i = 1; i < octaves; i++) {
            ampFractal += amp;
            amp *= gain;
        }
        return sum / ampFractal;
    }
    /**
     * Generates ridged-multi simplex noise with the given amount of octaves and default frequency (0.03125), lacunarity
     * (2) and gain (0.5) in 3D.
     * @param x
     * @param y
     * @param z
     * @param seed
     * @param octaves
     * @return noise as a float between -1f and 1f
     */
    public float layered3D(float x, float y, float z, int seed, int octaves, float frequency)
    {
        x *= frequency;
        y *= frequency;
        z *= frequency;

        float sum = singleSimplex(seed, x, y, z);
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= 2f;
            y *= 2f;
            z *= 2f;

            amp *= 0.5f;
            sum += singleSimplex(seed + i, x, y, z) * amp;
        }
        amp = gain;
        float ampFractal = 1;
        for (int i = 1; i < octaves; i++) {
            ampFractal += amp;
            amp *= gain;
        }
        return sum / ampFractal;
    }
    /**
     * Generates layered simplex noise with the given amount of octaves and specified lacunarity (the amount of
     * frequency change between octaves) and gain (0.5) in 3D.
     * @param x
     * @param y
     * @param z
     * @param seed
     * @param octaves
     * @param frequency
     * @param lacunarity
     * @return noise as a float between -1f and 1f
     */
    public float layered3D(float x, float y, float z, int seed, int octaves, float frequency, float lacunarity)
    {
        x *= frequency;
        y *= frequency;
        z *= frequency;

        float sum = singleSimplex(seed, x, y, z);
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;

            amp *= 0.5f;
            sum += singleSimplex(seed + i, x, y, z) * amp;
        }
        amp = gain;
        float ampFractal = 1;
        for (int i = 1; i < octaves; i++) {
            ampFractal += amp;
            amp *= gain;
        }
        return sum / ampFractal;
    }

    /**
     * Generates layered simplex noise with the given amount of octaves and specified lacunarity (the amount of
     * frequency change between octaves) and gain (loosely, how much to emphasize lower-frequency octaves) in 3D.
     * @param x
     * @param y
     * @param z
     * @param seed
     * @param octaves
     * @param frequency
     * @param lacunarity
     * @param gain
     * @return noise as a float between -1f and 1f
     */
    public float layered3D(float x, float y, float z, int seed, int octaves, float frequency, float lacunarity, float gain)
    {
        x *= frequency;
        y *= frequency;
        z *= frequency;

        float sum = singleSimplex(seed, x, y, z);
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;

            amp *= gain;
            sum += singleSimplex(seed + i, x, y, z) * amp;
        }
        amp = gain;
        float ampFractal = 1;
        for (int i = 1; i < octaves; i++) {
            ampFractal += amp;
            amp *= gain;
        }
        return sum / ampFractal;
    }

    private float singleSimplexFractalFBM(float x, float y, float z) {
        int seed = this.seed;
        float sum = singleSimplex(seed, x, y, z);
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;

            amp *= gain;
            sum += singleSimplex(seed + i, x, y, z) * amp;
        }

        return sum * fractalBounding;
    }

    private float singleSimplexFractalBillow(float x, float y, float z) {
        int seed = this.seed;
        float sum = Math.abs(singleSimplex(seed, x, y, z)) * 2 - 1;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;

            amp *= gain;
            sum += (Math.abs(singleSimplex(seed + i, x, y, z)) * 2 - 1) * amp;
        }

        return sum * fractalBounding;
    }

    /**
     * Generates ridged-multi simplex noise with the given amount of octaves and default frequency (0.03125), lacunarity
     * (2) and gain (0.5).
     * @param x
     * @param y
     * @param z
     * @param seed
     * @param octaves
     * @return noise as a float between -1f and 1f
     */
    public float ridged3D(float x, float y, float z, int seed, int octaves)
    {
        return ridged3D(x, y, z, seed, octaves, 0.03125f, 2f);
    }
    /**
     * Generates ridged-multi simplex noise with the given amount of octaves, specified frequency, and the default
     * lacunarity (2) and gain (0.5).
     * @param x
     * @param y
     * @param z
     * @param seed
     * @param octaves
     * @return noise as a float between -1f and 1f
     */
    public float ridged3D(float x, float y, float z, int seed, int octaves, float frequency)
    {
        return ridged3D(x, y, z, seed, octaves, frequency, 2f);
    }
    /**
     * Generates ridged-multi simplex noise with the given amount of octaves and specified lacunarity (the amount of
     * frequency change between octaves); gain is not used.
     * @param x
     * @param y
     * @param z
     * @param seed any int
     * @param octaves how many "layers of detail" to generate; at least 1, but note this slows down with many octaves
     * @param frequency often about {@code 1f / 32f}, but generally adjusted for the use case
     * @param lacunarity when {@code octaves} is 2 or more, this affects the change between layers
     * @return noise as a float between -1f and 1f
     */
    public float ridged3D(float x, float y, float z, int seed, int octaves, float frequency, float lacunarity)
    {
        x *= frequency;
        y *= frequency;
        z *= frequency;

        float sum = 0, amp = 1, ampBias = 1f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(singleSimplex(seed + i, x, y, z));
            spike *= spike * amp;
            amp = Math.max(0f, Math.min(1f, spike * 2f));
            sum += (spike * ampBias);
            ampBias *= 2f;
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;
        }
        return sum / ((ampBias - 1f) * 0.5f) - 1f;
    }

    private float singleSimplexFractalRidgedMulti(float x, float y, float z) {
        int seed = this.seed;
        float sum = 0, amp = 1, ampBias = 1f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(singleSimplex(seed + i, x, y, z));
            spike *= spike * amp;
            amp = Math.max(0f, Math.min(1f, spike * 2f));
            sum += (spike * ampBias);
            ampBias *= 2f;
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;
        }
        return sum / ((ampBias - 1f) * 0.5f) - 1f;
    }

    public float getSimplex(float x, float y, float z) {
        return singleSimplex(seed, x * frequency, y * frequency, z * frequency);
    }

    public float singleSimplex(int seed, float x, float y, float z) {
        float t = (x + y + z) * F3;
        int i = fastFloor(x + t);
        int j = fastFloor(y + t);
        int k = fastFloor(z + t);

        t = (i + j + k) * G3;
        float x0 = x - (i - t);
        float y0 = y - (j - t);
        float z0 = z - (k - t);

        int i1, j1, k1;
        int i2, j2, k2;

        if (x0 >= y0) {
            if (y0 >= z0) {
                i1 = 1;
                j1 = 0;
                k1 = 0;
                i2 = 1;
                j2 = 1;
                k2 = 0;
            } else if (x0 >= z0) {
                i1 = 1;
                j1 = 0;
                k1 = 0;
                i2 = 1;
                j2 = 0;
                k2 = 1;
            } else // x0 < z0
            {
                i1 = 0;
                j1 = 0;
                k1 = 1;
                i2 = 1;
                j2 = 0;
                k2 = 1;
            }
        } else // x0 < y0
        {
            if (y0 < z0) {
                i1 = 0;
                j1 = 0;
                k1 = 1;
                i2 = 0;
                j2 = 1;
                k2 = 1;
            } else if (x0 < z0) {
                i1 = 0;
                j1 = 1;
                k1 = 0;
                i2 = 0;
                j2 = 1;
                k2 = 1;
            } else // x0 >= z0
            {
                i1 = 0;
                j1 = 1;
                k1 = 0;
                i2 = 1;
                j2 = 1;
                k2 = 0;
            }
        }

        float x1 = x0 - i1 + G3;
        float y1 = y0 - j1 + G3;
        float z1 = z0 - k1 + G3;
        float x2 = x0 - i2 + F3;
        float y2 = y0 - j2 + F3;
        float z2 = z0 - k2 + F3;
        float x3 = x0 + G33;
        float y3 = y0 + G33;
        float z3 = z0 + G33;

        float n = 0;

        t = 0.6f - x0 * x0 - y0 * y0 - z0 * z0;
        if (t > 0) {
            t *= t;
            n += t * t * gradCoord3D(seed, i, j, k, x0, y0, z0);
        }

        t = 0.6f - x1 * x1 - y1 * y1 - z1 * z1;
        if (t > 0) {
            t *= t;
            n += t * t * gradCoord3D(seed, i + i1, j + j1, k + k1, x1, y1, z1);
        }

        t = 0.6f - x2 * x2 - y2 * y2 - z2 * z2;
        if (t > 0) {
            t *= t;
            n += t * t * gradCoord3D(seed, i + i2, j + j2, k + k2, x2, y2, z2);
        }

        t = 0.6f - x3 * x3 - y3 * y3 - z3 * z3;
        if (t > 0)  {
            t *= t;
            n += t * t * gradCoord3D(seed, i + 1, j + 1, k + 1, x3, y3, z3);
        }

        return 31.5f * n;
    }

    public float getSimplex(float x, float y, float z, float w) {
        return singleSimplex(seed, x * frequency, y * frequency, z * frequency, w * frequency);
    }

    public float singleSimplex(int seed, float x, float y, float z, float w) {
        float n = 0f;
        float t = (x + y + z + w) * F4;
        int i = fastFloor(x + t);
        int j = fastFloor(y + t);
        int k = fastFloor(z + t);
        int l = fastFloor(w + t);
        t = (i + j + k + l) * G4;
        final float X0 = i - t;
        final float Y0 = j - t;
        final float Z0 = k - t;
        final float W0 = l - t;
        final float x0 = x - X0;
        final float y0 = y - Y0;
        final float z0 = z - Z0;
        final float w0 = w - W0;

        final int[] SIMPLEX_4D = SimplexAndFoam.SIMPLEX_4D;
        final int c = (x0 > y0 ? 128 : 0) | (x0 > z0 ? 64 : 0) | (y0 > z0 ? 32 : 0) | (x0 > w0 ? 16 : 0) | (y0 > w0 ? 8 : 0) | (z0 > w0 ? 4 : 0);
        final int i1 = SIMPLEX_4D[c] >>> 2,
                j1 = SIMPLEX_4D[c | 1] >>> 2,
                k1 = SIMPLEX_4D[c | 2] >>> 2,
                l1 = SIMPLEX_4D[c | 3] >>> 2,
                i2 = SIMPLEX_4D[c] >>> 1 & 1,
                j2 = SIMPLEX_4D[c | 1] >>> 1 & 1,
                k2 = SIMPLEX_4D[c | 2] >>> 1 & 1,
                l2 = SIMPLEX_4D[c | 3] >>> 1 & 1,
                i3 = SIMPLEX_4D[c] & 1,
                j3 = SIMPLEX_4D[c | 1] & 1,
                k3 = SIMPLEX_4D[c | 2] & 1,
                l3 = SIMPLEX_4D[c | 3] & 1;

        final float x1 = x0 - i1 + G4;
        final float y1 = y0 - j1 + G4;
        final float z1 = z0 - k1 + G4;
        final float w1 = w0 - l1 + G4;
        final float x2 = x0 - i2 + 2 * G4;
        final float y2 = y0 - j2 + 2 * G4;
        final float z2 = z0 - k2 + 2 * G4;
        final float w2 = w0 - l2 + 2 * G4;
        final float x3 = x0 - i3 + 3 * G4;
        final float y3 = y0 - j3 + 3 * G4;
        final float z3 = z0 - k3 + 3 * G4;
        final float w3 = w0 - l3 + 3 * G4;
        final float x4 = x0 - 1 + 4 * G4;
        final float y4 = y0 - 1 + 4 * G4;
        final float z4 = z0 - 1 + 4 * G4;
        final float w4 = w0 - 1 + 4 * G4;

        t = 0.62f - x0 * x0 - y0 * y0 - z0 * z0 - w0 * w0;
        if (t > 0) {
            t *= t;
            n = t * t * gradCoord4D(seed, i, j, k, l, x0, y0, z0, w0);
        }
        t = 0.62f - x1 * x1 - y1 * y1 - z1 * z1 - w1 * w1;
        if (t > 0) {
            t *= t;
            n += t * t * gradCoord4D(seed, i + i1, j + j1, k + k1, l + l1, x1, y1, z1, w1);
        }
        t = 0.62f - x2 * x2 - y2 * y2 - z2 * z2 - w2 * w2;
        if (t > 0) {
            t *= t;
            n += t * t * gradCoord4D(seed, i + i2, j + j2, k + k2, l + l2, x2, y2, z2, w2);
        }
        t = 0.62f - x3 * x3 - y3 * y3 - z3 * z3 - w3 * w3;
        if (t > 0) {
            t *= t;
            n += t * t * gradCoord4D(seed, i + i3, j + j3, k + k3, l + l3, x3, y3, z3, w3);
        }
        t = 0.62f - x4 * x4 - y4 * y4 - z4 * z4 - w4 * w4;
        if (t > 0) {
            t *= t;
            n += t * t * gradCoord4D(seed, i + 1, j + 1, k + 1, l + 1, x4, y4, z4, w4);
        }

        return 14.75f * n;
    }
    
    // Simplex Noise
    public float getSimplexFractal(float x, float y, float z, float w) {
        x *= frequency;
        y *= frequency;
        z *= frequency;
        w *= frequency;
        
        switch (fractalType) {
            case FBM:
                return singleSimplexFractalFBM(x, y, z, w);
            case BILLOW:
                return singleSimplexFractalBillow(x, y, z, w);
            case RIDGED_MULTI:
                return singleSimplexFractalRidgedMulti(x, y, z, w);
            default:
                return 0;
        }
    }

    private float singleSimplexFractalFBM(float x, float y, float z, float w) {
        int seed = this.seed;
        float sum = singleSimplex(seed, x, y, z, w);
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;
            w *= lacunarity;

            amp *= gain;
            sum += singleSimplex(seed + i, x, y, z, w) * amp;
        }

        return sum * fractalBounding;
    }
    private float singleSimplexFractalRidgedMulti(float x, float y, float z, float w) {
        int seed = this.seed;
        float sum = 0, amp = 1, ampBias = 1f, spike;
        for (int i = 0; i < octaves; i++) {
            spike = 1f - Math.abs(singleSimplex(seed + i, x, y, z, w));
            spike *= spike * amp;
            amp = Math.max(0f, Math.min(1f, spike * 2f));
            sum += (spike * ampBias);
            ampBias *= 2f;
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;
            w *= lacunarity;
        }
        return sum / ((ampBias - 1f) * 0.5f) - 1f;
    }

    private float singleSimplexFractalBillow(float x, float y, float z, float w) {
        int seed = this.seed;
        float sum = Math.abs(singleSimplex(seed, x, y, z, w)) * 2 - 1;
        float amp = 1;

        for (int i = 1; i < octaves; i++) {
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;
            w *= lacunarity;

            amp *= gain;
            sum += (Math.abs(singleSimplex(seed + i, x, y, z, w)) * 2 - 1) * amp;
        }

        return sum * fractalBounding;
    }
    





    /**
     * Produces 1D noise that "tiles" by repeating
     * its output every {@code sizeX} units that {@code x} increases or decreases by. This doesn't precalculate an
     * array, instead calculating just one value so that later calls with different x will tile seamlessly.
     * <br>
     * Internally, this just samples out of a circle from a source of 2D noise.
     * @param x the x-coordinate to sample
     * @param sizeX the range of x to generate before repeating; must be greater than 0
     * @param seed the noise seed, as a long
     * @return continuous noise from -1.0 to 1.0, inclusive 
     */
    public float seamless1D(float x, float sizeX, int seed)
    {
        x /= sizeX;
        return getNoiseWithSeed(cosTurns(x), sinTurns(x), seed);
    }

    /**
     * Produces 2D noise that tiles every {@code sizeX} units on the x-axis and {@code sizeY} units on the y-axis.
     * @param x the x-coordinate to sample
     * @param y the y-coordinate to sample
     * @param sizeX the range of x to generate before repeating; must be greater than 0
     * @param sizeY the range of y to generate before repeating; must be greater than 0
     * @param seed the noise seed, as a long
     * @return continuous noise from -1.0 to 1.0, inclusive 
     */
    public float seamless2D(float x, float y, float sizeX, float sizeY, int seed)
    {
        x /= sizeX;
        y /= sizeY;
        return getNoiseWithSeed(cosTurns(x), sinTurns(x), cosTurns(y), sinTurns(y), seed);
    }

    /**
     * A fairly-close approximation of {@link Math#sin(double)} that can be significantly faster (between 8x and 80x
     * faster sin() calls in benchmarking, and both takes and returns floats; if you have access to libGDX you should
     * consider its more-precise and sometimes-faster MathUtils.sin() method. Because this method doesn't rely on a
     * lookup table, where libGDX's MathUtils does, applications that have a bottleneck on memory may perform better
     * with this method than with MathUtils. Takes the same arguments Math.sin() does, so one angle in radians,
     * which may technically be any float (but this will lose precision on fairly large floats, such as those that are
     * larger than {@link Integer#MAX_VALUE}, because those floats themselves will lose precision at that scale). The
     * difference between the result of this method and {@link Math#sin(double)} should be under 0.0011 at
     * all points between -pi and pi, with an average difference of about 0.0005; not all points have been checked for
     * potentially higher errors, though.
     * <br>
     * Unlike in previous versions of this method, the sign of the input doesn't affect performance here, at least not
     * by a measurable amount.
     * <br>
     * The technique for sine approximation is mostly from
     * <a href="https://web.archive.org/web/20080228213915/http://devmaster.net/forums/showthread.php?t=5784">this archived DevMaster thread</a>,
     * with credit to "Nick". Changes have been made to accelerate wrapping from any float to the valid input range.
     * @param radians an angle in radians as a float, often from 0 to pi * 2, though not required to be.
     * @return the sine of the given angle, as a float between -1f and 1f (both inclusive)
     */
    public static float sin(float radians)
    {
        radians *= 0.6366197723675814f;
        final int floor = (radians >= 0.0 ? (int) radians : (int) radians - 1) & -2;
        radians -= floor;
        radians *= 2f - radians;
        return radians * (-0.775f - 0.225f * radians) * ((floor & 2) - 1);
    }

    /**
     * A fairly-close approximation of {@link Math#cos(double)} that can be significantly faster (between 8x and 80x
     * faster cos() calls in benchmarking, and both takes and returns floats; if you have access to libGDX you should
     * consider its more-precise and sometimes-faster MathUtils.cos() method. Because this method doesn't rely on a
     * lookup table, where libGDX's MathUtils does, applications that have a bottleneck on memory may perform better
     * with this method than with MathUtils. Takes the same arguments Math.cos() does, so one angle in radians,
     * which may technically be any float (but this will lose precision on fairly large floats, such as those that are
     * larger than {@link Integer#MAX_VALUE}, because those floats themselves will lose precision at that scale). The
     * difference between the result of this method and {@link Math#cos(double)} should be under 0.0011 at
     * all points between -pi and pi, with an average difference of about 0.0005; not all points have been checked for
     * potentially higher errors, though.
     * <br>
     * Unlike in previous versions of this method, the sign of the input doesn't affect performance here, at least not
     * by a measurable amount.
     * <br>
     * The technique for cosine approximation is mostly from
     * <a href="https://web.archive.org/web/20080228213915/http://devmaster.net/forums/showthread.php?t=5784">this archived DevMaster thread</a>,
     * with credit to "Nick". Changes have been made to accelerate wrapping from any float to the valid input range.
     * @param radians an angle in radians as a float, often from 0 to pi * 2, though not required to be.
     * @return the cosine of the given angle, as a float between -1f and 1f (both inclusive)
     */
    public static float cos(float radians)
    {
        radians = radians * 0.6366197723675814f + 1f;
        final int floor = (radians >= 0.0 ? (int) radians : (int) radians - 1) & -2;
        radians -= floor;
        radians *= 2f - radians;
        return radians * (-0.775f - 0.225f * radians) * ((floor & 2) - 1);
    }
    /**
     * A variation on {@link Math#sin(double)} that takes its input as a fraction of a turn instead of in radians (it
     * also takes and returns a float); one turn is equal to 360 degrees or two*PI radians. This can be useful as a
     * building block for other measurements; to make a sine method that takes its input in grad (with 400 grad equal to
     * 360 degrees), you would just divide the grad value by 400.0 (or multiply it by 0.0025) and pass it to this
     * method. Similarly for binary degrees, also called brad (with 256 brad equal to 360 degrees), you would divide by
     * 256.0 or multiply by 0.00390625 before passing that value here. The brad case is especially useful because you
     * can use a byte for any brad values, and adding up those brad values will wrap correctly (256 brad goes back to 0)
     * while keeping perfect precision for the results (you still divide by 256.0 when you pass the brad value to this
     * method).
     * <br>
     * The technique for sine approximation is mostly from
     * <a href="https://web.archive.org/web/20080228213915/http://devmaster.net/forums/showthread.php?t=5784">this archived DevMaster thread</a>,
     * with credit to "Nick". Changes have been made to accelerate wrapping from any double to the valid input range.
     * @param turns an angle as a fraction of a turn as a float, with 0.5 here equivalent to PI radians in {@link #sin(float)}
     * @return the sine of the given angle, as a float between -1.0 and 1.0 (both inclusive)
     */
    public static float sinTurns(float turns)
    {
        turns *= 4f;
        final long floor = (turns >= 0.0 ? (long) turns : (long) turns - 1L) & -2L;
        turns -= floor;
        turns *= 2f - turns;
        return turns * (-0.775f - 0.225f * turns) * ((floor & 2L) - 1L);
    }

    /**
     * A variation on {@link Math#cos(double)} that takes its input as a fraction of a turn instead of in radians (it
     * also takes and returns a float); one turn is equal to 360 degrees or two*PI radians. This can be useful as a
     * building block for other measurements; to make a cosine method that takes its input in grad (with 400 grad equal
     * to 360 degrees), you would just divide the grad value by 400.0 (or multiply it by 0.0025) and pass it to this
     * method. Similarly for binary degrees, also called brad (with 256 brad equal to 360 degrees), you would divide by
     * 256.0 or multiply by 0.00390625 before passing that value here. The brad case is especially useful because you
     * can use a byte for any brad values, and adding up those brad values will wrap correctly (256 brad goes back to 0)
     * while keeping perfect precision for the results (you still divide by 256.0 when you pass the brad value to this
     * method).
     * <br>
     * The technique for cosine approximation is mostly from
     * <a href="https://web.archive.org/web/20080228213915/http://devmaster.net/forums/showthread.php?t=5784">this archived DevMaster thread</a>,
     * with credit to "Nick". Changes have been made to accelerate wrapping from any double to the valid input range.
     * @param turns an angle as a fraction of a turn as a float, with 0.5 here equivalent to PI radians in {@link #cos(float)}
     * @return the cosine of the given angle, as a float between -1.0 and 1.0 (both inclusive)
     */
    public static float cosTurns(float turns)
    {
        turns = turns * 4f + 1f;
        final long floor = (turns >= 0.0 ? (long) turns : (long) turns - 1L) & -2L;
        turns -= floor;
        turns *= 2f - turns;
        return turns * (-0.775f - 0.225f * turns) * ((floor & 2L) - 1L);
    }


    /**
     * Can be useful for 1D noise; takes an int seed (which determines what shape the curving 1D path will have) and a
     * float value (which should be how far along the path to go), returning a float between -1.0f inclusive and 1.0f
     * exclusive.
     * @param seed any int; determines the path's shape
     * @param value how far along the path to go; should frequently be between integer values
     * @return a float between -1.0f inclusive and 1.0f
     */
    public static float swayRandomized(int seed, float value) {
        final int floor = value >= 0f ? (int) value : (int) value - 1;
        final float start = ((((seed += floor) ^ 0xD1B54A35) * 0x1D2473 & 0x1FFFFF) - 0x100000) * 0x1p-20f,
                end = (((seed + 1 ^ 0xD1B54A35) * 0x1D2473 & 0x1FFFFF) - 0x100000) * 0x1p-20f;
        value -= floor;
        value *= value * (3f - 2f * value);
        return (1f - value) * start + value * end;
    }


    /**
     * A 32-bit point hash that smashes x and y into s using XOR and multiplications by harmonious numbers,
     * then runs a simple unary hash on s and returns it. Has better performance than HastyPointHash, especially for
     * ints, and has slightly fewer collisions in a hash table of points. GWT-optimized. Inspired by Pelle Evensen's
     * rrxmrrxmsx_0 unary hash, though this doesn't use its code or its full algorithm. The unary hash used here has
     * been stripped down heavily, both for speed and because unless points are selected specifically to target
     * flaws in the hash, it doesn't need the intense resistance to bad inputs that rrxmrrxmsx_0 has.
     * @param x x position, as an int
     * @param y y position, as an int
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 32-bit hash of the x,y point with the given state s
     */
    public static int hashAll(int x, int y, int s) {
        s ^= x * 0x1827F5 ^ y * 0x123C21;
        return (s = (s ^ (s << 19 | s >>> 13) ^ (s << 5 | s >>> 27) ^ 0xD1B54A35) * 0x125493) ^ s >>> 11;
    }
    /**
     * A 32-bit point hash that smashes x, y, and z into s using XOR and multiplications by harmonious numbers,
     * then runs a simple unary hash on s and returns it. Has better performance than HastyPointHash, especially for
     * ints, and has slightly fewer collisions in a hash table of points. GWT-optimized. Inspired by Pelle Evensen's
     * rrxmrrxmsx_0 unary hash, though this doesn't use its code or its full algorithm. The unary hash used here has
     * been stripped down heavily, both for speed and because unless points are selected specifically to target
     flaws in the hash, it doesn't need the intense resistance to bad inputs that rrxmrrxmsx_0 has.
     * @param x x position, as an int
     * @param y y position, as an int
     * @param z z position, as an int
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 32-bit hash of the x,y,z point with the given state s
     */
    public static int hashAll(int x, int y, int z, int s) {
        s ^= x * 0x1A36A9 ^ y * 0x157931 ^ z * 0x119725;
        return (s = (s ^ (s << 19 | s >>> 13) ^ (s << 5 | s >>> 27) ^ 0xD1B54A35) * 0x125493) ^ s >>> 11;
    }

    /**
     * A 32-bit point hash that smashes x, y, z, and w into s using XOR and multiplications by harmonious numbers,
     * then runs a simple unary hash on s and returns it. Has better performance than HastyPointHash, especially for
     * ints, and has slightly fewer collisions in a hash table of points. GWT-optimized. Inspired by Pelle Evensen's
     * rrxmrrxmsx_0 unary hash, though this doesn't use its code or its full algorithm. The unary hash used here has
     * been stripped down heavily, both for speed and because unless points are selected specifically to target
     * flaws in the hash, it doesn't need the intense resistance to bad inputs that rrxmrrxmsx_0 has.
     * @param x x position, as an int
     * @param y y position, as an int
     * @param z z position, as an int
     * @param w w position, as an int
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 32-bit hash of the x,y,z,w point with the given state s
     */
    public static int hashAll(int x, int y, int z, int w, int s) {
        s ^= x * 0x1B69E1 ^ y * 0x177C0B ^ z * 0x141E5D ^ w * 0x113C31;
        return (s = (s ^ (s << 19 | s >>> 13) ^ (s << 5 | s >>> 27) ^ 0xD1B54A35) * 0x125493) ^ s >>> 11;
    }

    /**
     * A 8-bit point hash that smashes x and y into s using XOR and multiplications by harmonious numbers,
     * then runs a simple unary hash on s and returns it. Has better performance than HastyPointHash, especially for
     * ints, and has slightly fewer collisions in a hash table of points. GWT-optimized. Inspired by Pelle Evensen's
     * rrxmrrxmsx_0 unary hash, though this doesn't use its code or its full algorithm. The unary hash used here has
     * been stripped down heavily, both for speed and because unless points are selected specifically to target
     * flaws in the hash, it doesn't need the intense resistance to bad inputs that rrxmrrxmsx_0 has.
     * @param x x position, as an int
     * @param y y position, as an int
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 8-bit hash of the x,y point with the given state s
     */
    public static int hash256(int x, int y, int s) {
        s ^= x * 0x1827F5 ^ y * 0x123C21;
        return (s ^ (s << 19 | s >>> 13) ^ (s << 5 | s >>> 27) ^ 0xD1B54A35) * 0x125493 >>> 24;
    }
    /**
     * A 8-bit point hash that smashes x, y, and z into s using XOR and multiplications by harmonious numbers,
     * then runs a simple unary hash on s and returns it. Has better performance than HastyPointHash, especially for
     * ints, and has slightly fewer collisions in a hash table of points. GWT-optimized. Inspired by Pelle Evensen's
     * rrxmrrxmsx_0 unary hash, though this doesn't use its code or its full algorithm. The unary hash used here has
     * been stripped down heavily, both for speed and because unless points are selected specifically to target
     flaws in the hash, it doesn't need the intense resistance to bad inputs that rrxmrrxmsx_0 has.
     * @param x x position, as an int
     * @param y y position, as an int
     * @param z z position, as an int
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 8-bit hash of the x,y,z point with the given state s
     */
    public static int hash256(int x, int y, int z, int s) {
        s ^= x * 0x1A36A9 ^ y * 0x157931 ^ z * 0x119725;
        return (s ^ (s << 19 | s >>> 13) ^ (s << 5 | s >>> 27) ^ 0xD1B54A35) * 0x125493 >>> 24;
    }

    /**
     * A 8-bit point hash that smashes x, y, z, and w into s using XOR and multiplications by harmonious numbers,
     * then runs a simple unary hash on s and returns it. Has better performance than HastyPointHash, especially for
     * ints, and has slightly fewer collisions in a hash table of points. GWT-optimized. Inspired by Pelle Evensen's
     * rrxmrrxmsx_0 unary hash, though this doesn't use its code or its full algorithm. The unary hash used here has
     * been stripped down heavily, both for speed and because unless points are selected specifically to target
     * flaws in the hash, it doesn't need the intense resistance to bad inputs that rrxmrrxmsx_0 has.
     * @param x x position, as an int
     * @param y y position, as an int
     * @param z z position, as an int
     * @param w w position, as an int
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 8-bit hash of the x,y,z,w point with the given state s
     */
    public static int hash256(int x, int y, int z, int w, int s) {
        s ^= x * 0x1B69E1 ^ y * 0x177C0B ^ z * 0x141E5D ^ w * 0x113C31;
        return (s ^ (s << 19 | s >>> 13) ^ (s << 5 | s >>> 27) ^ 0xD1B54A35) * 0x125493 >>> 24;
    }

    /**
     * A 6-bit point hash that smashes x and y into s using XOR and multiplications by harmonious numbers,
     * then runs a simple unary hash on s and returns it. Has better performance than HastyPointHash, especially for
     * ints, and has slightly fewer collisions in a hash table of points. GWT-optimized. Inspired by Pelle Evensen's
     * rrxmrrxmsx_0 unary hash, though this doesn't use its code or its full algorithm. The unary hash used here has
     * been stripped down heavily, both for speed and because unless points are selected specifically to target
     * flaws in the hash, it doesn't need the intense resistance to bad inputs that rrxmrrxmsx_0 has.
     * @param x x position, as an int
     * @param y y position, as an int
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 6-bit hash of the x,y point with the given state s
     */
    public static int hash64(int x, int y, int s) {
        s ^= x * 0x1827F5 ^ y * 0x123C21;
        return (s ^ (s << 19 | s >>> 13) ^ (s << 5 | s >>> 27) ^ 0xD1B54A35) * 0x125493 >>> 26;
    }
    /**
     * A 6-bit point hash that smashes x, y, and z into s using XOR and multiplications by harmonious numbers,
     * then runs a simple unary hash on s and returns it. Has better performance than HastyPointHash, especially for
     * ints, and has slightly fewer collisions in a hash table of points. GWT-optimized. Inspired by Pelle Evensen's
     * rrxmrrxmsx_0 unary hash, though this doesn't use its code or its full algorithm. The unary hash used here has
     * been stripped down heavily, both for speed and because unless points are selected specifically to target
     flaws in the hash, it doesn't need the intense resistance to bad inputs that rrxmrrxmsx_0 has.
     * @param x x position, as an int
     * @param y y position, as an int
     * @param z z position, as an int
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 6-bit hash of the x,y,z point with the given state s
     */
    public static int hash64(int x, int y, int z, int s) {
        s ^= x * 0x1A36A9 ^ y * 0x157931 ^ z * 0x119725;
        return (s ^ (s << 19 | s >>> 13) ^ (s << 5 | s >>> 27) ^ 0xD1B54A35) * 0x125493 >>> 26;
    }

    /**
     * A 6-bit point hash that smashes x, y, z, and w into s using XOR and multiplications by harmonious numbers,
     * then runs a simple unary hash on s and returns it. Has better performance than HastyPointHash, especially for
     * ints, and has slightly fewer collisions in a hash table of points. GWT-optimized. Inspired by Pelle Evensen's
     * rrxmrrxmsx_0 unary hash, though this doesn't use its code or its full algorithm. The unary hash used here has
     * been stripped down heavily, both for speed and because unless points are selected specifically to target
     * flaws in the hash, it doesn't need the intense resistance to bad inputs that rrxmrrxmsx_0 has.
     * @param x x position, as an int
     * @param y y position, as an int
     * @param z z position, as an int
     * @param w w position, as an int
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 6-bit hash of the x,y,z,w point with the given state s
     */
    public static int hash64(int x, int y, int z, int w, int s) {
        s ^= x * 0x1B69E1 ^ y * 0x177C0B ^ z * 0x141E5D ^ w * 0x113C31;
        return (s ^ (s << 19 | s >>> 13) ^ (s << 5 | s >>> 27) ^ 0xD1B54A35) * 0x125493 >>> 26;
    }

    /**
     * A 5-bit point hash that smashes x and y into s using XOR and multiplications by harmonious numbers,
     * then runs a simple unary hash on s and returns it. Has better performance than HastyPointHash, especially for
     * ints, and has slightly fewer collisions in a hash table of points. GWT-optimized. Inspired by Pelle Evensen's
     * rrxmrrxmsx_0 unary hash, though this doesn't use its code or its full algorithm. The unary hash used here has
     * been stripped down heavily, both for speed and because unless points are selected specifically to target
     * flaws in the hash, it doesn't need the intense resistance to bad inputs that rrxmrrxmsx_0 has.
     * @param x x position, as an int
     * @param y y position, as an int
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 5-bit hash of the x,y point with the given state s
     */
    public static int hash32(int x, int y, int s) {
        s ^= x * 0x1827F5 ^ y * 0x123C21;
        return (s ^ (s << 19 | s >>> 13) ^ (s << 5 | s >>> 27) ^ 0xD1B54A35) * 0x125493 >>> 27;
    }
    /**
     * A 5-bit point hash that smashes x, y, and z into s using XOR and multiplications by harmonious numbers,
     * then runs a simple unary hash on s and returns it. Has better performance than HastyPointHash, especially for
     * ints, and has slightly fewer collisions in a hash table of points. GWT-optimized. Inspired by Pelle Evensen's
     * rrxmrrxmsx_0 unary hash, though this doesn't use its code or its full algorithm. The unary hash used here has
     * been stripped down heavily, both for speed and because unless points are selected specifically to target
     flaws in the hash, it doesn't need the intense resistance to bad inputs that rrxmrrxmsx_0 has.
     * @param x x position, as an int
     * @param y y position, as an int
     * @param z z position, as an int
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 5-bit hash of the x,y,z point with the given state s
     */
    public static int hash32(int x, int y, int z, int s) {
        s ^= x * 0x1A36A9 ^ y * 0x157931 ^ z * 0x119725;
        return (s ^ (s << 19 | s >>> 13) ^ (s << 5 | s >>> 27) ^ 0xD1B54A35) * 0x125493 >>> 27;
    }

    /**
     * A 5-bit point hash that smashes x, y, z, and w into s using XOR and multiplications by harmonious numbers,
     * then runs a simple unary hash on s and returns it. Has better performance than HastyPointHash, especially for
     * ints, and has slightly fewer collisions in a hash table of points. GWT-optimized. Inspired by Pelle Evensen's
     * rrxmrrxmsx_0 unary hash, though this doesn't use its code or its full algorithm. The unary hash used here has
     * been stripped down heavily, both for speed and because unless points are selected specifically to target
     * flaws in the hash, it doesn't need the intense resistance to bad inputs that rrxmrrxmsx_0 has.
     * @param x x position, as an int
     * @param y y position, as an int
     * @param z z position, as an int
     * @param w w position, as an int
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 5-bit hash of the x,y,z,w point with the given state s
     */
    public static int hash32(int x, int y, int z, int w, int s) {
        s ^= x * 0x1B69E1 ^ y * 0x177C0B ^ z * 0x141E5D ^ w * 0x113C31;
        return (s ^ (s << 19 | s >>> 13) ^ (s << 5 | s >>> 27) ^ 0xD1B54A35) * 0x125493 >>> 27;
    }

    /**
     * A 11-bit point hash that smashes x and y into s using XOR and multiplications by harmonious numbers,
     * then runs a simple unary hash on s and returns it. Has better performance than HastyPointHash, especially for
     * ints, and has slightly fewer collisions in a hash table of points. GWT-optimized. Inspired by Pelle Evensen's
     * rrxmrrxmsx_0 unary hash, though this doesn't use its code or its full algorithm. The unary hash used here has
     * been stripped down heavily, both for speed and because unless points are selected specifically to target
     * flaws in the hash, it doesn't need the intense resistance to bad inputs that rrxmrrxmsx_0 has.
     * @param x x position, as an int
     * @param y y position, as an int
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 11-bit hash of the x,y point with the given state s
     */
    public static int hash2048(int x, int y, int s) {
        s ^= x * 0x1827F5 ^ y * 0x123C21;
        return (s ^ (s << 19 | s >>> 13) ^ (s << 5 | s >>> 27) ^ 0xD1B54A35) * 0x125493 >>> 21;
    }
    /**
     * A 11-bit point hash that smashes x, y, and z into s using XOR and multiplications by harmonious numbers,
     * then runs a simple unary hash on s and returns it. Has better performance than HastyPointHash, especially for
     * ints, and has slightly fewer collisions in a hash table of points. GWT-optimized. Inspired by Pelle Evensen's
     * rrxmrrxmsx_0 unary hash, though this doesn't use its code or its full algorithm. The unary hash used here has
     * been stripped down heavily, both for speed and because unless points are selected specifically to target
     flaws in the hash, it doesn't need the intense resistance to bad inputs that rrxmrrxmsx_0 has.
     * @param x x position, as an int
     * @param y y position, as an int
     * @param z z position, as an int
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 11-bit hash of the x,y,z point with the given state s
     */
    public static int hash2048(int x, int y, int z, int s) {
        s ^= x * 0x1A36A9 ^ y * 0x157931 ^ z * 0x119725;
        return (s ^ (s << 19 | s >>> 13) ^ (s << 5 | s >>> 27) ^ 0xD1B54A35) * 0x125493 >>> 21;
    }

    /**
     * A 11-bit point hash that smashes x, y, z, and w into s using XOR and multiplications by harmonious numbers,
     * then runs a simple unary hash on s and returns it. Has better performance than HastyPointHash, especially for
     * ints, and has slightly fewer collisions in a hash table of points. GWT-optimized. Inspired by Pelle Evensen's
     * rrxmrrxmsx_0 unary hash, though this doesn't use its code or its full algorithm. The unary hash used here has
     * been stripped down heavily, both for speed and because unless points are selected specifically to target
     * flaws in the hash, it doesn't need the intense resistance to bad inputs that rrxmrrxmsx_0 has.
     * @param x x position, as an int
     * @param y y position, as an int
     * @param z z position, as an int
     * @param w w position, as an int
     * @param s any int, a seed to be able to produce many hashes for a given point
     * @return 11-bit hash of the x,y,z,w point with the given state s
     */
    public static int hash2048(int x, int y, int z, int w, int s) {
        s ^= x * 0x1B69E1 ^ y * 0x177C0B ^ z * 0x141E5D ^ w * 0x113C31;
        return (s ^ (s << 19 | s >>> 13) ^ (s << 5 | s >>> 27) ^ 0xD1B54A35) * 0x125493 >>> 21;
    }

    protected static final Float2[] phiGrad2f = {
            new Float2(0.6499429579167653f, 0.759982994187637f),
            new Float2(-0.1551483029088119f, 0.9878911904175052f),
            new Float2(-0.8516180517334043f, 0.5241628506120981f),
            new Float2(-0.9518580082090311f, -0.30653928330368374f),
            new Float2(-0.38568876701087174f, -0.9226289476282616f),
            new Float2(0.4505066120763985f, -0.8927730912586049f),
            new Float2(0.9712959670388622f, -0.23787421973396244f),
            new Float2(0.8120673355833279f, 0.5835637432865366f),
            new Float2(0.08429892519436613f, 0.9964405106232257f),
            new Float2(-0.702488350003267f, 0.7116952424385647f),
            new Float2(-0.9974536374007479f, -0.07131788861160528f),
            new Float2(-0.5940875849508908f, -0.804400361391775f),
            new Float2(0.2252075529515288f, -0.9743108118529653f),
            new Float2(0.8868317111719171f, -0.4620925405802277f),
            new Float2(0.9275724981153959f, 0.373643226540993f),
            new Float2(0.3189067150428103f, 0.9477861083074618f),
            new Float2(-0.5130301507665112f, 0.8583705868705491f),
            new Float2(-0.9857873824221494f, 0.1679977281313266f),
            new Float2(-0.7683809836504446f, -0.6399927061806058f),
            new Float2(-0.013020236219374872f, -0.9999152331316848f),
            new Float2(0.7514561619680513f, -0.6597830223946701f),
            new Float2(0.9898275175279653f, 0.14227257481477412f),
            new Float2(0.5352066871710182f, 0.8447211386057674f),
            new Float2(-0.29411988281443646f, 0.9557685360657266f),
            new Float2(-0.9175289804081126f, 0.39766892022290273f),
            new Float2(-0.8985631161871687f, -0.43884430750324743f),
            new Float2(-0.2505005588110731f, -0.968116454790094f),
            new Float2(0.5729409678802212f, -0.8195966369650838f),
            new Float2(0.9952584535626074f, -0.09726567026534665f),
            new Float2(0.7207814785200723f, 0.6931623620930514f),
            new Float2(-0.05832476124070039f, 0.998297662136006f),
            new Float2(-0.7965970142012075f, 0.6045107087270838f),
            new Float2(-0.977160478114496f, -0.21250270589112422f),
            new Float2(-0.4736001288089817f, -0.8807399831914728f),
            new Float2(0.36153434093875386f, -0.9323587937709286f),
            new Float2(0.9435535266854258f, -0.3312200813348966f),
            new Float2(0.8649775992346886f, 0.5018104750024599f),
            new Float2(0.1808186720712497f, 0.9835164502083277f),
            new Float2(-0.6299339540895539f, 0.7766487066139361f),
            new Float2(-0.9996609468975833f, 0.02603826506945166f),
            new Float2(-0.6695112313914258f, -0.7428019325774111f),
            new Float2(0.12937272671950842f, -0.9915960354807594f),
            new Float2(0.8376810167470904f, -0.5461597881403947f),
            new Float2(0.959517028911149f, 0.28165061908243916f),
            new Float2(0.4095816551369482f, 0.9122734610714476f),
            new Float2(-0.42710760401484793f, 0.9042008043530463f),
            new Float2(-0.9647728141412515f, 0.2630844295924223f),
            new Float2(-0.8269869890664444f, -0.562221059650754f),
            new Float2(-0.11021592552380209f, -0.9939076666174438f),
            new Float2(0.6837188597775012f, -0.72974551782423f),
            new Float2(0.998972441738333f, 0.04532174585508431f),
            new Float2(0.6148313475439905f, 0.7886586169422362f),
            new Float2(-0.1997618324529528f, 0.9798444827088829f),
            new Float2(-0.8744989400706802f, 0.48502742583822706f),
            new Float2(-0.9369870231562731f, -0.3493641630687752f),
            new Float2(-0.3434772946489506f, -0.9391609809082988f),
            new Float2(0.4905057254335028f, -0.8714379687143274f),
            new Float2(0.9810787787756657f, -0.1936089611460388f),
            new Float2(0.7847847614201463f, 0.6197684069414349f),
            new Float2(0.03905187955516296f, 0.9992371844077906f),
            new Float2(-0.7340217731995672f, 0.6791259356474049f),
            new Float2(-0.9931964444524306f, -0.1164509455824639f),
            new Float2(-0.5570202966000876f, -0.830498879695542f),
            new Float2(0.2691336060685578f, -0.9631028512493016f),
            new Float2(0.9068632806061f, -0.4214249521425399f),
            new Float2(0.9096851999779008f, 0.4152984913783901f),
            new Float2(0.27562369868737335f, 0.9612656119522284f),
            new Float2(-0.5514058359842319f, 0.8342371389734039f),
            new Float2(-0.9923883787916933f, 0.12314749546456379f),
            new Float2(-0.7385858406439617f, -0.6741594440488484f),
            new Float2(0.032311046904542805f, -0.9994778618098213f),
            new Float2(0.7805865154410089f, -0.6250477517051506f),
            new Float2(0.9823623706068018f, 0.18698709264487903f),
            new Float2(0.49637249435561115f, 0.8681096398768929f),
            new Float2(-0.3371347561867868f, 0.9414564016304079f),
            new Float2(-0.9346092156607797f, 0.35567627697379833f),
            new Float2(-0.877750600058892f, -0.47911781859606817f),
            new Float2(-0.20636642697019966f, -0.9784747813917093f),
            new Float2(0.6094977881394418f, -0.7927877687333024f),
            new Float2(0.998644017504346f, -0.052058873429796634f),
            new Float2(0.6886255051458764f, 0.7251171723677399f),
            new Float2(-0.10350942208147358f, 0.9946284731196666f),
            new Float2(-0.8231759450656516f, 0.567786371327519f),
            new Float2(-0.9665253951623188f, -0.2565709658288005f),
            new Float2(-0.43319680340129196f, -0.9012993562201753f),
            new Float2(0.4034189716368784f, -0.9150153732716426f),
            new Float2(0.9575954428121146f, -0.28811624026678895f),
            new Float2(0.8413458575409575f, 0.5404971304259356f),
            new Float2(0.13605818775026976f, 0.9907008476558967f),
            new Float2(-0.664485735550556f, 0.7473009482463117f),
            new Float2(-0.999813836664718f, -0.01929487014147803f),
            new Float2(-0.6351581891853917f, -0.7723820781910558f),
            new Float2(0.17418065221630152f, -0.984713714941304f),
            new Float2(0.8615731658120597f, -0.5076334109892543f),
            new Float2(0.945766171482902f, 0.32484819358982736f),
            new Float2(0.3678149601703667f, 0.9298990026206456f),
            new Float2(-0.4676486851245607f, 0.883914423064399f),
            new Float2(-0.9757048995218635f, 0.2190889067228882f),
            new Float2(-0.8006563717736747f, -0.5991238388999518f),
            new Float2(-0.06505704156910719f, -0.9978815467490495f),
            new Float2(0.716089639712196f, -0.6980083293893113f),
            new Float2(0.9958918787052943f, 0.09055035024139549f),
            new Float2(0.5784561871098056f, 0.8157134543418942f),
            new Float2(-0.24396482815448167f, 0.9697840804135497f),
            new Float2(-0.8955826311865743f, 0.4448952131872543f),
            new Float2(-0.9201904205900768f, -0.39147105876968413f),
            new Float2(-0.3005599364234082f, -0.9537629289384008f),
            new Float2(0.5294967923694863f, -0.84831193960148f),
            new Float2(0.9888453593035162f, -0.1489458135829932f),
            new Float2(0.7558893631265085f, 0.6546993743025888f),
            new Float2(-0.006275422246980369f, 0.9999803093439501f),
            new Float2(-0.764046696121276f, 0.6451609459244744f),
            new Float2(-0.9868981170802014f, -0.16134468229090512f),
            new Float2(-0.5188082666339063f, -0.8548906260290385f),
            new Float2(0.31250655826478446f, -0.9499156020623616f),
            new Float2(0.9250311403279032f, -0.3798912863223621f),
            new Float2(0.889928392754896f, 0.45610026942404636f),
            new Float2(0.2317742435145519f, 0.9727696027545563f),
            new Float2(-0.5886483179573486f, 0.8083892365475831f),
            new Float2(-0.996949901406418f, 0.0780441803450664f),
            new Float2(-0.707272817672466f, -0.7069407057042696f),
            new Float2(0.07757592706207364f, -0.9969864470194466f),
            new Float2(0.8081126726681943f, -0.5890279350532263f),
            new Float2(0.9728783545459001f, 0.23131733021125322f),
            new Float2(0.4565181982253288f, 0.8897140746830408f),
            new Float2(-0.3794567783511009f, 0.9252094645881026f),
            new Float2(-0.9497687200714887f, 0.31295267753091066f),
            new Float2(-0.8551342041690687f, -0.5184066867432686f),
            new Float2(-0.16180818807538452f, -0.9868222283024238f),
            new Float2(0.6448020194233159f, -0.7643496292585048f),
            new Float2(0.9999772516247822f, -0.006745089543285545f),
            new Float2(0.6550543261176665f, 0.7555817823601425f),
            new Float2(-0.14848135899860646f, 0.9889152066936411f),
            new Float2(-0.848063153443784f, 0.5298951667745091f),
            new Float2(-0.9539039899003245f, -0.300111942535184f),
            new Float2(-0.3919032080850608f, -0.9200064540494471f),
            new Float2(0.44447452934057863f, -0.8957914895596358f),
            new Float2(0.9696693887216105f, -0.24442028675267172f),
            new Float2(0.8159850520735595f, 0.5780730012658526f),
            new Float2(0.0910180879994953f, 0.9958492394217692f),
            new Float2(-0.6976719213969089f, 0.7164173993520435f),
            new Float2(-0.9979119924958648f, -0.06458835214597858f),
            new Float2(-0.5994998228898376f, -0.8003748886334786f),
            new Float2(0.2186306161766729f, -0.9758076929755208f),
            new Float2(0.8836946816279001f, -0.46806378802740584f),
            new Float2(0.9300716543684309f, 0.36737816720699407f),
            new Float2(0.32529236260160294f, 0.9456134933645286f),
            new Float2(-0.5072286936943775f, 0.8618114946396893f),
            new Float2(-0.9846317976415725f, 0.17464313062106204f),
            new Float2(-0.7726803123417516f, -0.6347953488483143f),
            new Float2(-0.019764457813331488f, -0.9998046640256011f),
            new Float2(0.7469887719961158f, -0.6648366525032559f),
            new Float2(0.9907646418168752f, 0.13559286310672486f),
            new Float2(0.5408922318074902f, 0.8410919055432124f),
            new Float2(-0.2876664477065717f, 0.9577306588304888f),
            new Float2(-0.9148257956391065f, 0.40384868903250853f),
            new Float2(-0.9015027194859215f, -0.4327734358292892f),
            new Float2(-0.2570248925062563f, -0.9664047830139022f),
            new Float2(0.5673996816983953f, -0.8234425306046317f),
            new Float2(0.9945797473944409f, -0.10397656501736473f),
            new Float2(0.7254405241129018f, 0.6882848581617921f),
            new Float2(-0.05158982732517303f, 0.9986683582233687f),
            new Float2(-0.7925014140531963f, 0.609870075281354f),
            new Float2(-0.9785715990807187f, -0.20590683687679034f),
            new Float2(-0.47953002522651733f, -0.8775254725113429f),
            new Float2(0.35523727306945746f, -0.9347761656258549f),
            new Float2(0.9412979532686209f, -0.33757689964259285f),
            new Float2(0.868342678987353f, 0.4959647082697184f),
            new Float2(0.18744846526420056f, 0.9822744386728669f),
            new Float2(-0.6246810590458048f, 0.7808800000444446f),
            new Float2(-0.9994625758058275f, 0.03278047534097766f),
            new Float2(-0.674506266646887f, -0.738269121834361f),
            new Float2(0.12268137965007223f, -0.9924461089082646f),
            new Float2(0.8339780641890598f, -0.5517975973592748f),
            new Float2(0.9613949601033843f, 0.2751721837101493f),
            new Float2(0.41572570400265835f, 0.9094900433932711f),
            new Float2(-0.42099897262033487f, 0.907061114287578f),
            new Float2(-0.9629763390922247f, 0.2695859238694348f),
            new Float2(-0.8307604078465821f, -0.5566301687427484f),
            new Float2(-0.11691741449967302f, -0.9931416405461567f),
            new Float2(0.6787811074228051f, -0.7343406622310046f),
            new Float2(0.999255415972447f, 0.03858255628819732f),
            new Float2(0.6201369341201711f, 0.7844935837468874f),
            new Float2(-0.19314814942146824f, 0.9811696042861612f),
            new Float2(-0.8712074932224428f, 0.4909149659086258f),
            new Float2(-0.9393222007870077f, -0.34303615422962713f),
            new Float2(-0.3498042060103595f, -0.9368228314134226f),
            new Float2(0.4846166400948296f, -0.8747266499559725f),
            new Float2(0.9797505510481769f, -0.20022202106859724f),
            new Float2(0.7889473022428521f, 0.6144608647291752f),
            new Float2(0.045790935472179155f, 0.9989510449609544f),
            new Float2(-0.7294243101497431f, 0.684061529222753f),
            new Float2(-0.9939593229024027f, -0.10974909756074072f),
            new Float2(-0.562609414602539f, -0.8267228354174018f),
            new Float2(0.26263126874523307f, -0.9648962724963078f),
            new Float2(0.9040001019019392f, -0.4275322394408211f),
            new Float2(0.9124657316291773f, 0.4091531358824348f),
            new Float2(0.28210125132356934f, 0.9593846381935018f),
            new Float2(-0.5457662881946498f, 0.8379374431723614f),
            new Float2(-0.9915351626845509f, 0.12983844253579577f),
            new Float2(-0.7431163048326799f, -0.6691622803863227f),
            new Float2(0.02556874420628532f, -0.9996730662170076f),
            new Float2(0.7763527553119807f, -0.6302986588273021f),
            new Float2(0.9836012681423212f, 0.1803567168386515f),
            new Float2(0.5022166799422209f, 0.8647418148718223f),
            new Float2(-0.330776879188771f, 0.9437089891455613f),
            new Float2(-0.9321888864830543f, 0.3619722087639923f),
            new Float2(-0.8809623252471085f, -0.47318641305008735f),
            new Float2(-0.21296163248563432f, -0.9770605626515961f),
            new Float2(0.604136498566135f, -0.7968808512571063f),
            new Float2(0.9982701582127194f, -0.05879363249495786f),
            new Float2(0.6935008202914851f, 0.7204558364362367f),
            new Float2(-0.09679820929680796f, 0.9953040272584711f),
            new Float2(-0.8193274492343137f, 0.5733258505694586f),
            new Float2(-0.9682340024187017f, -0.25004582891994304f),
            new Float2(-0.4392662937408502f, -0.8983569018954422f),
            new Float2(0.39723793388455464f, -0.9177156552457467f),
            new Float2(0.9556302892322005f, -0.2945687530984589f),
            new Float2(0.8449724198323217f, 0.5348098818484104f),
            new Float2(0.14273745857559722f, 0.9897605861618151f),
            new Float2(-0.6594300077680133f, 0.7517659641504648f),
            new Float2(-0.9999212381512442f, -0.01255059735959867f),
            new Float2(-0.6403535266476091f, -0.768080308893523f),
            new Float2(0.16753470770767478f, -0.9858661784001437f),
            new Float2(0.8581295336101056f, -0.5134332513054668f),
            new Float2(0.9479357869928937f, 0.31846152630759517f),
            new Float2(0.37407884501651706f, 0.9273969040875156f),
            new Float2(-0.461675964944643f, 0.8870486477034012f),
            new Float2(-0.9742049295269273f, 0.22566513972130173f),
            new Float2(-0.8046793020829978f, -0.5937097108850584f),
            new Float2(-0.07178636201352963f, -0.9974200309943962f),
            new Float2(0.7113652211526822f, -0.7028225395748172f),
            new Float2(0.9964799940037152f, 0.08383091047075403f),
            new Float2(0.5839450884626246f, 0.8117931594072332f),
            new Float2(-0.23741799789097484f, 0.9714075840127259f),
            new Float2(-0.8925614000865144f, 0.45092587758477687f),
            new Float2(-0.9228099950981292f, -0.38525538665538556f),
            new Float2(-0.30698631553196837f, -0.95171392869712f),
            new Float2(0.5237628071845146f, -0.8518641451605984f),
            new Float2(0.9878182118285335f, -0.15561227580071732f),
            new Float2(0.7602881737752754f, 0.6495859395164404f),
            new Float2(4.6967723669845613E-4f, 0.9999998897016406f),
            new Float2(-0.7596776469502666f, 0.6502998329417794f),
            new Float2(-0.9879639510809196f, -0.15468429579171308f),
            new Float2(-0.5245627784110601f, -0.8513717704420726f),
            new Float2(0.3060921834538644f, -0.9520018777441807f),
            new Float2(0.9224476966294768f, -0.3861220622846781f),
            new Float2(0.8929845854878761f, 0.45008724718774934f),
            new Float2(0.23833038910266038f, 0.9711841358002995f),
            new Float2(-0.5831822693781987f, 0.8123413326200348f),
            new Float2(-0.9964008074312266f, 0.0847669213219385f),
            new Float2(-0.712025106726807f, -0.7021540054650968f),
            new Float2(0.07084939947717452f, -0.9974870237721009f),
            new Float2(0.8041212432524677f, -0.5944653279629567f),
            new Float2(0.9744164792492415f, 0.22474991650168097f),
            new Float2(0.462509014279733f, 0.8866145790082576f),
    };
    // takes slightly less storage than an array of float[2]
    private static class Float2 {
        public final float x, y;

        Float2(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    // takes slightly less storage than an array of float[3]
    private static class Float3 {
        public final float x, y, z;

        Float3(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final Float3[] GRAD_3D =
            {
                    new Float3(-0.448549002408981f,  1.174316525459290f,  0.000000000000001f  ),
                    new Float3(0.000000000000001f,  1.069324374198914f,  0.660878777503967f   ),
                    new Float3(0.448549002408981f,  1.174316525459290f,  0.000000000000001f   ),
                    new Float3(0.000000000000001f,  1.069324374198914f, -0.660878777503967f   ),
                    new Float3(-0.725767493247986f,  0.725767493247986f, -0.725767493247986f  ),
                    new Float3(-1.069324374198914f,  0.660878777503967f,  0.000000000000001f  ),
                    new Float3(-0.725767493247986f,  0.725767493247986f,  0.725767493247986f  ),
                    new Float3(0.725767493247986f,  0.725767493247986f,  0.725767493247986f   ),
                    new Float3(1.069324374198914f,  0.660878777503967f,  0.000000000000000f   ),
                    new Float3(0.725767493247986f,  0.725767493247986f, -0.725767493247986f   ),
                    new Float3(-0.660878777503967f,  0.000000000000003f, -1.069324374198914f  ),
                    new Float3(-1.174316525459290f,  0.000000000000003f, -0.448549002408981f  ),
                    new Float3(0.000000000000000f,  0.448549002408981f, -1.174316525459290f   ),
                    new Float3(-0.660878777503967f,  0.000000000000001f,  1.069324374198914f  ),
                    new Float3(0.000000000000001f,  0.448549002408981f,  1.174316525459290f   ),
                    new Float3(-1.174316525459290f,  0.000000000000001f,  0.448549002408981f  ),
                    new Float3(0.660878777503967f,  0.000000000000001f,  1.069324374198914f   ),
                    new Float3(1.174316525459290f,  0.000000000000001f,  0.448549002408981f   ),
                    new Float3(0.660878777503967f,  0.000000000000001f, -1.069324374198914f   ),
                    new Float3(1.174316525459290f,  0.000000000000001f, -0.448549002408981f   ),
                    new Float3(-0.725767493247986f, -0.725767493247986f, -0.725767493247986f  ),
                    new Float3(-1.069324374198914f, -0.660878777503967f, -0.000000000000001f  ),
                    new Float3(-0.000000000000001f, -0.448549002408981f, -1.174316525459290f  ),
                    new Float3(-0.000000000000001f, -0.448549002408981f,  1.174316525459290f  ),
                    new Float3(-0.725767493247986f, -0.725767493247986f,  0.725767493247986f  ),
                    new Float3(0.725767493247986f, -0.725767493247986f,  0.725767493247986f   ),
                    new Float3(1.069324374198914f, -0.660878777503967f,  0.000000000000001f   ),
                    new Float3(0.725767493247986f, -0.725767493247986f, -0.725767493247986f   ),
                    new Float3(-0.000000000000004f, -1.069324374198914f, -0.660878777503967f  ),
                    new Float3(-0.448549002408981f, -1.174316525459290f, -0.000000000000003f  ),
                    new Float3(-0.000000000000003f, -1.069324374198914f,  0.660878777503967f  ),
                    new Float3(0.448549002408981f, -1.174316525459290f,  0.000000000000003f   ),
            };
    protected static final float[] grad4f =
            {
                    -0.5875167f, 1.4183908f, 1.4183908f, 1.4183908f,
                    -0.5875167f, 1.4183908f, 1.4183908f, -1.4183908f,
                    -0.5875167f, 1.4183908f, -1.4183908f, 1.4183908f,
                    -0.5875167f, 1.4183908f, -1.4183908f, -1.4183908f,
                    -0.5875167f, -1.4183908f, 1.4183908f, 1.4183908f,
                    -0.5875167f, -1.4183908f, 1.4183908f, -1.4183908f,
                    -0.5875167f, -1.4183908f, -1.4183908f, 1.4183908f,
                    -0.5875167f, -1.4183908f, -1.4183908f, -1.4183908f,
                    1.4183908f, -0.5875167f, 1.4183908f, 1.4183908f,
                    1.4183908f, -0.5875167f, 1.4183908f, -1.4183908f,
                    1.4183908f, -0.5875167f, -1.4183908f, 1.4183908f,
                    1.4183908f, -0.5875167f, -1.4183908f, -1.4183908f,
                    -1.4183908f, -0.5875167f, 1.4183908f, 1.4183908f,
                    -1.4183908f, -0.5875167f, 1.4183908f, -1.4183908f,
                    -1.4183908f, -0.5875167f, -1.4183908f, 1.4183908f,
                    -1.4183908f, -0.5875167f, -1.4183908f, -1.4183908f,
                    1.4183908f, 1.4183908f, -0.5875167f, 1.4183908f,
                    1.4183908f, 1.4183908f, -0.5875167f, -1.4183908f,
                    1.4183908f, -1.4183908f, -0.5875167f, 1.4183908f,
                    1.4183908f, -1.4183908f, -0.5875167f, -1.4183908f,
                    -1.4183908f, 1.4183908f, -0.5875167f, 1.4183908f,
                    -1.4183908f, 1.4183908f, -0.5875167f, -1.4183908f,
                    -1.4183908f, -1.4183908f, -0.5875167f, 1.4183908f,
                    -1.4183908f, -1.4183908f, -0.5875167f, -1.4183908f,
                    1.4183908f, 1.4183908f, 1.4183908f, -0.5875167f,
                    1.4183908f, 1.4183908f, -1.4183908f, -0.5875167f,
                    1.4183908f, -1.4183908f, 1.4183908f, -0.5875167f,
                    1.4183908f, -1.4183908f, -1.4183908f, -0.5875167f,
                    -1.4183908f, 1.4183908f, 1.4183908f, -0.5875167f,
                    -1.4183908f, 1.4183908f, -1.4183908f, -0.5875167f,
                    -1.4183908f, -1.4183908f, 1.4183908f, -0.5875167f,
                    -1.4183908f, -1.4183908f, -1.4183908f, -0.5875167f,
                    0.5875167f, 1.4183908f, 1.4183908f, 1.4183908f,
                    0.5875167f, 1.4183908f, 1.4183908f, -1.4183908f,
                    0.5875167f, 1.4183908f, -1.4183908f, 1.4183908f,
                    0.5875167f, 1.4183908f, -1.4183908f, -1.4183908f,
                    0.5875167f, -1.4183908f, 1.4183908f, 1.4183908f,
                    0.5875167f, -1.4183908f, 1.4183908f, -1.4183908f,
                    0.5875167f, -1.4183908f, -1.4183908f, 1.4183908f,
                    0.5875167f, -1.4183908f, -1.4183908f, -1.4183908f,
                    1.4183908f, 0.5875167f, 1.4183908f, 1.4183908f,
                    1.4183908f, 0.5875167f, 1.4183908f, -1.4183908f,
                    1.4183908f, 0.5875167f, -1.4183908f, 1.4183908f,
                    1.4183908f, 0.5875167f, -1.4183908f, -1.4183908f,
                    -1.4183908f, 0.5875167f, 1.4183908f, 1.4183908f,
                    -1.4183908f, 0.5875167f, 1.4183908f, -1.4183908f,
                    -1.4183908f, 0.5875167f, -1.4183908f, 1.4183908f,
                    -1.4183908f, 0.5875167f, -1.4183908f, -1.4183908f,
                    1.4183908f, 1.4183908f, 0.5875167f, 1.4183908f,
                    1.4183908f, 1.4183908f, 0.5875167f, -1.4183908f,
                    1.4183908f, -1.4183908f, 0.5875167f, 1.4183908f,
                    1.4183908f, -1.4183908f, 0.5875167f, -1.4183908f,
                    -1.4183908f, 1.4183908f, 0.5875167f, 1.4183908f,
                    -1.4183908f, 1.4183908f, 0.5875167f, -1.4183908f,
                    -1.4183908f, -1.4183908f, 0.5875167f, 1.4183908f,
                    -1.4183908f, -1.4183908f, 0.5875167f, -1.4183908f,
                    1.4183908f, 1.4183908f, 1.4183908f, 0.5875167f,
                    1.4183908f, 1.4183908f, -1.4183908f, 0.5875167f,
                    1.4183908f, -1.4183908f, 1.4183908f, 0.5875167f,
                    1.4183908f, -1.4183908f, -1.4183908f, 0.5875167f,
                    -1.4183908f, 1.4183908f, 1.4183908f, 0.5875167f,
                    -1.4183908f, 1.4183908f, -1.4183908f, 0.5875167f,
                    -1.4183908f, -1.4183908f, 1.4183908f, 0.5875167f,
                    -1.4183908f, -1.4183908f, -1.4183908f, 0.5875167f,
            };
    protected static final float[] grad6f = {
            0.31733186658157f, 0.043599150809166f, -0.63578104939541f,
            0.60224147484783f, -0.061995657882187f, 0.35587048501823f,
            -0.54645425808647f, -0.75981513883963f, -0.035144342454363f,
            0.13137365402959f, 0.29650029456531f, 0.13289887942467f,
            0.72720729277573f, -0.0170513084554f, 0.10403853926717f,
            0.57016794579524f, 0.10006650294475f, -0.35348266879289f,
            0.0524867271859f, 0.16599786784909f, -0.49406271077513f,
            0.51847470894887f, 0.63927166664011f, -0.21933445140234f,
            -0.57224122530978f, -0.089985946187774f, 0.44829955643248f,
            0.53836681748476f, -0.051299333576026f, -0.41352093713992f,
            -0.35034584363296f, -0.37367516013323f, -0.52676009109159f,
            0.12379417201967f, 0.42566489477591f, 0.51345191723381f,
            0.40936909283115f, 0.33036021753157f, 0.46771483894695f,
            0.15073372728805f, 0.51541333179083f, -0.46491971651678f,
            -0.64339751231027f, -0.29341468636474f, -0.50841617762291f,
            -0.080659811936781f, -0.46873502824317f, -0.12345817650503f,
            0.46950904113222f, 0.41685007896275f, -0.33378791988356f,
            -0.39617029121348f, 0.54659770033168f, 0.19662896748851f,
            -0.49213884108338f, 0.50450587466563f, -0.0073247243900323f,
            0.57958418990163f, 0.39591449230465f, 0.10272980841415f,
            0.34572956497624f, 0.62770109739866f, 0.12165109216674f,
            0.35267248385686f, 0.34842369637704f, -0.47527514024373f,
            0.076282233884284f, 0.56461194794873f, -0.392426730607f,
            -0.20639693057567f, 0.33197602170266f, 0.60711436994661f,
            0.46792592791359f, -0.38434666353171f, -0.46719345820863f,
            -0.40169520060432f, -0.061343490026986f, 0.49993117813162f,
            -0.25398819915038f, -0.82255018555745f, 0.40372967512401f,
            0.21051604195389f, 0.020384827146984f, 0.22621006002887f,
            0.23269489013955f, -0.42234243708413f, -0.18886779174866f,
            0.44290933725703f, -0.40895242871151f, 0.60695810498111f,
            -0.13615585122038f, 0.26142849716038f, 0.68738606675966f,
            0.42914965171764f, 0.26332301994884f, 0.43256061294487f,
            0.06145597366231f, -0.25432792035414f, 0.65050463165568f,
            0.35622065678761f, -0.52670947710524f, -0.32259598080167f,
            -0.28027055313228f, 0.30275296247348f, 0.39083872911587f,
            0.17564171472763f, 0.25278203996272f, 0.76307625890429f,
            -0.62937098181034f, -0.24958587788613f, 0.11855057687171f,
            0.52714220921895f, 0.47759151204224f, -0.14687496867489f,
            0.68607574135496f, 0.28465344118508f, 0.57132493696771f,
            0.11365238375433f, -0.32111327299854f, -0.076352560636185f,
            0.42669573845021f, -0.1643996530281f, -0.54881376863042f,
            -0.56551221465284f, 0.4027156095588f, -0.087880721039792f,
            -0.30211042220321f, -0.47278547361731f, 0.050137867251391f,
            0.46804387457884f, -0.39450159355792f, 0.55497099667426f,
            0.31255895138908f, 0.034478918459459f, -0.079232996020732f,
            0.39803160685016f, 0.82281399721198f, 0.24369695191021f,
            -0.5524321671417f, 0.49350231710234f, 0.52530668244467f,
            0.253625789825f, 0.26218499242504f, -0.20557247282514f,
            0.060763010271891f, -0.023938406391206f, 0.36557410300471f,
            0.55368747615095f, 0.25557899769702f, -0.70014279913759f,
            0.36398574324757f, 0.049110464042478f, -0.2428951164628f,
            -0.18733973495522f, 0.020130805835303f, 0.87784000694654f,
            -0.62385490124849f, 0.020947599003133f, -0.44548631925386f,
            -0.21069894502123f, -0.60559127508405f, 0.027809382425643f,
            0.51562840479369f, -0.27416131751628f, -0.14365580420426f,
            -0.46525735490594f, 0.16338488557607f, 0.62862302132303f,
            0.52085189275139f, 0.51359303425374f, 0.021844789421786f,
            0.53521775458267f, -0.23767218281397f, -0.34858599348565f,
            0.12263603513069f, 0.53912951801629f, 0.57550729534804f,
            -0.10335514143554f, 0.57524709075397f, 0.14662748040551f,
            0.40942178494947f, 0.17197663954561f, -0.025238012475873f,
            -0.20104824969996f, -0.60303014654018f, 0.63094779803243f,
            0.051685704973311f, 0.23577798459204f, -0.19154992327678f,
            -0.67743578708385f, -0.51070301615526f, 0.43047548181493f,
            0.21373839204543f, -0.44348268823586f, 0.34347986958921f,
            -0.49945694096162f, 0.45888698118478f, -0.42382317871053f,
            -0.60376535923059f, -0.065300874745824f, 0.49448067868339f,
            0.12358559784007f, 0.58623743735263f, -0.16656623971303f,
            0.44140930948322f, -0.41692548571374f, -0.23774988226818f,
            -0.27542786466885f, 0.39264397083621f, 0.58717642823542f,
            -0.67860697457746f, 0.2070991391515f, -0.12832398784247f,
            -0.58381216132288f, 0.24050209342748f, 0.2854077401022f,
            -0.021324501342617f, 0.0098658783730532f, 0.2694901128571f,
            0.42580554353158f, -0.82903198308789f, -0.24128534823695f,
            -0.20344882384938f, 0.51719618805529f, 0.24379623299129f,
            0.11303683173372f, -0.46058654895958f, -0.63777957124993f,
            0.15686479897897f, -0.67777169905813f, -0.04974608057712f,
            0.51313211803344f, 0.49928667286231f, -0.030863149692696f,
            0.53527130791104f, -0.50102597915466f, -0.60754472649714f,
            -0.25235098830686f, 0.13490559284448f, 0.10708155847142f,
            -0.20613512232544f, 0.39533044356843f, -0.34422306275706f,
            0.4792145528465f, -0.19178040223502f, -0.64521804411898f,
            0.3304779611047f, 0.49148538926455f, -0.30004348427342f,
            0.33473309391851f, 0.31079743137844f, 0.59208027276116f,
            -0.52688857216953f, 0.40250311061529f, 0.38833191043333f,
            0.50432308135853f, -0.33327489215794f, -0.21015252001231f,
            -0.30306420816123f, -0.34460825415019f, -0.26894228639121f,
            -0.58579646837355f, -0.51178483212848f, 0.33464319317466f,
            -0.20258582390514f, -0.29195675136034f, 0.11887973573086f,
            0.91211540292822f, 0.034118810787236f, -0.16269371903027f,
            0.61207678339522f, -0.21883722070929f, -0.23415725333464f,
            0.0041447691596985f, -0.34019274152454f, 0.6378827339521f,
            0.11272999861808f, -0.54780877011146f, -0.62497664375172f,
            -0.41373740141301f, 0.33306010353229f, 0.12039112788093f,
            0.24918468395037f, -0.068734287809286f, -0.42234580029763f,
            0.12235329631887f, -0.26545138767734f, 0.81815148205875f,
            0.32048708659406f, -0.40233908147851f, 0.24633289057781f,
            -0.37087758270512f, -0.55466799718133f, -0.47908728788262f,
            -0.33748729653627f, -0.45507986822699f, -0.50597645316527f,
            -0.2863701644881f, -0.5404199724601f, -0.22120318557996f,
            -0.23520314824941f, 0.82195093398991f, -0.22661283339659f,
            0.16382454786402f, -0.41400232366734f, -0.13959354720703f,
            -0.30495751902889f, -0.47964557116121f, -0.68490238495876f,
            -0.4324077675155f, -0.13521732523742f, -0.050887702629247f,
            -0.56629250538137f, 0.19768903044f, -0.080075220953828f,
            -0.29952637623112f, 0.095974426142512f, -0.73136356489112f,
            -0.21316607993139f, 0.47585902758173f, -0.49429850443227f,
            -0.24146904800157f, 0.45631329089651f, 0.46610972545109f,
            0.12647584748018f, -0.10203700758813f, 0.20801341293098f,
            0.66418891258418f, -0.65219775460192f, -0.2526141453282f,
            -0.69345279552921f, 0.30149980453822f, -0.46870940095961f,
            0.20092958919922f, -0.21817920622376f, 0.34721422759447f,
            -0.69001417476102f, 0.09722776919634f, -0.37852252163632f,
            -0.24995374433763f, 0.24829304775112f, 0.4970126640943f,
            -0.82278510972964f, 0.050748830242865f, -0.3934733016285f,
            0.00029980431140623f, -0.34677214869339f, -0.21301870187776f,
            -0.51821811089111f, -0.22147302694699f, 0.53524316281446f,
            0.12892242816244f, -0.5543955478928f, -0.26821451961648f,
            -0.21006612796354f, 0.26079212570498f, -0.021870637510645f,
            0.72402587064608f, -0.27651658712238f, 0.53544979218311f,
            -0.099744280251479f, -0.4534212871731f, 0.71954978543864f,
            -0.31082396323078f, -0.26933824624449f, 0.31233586755618f,
            -0.48121951222937f, -0.43051247772929f, -0.5038415181805f,
            0.12342710418307f, 0.037467829082858f, -0.55909965468017f,
            -0.51180831908824f, -0.079955485578946f, -0.53046702060975f,
            0.48748209854708f, 0.16148937559829f, -0.43191028009105f,
            -0.38131649706702f, 0.46242477534251f, 0.46416075424014f,
            -0.20634110277567f, -0.53778490132009f, 0.30582118902172f,
            0.6245043069106f, 0.14316692963071f, -0.1436103838143f,
            0.27519251589203f, -0.60467865310212f, -0.35708047307373f,
            0.52425890739441f, -0.20390682829262f, -0.33609142609195f,
            0.51803372559413f, 0.28921536255925f, 0.46756035964091f,
            -0.4455164148456f, 0.31831805515328f, 0.24217750314789f,
            0.49821219078654f, -0.47209418708575f, 0.41285649844363f,
            -0.015857310429397f, -0.45214512052441f, -0.14591363373753f,
            0.74070676188619f, 0.0098874230592725f, -0.47463489014478f,
            0.24260837156464f, 0.44639366601915f, 0.31528570191456f,
            0.45334773303464f, -0.47964168123625f, -0.45484996397296f,
            0.47123463487178f, 0.64525048646519f, -0.064257637508608f,
            -0.18737730572971f, -0.11735335340515f, -0.55549853319118f,
            -0.025197229767488f, -0.257963271803f, 0.26277107860996f,
            -0.58236203161499f, -0.41893538667715f, 0.59086294196016f,
            -0.48940330017687f, 0.33728563842186f, -0.057634928591543f,
            0.44862021996899f, -0.40048256377746f, 0.53080564921806f,
            0.73350664260388f, -0.021482988114587f, 0.016568147533453f,
            0.0021905972927896f, 0.49384961731337f, 0.46619710394628f,
            -0.25151229880228f, -0.62009962583403f, -0.26948657433033f,
            0.31711936293198f, -0.35081923073755f, 0.50592112116981f,
            0.0094298597779172f, -0.35925999444899f, 0.47529205807388f,
            -0.26709475088579f, -0.53352146543694f, 0.53754630836074f,
            -0.5948549517534f, -0.53195924881292f, -0.094383768924555f,
            -0.41704491211939f, -0.41397531920841f, -0.09463944474724f,
            -0.74917126125127f, -0.24166385705367f, 0.22864554725283f,
            0.31721357549513f, 0.06066292638611f, -0.47303041351952f,
            -0.3300396030254f, -0.08758658200966f, -0.096726092930468f,
            -0.39607089556472f, 0.55566932028997f, 0.63906648027271f,
            -0.58933068378397f, -0.38176870540341f, 0.46748019640554f,
            -0.061358837959321f, 0.36268480315292f, -0.39127879224432f,
            -0.066556695042975f, -0.73863083674701f, -0.32153946998935f,
            0.57454599361106f, -0.090856896694743f, -0.09082394033963f,
            -0.36335404704287f, -0.41643677881158f, -0.57839830999334f,
            -0.030959887755637f, 0.5989792522053f, -0.016582566905843f,
            0.23126668855143f, 0.2107790785413f, -0.14272193312959f,
            -0.29232225134991f, -0.48451339172564f, -0.74934159314943f,
            0.48188197979627f, -0.040214759215399f, -0.15667971883369f,
            0.16054853668069f, -0.6083975436752f, -0.58796308779952f,
            0.31319356064062f, -0.19280657835646f, 0.76136690598738f,
            -0.084506239097717f, 0.4768786755523f, -0.22472488900872f,
            0.67504537519138f, 0.36920158913876f, 0.40321048682396f,
            0.034436041975613f, -0.29332731631919f, 0.39774172001359f,
            -0.1459159803857f, -0.59726183207777f, -0.036384224081948f,
            -0.65093487874945f, 0.39515711468056f, -0.20198429937477f,
            0.60092128630869f, 0.18110182176699f, 0.2579491954112f,
            -0.39594768022975f, 0.15112959843347f, 0.59995268930018f,
            -0.42310244265976f, -0.26937197256148f, 0.074700012546319f,
            0.53119510349465f, 0.41614374632783f, 0.53618944036115f,
            0.0071605427687482f, -0.69599782505338f, -0.053138604739257f,
            -0.00054500262230378f, 0.69533871546989f, 0.1709263483943f,
            0.12447149375466f, 0.33265313001972f, 0.35070015349473f,
            0.53879932284829f, 0.37648083373421f, 0.56463759722353f,
            0.29540077719054f, 0.04954124873475f, -0.48345087234985f,
            0.72758494948264f, 0.070069102610626f, 0.377186640377f,
            0.4882414260383f, 0.45135801463006f, 0.48450857902353f,
            -0.26042407965644f, -0.4251358047458f, 0.2731053563007f,
            -0.49806371818291f, -0.4719759672029f, 0.029647087810764f,
            -0.13788472163255f, -0.45346141932978f, -0.5510470160674f,
            -0.5359511936033f, -0.53585470245895f, 0.1771036246335f,
            -0.4537763243703f, 0.41838964069644f, 0.11527149720722f,
            -0.36846431808379f, -0.46533180802325f, 0.65800816763703f,
            -0.28691297783558f, 0.31521457275327f, 0.18178647457201f,
            -0.29243126901345f, -0.4352956525447f, -0.58895978125929f,
            -0.49649471729812f, 0.29271342931272f, 0.21433587621517f,
            0.056256690265475f, -0.50387710054371f, 0.48145041862725f,
            0.44723671964597f, -0.55771174894027f, -0.0092449146014199f,
            -0.40973125164006f, -0.73147173623276f, -0.094076302480945f,
            0.43033451471976f, 0.014334271843521f, -0.32066459724334f,
            0.26752725373294f, 0.50477344684769f, 0.065069516529324f,
            0.36001097578267f, 0.59393393889869f, -0.43247366096278f,
            0.48945720845334f, 0.6043315650632f, 0.12458128550608f,
            -0.48327805813458f, -0.25681943056744f, 0.28316179557217f,
            -0.45182760404001f, 0.21574002665039f, -0.31462623994251f,
            0.25279349500371f, 0.44865729380505f, -0.62058075048081f,
            0.44017304540101f, 0.43789555905674f, 0.58423563606269f,
            0.41842994331139f, -0.26836655962348f, 0.16143005677844f,
            -0.67897032028819f, -0.32730885869255f, -0.0243997359109f,
            0.40649244381227f, 0.47711065295824f, -0.19596475712206f,
            0.57441588138131f, 0.09386994843744f, 0.28400793066375f,
            0.59394229842661f, 0.45349906020748f, 0.14881354725974f,
            -0.3393739967757f, -0.54929055652002f, 0.26209493900588f,
            0.0733800373509f, 0.56557076402003f, 0.43492125584075f,
            0.050007991188197f, 0.74652764513134f, -0.36432144611385f,
            -0.20993543754239f, -0.1352041047841f, 0.49508839805322f,
            -0.041332158875019f, -0.20655741061568f, 0.52511282214888f,
            0.047248635933477f, -0.6276121766011f, -0.5326844609727f,
            -0.1889491176448f, 0.05188976739355f, -0.45677123586268f,
            0.42884456750344f, 0.61612085530435f, -0.43526216197988f,
            -0.65873541163911f, -0.094770059351695f, 0.40844030815782f,
            0.35536013391048f, -0.16940065827957f, 0.48506226422661f,
            -0.45779281442862f, -0.46052673126242f, 0.34138050378631f,
            -0.54943270263121f, 0.37140594702643f, -0.14826175595089f,
            -0.069378715405383f, -0.14845488608058f, -0.73991837897813f,
            0.41519184526768f, -0.11098464009855f, -0.49088356499611f,
            0.46422563805447f, 0.46130716873201f, -0.44207791495441f,
            0.12050605352899f, 0.34969556083561f, -0.4893349322843f,
            -0.35482925073362f, 0.28146983672487f, -0.35356606227648f,
            -0.38774754218768f, 0.35979702647173f, -0.62454776976122f,
            -0.48343191508515f, 0.41492185792886f, -0.50175316406656f,
            0.21953122931153f, -0.54083165333237f, 0.041040952107647f,
            -0.51280508048852f, -0.54131124436697f, -0.0099287129207481f,
            0.23788701199175f, 0.4350333223576f, 0.44505087885649f,
            0.2253837335044f, -0.30117119745248f, 0.46587685049056f,
            -0.46672901001472f, -0.59182069765377f, 0.27086737661249f,
            0.43015756480475f, -0.067851118947538f, -0.26917802105288f,
            -0.57731860676632f, -0.53950120703807f, -0.33696522367557f,
            0.20858352742161f, 0.63695057987625f, 0.49453142202915f,
            -0.046235371593379f, -0.54436247241885f, -0.088075720520231f,
            -0.35626464703623f, 0.067539543974725f, -0.18142793486226f,
            -0.49044207117167f, 0.5542388249925f, 0.53654796190017f,
            0.52238539932434f, 0.55175875223621f, 0.29070268774296f,
            -0.14119026819648f, -0.55841587206055f, -0.080029639759127f,
            -0.025988002903175f, 0.46612949273683f, -0.56880970348453f,
            -0.44824563336003f, -0.030000490931808f, 0.50663523727173f,
            0.047284583258099f, -0.26595723160738f, 0.21032033434131f,
            0.52986834914146f, -0.52245334572957f, -0.5736534757312f,
            -0.31924244568277f, -0.13888420092891f, 0.30725800370737f,
            0.49792332552544f, 0.61035592292817f, -0.40487771982263f,
            0.038758575627018f, -0.53813545398707f, -0.56167256912901f,
            0.46815373895572f, -0.14142713486975f, 0.39276248966752f,
            -0.19936871608885f, 0.12488860648831f, -0.62990029833727f,
            -0.29296146144627f, 0.49734531468753f, 0.46335923993672f,
            -0.078826705546604f, -0.15548800857414f, 0.57456768467721f,
            0.5558854465212f, -0.56893054194692f, -0.082408823513622f,
            0.11678856295109f, 0.53358760166951f, 0.49302489382249f,
            -0.53981846952046f, -0.237913367643f, -0.33251226509871f,
            0.39126928439834f, -0.39416116630681f, -0.35778844984527f,
            -0.39395609960567f, 0.50270356681194f, -0.39448759513757f,
            -0.17961290695406f, 0.34239532682819f, -0.21870225043453f,
            -0.23322835296688f, 0.75997835134209f, 0.41317237364121f,
            0.29699501400111f, 0.17195435585404f, -0.34903627841034f,
            -0.31751884057854f, -0.59661546358767f, 0.55102732418683f,
            -0.2237291316445f, -0.51254305965518f, -0.31277318571798f,
            0.54270199705442f, -0.34885011313806f, 0.41616819064585f,
            0.53534023676892f, 0.45905986582643f, -0.20308675275303f,
            0.019523641323632f, 0.3378580580099f, 0.58898336258938f,
            -0.045038463119119f, -0.52553334288797f, -0.6098545897634f,
            0.46226027841702f, -0.36069029000651f, 0.077984430434637f,
            -0.40129033029845f, 0.39526722066586f, -0.20379584931963f,
            0.45466492237669f, 0.46504795737483f, -0.46712669863522f,
            -0.43845831945339f, -0.59284534057943f, 0.050241908216277f,
            -0.36494839821973f, 0.32363879325018f, 0.46458051299488f,
            -0.46057360356064f, -0.34584626825548f, -0.12264748451482f,
            0.48835437094478f, 0.21102526990984f, 0.60843919401837f,
            -0.086047549693024f, -0.16981605114589f, -0.37222833669973f,
            0.45158609930017f, -0.55710254634126f, 0.55759406480139f,
            0.54697451263099f, -0.45070837355303f, 0.032962522247893f,
            -0.48584332140086f, -0.28055687213837f, 0.42642516953676f,
            0.34061925303691f, 0.38443007758012f, 0.61614808332652f,
            -0.55774172327958f, -0.075660378162998f, 0.19938218730551f,
            0.30626924920956f, -0.057939049897675f, -0.10461119704504f,
            -0.4395638756485f, -0.57307193269415f, 0.60849886616281f,
            -0.52519951444608f, -0.42567534157254f, -0.19896500097138f,
            0.48819483593271f, 0.12539008064447f, 0.49932157157064f,
            -0.10173361116951f, -0.07873850987854f, 0.3713554090283f,
            0.65889542748449f, 0.63411890875068f, 0.096414235519521f,
            0.60342393773609f, 0.057617370697663f, 0.35558841250938f,
            0.20766418929404f, 0.030670189501999f, -0.67974377143949f,
            -0.071971052874019f, -0.44567383014704f, 0.65917594080871f,
            0.44113802003588f, -0.29627117199757f, 0.28160739274962f,
            0.38284479693596f, 0.43552320173998f, -0.4282368470258f,
            -0.54809258921772f, -0.27202273485667f, 0.32551612927831f,
            -0.74755699288716f, -0.20979308948438f, 0.19268299390085f,
            0.27864013929953f, -0.39085278833717f, 0.36001727246301f,
            -0.64575536737195f, 0.59253747557756f, 0.040885512266333f,
            -0.20167391777406f, -0.43481684011627f, -0.02212841779644f,
            0.45874103754271f, -0.0066587566394561f, -0.30494054091993f,
            0.52731059172348f, -0.64443887148677f, 0.056264275617853f,
            0.61573773369959f, -0.00074622703454316f, 0.25455659350429f,
            0.30670278147618f, -0.18573195942296f, 0.65383825999316f,
            -0.089919562456316f, -0.28968403215216f, -0.60618287937171f,
            0.53370861364121f, 0.37921556323246f, -0.33450055738044f,
            -0.47481167613763f, 0.3899274103573f, -0.1047963185367f,
            0.45545456567005f, 0.12142073778317f, 0.62397625076847f,
            0.59154225785278f, -0.10812441303593f, -0.4685834521013f,
            -0.36007270807588f, -0.1012374701199f, 0.52812407295968f,
            -0.01292122984647f, -0.23607532114711f, -0.57680411110671f,
            -0.44955815301222f, -0.31913443306122f, -0.55448100298376f,
            0.54231398466289f, -0.31845386154668f, -0.38636423612049f,
            0.22187979539931f, -0.6346425853783f, -0.056599490898788f,
            -0.41950690366157f, -0.4578028963184f, 0.31139813874057f,
            0.39787962066193f, -0.20885901240181f, 0.56172180435883f,
            -0.031404881097728f, 0.56267475273157f, -0.5556815383811f,
            0.33075363850824f, 0.39071115867626f, 0.3340294973255f,
            -0.51485161085589f, -0.34037011091125f, -0.46826090820473f,
            -0.60086679836276f, -0.075069409610657f, 0.18202033570633f,
            -0.49669644859095f, 0.13236483793072f, 0.53440735955877f,
            0.4720120049858f, -0.05992551666341f, -0.47306929861073f,
            -0.32796852486185f, 0.65593302097807f, 0.20800030327303f,
            -0.38965914824176f, -0.51564565153044f, -0.034636725857177f,
            -0.30473794783797f, 0.12584230588041f, 0.63911213518179f,
            0.11269477188219f, 0.62944339013855f, 0.27191006392352f,
            -0.53642197294029f, 0.50742224701512f, -0.22907820767928f,
            0.47022559371179f, -0.1914125650624f, 0.38019261684316f,
            -0.28865425091309f, 0.76169672032907f, -0.36166127667225f,
            -0.30555403321368f, -0.12541657537884f, -0.31081403770203f,
            0.0025978417989835f, 0.3737146483793f, -0.3151511957077f,
            0.62032810853005f, 0.60524642517936f, -0.09939888944988f,
            -0.40019833530022f, 0.15931480693456f, -0.61653030345628f,
            -0.49479441153976f, -0.021517911098538f, -0.43481713333933f,
            -0.26445143166732f, -0.48401155081335f, 0.27737058096082f,
            -0.12537486208624f, -0.46956235249512f, 0.61859207953377f,
            -0.49776294425122f, 0.6509513246149f, -0.20147785800704f,
            0.26022926925791f, 0.39526195830317f, -0.25288299425858f,
            0.20792543895216f, 0.6725599557329f, 0.013296712014115f,
            0.069082404776847f, -0.37233547685047f, 0.60070560947898f,
            -0.60329265885108f, 0.40708027238668f, -0.17229997007444f,
            -0.52997954496878f, 0.22211745651394f, -0.33229784433365f,
            0.61826884506104f, -0.62582169643111f, 0.33820439950773f,
            0.23870919720066f, -0.20670655096227f, -0.10953969425599f,
            -0.63678168786213f, -0.51101649337563f, -0.19131817442969f,
            -0.49493417544846f, -0.22614515287593f, 0.025828539221376f,
            0.7068462559507f, 0.072932806612059f, -0.30827034359477f,
            -0.52659704221432f, -0.33954839093364f, 0.086145323573817f,
            -0.52429050496975f, 0.39091424683727f, 0.52819210715237f,
            -0.16569162349745f, 0.447191673089f, 0.25667977984796f,
            0.85033978527922f, -0.37311666188152f, -0.031585518143925f,
            -0.063546921071094f, -0.35026506762952f, 0.099923633151172f,
            -0.43149574251927f, 0.16017753208259f, -0.36624037246965f,
            0.49372029676385f, -0.60067103922455f, 0.2223896202103f,
            -0.43599537393092f, -0.360658355506f, -0.42475053011196f,
            -0.52301759011739f, 0.039454536357949f, 0.47362064109658f,
            -0.35793170214797f, -0.43917817788312f, -0.49072242572643f,
            -0.32880277826743f, -0.38509560837703f, -0.42636724894184f,
            -0.043679644403255f, 0.74697226557232f, -0.40732954428872f,
            -0.48088968590275f, 0.18029290312902f, -0.10220931735307f,
            -0.058902573502295f, 0.0082595236590186f, 0.7136596141971f,
            -0.53043791172483f, 0.22906331492979f, 0.39155822265168f,
            0.43459649233879f, 0.18964470832196f, 0.15217427204218f,
            0.59694624534505f, 0.053786588105393f, 0.62671041756872f,
            -0.48833575031057f, 0.068909881680922f, 0.60168404074737f,
            -0.055455043023162f, -0.62426261497771f, -0.044461939113733f,
            -0.71822145541427f, 0.054494951105527f, 0.25733756171599f,
            -0.42706881935297f, -0.44024663347316f, 0.19687748949208f,
            0.4723221071836f, 0.63009683957253f, 0.2166256995021f,
            0.31063720960745f, 0.079455887335627f, 0.47974409023622f,
            -0.39506538843406f, 0.42517729990346f, 0.29375773990216f,
            0.044503633424429f, -0.46173213926286f, 0.60139575234582f,
            -0.40354126620316f, 0.41304136826673f, -0.29533980868045f,
            -0.45300699221804f, 0.23702354154238f, -0.56385297528377f,
            -0.62315380378984f, -0.42397903326965f, 0.53044082394843f,
            0.37874432092957f, 0.054922713129263f, 0.063952196248596f,
            0.41959045692314f, -0.83420441875842f, -0.25505372502578f,
            0.25012310515014f, 0.010974237503127f, 0.017675743681809f,
            -0.25231575134089f, -0.17034034508503f, -0.0022254428444259f,
            -0.4967771056787f, 0.43184899693064f, -0.68850194407078f,
            -0.1852812882862f, -0.48330898597592f, 0.13528868642679f,
            0.15202104844417f, 0.57661281495368f, -0.59848767913131f,
            0.64287473226568f, -0.30923674494923f, 0.22234318117192f,
            0.099248962994541f, 0.64370450011427f, 0.13206961744112f,
            -0.49018899717866f, 0.68654120859156f, -0.27238863334662f,
            -0.085832423495263f, 0.44161945604453f, 0.10856057983467f,
            0.48795432482822f, 0.42184193883513f, -0.43797315744756f,
            0.35186997012044f, -0.46483432791096f, 0.22857392808385f,
            0.52970834834669f, -0.50684486922008f, -0.39782161731912f,
            -0.3932709335414f, -0.34863027587322f, 0.16748196501934f,
            -0.46048505533f, -0.3887126918161f, -0.68287320410729f,
            -0.18448530888361f, -0.25358256326157f, 0.26870280714361f,
            0.6889557358588f, -0.3101022706485f, -0.35882194962822f,
            0.30088738418801f, -0.039139540883101f, -0.45646277242166f,
            -0.21954767479275f, 0.40838837410593f, 0.23284186868997f,
            0.30349649888064f, 0.57233263099925f, 0.55778817953937f,
            0.57731035290905f, 0.091218309942656f, 0.70670016667131f,
            0.016358033634041f, 0.3939245235472f, -0.059352634867484f,
            0.50055570130024f, -0.021749790970703f, 0.56767851040093f,
            0.50580176326624f, 0.34691320957643f, 0.22478399991032f,
            -0.37901911159632f, 0.53804099887537f, -0.46780195460858f,
            0.51497346779204f, -0.27981005467588f, 0.067278440906787f,
            0.67241900483514f, 0.074099582737f, 0.43138117954806f,
            0.054567519697911f, -0.37927768894619f, 0.45764946429346f,
            0.14529189179172f, -0.23854982910384f, 0.45401647091062f,
            0.25466539906731f, 0.46182069803887f, -0.66160446396375f,
            -0.15570980059397f, -0.38476787034627f, 0.37322840954917f,
            -0.43977613626294f, -0.61243005550684f, -0.34631643815896f,
            -0.19590302894013f, 0.42065974653653f, 0.43447548638809f,
            -0.10575548452794f, 0.70439951675651f, -0.29754920754254f,
            -0.13558865796725f, 0.1427073453776f, 0.49647494823192f,
            -0.65533234019218f, -0.11714854214663f, 0.5211321311867f,
            -0.6228374766114f, 0.20812698103217f, -0.16205154548883f,
            0.20384566967497f, -0.59321895467652f, 0.38604941246779f,
            0.44487837128099f, -0.37224943035393f, -0.22188447638327f,
            0.48921538939858f, 0.41432418029434f, -0.45087099253189f,
            0.66422841315008f, 0.21517761068003f, 0.094012579794123f,
            -0.4358159040875f, 0.22245680154647f, -0.51404116085847f,
            -0.11369362736032f, 0.32284689991698f, -0.38818285117689f,
            0.49680024166881f, 0.047684866166158f, -0.69503480904222f,
            -0.5137200731924f, -0.50673230867252f, 0.32715252974108f,
            -0.26799714004956f, -0.47616510509846f, 0.27153195326233f,
            -0.47315177716491f, -0.45711495983609f, -0.31178280842352f,
            -0.51697763052226f, -0.14302372043059f, -0.42689944315384f,
            -0.050442035795027f, 0.23609184251469f, 0.38634880236106f,
            0.56012774305243f, 0.38963669840218f, -0.57174382424149f,
            -0.15472134925391f, -0.15333579424307f, -0.14189768300467f,
            0.032279269476252f, -0.66054298438621f, -0.70360180527557f,
            -0.10345191679557f, -0.30503725808375f, 0.31038263802383f,
            0.36878846502877f, -0.76824774853417f, 0.2714830658427f,
            -0.060212868606223f, -0.4172755444983f, 0.39199300681258f,
            -0.44040104260082f, 0.24955102139032f, -0.64215903203727f,
            0.25443195353315f, -0.013789583113498f, 0.44365000614699f,
            0.53296203342425f, -0.55057750350733f, -0.38867053403178f,
            -0.36068564301268f, -0.65616661625162f, -0.48495997865466f,
            0.24088316031012f, -0.18080297655217f, -0.33682435258394f,
            -0.53824550487673f, -0.096728907851005f, -0.5208619866167f,
            0.33195321221408f, -0.032263947064791f, 0.56427315050798f,
            0.40151657866643f, -0.44825725748635f, -0.54910020122855f,
            -0.095936272447708f, 0.5719563905078f, 0.00097783623607218f,
            0.21961099467771f, 0.62823723408945f, -0.010045934028323f,
            -0.6610564872634f, -0.17161595423903f, -0.30089924032373f,
            0.27961471530636f, 0.054523395513076f, 0.61485903249347f,
            0.11958885677663f, -0.61032561244673f, -0.39241856813031f,
            -0.30223065341134f, -0.23605925177166f, -0.09697276975263f,
            -0.46458104180761f, -0.37853464945647f, 0.69599203908657f,
            0.0023635513043496f, 0.62702100484886f, 0.49658954056984f,
            -0.20369645124455f, -0.56457560315907f, 0.00021299797811461f,
            -0.64198493892962f, 0.59676262320476f, 0.46274573284143f,
            0.088421912306785f, 0.098029994490406f, -0.012953072012707f,
            -0.053965435026011f, 0.13439533803278f, -0.33103493780685f,
            0.55991756423782f, -0.58127599631056f, -0.46696041830103f,
            -0.43965993689353f, 0.07544961763381f, 0.1509639518808f,
            -0.38868406689028f, -0.0033436054452783f, -0.79191533434483f,
            -0.21743914630025f, -0.32019630124298f, -0.56067107727615f,
            0.027284914419519f, -0.49444926389798f, -0.53908992599417f,
            -0.36492599248168f, 0.52529904803377f, 0.18002253442693f,
            0.14829474115897f, 0.17212619314998f, -0.71194315827942f,
            0.0051876209353066f, 0.50490293404098f, 0.24361032552454f,
            0.13688117617809f, -0.61381291176911f, -0.5386997104485f,
            0.66421180843392f, 0.21833854629637f, -0.087909936660014f,
            0.15624552502148f, -0.68780724971724f, 0.077015056461268f,
            0.52710630558705f, -0.42143671471468f, -0.069964559463205f,
            -0.24196341534187f, -0.68814841622245f, 0.08695091377684f,
            0.62392249806692f, -0.23663281560035f, -0.59058622185178f,
            0.22685863859977f, -0.36683948058558f, -0.14105848121323f,
            0.18069852004855f, -0.083828559172887f, 0.66240167877879f,
            0.16722813432165f, -0.25503640214793f, -0.65462662498637f,
            -0.37112528006203f, 0.43100319401562f, -0.11342774633614f,
            0.14418808646988f, 0.5753326931164f, 0.55842502411684f,
            0.55378724068611f, 0.21098160548047f, -0.3224976646632f,
            0.31268307369255f, -0.37624695517597f, -0.55269271266764f,
            0.2601465870231f, 0.56373458886982f, -0.21638357910201f,
            0.41216916619413f, -0.25078072187299f, -0.57873208070982f,
            0.11217864148346f, 0.54196554704815f, -0.31989128683717f,
            0.54691221598945f, 0.24062434044524f, 0.48409277788476f,
            0.087564423746579f, -0.12083081671284f, 0.69931172084498f,
            0.35220575672909f, 0.28770484569954f, -0.53091668762919f,
            0.3395702120398f, 0.042520943289575f, -0.30935928261896f,
            0.61022210846475f, 0.54650816974112f, 0.34079124619266f,
            0.32746112891934f, 0.32095220193351f, -0.61142534799442f,
            0.32197324480666f, -0.38236071343678f, 0.40749411210419f,
            0.58741915356593f, -0.30916030490652f, -0.57642977381104f,
            -0.038846190358607f, 0.047926713761208f, -0.4725265742377f,
            0.026224389898652f, 0.031768907187292f, -0.12510902263321f,
            0.36102734397001f, -0.72217212865059f, 0.57513252722531f,
            -0.27510374152496f, -0.5153402145828f, 0.025774022629799f,
            0.59201067073603f, 0.40728366085253f, -0.37645913420642f,
            -0.29983338495183f, -0.61017291361195f, -0.18551919513643f,
            0.50515945610161f, 0.18206593801497f, -0.46372136367049f,
            -0.64290893575119f, -0.34887011406157f, -0.55318606770362f,
            -0.21230198963112f, -0.19828983785672f, 0.2730419816548f,
            -0.32778879906348f, -0.094317293167129f, 0.57811170538439f,
            0.54346692190204f, 0.17699503497579f, -0.47197676839855f,
            -0.075738705663962f, 0.53381750682665f, -0.13406342524856f,
            0.71765386263773f, 0.34271060834977f, 0.24259408122628f,
            -0.30574273227855f, 0.17419449782542f, -0.78861555508124f,
            0.43305678368813f, 0.064853328282818f, 0.25003806266734f,
            0.4397035983709f, -0.51651518914239f, -0.3972346186176f,
            -0.34513492086703f, 0.32129829777342f, -0.39965829527563f,
            -0.25184899643619f, -0.35937572373004f, 0.15273239148905f,
            -0.51640931868766f, 0.4218715745627f, -0.58261460582976f,
            -0.57396000790758f, 0.1912786199605f, 0.45995634753032f,
            -0.43664716984512f, 0.4601630113166f, 0.14146310231856f,
            0.11500068018889f, 0.05112652754666f, -0.25672855859366f,
            -0.54715738035577f, 0.67669928552409f, 0.40118355777989f,
            -0.45252668004418f, -0.40809988524453f, -0.064931545867856f,
            0.19116562077283f, 0.76523014995576f, 0.048337406798767f,
            -0.080075651760374f, 0.75305314115418f, 0.34797424409913f,
            0.29104493928016f, 0.0040185919664457f, -0.46977598520425f,
            -0.3890257668276f, 0.49100041230416f, -0.17812126809985f,
            -0.43787557151231f, -0.46923187878333f, 0.40489108352503f,
            0.37433236324043f, -0.29441766760791f, -0.066285137006724f,
            0.33217472508825f, 0.73917165688328f, 0.33479099915638f,
            -0.02973230696179f, -0.51371026289118f, 0.34133522703692f,
            -0.41361792362786f, -0.51561746819514f, -0.4263412462482f,
            0.51057171220039f, -0.23740201245544f, 0.26673587003088f,
            0.5521767379032f, 0.16849318602455f, 0.52774964064755f,
    };

}
