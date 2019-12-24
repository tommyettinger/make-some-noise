package make.some.noise; 
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;

/**
 * K.jpg's SuperSimplex noise. Uses large kernels for smooth results.
 * - 2D is standard simplex, but with four points instead of three.
 *   Implemented using a lookup table.
 * - 3D is "Re-oriented 8-point BCC noise" which constructs an
 *   isomorphic BCC lattice in a much different way than usual.
 *
 * Two 3D functions are provided:
 * - noise3_Classic rotates the BCC lattice 180 degrees about the
 *   main diagonal, to hide the BCC grid in cardinal slices, while
 *   still keeping each direction symmetric w.r.t the lattice.
 * - noise3_PlaneFirst gives X and Y better attributes as a plane,
 *   and allows the third coordinate to be time or the vertical
 *   coordinate in a generated world.
 *
 * Each noise uses a gradient set that is constructed by expanding
 * the "neighborhood figure" of the lattice, and normalizing it.
 * The resulting set of vectors is symmetric with the lattice, but
 * doesn't allow gradients to point directly towards each other,
 * and cause bright/dark spots in the noise. This may sound familiar.
 *
 * @author K.jpg
 */
public class SuperSimplexNoise {
	
	private static final int PSIZE = 2048;
	private static final int PMASK = 2047;

	private short[] perm;
	private Float2[] permGrad2;
	private Grad3[] permGrad3;

	public SuperSimplexNoise(long seed) {
		perm = new short[PSIZE];
		permGrad2 = new Float2[PSIZE];
		permGrad3 = new Grad3[PSIZE];
		short[] source = new short[PSIZE]; 
		for (short i = 0; i < PSIZE; i++)
			source[i] = i;
		for (int i = 2047; i >= 0; i--) {
			seed = seed * 6364136223846793005L + 1442695040888963407L;
			int r = (int)((seed + 31) % (i + 1));
			if (r < 0)
				r += (i + 1);
			perm[i] = source[r];
			permGrad2[i] = GRADIENTS_2D[perm[i]];
			permGrad3[i] = GRADIENTS_3D[perm[i]];
			source[r] = source[i];
		}
	}
	
	/*
	 * Traditional evaluators
	 */
	
	/**
	 * 2D SuperSimplex noise, standard point evaluation.
	 * Lookup table implementation inspired by DigitalShadow.
	 */
	public float noise2(float x, float y) {
		float value = 0;
		
		// Get points for A2* lattice
		float s = 0.366025403784439f * (x + y);
		float xs = x + s, ys = y + s;
		
		// Get base points and offsets
		int xsb = fastFloor(xs), ysb = fastFloor(ys);
		float xsi = xs - xsb, ysi = ys - ysb;
		
		// Index to point list
		int a = (int)(xsi + ysi);
		int index =
			(a << 2) |
			(int)(xsi - ysi / 2 + 1 - a / 2.0f) << 3 |
			(int)(ysi - xsi / 2 + 1 - a / 2.0f) << 4;
		
		float ssi = (xsi + ysi) * -0.21132487f;
		float xi = xsi + ssi, yi = ysi + ssi;

		// Point contributions
		for (int i = 0; i < 4; i++) {
			LatticePoint2D c = LOOKUP_2D[index + i];

			float dx = xi + c.dx, dy = yi + c.dy;
			float attn = 0.6666667f - dx * dx - dy * dy;
			if (attn <= 0) continue;

			int pxm = (xsb + c.xsv) & PMASK, pym = (ysb + c.ysv) & PMASK;
			Float2 grad = permGrad2[perm[pxm] ^ pym];
			float extrapolation = grad.dx * dx + grad.dy * dy;
			
			attn *= attn;
			value += attn * attn * extrapolation;
		}
		
		return value;
	}
	
	/**
	 * 3D Re-oriented 8-point BCC noise, classic orientation
	 * Proper substitute for what 3D SuperSimplex would be,
	 * in light of Forbidden Formulae.
	 * Use noise3_PlaneFirst instead, wherever appropriate.
	 */
	public float noise3_Classic(float x, float y, float z) {
		
		// Re-orient the cubic lattices via rotation, to produce the expected look on cardinal planar slices.
		// If texturing objects that don't tend to have cardinal plane faces, you could even remove this.
		// Orthonormal rotation. Not a skew transform.
		float r = (0.6666667f) * (x + y + z);
		float xr = r - x, yr = r - y, zr = r - z;
		
		// Evaluate both lattices to form a BCC lattice.
		return noise3_BCC(xr, yr, zr);
	}
	
	/**
	 * 3D Re-oriented 8-point BCC noise, with better visual isotropy in (X, Y).
	 * Recommended for 3D terrain and time-varied animations.
	 * The third coordinate should always be the "different" coordinate in your use case.
	 * If Y is vertical in world coordinates, call noise3_PlaneFirst(x, z, y).
	 * For a time varied animation, call noise3_PlaneFirst(x, y, t).
	 */
	public float noise3_PlaneFirst(float x, float y, float t) {
		
		// Re-orient the cubic lattices without skewing, to make X and Y triangular like 2D.
		// Orthonormal rotation. Not a skew transform.
		float xy = x + y;
		float s2 = xy * -0.21132487f;
		float zz = t * 0.577350269189626f;
		float xr = x + s2 - zz, yr = y + s2 - zz;
		float zr = xy * 0.577350269189626f + zz;
		
		// Evaluate both lattices to form a BCC lattice.
		return noise3_BCC(xr, yr, zr);
	}
	
