package make.some.noise;

import org.junit.Test;

/**
 * Created by Tommy Ettinger on 6/20/2019.
 */
public class NoiseTests {

	@Test
	public void testRange2D()
	{
		Noise noise = new Noise(543212345);
		long state = 12345678L;
		float x, y, result;
		int failures = 0;
		for (int i = 0; i < 10000000; i++) {
			x = (state >> 60) / (1.01f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 60) / (1.01f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.singleSimplex((int)state, x, y);
			if(result < -1f || result > 1f)
			{
				++failures;
				System.out.println("FAILURE #" + failures + ": " + x + ", " + y);
			}
		}
	}

	@Test
	public void testRange3D()
	{
		Noise noise = new Noise(543212345);
		long state = 12345678L;
		float x, y, z, result;
		int failures = 0;
		for (int i = 0; i < 10000000; i++) {
			x = (state >> 60) / (1.01f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 60) / (1.01f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			z = (state >> 60) / (1.01f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.singleSimplex((int)state, x, y, z);
			if(result < -1f || result > 1f)
			{
				++failures;
				System.out.println("FAILURE #" + failures + ": " + x + ", " + y + ", " + z);
			}
		}
	}

	@Test
	public void testRange4D()
	{
		Noise noise = new Noise(543212345);
		long state = 12345678L;
		float x, y, z, w, result;
		int failures = 0;
		for (int i = 0; i < 10000000; i++) {
			x = (state >> 60) / (1.01f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 60) / (1.01f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			z = (state >> 60) / (1.01f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			w = (state >> 60) / (1.01f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.singleSimplex((int)state, x, y, z, w);
			if(result < -1f || result > 1f)
			{
				++failures;
				System.out.println("FAILURE #" + failures + ": " + x + ", " + y + ", " + z + ", " + w);
			}
		}
	}

	@Test
	public void testRange6D()
	{
		Noise noise = new Noise(543212345);
		long state = 12345678L;
		float x, y, z, w, u, v, result;
		int failures = 0;
		for (int i = 0; i < 10000000; i++) {
			x = (state >> 60) / (1.01f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 60) / (1.01f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			z = (state >> 60) / (1.01f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			w = (state >> 60) / (1.01f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			u = (state >> 60) / (1.01f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			v = (state >> 60) / (1.01f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.singleSimplex((int)state, x, y, z, w, u, v);
			if(result < -1f || result > 1f)
			{
				++failures;
				System.out.println("FAILURE #" + failures + ": " + x + ", " + y + ", " + z + ", " + w + ", " + u + ", " + v);
			}
		}
	}
}
