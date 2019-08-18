package make.some.noise;

import org.junit.Test;

/**
 * Created by Tommy Ettinger on 6/20/2019.
 */
public class NoiseTests {

	@Test
	public void testRange2D()
	{
		Noise noise = new Noise(543212345, 3.14159265f);
		long state = 12345678L;
		float x, y, result;
		float min = 0.5f, max = 0.5f;
		for (int i = 0; i < 100000000; i++) {
			x = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.singleSimplex((int)state, x, y);
			min = Math.min(min, result);
			max = Math.max(max, result);
			if(result > 1f)
				System.out.println("TOO HIGH AT x="+x+",y="+y);
			if(result < -1f)
				System.out.println("TOO LOW AT x="+x+",y="+y);
		}
		System.out.println("2D min="+min+",max="+max);
	}

	@Test
	public void testRange3D()
	{
		Noise noise = new Noise(543212345, 3.14159265f);
		long state = 12345678L;
		float x, y, z, result;
		float min = 0.5f, max = 0.5f;
		for (int i = 0; i < 10000000; i++) {
			x = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			z = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.singleSimplex((int)state, x, y, z);
			min = Math.min(min, result);
			max = Math.max(max, result);
			if(result > 1f)
				System.out.println("TOO HIGH AT x="+x+",y="+y);
			if(result < -1f)
				System.out.println("TOO LOW AT x="+x+",y="+y);

		}
		System.out.println("3D min="+min+",max="+max);
	}

	@Test
	public void testRange4D()
	{
		Noise noise = new Noise(543212345, 3.14159265f);
		long state = 12345678L;
		float x, y, z, w, result;
		float min = 0.5f, max = 0.5f;
		for (int i = 0; i < 10000000; i++) {
			x = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			z = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			w = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.singleSimplex((int)state, x, y, z, w);
			min = Math.min(min, result);
			max = Math.max(max, result);
			if(result > 1f)
				System.out.println("TOO HIGH AT x="+x+",y="+y);
			if(result < -1f)
				System.out.println("TOO LOW AT x="+x+",y="+y);

		}
		System.out.println("4D min="+min+",max="+max);
	}

	@Test
	public void testRange6D()
	{
		Noise noise = new Noise(543212345, 3.14159265f);
		long state = 12345678L;
		float x, y, z, w, u, v, result;
		float min = 0.5f, max = 0.5f;
		for (int i = 0; i < 10000000; i++) {
			x = (state >> 57) / (1.0001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 57) / (1.0001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			z = (state >> 57) / (1.0001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			w = (state >> 57) / (1.0001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			u = (state >> 57) / (1.0001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			v = (state >> 57) / (1.0001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.singleSimplex((int)state, x, y, z, w, u, v);
			min = Math.min(min, result);
			max = Math.max(max, result);
			if(result > 1f)
				System.out.println("TOO HIGH AT x="+x+",y="+y);
			if(result < -1f)
				System.out.println("TOO LOW AT x="+x+",y="+y);

		}
		System.out.println("6D min="+min+",max="+max);
	}
}