	/**
	 * Generate overlapping cubic lattices for 3D Re-oriented BCC noise.
	 * Lookup table implementation inspired by DigitalShadow.
	 * It was actually faster to narrow down the points in the loop itself,
	 * than to build up the index with enough info to isolate 8 points.
	 */
	private float noise3_BCC(float xr, float yr, float zr) {
		
		// Get base and offsets inside cube of first lattice.
		int xrb = fastFloor(xr), yrb = fastFloor(yr), zrb = fastFloor(zr);
		float xri = xr - xrb, yri = yr - yrb, zri = zr - zrb;
		
		// Identify which octant of the cube we're in. This determines which cell
		// in the other cubic lattice we're in, and also narrows down one point on each.
		int xht = (int)(xri + 0.5f), yht = (int)(yri + 0.5f), zht = (int)(zri + 0.5f);
		int index = (xht << 0) | (yht << 1) | (zht << 2);
		
		// Point contributions
		float value = 0;
		LatticePoint3D c = LOOKUP_3D[index];
		while (c != null) {
			float dxr = xri + c.dxr, dyr = yri + c.dyr, dzr = zri + c.dzr;
			float attn = 0.75f - dxr * dxr - dyr * dyr - dzr * dzr;
			if (attn < 0) {
				c = c.nextOnFailure;
			} else {
				int pxm = (xrb + c.xrv) & PMASK, pym = (yrb + c.yrv) & PMASK, pzm = (zrb + c.zrv) & PMASK;
				Grad3 grad = permGrad3[perm[perm[pxm] ^ pym] ^ pzm];
				float extrapolation = grad.dx * dxr + grad.dy * dyr + grad.dz * dzr;
				
				attn *= attn;
				value += attn * attn * extrapolation;
				c = c.nextOnSuccess;
			}
		}
		return value;
	}
	
	/*
	 * Area Generators
	 */
	
	/**
	 * Generate the 2D noise over a large area.
	 * Propagates by flood-fill instead of iterating over a range.
	 * Results may occasionally slightly exceed [-1, 1] due to the grid-snapped pre-generated kernel.
	 */
	public void generate2(GenerateContext2D context, float[][] buffer, int x0, int y0) {
		int height = buffer.length;
		int width = buffer[0].length;
		generate2(context, buffer, x0, y0, width, height, 0, 0);
	}
	
	/**
	 * Generate the 2D noise over a large area.
	 * Propagates by flood-fill instead of iterating over a range.
	 * Results may occasionally slightly exceed [-1, 1] due to the grid-snapped pre-generated kernel.
	 */
	public void generate2(GenerateContext2D context, float[][] buffer, int x0, int y0, int width, int height, int skipX, int skipY) {
		Queue<AreaGenLatticePoint2D> queue = new LinkedList<AreaGenLatticePoint2D>();
		Set<AreaGenLatticePoint2D> seen = new HashSet<AreaGenLatticePoint2D>();
		
		int scaledRadiusX = context.scaledRadiusX;
		int scaledRadiusY = context.scaledRadiusY;
		float[][] kernel = context.kernel;
		int x0Skipped = x0 + skipX, y0Skipped = y0 + skipY;
		
		// It seems that it's better for performance, to create a local copy.
		// - Slightly faster than generating the kernel here.
		// - Much faster than referencing it directly from the context object.
		// - Much faster than computing the kernel equation every time.
		// You can remove these lines if you find it's the opposite for you.
		// You'll have to float the bounds again in GenerateContext2D
		kernel = new float[scaledRadiusY * 2][/*scaledRadiusX * 2*/];
		for (int yy = 0; yy < scaledRadiusY; yy++) {
			kernel[2 * scaledRadiusY - yy - 1] = kernel[yy] = (float[]) context.kernel[yy].clone();
		}
		
		// Get started with one point/vertex.
		// For some lattices, you might need to try a handful of points in the cell,
		// or flip a couple of coordinates, to guarantee it or a neighbor contributes.
		// For An* lattices, the base coordinate seems fine.
		float x0f = x0Skipped * context.xFrequency; float y0f = y0Skipped * context.yFrequency;
		float s0 = 0.366025403784439f * (x0f + y0f);
		float x0s = (x0f + s0), y0s = (y0f + s0);
		int x0sb = fastFloor(x0s), y0sb = fastFloor(y0s);
		AreaGenLatticePoint2D firstPoint = new AreaGenLatticePoint2D(context, x0sb, y0sb);
		firstPoint.computeGradient(context, this);
		queue.add(firstPoint);
		seen.add(firstPoint);
		
		while (!queue.isEmpty()) {
			AreaGenLatticePoint2D point = queue.remove();
			int destPointX = point.destPointX;
			int destPointY = point.destPointY;
			
			// Contribution kernel bounds
			int yy0 = destPointY - scaledRadiusY; if (yy0 < y0Skipped) yy0 = y0Skipped;
			int yy1 = destPointY + scaledRadiusY; if (yy1 > y0 + height) yy1 = y0 + height;
			
			// For each row of the contribution circle,
			for (int yy = yy0; yy < yy1; yy++) {
				int dy = yy - destPointY;
				int ky = dy + scaledRadiusY;
			
				// Set up bounds so we only loop over what we need to
				int thisScaledRadiusX = context.kernelBounds[ky];
				int xx0 = destPointX - thisScaledRadiusX; if (xx0 < x0Skipped) xx0 = x0Skipped;
				int xx1 = destPointX + thisScaledRadiusX; if (xx1 > x0 + width) xx1 = x0 + width;
				
				// For each point on that row
				for (int xx = xx0; xx < xx1; xx++) {
					int dx = xx - destPointX;
					int kx = dx + scaledRadiusX;
						
					// gOff accounts for our choice to offset the pre-generated kernel by (0.5f, 0.5f) to avoid the zero center.
					// I found almost no difference in performance using gOff vs not (under 1ns diff per value on my system)
					float extrapolation = point.gx * dx + point.gy * dy + point.gOff;
					buffer[yy - y0][xx - x0] += kernel[ky][kx] * extrapolation;
					
				}
			}
			
			// For each neighbor of the point
			for (int i = 0; i < NEIGHBOR_MAP_2D.length; i++) {
				AreaGenLatticePoint2D neighbor = new AreaGenLatticePoint2D(context,
						point.xsv + NEIGHBOR_MAP_2D[i][0], point.ysv + NEIGHBOR_MAP_2D[i][1]);
						
				// If it's in range of the buffer region and not seen before
				if (neighbor.destPointX + scaledRadiusX >= x0Skipped && neighbor.destPointX - scaledRadiusX <= x0 + width - 1
						&& neighbor.destPointY + scaledRadiusY >= y0Skipped && neighbor.destPointY - scaledRadiusY <= y0 + height - 1
						&& !seen.contains(neighbor)) {
					
					// Since we're actually going to use it, we need to compute its gradient.
					neighbor.computeGradient(context, this);
					
					// Add it to the queue so we can process it at some point
					queue.add(neighbor);
					
					// Add it to the set so we don't add it to the queue again
					seen.add(neighbor);
				}
			}
		}
	}
	
