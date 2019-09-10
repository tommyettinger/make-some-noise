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
		int higher = 0, lower = 0;
		for (int i = 0; i < 100000000; i++) {
			x = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.singleSimplex((int)state, x, y);
			min = Math.min(min, result);
			max = Math.max(max, result);
			if(result > 1f)
				higher++;
			if(result < -1f)
				lower++;
		}
		System.out.println("2D min="+min+",max="+max+",tooHighCount="+higher+",tooLowCount="+lower);
	}

	@Test
	public void testRange3D()
	{
		Noise noise = new Noise(543212345, 3.14159265f);
		long state = 12345678L;
		float x, y, z, result;
		float min = 0.5f, max = 0.5f;
		int higher = 0, lower = 0;
		for (int i = 0; i < 10000000; i++) {
			x = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			z = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.singleSimplex((int)state, x, y, z);
			min = Math.min(min, result);
			max = Math.max(max, result);
			if(result > 1f)
				higher++;
			if(result < -1f)
				lower++;
		}
		System.out.println("3D min="+min+",max="+max+",tooHighCount="+higher+",tooLowCount="+lower);
	}

	@Test
	public void testRange4D()
	{
		Noise noise = new Noise(543212345, 3.14159265f);
		long state = 12345678L;
		float x, y, z, w, result;
		float min = 0.5f, max = 0.5f;
		int higher = 0, lower = 0;
		for (int i = 0; i < 10000000; i++) {
			x = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			z = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			w = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.singleSimplex((int)state, x, y, z, w);
			min = Math.min(min, result);
			max = Math.max(max, result);
			if(result > 1f)
				higher++;
			if(result < -1f)
				lower++;
		}
		System.out.println("4D min="+min+",max="+max+",tooHighCount="+higher+",tooLowCount="+lower);
	}

	@Test
	public void testRange6D()
	{
		Noise noise = new Noise(543212345, 3.14159265f);
		long state = 12345678L;
		float x, y, z, w, u, v, result;
		float min = 0.5f, max = 0.5f;
		int higher = 0, lower = 0;
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
				higher++;
			if(result < -1f)
				lower++;
		}
		System.out.println("6D min="+min+",max="+max+",tooHighCount="+higher+",tooLowCount="+lower);
	}

	@Test
	public void testRangeBillow2D()
	{
		Noise noise = new Noise(543212345, 3.14159265f, Noise.SIMPLEX_FRACTAL, 2, 2f, 0.5f);
		noise.setFractalType(Noise.BILLOW);
		long state = 12345678L;
		float x, y, result;
		float min = 0.5f, max = 0.5f;
		int higher = 0, lower = 0;
		for (int i = 0; i < 100000000; i++) {
			x = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.getNoiseWithSeed(x, y, (int)state);
			min = Math.min(min, result);
			max = Math.max(max, result);
			if(result > 1f)
				higher++;
			if(result < -1f)
				lower++;
		}
		System.out.println("Billow 2D min="+min+",max="+max+",tooHighCount="+higher+",tooLowCount="+lower);
	}

	@Test
	public void testRangeFBM3D()
	{
		Noise noise = new Noise(543212345, 3.14159265f, Noise.SIMPLEX_FRACTAL, 3, 2f, 0.5f);
		noise.setFractalType(Noise.FBM);
		long state = 12345678L;
		float x, y, z, result;
		float min = 0.5f, max = 0.5f;
		int higher = 0, lower = 0;
		for (int i = 0; i < 10000000; i++) {
			x = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			z = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.getNoiseWithSeed(x, y, z, (int)state);
			min = Math.min(min, result);
			max = Math.max(max, result);
			if(result > 1f)
				higher++;
			if(result < -1f)
				lower++;
		}
		System.out.println("FBM 3D min="+min+",max="+max+",tooHighCount="+higher+",tooLowCount="+lower);
	}

	@Test
	public void testRangeFBM4D()
	{
		Noise noise = new Noise(543212345, 3.14159265f, Noise.PERLIN_FRACTAL, 1, 2f, 0.5f);
		noise.setFractalType(Noise.FBM);
		long state = 12345678L;
		float x, y, z, w, result;
		float min = 0.5f, max = 0.5f;
		int higher = 0, lower = 0;
		for (int i = 0; i < 10000000; i++) {
			x = (state >> 58) / (8.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-21f));
			y = (state >> 58) / (8.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-21f));
			z = (state >> 58) / (8.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-21f));
			w = (state >> 58) / (8.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-21f));
			result = noise.getNoiseWithSeed(x, y, z, w, (int)state);				
			min = Math.min(min, result);
			max = Math.max(max, result);
			if (result > 1f)
				higher++;
			if (result < -1f)
				lower++;
		}
		System.out.println("FBM 4D min="+min+",max="+max+",tooHighCount="+higher+",tooLowCount="+lower);
	}

	@Test
	public void testRangeBillow3D()
	{
		Noise noise = new Noise(543212345, 3.14159265f, Noise.SIMPLEX_FRACTAL, 2, 2f, 0.5f);
		noise.setFractalType(Noise.BILLOW);
		long state = 12345678L;
		float x, y, z, result;
		float min = 0.5f, max = 0.5f;
		int higher = 0, lower = 0;
		for (int i = 0; i < 10000000; i++) {
			x = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			z = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.getNoiseWithSeed(x, y, z, (int)state);
			min = Math.min(min, result);
			max = Math.max(max, result);
			if(result > 1f)
				higher++;
			if(result < -1f)
				lower++;
		}
		System.out.println("Billow 3D min="+min+",max="+max+",tooHighCount="+higher+",tooLowCount="+lower);
	}

	@Test
	public void testRangeBillowInverse2D()
	{
		Noise noise = new Noise(543212345, 3.14159265f, Noise.SIMPLEX_FRACTAL, 2, 0.5f, 2f);
		noise.setFractalType(Noise.BILLOW);
		long state = 12345678L;
		float x, y, result;
		float min = 0.5f, max = 0.5f;
		int higher = 0, lower = 0;
		for (int i = 0; i < 100000000; i++) {
			x = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.getNoiseWithSeed(x, y, (int)state);
			min = Math.min(min, result);
			max = Math.max(max, result);
			if(result > 1f)
				higher++;
			if(result < -1f)
				lower++;
		}
		System.out.println("BillowInverse 2D min="+min+",max="+max+",tooHighCount="+higher+",tooLowCount="+lower);
	}

	@Test
	public void testRangeBillowInverse3D()
	{
		Noise noise = new Noise(543212345, 3.14159265f, Noise.SIMPLEX_FRACTAL, 2, 0.5f, 2f);
		noise.setFractalType(Noise.BILLOW);
		long state = 12345678L;
		float x, y, z, result;
		float min = 0.5f, max = 0.5f;
		int higher = 0, lower = 0;
		for (int i = 0; i < 10000000; i++) {
			x = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			z = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.getNoiseWithSeed(x, y, z, (int)state);
			min = Math.min(min, result);
			max = Math.max(max, result);
			if(result > 1f)
				higher++;
			if(result < -1f)
				lower++;
		}
		System.out.println("BillowInverse 3D min="+min+",max="+max+",tooHighCount="+higher+",tooLowCount="+lower);
	}




	@Test
	public void testRangeRidged2D()
	{
		Noise noise = new Noise(543212345, 3.14159265f, Noise.SIMPLEX_FRACTAL, 3, 2f, 0.5f);
		noise.setFractalType(Noise.RIDGED_MULTI);
		long state = 12345678L;
		float x, y, result;
		float min = 0.5f, max = 0.5f;
		int higher = 0, lower = 0;
		for (int i = 0; i < 100000000; i++) {
			x = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.getNoiseWithSeed(x, y, (int)state);
			min = Math.min(min, result);
			max = Math.max(max, result);
			if(result > 1f)
				higher++;
			if(result < -1f)
				lower++;
		}
		System.out.println("Ridged 2D min="+min+",max="+max+",tooHighCount="+higher+",tooLowCount="+lower);
	}

	@Test
	public void testRangeRidged3D()
	{
		Noise noise = new Noise(543212345, 3.14159265f, Noise.SIMPLEX_FRACTAL, 3, 2f, 0.5f);
		noise.setFractalType(Noise.RIDGED_MULTI);
		long state = 12345678L;
		float x, y, z, result;
		float min, max;
		min = max = noise.getNoiseWithSeed(0, 0, 0, (int)state);
		int higher = 0, lower = 0;
		for (int i = 0; i < 10000000; i++) {
			x = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			z = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.getNoiseWithSeed(x, y, z, (int)state);
			min = Math.min(min, result);
			max = Math.max(max, result);
			if(result > 1f)
				higher++;
			if(result < -1f)
				lower++;
		}
		System.out.println("Ridged 3D min="+min+",max="+max+",tooHighCount="+higher+",tooLowCount="+lower);
	}

	@Test
	public void testRangeRidgedInverse2D()
	{
		Noise noise = new Noise(543212345, 3.14159265f, Noise.SIMPLEX_FRACTAL, 2, 0.5f, 2f);
		noise.setFractalType(Noise.RIDGED_MULTI);
		long state = 12345678L;
		float x, y, result;
		float min = 0.5f, max = 0.5f;
		int higher = 0, lower = 0;
		for (int i = 0; i < 100000000; i++) {
			x = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.getNoiseWithSeed(x, y, (int)state);
			min = Math.min(min, result);
			max = Math.max(max, result);
			if(result > 1f)
				higher++;
			if(result < -1f)
				lower++;
		}
		System.out.println("RidgedInverse 2D min="+min+",max="+max+",tooHighCount="+higher+",tooLowCount="+lower);
	}

	@Test
	public void testRangeRidgedInverse3D()
	{
		Noise noise = new Noise(543212345, 3.14159265f, Noise.SIMPLEX_FRACTAL, 3, 0.5f, 2f);
		noise.setFractalType(Noise.RIDGED_MULTI);
		long state = 12345678L;
		float x, y, z, result;
		float min, max;
		min = max = noise.getNoiseWithSeed(0, 0, 0, (int)state);
		int higher = 0, lower = 0;
		for (int i = 0; i < 10000000; i++) {
			x = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			y = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			z = (state >> 58) / (1.001f - (((state = (state << 29 | state >>> 35) * 0xAC564B05L) * 0x818102004182A025L & 0xffffffL) * 0x1p-24f));
			result = noise.getNoiseWithSeed(x, y, z, (int)state);
			min = Math.min(min, result);
			max = Math.max(max, result);
			if(result > 1f)
				higher++;
			if(result < -1f)
				lower++;
		}
		System.out.println("RidgedInverse 3D min="+min+",max="+max+",tooHighCount="+higher+",tooLowCount="+lower);
	}

}