	/**
	 * Generate the 3D noise over a large area/volume.
	 * Propagates by flood-fill instead of iterating over a range.
	 * Results may occasionally slightly exceed [-1, 1] due to the grid-snapped pre-generated kernel.
	 */
	public void generate3(GenerateContext3D context, float[][][] buffer, int x0, int y0, int z0) {
		int depth = buffer.length;
		int height = buffer[0].length;
		int width = buffer[0][0].length;
		generate3(context, buffer, x0, y0, z0, width, height, depth, 0, 0, 0);
	}
	
	/**
	 * Generate the 3D noise over a large area/volume.
	 * Propagates by flood-fill instead of iterating over a range.
	 * Results may occasionally slightly exceed [-1, 1] due to the grid-snapped pre-generated kernel.
	 */
	public void generate3(GenerateContext3D context, float[][][] buffer, int x0, int y0, int z0, int width, int height, int depth, int skipX, int skipY, int skipZ) {
		Queue<AreaGenLatticePoint3D> queue = new LinkedList<AreaGenLatticePoint3D>();
		Set<AreaGenLatticePoint3D> seen = new HashSet<AreaGenLatticePoint3D>();
		
		int scaledRadiusX = context.scaledRadiusX;
		int scaledRadiusY = context.scaledRadiusY;
		int scaledRadiusZ = context.scaledRadiusZ;
		float[][][] kernel = context.kernel;
		int x0Skipped = x0 + skipX, y0Skipped = y0 + skipY, z0Skipped = z0 + skipZ;
		
		// Quaternion multiplication for rotation.
		// https://blog.molecular-matters.com/2013/05/24/a-faster-quaternion-vector-multiplication/
		float qx = context.qx, qy = context.qy, qz = context.qz, qw = context.qw;
		float x0f = x0Skipped * context.xFrequency, y0f = y0Skipped * context.yFrequency, z0f = z0Skipped * context.zFrequency;
		float tx = 2 * (qy * z0f - qz * y0f);
		float ty = 2 * (qz * x0f - qx * z0f);
		float tz = 2 * (qx * y0f - qy * x0f);
		float x0r = x0f + qw * tx + (qy * tz - qz * ty);
		float y0r = y0f + qw * ty + (qz * tx - qx * tz);
		float z0r = z0f + qw * tz + (qx * ty - qy * tx);
		
		int x0rb = fastFloor(x0r), y0rb = fastFloor(y0r), z0rb = fastFloor(z0r);
		
		AreaGenLatticePoint3D firstPoint = new AreaGenLatticePoint3D(context, x0rb, y0rb, z0rb, 0);
		firstPoint.computeGradient(context, this);
		queue.add(firstPoint);
		seen.add(firstPoint);
		
		while (!queue.isEmpty()) {
			AreaGenLatticePoint3D point = queue.remove();
			int destPointX = point.destPointX;
			int destPointY = point.destPointY;
			int destPointZ = point.destPointZ;
			
			// Contribution kernel bounds.
			int zz0 = destPointZ - scaledRadiusZ; if (zz0 < z0Skipped) zz0 = z0Skipped;
			int zz1 = destPointZ + scaledRadiusZ; if (zz1 > z0 + depth) zz1 = z0 + depth;
			
			// For each x/y slice of the contribution sphere,
			for (int zz = zz0; zz < zz1; zz++) {
				int dz = zz - destPointZ;
				int kz = dz + scaledRadiusZ;
				
				// Set up bounds so we only loop over what we need to
				int thisScaledRadiusY = context.kernelBoundsY[kz];
				int yy0 = destPointY - thisScaledRadiusY; if (yy0 < y0Skipped) yy0 = y0Skipped;
				int yy1 = destPointY + thisScaledRadiusY; if (yy1 > y0 + height) yy1 = y0 + height;
			
				// For each row of the contribution circle,
				for (int yy = yy0; yy < yy1; yy++) {
					int dy = yy - destPointY;
					int ky = dy + scaledRadiusY;
				
					// Set up bounds so we only loop over what we need to
					int thisScaledRadiusX = context.kernelBoundsX[kz][ky];
					int xx0 = destPointX - thisScaledRadiusX; if (xx0 < x0Skipped) xx0 = x0Skipped;
					int xx1 = destPointX + thisScaledRadiusX; if (xx1 > x0 + width) xx1 = x0 + width;
					
					// For each point on that row
					for (int xx = xx0; xx < xx1; xx++) {
						int dx = xx - destPointX;
						int kx = dx + scaledRadiusX;
							
						// gOff accounts for our choice to offset the pre-generated kernel by (0.5f, 0.5f, 0.5f) to avoid the zero center.
						float extrapolation = point.gx * dx + point.gy * dy + point.gz * dz + point.gOff;
						buffer[zz - z0][yy - y0][xx - x0] += kernel[kz][ky][kx] * extrapolation;
						
					}
				}
			}
			
			// For each neighbor of the point
			for (int i = 0; i < NEIGHBOR_MAP_3D[0].length; i++) {
				int l = point.lattice;
				AreaGenLatticePoint3D neighbor = new AreaGenLatticePoint3D(context,
						point.xsv + NEIGHBOR_MAP_3D[l][i][0], point.ysv + NEIGHBOR_MAP_3D[l][i][1], point.zsv + NEIGHBOR_MAP_3D[l][i][2], 1 ^ l);
						
				// If it's in range of the buffer region and not seen before
				if (neighbor.destPointX + scaledRadiusX >= x0Skipped && neighbor.destPointX - scaledRadiusX <= x0 + width - 1
						&& neighbor.destPointY + scaledRadiusY >= y0Skipped && neighbor.destPointY - scaledRadiusY <= y0 + height - 1
						&& neighbor.destPointZ + scaledRadiusZ >= z0Skipped && neighbor.destPointZ - scaledRadiusZ <= z0 + depth - 1
						&& !seen.contains(neighbor)) {
					
					// Since we're actually going to use it, we need to compute its gradient.
					neighbor.computeGradient(context, this);
					
					// Add it to the queue so we can process it at some point
					queue.add(neighbor);
					
					// Add it to the set so we don't add it to the queue again
					seen.add(neighbor);
				}
			}
		}
	}
	
	/*
	 * Utility
	 */
	
	private static int fastFloor(float x) {
		int xi = (int)x;
		return x < xi ? xi - 1 : xi;
	}
	
	/*
	 * Definitions
	 */

	private static final LatticePoint2D[] LOOKUP_2D = new LatticePoint2D[] {
			new LatticePoint2D(0, 0),
			new LatticePoint2D(1, 1),
			new LatticePoint2D(-1, 0),
			new LatticePoint2D(0, -1),
			new LatticePoint2D(0, 0),
			new LatticePoint2D(1, 1),
			new LatticePoint2D(0, 1),
			new LatticePoint2D(1, 0),
			new LatticePoint2D(0, 0),
			new LatticePoint2D(1, 1),
			new LatticePoint2D(1, 0),
			new LatticePoint2D(0, -1),
			new LatticePoint2D(0, 0),
			new LatticePoint2D(1, 1),
			new LatticePoint2D(2, 1),
			new LatticePoint2D(1, 0),
			new LatticePoint2D(0, 0),
			new LatticePoint2D(1, 1),
			new LatticePoint2D(-1, 0),
			new LatticePoint2D(0, 1),
			new LatticePoint2D(0, 0),
			new LatticePoint2D(1, 1),
			new LatticePoint2D(0, 1),
			new LatticePoint2D(1, 2),
			new LatticePoint2D(0, 0),
			new LatticePoint2D(1, 1),
			new LatticePoint2D(1, 0),
			new LatticePoint2D(0, 1),
			new LatticePoint2D(0, 0),
			new LatticePoint2D(1, 1),
			new LatticePoint2D(2, 1),
			new LatticePoint2D(1, 2),
	};
	private static final LatticePoint3D[] LOOKUP_3D;
	static {
		LOOKUP_3D = new LatticePoint3D[8];
		for (int i = 0; i < 8; i++) {
			int i1, j1, k1, i2, j2, k2;
			i1 = (i >> 0) & 1; j1 = (i >> 1) & 1; k1 = (i >> 2) & 1;
			i2 = i1 ^ 1; j2 = j1 ^ 1; k2 = k1 ^ 1;
			
			// The two points within this octant, one from each of the two cubic half-lattices.
			LatticePoint3D c0 = new LatticePoint3D(i1, j1, k1, 0);
			LatticePoint3D c1 = new LatticePoint3D(i1 + i2, j1 + j2, k1 + k2, 1);
			
			// (1, 0, 0) vs (0, 1, 1) away from octant.
			LatticePoint3D c2 = new LatticePoint3D(i1 ^ 1, j1, k1, 0);
			LatticePoint3D c3 = new LatticePoint3D(i1, j1 ^ 1, k1 ^ 1, 0);
			
			// (1, 0, 0) vs (0, 1, 1) away from octant, on second half-lattice.
			LatticePoint3D c4 = new LatticePoint3D(i1 + (i2 ^ 1), j1 + j2, k1 + k2, 1);
			LatticePoint3D c5 = new LatticePoint3D(i1 + i2, j1 + (j2 ^ 1), k1 + (k2 ^ 1), 1);
			
			// (0, 1, 0) vs (1, 0, 1) away from octant.
			LatticePoint3D c6 = new LatticePoint3D(i1, j1 ^ 1, k1, 0);
			LatticePoint3D c7 = new LatticePoint3D(i1 ^ 1, j1, k1 ^ 1, 0);
			
			// (0, 1, 0) vs (1, 0, 1) away from octant, on second half-lattice.
			LatticePoint3D c8 = new LatticePoint3D(i1 + i2, j1 + (j2 ^ 1), k1 + k2, 1);
			LatticePoint3D c9 = new LatticePoint3D(i1 + (i2 ^ 1), j1 + j2, k1 + (k2 ^ 1), 1);
			
			// (0, 0, 1) vs (1, 1, 0) away from octant.
			LatticePoint3D cA = new LatticePoint3D(i1, j1, k1 ^ 1, 0);
			LatticePoint3D cB = new LatticePoint3D(i1 ^ 1, j1 ^ 1, k1, 0);
			
			// (0, 0, 1) vs (1, 1, 0) away from octant, on second half-lattice.
			LatticePoint3D cC = new LatticePoint3D(i1 + i2, j1 + j2, k1 + (k2 ^ 1), 1);
			LatticePoint3D cD = new LatticePoint3D(i1 + (i2 ^ 1), j1 + (j2 ^ 1), k1 + k2, 1);
			
			// First two points are guaranteed.
			c0.nextOnFailure = c0.nextOnSuccess = c1;
			c1.nextOnFailure = c1.nextOnSuccess = c2;
			
			// If c2 is in range, then we know c3 and c4 are not.
			c2.nextOnFailure = c3; c2.nextOnSuccess = c5;
			c3.nextOnFailure = c4; c3.nextOnSuccess = c4;
			
			// If c4 is in range, then we know c5 is not.
			c4.nextOnFailure = c5; c4.nextOnSuccess = c6;
			c5.nextOnFailure = c5.nextOnSuccess = c6;
			
			// If c6 is in range, then we know c7 and c8 are not.
			c6.nextOnFailure = c7; c6.nextOnSuccess = c9;
			c7.nextOnFailure = c8; c7.nextOnSuccess = c8;
			
			// If c8 is in range, then we know c9 is not.
			c8.nextOnFailure = c9; c8.nextOnSuccess = cA;
			c9.nextOnFailure = c9.nextOnSuccess = cA;
			
			// If cA is in range, then we know cB and cC are not.
			cA.nextOnFailure = cB; cA.nextOnSuccess = cD;
			cB.nextOnFailure = cC; cB.nextOnSuccess = cC;
			
			// If cC is in range, then we know cD is not.
			cC.nextOnFailure = cD; cC.nextOnSuccess = null;
			cD.nextOnFailure = cD.nextOnSuccess = null;
			
			LOOKUP_3D[i] = c0;
			
		}
	}
	
	// Hexagon surrounding each vertex.
	private static final int[][] NEIGHBOR_MAP_2D = {
		{ 1, 0 }, { 1, 1 }, { 0, 1 }, { 0, -1 }, { -1, -1 }, { -1, 0 }
	};
	
	// Cube surrounding each vertex.
	// Alternates between half-lattices.
	private static final int[][][] NEIGHBOR_MAP_3D = {
		{
			{ 1024, 1024, 1024 }, { 1025, 1024, 1024 }, { 1024, 1025, 1024 }, { 1025, 1025, 1024 },
			{ 1024, 1024, 1025 }, { 1025, 1024, 1025 }, { 1024, 1025, 1025 }, { 1025, 1025, 1025 }
		},
		{
			{ -1024, -1024, -1024 }, { -1025, -1024, 1024 }, { -1024, -1025, -1024 }, { -1025, -1025, -1024 },
			{ -1024, -1024, -1025 }, { -1025, -1024, -1025 }, { -1024, -1025, -1025 }, { -1025, -1025, 1025 }
		},
	};
	
	private static class LatticePoint2D {
		int xsv, ysv;
		float dx, dy;
		public LatticePoint2D(int xsv, int ysv) {
			this.xsv = xsv; this.ysv = ysv;
			float ssv = (xsv + ysv) * -0.21132487f;
			this.dx = -xsv - ssv;
			this.dy = -ysv - ssv;
		}
	}
	
	private static class LatticePoint3D {
		public float dxr, dyr, dzr;
		public int xrv, yrv, zrv;
		LatticePoint3D nextOnFailure, nextOnSuccess;
		public LatticePoint3D(int xrv, int yrv, int zrv, int lattice) {
			this.dxr = -xrv + lattice * 0.5f; this.dyr = -yrv + lattice * 0.5f; this.dzr = -zrv + lattice * 0.5f;
			this.xrv = xrv + lattice * 1024; this.yrv = yrv + lattice * 1024; this.zrv = zrv + lattice * 1024;
		}
	}
	
	private static class AreaGenLatticePoint2D {
		int xsv, ysv;
		int destPointX, destPointY;
		float gx, gy, gOff;
		public AreaGenLatticePoint2D(GenerateContext2D context, int xsv, int ysv) {
			this.xsv = xsv; this.ysv = ysv;
			float ssv = (xsv + ysv) * -0.21132487f;
			// this.destPointX = (int)Math.round((xsv + ssv) * context.xFrequencyInverse + .5);
			// this.destPointY = (int)Math.round((ysv + ssv) * context.yFrequencyInverse + .5);
			this.destPointX = (int)Math.ceil((xsv + ssv) * context.xFrequencyInverse);
			this.destPointY = (int)Math.ceil((ysv + ssv) * context.yFrequencyInverse);
		}
		public void computeGradient(GenerateContext2D context, SuperSimplexNoise instance) {
			int pxm = xsv & PMASK, pym = ysv & PMASK;
			Float2 grad = instance.permGrad2[instance.perm[pxm] ^ pym];
			this.gx = grad.dx * context.xFrequency;
			this.gy = grad.dy * context.yFrequency;
			this.gOff = 0.5f * (this.gx + this.gy); // to correct for (0.5f, 0.5f)-offset kernel
		}
		public int hashCode() {
			return xsv * 7841 + ysv;
		}
		public boolean equals(Object obj) {
			if (!(obj instanceof AreaGenLatticePoint2D)) return false;
			AreaGenLatticePoint2D other = (AreaGenLatticePoint2D) obj;
			return (other.xsv == this.xsv && other.ysv == this.ysv);
		}
	}
	
	private static class AreaGenLatticePoint3D {
		int xsv, ysv, zsv, lattice;
		int destPointX, destPointY, destPointZ;
		float gx, gy, gz, gOff;
		public AreaGenLatticePoint3D(GenerateContext3D context, int xsv, int ysv, int zsv, int lattice) {
			this.xsv = xsv; this.ysv = ysv; this.zsv = zsv; this.lattice = lattice;
			float xr = (xsv - lattice * 1024.5f);
			float yr = (ysv - lattice * 1024.5f);
			float zr = (zsv - lattice * 1024.5f);
			
			// Quaternion multiplication for inverse rotation.
			// https://blog.molecular-matters.com/2013/05/24/a-faster-quaternion-vector-multiplication/
			float qx = -context.qx, qy = -context.qy, qz = -context.qz, qw = context.qw;
			float tx = 2 * (qy * zr - qz * yr);
			float ty = 2 * (qz * xr - qx * zr);
			float tz = 2 * (qx * yr - qy * xr);
			float xrr = xr + qw * tx + (qy * tz - qz * ty);
			float yrr = yr + qw * ty + (qz * tx - qx * tz);
			float zrr = zr + qw * tz + (qx * ty - qy * tx);
		
			this.destPointX = (int)Math.ceil(xrr * context.xFrequencyInverse);
			this.destPointY = (int)Math.ceil(yrr * context.yFrequencyInverse);
			this.destPointZ = (int)Math.ceil(zrr * context.zFrequencyInverse);
		}
		public void computeGradient(GenerateContext3D context, SuperSimplexNoise instance) {
			int pxm = xsv & PMASK, pym = ysv & PMASK, pzm = zsv & PMASK;
			Grad3 grad = context.gradients[instance.perm[instance.perm[instance.perm[pxm] ^ pym] ^ pzm]];
			this.gx = grad.dx * context.xFrequency;
			this.gy = grad.dy * context.yFrequency;
			this.gz = grad.dz * context.zFrequency;
			this.gOff = 0.5f * (this.gx + this.gy + this.gz); // to correct for (0.5f, 0.5f, 0.5f)-offset kernel
		}
		public int hashCode() {
			return xsv * 2122193 + ysv * 2053 + zsv * 2 + lattice;
		}
		public boolean equals(Object obj) {
			if (!(obj instanceof AreaGenLatticePoint3D)) return false;
			AreaGenLatticePoint3D other = (AreaGenLatticePoint3D) obj;
			return (other.xsv == this.xsv && other.ysv == this.ysv && other.zsv == this.zsv);
		}
	}
	
	public static class GenerateContext2D {
		
		float xFrequency;
		float yFrequency;
		float xFrequencyInverse;
		float yFrequencyInverse;
		int scaledRadiusX;
		int scaledRadiusY;
		float[][] kernel;
		int[] kernelBounds;
		
		public GenerateContext2D(float xFrequency, float yFrequency, float amplitude) {
		
			// These will be used by every call to generate
			this.xFrequency = xFrequency;
			this.yFrequency = yFrequency;
			this.xFrequencyInverse = 1.0f / xFrequency;
			this.yFrequencyInverse = 1.0f / yFrequency;
			
			float preciseScaledRadiusX = 0.816496580927726f * xFrequencyInverse;//Math.sqrt(2.0 / 3.0) * xFrequencyInverse;
			float preciseScaledRadiusY = 0.816496580927726f * yFrequencyInverse;//Math.sqrt(2.0f / 3.0f) * yFrequencyInverse;
			
			// 0.25f because we offset center by 0.5f
			this.scaledRadiusX = (int)Math.ceil(preciseScaledRadiusX + 0.25f);
			this.scaledRadiusY = (int)Math.ceil(preciseScaledRadiusY + 0.25f);
		
			// So will these
			kernel = new float[scaledRadiusY/* * 2*/][];
			kernelBounds = new int[scaledRadiusY * 2];
			for (int yy = 0; yy < scaledRadiusY * 2; yy++) {
				
				// Pre-generate boundary of circle
				kernelBounds[yy] = (int)Math.ceil(
						Math.sqrt(1.0f
							- (yy + 0.5f - scaledRadiusY) * (yy + 0.5f - scaledRadiusY) / (scaledRadiusY * scaledRadiusY)
						) * scaledRadiusX);
						
				if (yy < scaledRadiusY) {
					kernel[yy] = new float[scaledRadiusX * 2];
					
					// Pre-generate kernel
					for (int xx = 0; xx < scaledRadiusX * 2; xx++) {
						float dx = (xx + 0.5f - scaledRadiusX) * xFrequency;
						float dy = (yy + 0.5f - scaledRadiusY) * yFrequency;
						float attn = (0.6666667f) - dx * dx - dy * dy;
						if (attn > 0) {
							attn *= attn;
							kernel[yy][xx] = attn * attn * amplitude;
						} else {
							kernel[yy][xx] = 0.0f;
						}
					}
				} /* else kernel[yy] = kernel[2 * scaledRadiusY - yy - 1]; */
			}
		}
	}
	
	public static class GenerateContext3D {
		
		float xFrequency;
		float yFrequency;
		float zFrequency;
		float xFrequencyInverse;
		float yFrequencyInverse;
		float zFrequencyInverse;
		int scaledRadiusX;
		int scaledRadiusY;
		int scaledRadiusZ;
		float[][][] kernel;
		int[] kernelBoundsY;
		int[][] kernelBoundsX;
		
		float qx, qy, qz, qw;
		Grad3[] gradients;
		
		public GenerateContext3D(LatticeOrientation3D orientation, float xFrequency, float yFrequency, float zFrequency, float amplitude) {
			
			// Set up quaternions for lattice orientation.
			// Could use matrices, but I already wrote this code before I moved them into here.
			switch(orientation) {
				case Classic:
					qx = qy = qz = 0.577350269189626f;
					qw = 0;
					gradients = GRADIENTS_3D_C;
					break;
				case PlaneFirst:
					qx = 0.3250575836718682f;
					qy = -0.3250576f;
					qz = 0.0f;
					qw = 0.8880738339771154f;
					gradients = GRADIENTS_3D_PF;
					break;
			}
		
			// These will be used by every call to generate
			this.xFrequency = xFrequency;
			this.yFrequency = yFrequency;
			this.zFrequency = zFrequency;
			this.xFrequencyInverse = 1.0f / xFrequency;
			this.yFrequencyInverse = 1.0f / yFrequency;
			this.zFrequencyInverse = 1.0f / zFrequency;
			
			float preciseScaledRadiusX = 0.8660254037844386f * xFrequencyInverse;
			float preciseScaledRadiusY = 0.8660254037844386f * yFrequencyInverse;
			float preciseScaledRadiusZ = 0.8660254037844386f * zFrequencyInverse;
			
			// 0.25f because we offset center by 0.5f
			this.scaledRadiusX = (int)Math.ceil(preciseScaledRadiusX + 0.25f);
			this.scaledRadiusY = (int)Math.ceil(preciseScaledRadiusY + 0.25f);
			this.scaledRadiusZ = (int)Math.ceil(preciseScaledRadiusZ + 0.25f);
		
			// So will these
			kernel = new float[scaledRadiusZ * 2][][];
			kernelBoundsY = new int[scaledRadiusZ * 2];
			kernelBoundsX = new int[scaledRadiusZ * 2][];
			for (int zz = 0; zz < scaledRadiusZ * 2; zz++) {
				
				// Pre-generate boundary of sphere
				kernelBoundsY[zz] = (int)Math.ceil(
						Math.sqrt(1.0f - (zz + 0.5f - scaledRadiusZ) * (zz + 0.5f - scaledRadiusZ)
						/ (scaledRadiusZ * scaledRadiusZ)) * scaledRadiusY);
				
				if (zz < scaledRadiusZ) {
					kernel[zz] = new float[scaledRadiusY * 2][];
					kernelBoundsX[zz] = new int[scaledRadiusY * 2];
				} else {
					kernel[zz] = kernel[2 * scaledRadiusZ - zz - 1];
					kernelBoundsX[zz] = kernelBoundsX[2 * scaledRadiusZ - zz - 1];
				}
						
				if (zz < scaledRadiusZ) {
					for (int yy = 0; yy < scaledRadiusY * 2; yy++) {
						
						// Pre-generate boundary of sphere
						kernelBoundsX[zz][yy] = (int)Math.ceil(
								Math.sqrt(1.0f
									- (yy + 0.5f - scaledRadiusY) * (yy + 0.5f - scaledRadiusY) / (scaledRadiusY * scaledRadiusY)
									- (zz + 0.5f - scaledRadiusZ) * (zz + 0.5f - scaledRadiusZ) / (scaledRadiusZ * scaledRadiusZ)
								) * scaledRadiusX);
						
						if (yy < scaledRadiusY) {
							kernel[zz][yy] = new float[scaledRadiusX * 2];
					
							// Pre-generate kernel
							for (int xx = 0; xx < scaledRadiusX * 2; xx++) {
								float dx = (xx + 0.5f - scaledRadiusX) * xFrequency;
								float dy = (yy + 0.5f - scaledRadiusY) * yFrequency;
								float dz = (zz + 0.5f - scaledRadiusZ) * zFrequency;
								float attn = 0.75f - dx * dx - dy * dy - dz * dz;
								if (attn > 0) {
									attn *= attn;
									kernel[zz][yy][xx] = attn * attn * amplitude;
								} else {
									kernel[zz][yy][xx] = 0.0f;
								}
							}
							
						} else kernel[zz][yy] = kernel[zz][2 * scaledRadiusY - yy - 1];
					}
				}
			}
		}
	}
	
	public enum LatticeOrientation3D {
		Classic,
		PlaneFirst
	}
	
	/*
	 * Gradients
	 */
	
	public static class Float2 {
		float dx, dy;
		public Float2(float dx, float dy) {
			this.dx = dx; this.dy = dy;
		}
	}
	
	public static class Grad3 {
		float dx, dy, dz;
		public Grad3(float dx, float dy, float dz) {
			this.dx = dx; this.dy = dy; this.dz = dz;
		}
	}
	
//	public static final float N2 = 0.05382168030817933f;
	public static final float N3 = 0.2781926117527186f;
	private static final Float2[] GRADIENTS_2D = new Float2[] {
			new Float2(0.0f, 18.579874f),
			new Float2(9.289937f, 16.090643f),
			new Float2(16.090643f, 9.289937f),
			new Float2(18.579874f, 0.0f),
			new Float2(16.090643f, -9.289937f),
			new Float2(9.289937f, -16.090643f),
			new Float2(0.0f, -18.579874f),
			new Float2(-9.289937f, -16.090643f),
			new Float2(-16.090643f, -9.289937f),
			new Float2(-18.579874f, 0.0f),
			new Float2(-16.090643f, 9.289937f),
			new Float2(-9.289937f, 16.090643f),

			new Float2(0.0f, 18.579874f),
			new Float2(9.289937f, 16.090643f),
			new Float2(16.090643f, 9.289937f),
			new Float2(18.579874f, 0.0f),
			new Float2(16.090643f, -9.289937f),
			new Float2(9.289937f, -16.090643f),
			new Float2(0.0f, -18.579874f),
			new Float2(-9.289937f, -16.090643f),
			new Float2(-16.090643f, -9.289937f),
			new Float2(-18.579874f, 0.0f),
			new Float2(-16.090643f, 9.289937f),
			new Float2(-9.289937f, 16.090643f),

			new Float2(9.289937f, 16.090643f),
			new Float2(16.090643f, 9.289937f),
			new Float2(18.579874f, 0.0f),
			new Float2(9.289937f, -16.090643f),
			new Float2(0.0f, -18.579874f),
			new Float2(-9.289937f, -16.090643f),
			new Float2(-16.090643f, 9.289937f),
			new Float2(-9.289937f, 16.090643f),
	};
	private static final Grad3[] GRADIENTS_3D, GRADIENTS_3D_C, GRADIENTS_3D_PF;
	static {
//		for (int i = 0; i < grad2.length; i++) {
//			grad2[i].dx /= N2; grad2[i].dy /= N2;
//		}
		GRADIENTS_3D = new Grad3[PSIZE];
		GRADIENTS_3D_C = new Grad3[PSIZE];
		GRADIENTS_3D_PF = new Grad3[PSIZE];
		Grad3[] grad3 = {
			new Grad3(-2.2247448f, -2.2247448f,      -1.0f),
			new Grad3(-2.2247448f, -2.2247448f,       1.0f),
			new Grad3(-3.0862665f, -1.1721513f,  0.0f),
			new Grad3(-1.1721513f, -3.0862665f,  0.0f),
			new Grad3(-2.2247448f,      -1.0f, -2.2247448f),
			new Grad3(-2.2247448f,       1.0f, -2.2247448f),
			new Grad3(-1.1721513f,  0.0f, -3.0862665f),
			new Grad3(-3.0862665f,  0.0f, -1.1721513f),
			new Grad3(-2.2247448f,      -1.0f,                 2.22474487139f),
			new Grad3(-2.2247448f,       1.0f,                 2.22474487139f),
			new Grad3(-3.0862665f,  0.0f,                 1.1721513422464978f),
			new Grad3(-1.1721513f,  0.0f,                 3.0862664687972017f),
			new Grad3(-2.2247448f,       2.22474487139f,      -1.0f),
			new Grad3(-2.2247448f,       2.22474487139f,       1.0f),
			new Grad3(-1.1721513f,  3.0862664687972017f,  0.0f),
			new Grad3(-3.0862665f,  1.1721513422464978f,  0.0f),
			new Grad3(-1.0f, -2.2247448f, -2.2247448f),
			new Grad3( 1.0f, -2.2247448f, -2.2247448f),
			new Grad3( 0.0f, -3.0862665f, -1.1721513f),
			new Grad3( 0.0f, -1.1721513f, -3.0862665f),
			new Grad3(-1.0f, -2.2247448f,       2.22474487139f),
			new Grad3( 1.0f, -2.2247448f,       2.22474487139f),
			new Grad3( 0.0f, -1.1721513f,  3.0862664687972017f),
			new Grad3( 0.0f, -3.0862665f,  1.1721513422464978f),
			new Grad3(-1.0f,                 2.22474487139f, -2.2247448f),
			new Grad3( 1.0f,                 2.22474487139f, -2.2247448f),
			new Grad3( 0.0f,                 1.1721513422464978f, -3.0862665f),
			new Grad3( 0.0f,                 3.0862664687972017f, -1.1721513f),
			new Grad3(-1.0f,                 2.22474487139f,       2.22474487139f),
			new Grad3( 1.0f,                 2.22474487139f,       2.22474487139f),
			new Grad3( 0.0f,                 3.0862664687972017f,  1.1721513422464978f),
			new Grad3( 0.0f,                 1.1721513422464978f,  3.0862664687972017f),
			new Grad3( 2.22474487139f, -2.2247448f,      -1.0f),
			new Grad3( 2.22474487139f, -2.2247448f,       1.0f),
			new Grad3( 1.1721513422464978f, -3.0862665f,  0.0f),
			new Grad3( 3.0862664687972017f, -1.1721513f,  0.0f),
			new Grad3( 2.22474487139f,      -1.0f, -2.2247448f),
			new Grad3( 2.22474487139f,       1.0f, -2.2247448f),
			new Grad3( 3.0862664687972017f,  0.0f, -1.1721513f),
			new Grad3( 1.1721513422464978f,  0.0f, -3.0862665f),
			new Grad3( 2.22474487139f,      -1.0f,                 2.22474487139f),
			new Grad3( 2.22474487139f,       1.0f,                 2.22474487139f),
			new Grad3( 1.1721513422464978f,  0.0f,                 3.0862664687972017f),
			new Grad3( 3.0862664687972017f,  0.0f,                 1.1721513422464978f),
			new Grad3( 2.22474487139f,       2.22474487139f,      -1.0f),
			new Grad3( 2.22474487139f,       2.22474487139f,       1.0f),
			new Grad3( 3.0862664687972017f,  1.1721513422464978f,  0.0f),
			new Grad3( 1.1721513422464978f,  3.0862664687972017f,  0.0f)
		};
		Grad3[] grad3c = new Grad3[grad3.length];
		Grad3[] grad3pf = new Grad3[grad3.length];
		for (int i = 0; i < grad3.length; i++) {
			grad3[i].dx /= N3; grad3[i].dy /= N3; grad3[i].dz /= N3;
			float gxr = grad3[i].dx, gyr = grad3[i].dy, gzr = grad3[i].dz;	

			// Unrotated gradients for classic 3D
			float grr = (0.6666667f) * (gxr + gyr + gzr);
			float dx = grr - gxr, dy = grr - gyr, dz = grr - gzr;
			grad3c[i] = new Grad3( grr - gxr, grr - gyr, grr - gzr );
			
			// Unrotated gradients for plane-first 3D
			float s2 = (gxr + gyr) * -0.21132487f;
			float zz = gzr * 0.577350269189626f;
			grad3pf[i] = new Grad3( gxr + s2 + zz, gyr + s2 + zz, (gzr - gxr - gyr) * 0.577350269189626f );
		}
		for (int i = 0; i < PSIZE; i++) {
			GRADIENTS_3D[i] = grad3[i % grad3.length];
			GRADIENTS_3D_C[i] = grad3c[i % grad3c.length];
			GRADIENTS_3D_PF[i] = grad3pf[i % grad3pf.length];
		}
	}
}












