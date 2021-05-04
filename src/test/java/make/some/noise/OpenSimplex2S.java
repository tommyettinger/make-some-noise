package make.some.noise;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * K.jpg's OpenSimplex 2, smooth variant ("SuperSimplex")
 * <br>
 * Using OpenSimplex 2 in its July 5, 2020 revision. This is the latest
 * revision as of February 26, 2021.
 * <ul>
 *   <li>2D is standard simplex, modified to support larger kernels.
 *   Implemented using a lookup table.</li>
 *   <li>3D is "Re-oriented 8-point BCC noise" which constructs a
 *   congruent BCC lattice in a much different way than usual.</li>
 *   <li>4D uses a naïve pregenerated lookup table, and averages out
 *   to the expected performance.</li>
 * </ul>
 */
public class OpenSimplex2S {
	
	private static final int PSIZE = 2048;
	private static final int PMASK = 2047;

	private short[] perm;
	private Grad2[] permGrad2;
	private Grad3[] permGrad3;
	private Grad4[] permGrad4;

	public OpenSimplex2S(){
		this(1234567890987654321L);
	}

	public OpenSimplex2S(long seed) {
		perm = new short[PSIZE];
		permGrad2 = new Grad2[PSIZE];
		permGrad3 = new Grad3[PSIZE];
		permGrad4 = new Grad4[PSIZE];
		short[] source = new short[PSIZE]; 
		for (short i = 0; i < PSIZE; i++)
			source[i] = i;
		for (int i = PSIZE - 1; i >= 0; i--) {
			seed = seed * 6364136223846793005L + 1442695040888963407L;
			int r = (int)((seed >>> 32) * (i + 1) >>> 32);
			perm[i] = source[r];
			permGrad2[i] = GRADIENTS_2D[perm[i]];
			permGrad3[i] = GRADIENTS_3D[perm[i]];
			permGrad4[i] = GRADIENTS_4D[perm[i]];
			source[r] = source[i];
		}
	}

	/*
	 * Noise Evaluators
	 */

	/**
	 * 2D SuperSimplex noise, standard lattice orientation.
	 */
	public double noise2(double x, double y) {

		// Get points for A2* lattice
		double s = 0.366025403784439 * (x + y);
		double xs = x + s, ys = y + s;

		return noise2_Base(xs, ys);
	}

	/**
	 * 2D SuperSimplex noise, with Y pointing down the main diagonal.
	 * Might be better for a 2D sandbox style game, where Y is vertical.
	 * Probably slightly less optimal for heightmaps or continent maps.
	 */
	public double noise2_XBeforeY(double x, double y) {

		// Skew transform and rotation baked into one.
		double xx = x * 0.7071067811865476;
		double yy = y * 1.224744871380249;

		return noise2_Base(yy + xx, yy - xx);
	}

	/**
	 * 2D SuperSimplex noise base.
	 * Lookup table implementation inspired by DigitalShadow.
	 */
	private double noise2_Base(double xs, double ys) {
		double value = 0;

		// Get base points and offsets
		int xsb = fastFloor(xs), ysb = fastFloor(ys);
		double xsi = xs - xsb, ysi = ys - ysb;

		// Index to point list
		int a = (int)(xsi + ysi);
		int index =
			(a << 2) |
			(int)(xsi - ysi / 2 + 1 - a / 2.0) << 3 |
			(int)(ysi - xsi / 2 + 1 - a / 2.0) << 4;

		double ssi = (xsi + ysi) * -0.211324865405187;
		double xi = xsi + ssi, yi = ysi + ssi;

		// Point contributions
		for (int i = 0; i < 4; i++) {
			LatticePoint2D c = LOOKUP_2D[index + i];

			double dx = xi + c.dx, dy = yi + c.dy;
			double attn = 2.0 / 3.0 - dx * dx - dy * dy;
			if (attn <= 0) continue;

			int pxm = ((int) xsb + c.xsv) & PMASK, pym = ((int) ysb + c.ysv) & PMASK;
			Grad2 grad = permGrad2[perm[pxm] ^ pym];
			double extrapolation = grad.dx * dx + grad.dy * dy;

			attn *= attn;
			value += attn * attn * extrapolation;
		}

		return value;
	}

	/**
	 * 3D Re-oriented 8-point BCC noise, classic orientation
	 * Proper substitute for what 3D SuperSimplex would be,
	 * in light of Forbidden Formulae.
	 * Use noise3_XYBeforeZ or noise3_XZBeforeY instead, wherever appropriate.
	 */
	public double noise3_Classic(double x, double y, double z) {

		// Re-orient the cubic lattices via rotation, to produce the expected look on cardinal planar slices.
		// If texturing objects that don't tend to have cardinal plane faces, you could even remove this.
		// Orthonormal rotation. Not a skew transform.
		double r = (2.0 / 3.0) * (x + y + z);
		double xr = r - x, yr = r - y, zr = r - z;

		// Evaluate both lattices to form a BCC lattice.
		return noise3_BCC(xr, yr, zr);
	}

	/**
	 * 3D Re-oriented 8-point BCC noise, with better visual isotropy in (X, Y).
	 * Recommended for 3D terrain and time-varied animations.
	 * The Z coordinate should always be the "different" coordinate in your use case.
	 * If Y is vertical in world coordinates, call noise3_XYBeforeZ(x, z, Y) or use noise3_XZBeforeY.
	 * If Z is vertical in world coordinates, call noise3_XYBeforeZ(x, y, Z).
	 * For a time varied animation, call noise3_XYBeforeZ(x, y, T).
	 */
	public double noise3_XYBeforeZ(double x, double y, double z) {

		// Re-orient the cubic lattices without skewing, to make X and Y triangular like 2D.
		// Orthonormal rotation. Not a skew transform.
		double xy = x + y;
		double s2 = xy * -0.211324865405187;
		double zz = z * 0.577350269189626;
		double xr = x + s2 - zz, yr = y + s2 - zz;
		double zr = xy * 0.577350269189626 + zz;

		// Evaluate both lattices to form a BCC lattice.
		return noise3_BCC(xr, yr, zr);
	}

	/**
	 * 3D Re-oriented 8-point BCC noise, with better visual isotropy in (X, Z).
	 * Recommended for 3D terrain and time-varied animations.
	 * The Y coordinate should always be the "different" coordinate in your use case.
	 * If Y is vertical in world coordinates, call noise3_XZBeforeY(x, Y, z).
	 * If Z is vertical in world coordinates, call noise3_XZBeforeY(x, Z, y) or use noise3_XYBeforeZ.
	 * For a time varied animation, call noise3_XZBeforeY(x, T, y) or use noise3_XYBeforeZ.
	 */
	public double noise3_XZBeforeY(double x, double y, double z) {

		// Re-orient the cubic lattices without skewing, to make X and Z triangular like 2D.
		// Orthonormal rotation. Not a skew transform.
		double xz = x + z;
		double s2 = xz * -0.211324865405187;
		double yy = y * 0.577350269189626;
		double xr = x + s2 - yy; double zr = z + s2 - yy;
		double yr = xz * 0.577350269189626 + yy;

		// Evaluate both lattices to form a BCC lattice.
		return noise3_BCC(xr, yr, zr);
	}

	/**
	 * Generate overlapping cubic lattices for 3D Re-oriented BCC noise.
	 * Lookup table implementation inspired by DigitalShadow.
	 * It was actually faster to narrow down the points in the loop itself,
	 * than to build up the index with enough info to isolate 8 points.
	 */
	private double noise3_BCC(double xr, double yr, double zr) {

		// Get base and offsets inside cube of first lattice.
		int xrb = fastFloor(xr), yrb = fastFloor(yr), zrb = fastFloor(zr);
		double xri = xr - xrb, yri = yr - yrb, zri = zr - zrb;

		// Identify which octant of the cube we're in. This determines which cell
		// in the other cubic lattice we're in, and also narrows down one point on each.
		int xht = (int)(xri + 0.5), yht = (int)(yri + 0.5), zht = (int)(zri + 0.5);
		int index = (xht << 0) | (yht << 1) | (zht << 2);

		// Point contributions
		double value = 0;
		LatticePoint3D c = LOOKUP_3D[index];
		while (c != null) {
			double dxr = xri + c.dxr, dyr = yri + c.dyr, dzr = zri + c.dzr;
			double attn = 0.75 - dxr * dxr - dyr * dyr - dzr * dzr;
			if (attn < 0) {
				c = c.nextOnFailure;
			} else {
				int pxm = ((int) xrb + c.xrv) & PMASK, pym = ((int) yrb + c.yrv) & PMASK, pzm = ((int) zrb + c.zrv) & PMASK;
				Grad3 grad = permGrad3[perm[perm[pxm] ^ pym] ^ pzm];
				double extrapolation = grad.dx * dxr + grad.dy * dyr + grad.dz * dzr;

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
	public void generate2(GenerateContext2D context, double[][] buffer, int x0, int y0) {
		int height = buffer.length;
		int width = buffer[0].length;
		generate2(context, buffer, x0, y0, width, height, 0, 0);
	}

	/**
	 * Generate the 2D noise over a large area.
	 * Propagates by flood-fill instead of iterating over a range.
	 * Results may occasionally slightly exceed [-1, 1] due to the grid-snapped pre-generated kernel.
	 */
	public void generate2(GenerateContext2D context, double[][] buffer, int x0, int y0, int width, int height, int skipX, int skipY) {
		Queue<AreaGenLatticePoint2D> queue = new ArrayDeque<>();
		Set<AreaGenLatticePoint2D> seen = new HashSet<>();

		int scaledRadiusX = context.scaledRadiusX;
		int scaledRadiusY = context.scaledRadiusY;
		double[][] kernel = context.kernel;
		int x0Skipped = x0 + skipX, y0Skipped = y0 + skipY;

		// It seems that it's better for performance, to create a local copy.
		// - Slightly faster than generating the kernel here.
		// - Much faster than referencing it directly from the context object.
		// - Much faster than computing the kernel equation every time.
		// You can remove these lines if you find it's the opposite for you.
		// You'll have to double the bounds again in GenerateContext2D
		kernel = new double[scaledRadiusY * 2][/*scaledRadiusX * 2*/];
		for (int yy = 0; yy < scaledRadiusY; yy++) {
			kernel[2 * scaledRadiusY - yy - 1] = kernel[yy] = (double[]) context.kernel[yy].clone();
		}

		// Get started with one point/vertex.
		// For some lattices, you might need to try a handful of points in the cell,
		// or flip a couple of coordinates, to guarantee it or a neighbor contributes.
		// For An* lattices, the base coordinate seems fine.
		double x0f = x0Skipped * context.xFrequency; double y0f = y0Skipped * context.yFrequency;
		double x0s = context.orientation.s00 * x0f + context.orientation.s01 * y0f;
		double y0s = context.orientation.s10 * x0f + context.orientation.s11 * y0f;
		int x0sb = fastFloor(x0s), y0sb = fastFloor(y0s);
		AreaGenLatticePoint2D firstPoint = new AreaGenLatticePoint2D(context, x0sb, y0sb);
		queue.add(firstPoint);
		seen.add(firstPoint);

		while (!queue.isEmpty()) {
			AreaGenLatticePoint2D point = queue.remove();
			int destPointX = point.destPointX;
			int destPointY = point.destPointY;

			// Prepare gradient vector
			int pxm = point.xsv & PMASK, pym = point.ysv & PMASK;
			Grad2 grad = context.orientation.gradients[perm[perm[pxm] ^ pym]];
			double gx = grad.dx * context.xFrequency;
			double gy = grad.dy * context.yFrequency;
			double gOff = 0.5 * (gx + gy); // to correct for (0.5, 0.5)-offset kernel

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

					// gOff accounts for our choice to offset the pre-generated kernel by (0.5, 0.5) to avoid the zero center.
					// I found almost no difference in performance using gOff vs not (under 1ns diff per value on my system)
					double extrapolation = gx * dx + gy * dy + gOff;
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
	public void generate3(GenerateContext3D context, double[][][] buffer, int x0, int y0, int z0) {
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
	public void generate3(GenerateContext3D context, double[][][] buffer, int x0, int y0, int z0, int width, int height, int depth, int skipX, int skipY, int skipZ) {
		Queue<AreaGenLatticePoint3D> queue = new ArrayDeque<>();
		Set<AreaGenLatticePoint3D> seen = new HashSet<>();

		int scaledRadiusX = context.scaledRadiusX;
		int scaledRadiusY = context.scaledRadiusY;
		int scaledRadiusZ = context.scaledRadiusZ;
		double[][][] kernel = context.kernel;
		int x0Skipped = x0 + skipX, y0Skipped = y0 + skipY, z0Skipped = z0 + skipZ;

		// Quaternion multiplication for rotation.
		// https://blog.molecular-matters.com/2013/05/24/a-faster-quaternion-vector-multiplication/
		double qx = context.orientation.qx, qy = context.orientation.qy, qz = context.orientation.qz, qw = context.orientation.qw;
		double x0f = x0Skipped * context.xFrequency, y0f = y0Skipped * context.yFrequency, z0f = z0Skipped * context.zFrequency;
		double tx = 2 * (qy * z0f - qz * y0f);
		double ty = 2 * (qz * x0f - qx * z0f);
		double tz = 2 * (qx * y0f - qy * x0f);
		double x0r = x0f + qw * tx + (qy * tz - qz * ty);
		double y0r = y0f + qw * ty + (qz * tx - qx * tz);
		double z0r = z0f + qw * tz + (qx * ty - qy * tx);

		int x0rb = fastFloor(x0r), y0rb = fastFloor(y0r), z0rb = fastFloor(z0r);

		AreaGenLatticePoint3D firstPoint = new AreaGenLatticePoint3D(context, x0rb, y0rb, z0rb, 0);
		queue.add(firstPoint);
		seen.add(firstPoint);

		while (!queue.isEmpty()) {
			AreaGenLatticePoint3D point = queue.remove();
			int destPointX = point.destPointX;
			int destPointY = point.destPointY;
			int destPointZ = point.destPointZ;

			// Prepare gradient vector
			int pxm = point.xsv & PMASK, pym = point.ysv & PMASK, pzm = point.zsv & PMASK;
			Grad3 grad = context.orientation.gradients[perm[perm[perm[pxm] ^ pym] ^ pzm]];
			double gx = grad.dx * context.xFrequency;
			double gy = grad.dy * context.yFrequency;
			double gz = grad.dz * context.zFrequency;
			double gOff = 0.5 * (gx + gy + gz); // to correct for (0.5, 0.5, 0.5)-offset kernel

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

						// gOff accounts for our choice to offset the pre-generated kernel by (0.5, 0.5, 0.5) to avoid the zero center.
						double extrapolation = gx * dx + gy * dy + gz * dz + gOff;
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

	private static int fastFloor(double x) {
		int xi = (int)x;
		return x < xi ? xi - 1 : xi;
	}

	/*
	 * Definitions
	 */

	private static final LatticePoint2D[] LOOKUP_2D;
	private static final LatticePoint3D[] LOOKUP_3D;
	static {
		LOOKUP_2D = new LatticePoint2D[8 * 4];
		LOOKUP_3D = new LatticePoint3D[8];

		for (int i = 0; i < 8; i++) {
			int i1, j1, i2, j2;
			if ((i & 1) == 0) {
				if ((i & 2) == 0) { i1 = -1; j1 = 0; } else { i1 = 1; j1 = 0; }
				if ((i & 4) == 0) { i2 = 0; j2 = -1; } else { i2 = 0; j2 = 1; }
			} else {
				if ((i & 2) != 0) { i1 = 2; j1 = 1; } else { i1 = 0; j1 = 1; }
				if ((i & 4) != 0) { i2 = 1; j2 = 2; } else { i2 = 1; j2 = 0; }
			}
			LOOKUP_2D[i * 4 + 0] = new LatticePoint2D(0, 0);
			LOOKUP_2D[i * 4 + 1] = new LatticePoint2D(1, 1);
			LOOKUP_2D[i * 4 + 2] = new LatticePoint2D(i1, j1);
			LOOKUP_2D[i * 4 + 3] = new LatticePoint2D(i2, j2);
		}

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
		double dx, dy;
		public LatticePoint2D(int xsv, int ysv) {
			this.xsv = xsv; this.ysv = ysv;
			double ssv = (xsv + ysv) * -0.211324865405187;
			this.dx = -xsv - ssv;
			this.dy = -ysv - ssv;
		}
	}

	private static class LatticePoint3D {
		public double dxr, dyr, dzr;
		public int xrv, yrv, zrv;
		LatticePoint3D nextOnFailure, nextOnSuccess;
		public LatticePoint3D(int xrv, int yrv, int zrv, int lattice) {
			this.dxr = -xrv + lattice * 0.5; this.dyr = -yrv + lattice * 0.5; this.dzr = -zrv + lattice * 0.5;
			this.xrv = xrv + lattice * 1024; this.yrv = yrv + lattice * 1024; this.zrv = zrv + lattice * 1024;
		}
	}

	private static class AreaGenLatticePoint2D {
		int xsv, ysv;
		int destPointX, destPointY;
		public AreaGenLatticePoint2D(GenerateContext2D context, int xsv, int ysv) {
			this.xsv = xsv; this.ysv = ysv;

			//Matrix multiplication for inverse rotation. Simplex skew transforms have always been shorthand for matrices.
			this.destPointX = (int)Math.ceil((context.orientation.t00 * xsv + context.orientation.t01 * ysv) * context.xFrequencyInverse);
			this.destPointY = (int)Math.ceil((context.orientation.t10 * xsv + context.orientation.t11 * ysv) * context.yFrequencyInverse);
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
		public AreaGenLatticePoint3D(GenerateContext3D context, int xsv, int ysv, int zsv, int lattice) {
			this.xsv = xsv; this.ysv = ysv; this.zsv = zsv; this.lattice = lattice;
			double xr = (xsv - lattice * 1024.5);
			double yr = (ysv - lattice * 1024.5);
			double zr = (zsv - lattice * 1024.5);

			// Quaternion multiplication for inverse rotation.
			// https://blog.molecular-matters.com/2013/05/24/a-faster-quaternion-vector-multiplication/
			double qx = -context.orientation.qx, qy = -context.orientation.qy, qz = -context.orientation.qz, qw = context.orientation.qw;
			double tx = 2 * (qy * zr - qz * yr);
			double ty = 2 * (qz * xr - qx * zr);
			double tz = 2 * (qx * yr - qy * xr);
			double xrr = xr + qw * tx + (qy * tz - qz * ty);
			double yrr = yr + qw * ty + (qz * tx - qx * tz);
			double zrr = zr + qw * tz + (qx * ty - qy * tx);

			this.destPointX = (int)Math.ceil(xrr * context.xFrequencyInverse);
			this.destPointY = (int)Math.ceil(yrr * context.yFrequencyInverse);
			this.destPointZ = (int)Math.ceil(zrr * context.zFrequencyInverse);
		}
		public int hashCode() {
			return xsv * 2122193 + ysv * 2053 + zsv * 2 + lattice;
		}
		public boolean equals(Object obj) {
			if (!(obj instanceof AreaGenLatticePoint3D)) return false;
			AreaGenLatticePoint3D other = (AreaGenLatticePoint3D) obj;
			return (other.xsv == this.xsv && other.ysv == this.ysv && other.zsv == this.zsv && other.lattice == this.lattice);
		}
	}

	public static class GenerateContext2D {

		double xFrequency;
		double yFrequency;
		double xFrequencyInverse;
		double yFrequencyInverse;
		int scaledRadiusX;
		int scaledRadiusY;
		double[][] kernel;
		int[] kernelBounds;
		LatticeOrientation2D orientation;

		public GenerateContext2D(LatticeOrientation2D orientation, double xFrequency, double yFrequency, double amplitude) {

			// These will be used by every call to generate
			this.orientation = orientation;
			this.xFrequency = xFrequency;
			this.yFrequency = yFrequency;
			this.xFrequencyInverse = 1.0 / xFrequency;
			this.yFrequencyInverse = 1.0 / yFrequency;

			double preciseScaledRadiusX = Math.sqrt(2.0 / 3.0) * xFrequencyInverse;
			double preciseScaledRadiusY = Math.sqrt(2.0 / 3.0) * yFrequencyInverse;

			// 0.25 because we offset center by 0.5
			this.scaledRadiusX = (int)Math.ceil(preciseScaledRadiusX + 0.25);
			this.scaledRadiusY = (int)Math.ceil(preciseScaledRadiusY + 0.25);

			// So will these
			kernel = new double[scaledRadiusY/* * 2*/][];
			kernelBounds = new int[scaledRadiusY * 2];
			for (int yy = 0; yy < scaledRadiusY * 2; yy++) {

				// Pre-generate boundary of circle
				kernelBounds[yy] = (int)Math.ceil(
						Math.sqrt(1.0
								- (yy + 0.5 - scaledRadiusY) * (yy + 0.5 - scaledRadiusY) / (scaledRadiusY * scaledRadiusY)
						) * scaledRadiusX);

				if (yy < scaledRadiusY) {
					kernel[yy] = new double[scaledRadiusX * 2];

					// Pre-generate kernel
					for (int xx = 0; xx < scaledRadiusX * 2; xx++) {
						double dx = (xx + 0.5 - scaledRadiusX) * xFrequency;
						double dy = (yy + 0.5 - scaledRadiusY) * yFrequency;
						double attn = (2.0 / 3.0) - dx * dx - dy * dy;
						if (attn > 0) {
							attn *= attn;
							kernel[yy][xx] = attn * attn * amplitude;
						} else {
							kernel[yy][xx] = 0.0;
						}
					}
				} /* else kernel[yy] = kernel[2 * scaledRadiusY - yy - 1];*/
			}
		}
	}

	public static class GenerateContext3D {

		double xFrequency;
		double yFrequency;
		double zFrequency;
		double xFrequencyInverse;
		double yFrequencyInverse;
		double zFrequencyInverse;
		int scaledRadiusX;
		int scaledRadiusY;
		int scaledRadiusZ;
		double[][][] kernel;
		int[] kernelBoundsY;
		int[][] kernelBoundsX;
		LatticeOrientation3D orientation;

		public GenerateContext3D(LatticeOrientation3D orientation, double xFrequency, double yFrequency, double zFrequency, double amplitude) {

			// These will be used by every call to generate
			this.orientation = orientation;
			this.xFrequency = xFrequency;
			this.yFrequency = yFrequency;
			this.zFrequency = zFrequency;
			this.xFrequencyInverse = 1.0 / xFrequency;
			this.yFrequencyInverse = 1.0 / yFrequency;
			this.zFrequencyInverse = 1.0 / zFrequency;

			double preciseScaledRadiusX = Math.sqrt(0.75) * xFrequencyInverse;
			double preciseScaledRadiusY = Math.sqrt(0.75) * yFrequencyInverse;
			double preciseScaledRadiusZ = Math.sqrt(0.75) * zFrequencyInverse;

			// 0.25 because we offset center by 0.5
			this.scaledRadiusX = (int)Math.ceil(preciseScaledRadiusX + 0.25);
			this.scaledRadiusY = (int)Math.ceil(preciseScaledRadiusY + 0.25);
			this.scaledRadiusZ = (int)Math.ceil(preciseScaledRadiusZ + 0.25);

			// So will these
			kernel = new double[scaledRadiusZ * 2][][];
			kernelBoundsY = new int[scaledRadiusZ * 2];
			kernelBoundsX = new int[scaledRadiusZ * 2][];
			for (int zz = 0; zz < scaledRadiusZ * 2; zz++) {

				// Pre-generate boundary of sphere
				kernelBoundsY[zz] = (int)Math.ceil(
						Math.sqrt(1.0 - (zz + 0.5 - scaledRadiusZ) * (zz + 0.5 - scaledRadiusZ)
								/ (scaledRadiusZ * scaledRadiusZ)) * scaledRadiusY);

				if (zz < scaledRadiusZ) {
					kernel[zz] = new double[scaledRadiusY * 2][];
					kernelBoundsX[zz] = new int[scaledRadiusY * 2];
				} else {
					kernel[zz] = kernel[2 * scaledRadiusZ - zz - 1];
					kernelBoundsX[zz] = kernelBoundsX[2 * scaledRadiusZ - zz - 1];
				}

				if (zz < scaledRadiusZ) {
					for (int yy = 0; yy < scaledRadiusY * 2; yy++) {

						// Pre-generate boundary of sphere
						kernelBoundsX[zz][yy] = (int)Math.ceil(
								Math.sqrt(1.0
										- (yy + 0.5 - scaledRadiusY) * (yy + 0.5 - scaledRadiusY) / (scaledRadiusY * scaledRadiusY)
										- (zz + 0.5 - scaledRadiusZ) * (zz + 0.5 - scaledRadiusZ) / (scaledRadiusZ * scaledRadiusZ)
								) * scaledRadiusX);

						if (yy < scaledRadiusY) {
							kernel[zz][yy] = new double[scaledRadiusX * 2];

							// Pre-generate kernel
							for (int xx = 0; xx < scaledRadiusX * 2; xx++) {
								double dx = (xx + 0.5 - scaledRadiusX) * xFrequency;
								double dy = (yy + 0.5 - scaledRadiusY) * yFrequency;
								double dz = (zz + 0.5 - scaledRadiusZ) * zFrequency;
								double attn = 0.75 - dx * dx - dy * dy - dz * dz;
								if (attn > 0) {
									attn *= attn;
									kernel[zz][yy][xx] = attn * attn * amplitude;
								} else {
									kernel[zz][yy][xx] = 0.0;
								}
							}

						} else kernel[zz][yy] = kernel[zz][2 * scaledRadiusY - yy - 1];
					}
				}
			}
		}
	}

	public enum LatticeOrientation2D {
		// Simplex skew transforms have always been shorthand for the matrices they represent.
		// But when we bake the rotation into the skew transform, we need to use the general form.
		Standard(GRADIENTS_2D,
				1.366025403784439, 0.366025403784439, 0.366025403784439, 1.366025403784439,
				0.788675134594813, -0.211324865405187, -0.211324865405187, 0.788675134594813),
		XBeforeY(GRADIENTS_2D_X_BEFORE_Y,
				0.7071067811865476, 1.224744871380249, -0.7071067811865476, 1.224744871380249,
				0.7071067811865476, -0.7071067811865476, 0.40824829046764305, 0.40824829046764305);

		Grad2[] gradients;
		double s00, s01, s10, s11;
		double t00, t01, t10, t11;

		private LatticeOrientation2D(Grad2[] gradients,
									 double s00, double s01, double s10, double s11,
									 double t00, double t01, double t10, double t11) {
			this.gradients = gradients;
			this.s00 = s00; this.s01 = s01; this.s10 = s10; this.s11 = s11;
			this.t00 = t00; this.t01 = t01; this.t10 = t10; this.t11 = t11;
		}
	}

	public enum LatticeOrientation3D {
		// Quaternions for 3D. Could use matrices, but I already wrote this code before I moved them into here.
		Classic(GRADIENTS_3D_CLASSIC, 0.577350269189626, 0.577350269189626, 0.577350269189626, 0),
		XYBeforeZ(GRADIENTS_3D_XY_BEFORE_Z, 0.3250575836718682, -0.3250575836718682, 0, 0.8880738339771154),
		XZBeforeY(GRADIENTS_3D_XZ_BEFORE_Y, -0.3250575836718682, 0, 0.3250575836718682, 0.8880738339771154);

		Grad3[] gradients;
		double qx, qy, qz, qw;

		private LatticeOrientation3D(Grad3[] gradients, double qx, double qy, double qz, double qw) {
			this.gradients = gradients;
			this.qx = qx; this.qy = qy; this.qz = qz; this.qw = qw;
		}
	}

	/*
	 * Gradients
	 */

	public static class Grad2 {
		double dx, dy;
		public Grad2(double dx, double dy) {
			this.dx = dx; this.dy = dy;
		}
	}

	public static class Grad3 {
		double dx, dy, dz;
		public Grad3(double dx, double dy, double dz) {
			this.dx = dx; this.dy = dy; this.dz = dz;
		}
	}

	public static final double N2 = 0.05481866495625118;
	public static final double N3 = 0.2781926117527186;
	private static final Grad2[] GRADIENTS_2D, GRADIENTS_2D_X_BEFORE_Y;
	private static final Grad3[] GRADIENTS_3D, GRADIENTS_3D_CLASSIC, GRADIENTS_3D_XY_BEFORE_Z, GRADIENTS_3D_XZ_BEFORE_Y;
	static {

		GRADIENTS_2D = new Grad2[PSIZE];
		GRADIENTS_2D_X_BEFORE_Y = new Grad2[PSIZE];
		Grad2[] grad2 = {
				new Grad2( 0.130526192220052,  0.99144486137381),
				new Grad2( 0.38268343236509,   0.923879532511287),
				new Grad2( 0.608761429008721,  0.793353340291235),
				new Grad2( 0.793353340291235,  0.608761429008721),
				new Grad2( 0.923879532511287,  0.38268343236509),
				new Grad2( 0.99144486137381,   0.130526192220051),
				new Grad2( 0.99144486137381,  -0.130526192220051),
				new Grad2( 0.923879532511287, -0.38268343236509),
				new Grad2( 0.793353340291235, -0.60876142900872),
				new Grad2( 0.608761429008721, -0.793353340291235),
				new Grad2( 0.38268343236509,  -0.923879532511287),
				new Grad2( 0.130526192220052, -0.99144486137381),
				new Grad2(-0.130526192220052, -0.99144486137381),
				new Grad2(-0.38268343236509,  -0.923879532511287),
				new Grad2(-0.608761429008721, -0.793353340291235),
				new Grad2(-0.793353340291235, -0.608761429008721),
				new Grad2(-0.923879532511287, -0.38268343236509),
				new Grad2(-0.99144486137381,  -0.130526192220052),
				new Grad2(-0.99144486137381,   0.130526192220051),
				new Grad2(-0.923879532511287,  0.38268343236509),
				new Grad2(-0.793353340291235,  0.608761429008721),
				new Grad2(-0.608761429008721,  0.793353340291235),
				new Grad2(-0.38268343236509,   0.923879532511287),
				new Grad2(-0.130526192220052,  0.99144486137381)
		};
		Grad2[] grad2XBeforeY = new Grad2[grad2.length];
		for (int i = 0; i < grad2.length; i++) {
			grad2[i].dx /= N2; grad2[i].dy /= N2;

			// Unrotated gradients for XBeforeY 2D
			double xx = grad2[i].dx * 0.7071067811865476;
			double yy = grad2[i].dy * 0.7071067811865476;
			grad2XBeforeY[i] = new Grad2(xx - yy, xx + yy);
		}
		for (int i = 0; i < PSIZE; i++) {
			GRADIENTS_2D[i] = grad2[i % grad2.length];
			GRADIENTS_2D_X_BEFORE_Y[i] = grad2XBeforeY[i % grad2XBeforeY.length];
		}

		GRADIENTS_3D = new Grad3[PSIZE];
		GRADIENTS_3D_CLASSIC = new Grad3[PSIZE];
		GRADIENTS_3D_XY_BEFORE_Z = new Grad3[PSIZE];
		GRADIENTS_3D_XZ_BEFORE_Y = new Grad3[PSIZE];
		Grad3[] grad3 = {
				new Grad3(-2.22474487139,      -2.22474487139,      -1.0),
				new Grad3(-2.22474487139,      -2.22474487139,       1.0),
				new Grad3(-3.0862664687972017, -1.1721513422464978,  0.0),
				new Grad3(-1.1721513422464978, -3.0862664687972017,  0.0),
				new Grad3(-2.22474487139,      -1.0,                -2.22474487139),
				new Grad3(-2.22474487139,       1.0,                -2.22474487139),
				new Grad3(-1.1721513422464978,  0.0,                -3.0862664687972017),
				new Grad3(-3.0862664687972017,  0.0,                -1.1721513422464978),
				new Grad3(-2.22474487139,      -1.0,                 2.22474487139),
				new Grad3(-2.22474487139,       1.0,                 2.22474487139),
				new Grad3(-3.0862664687972017,  0.0,                 1.1721513422464978),
				new Grad3(-1.1721513422464978,  0.0,                 3.0862664687972017),
				new Grad3(-2.22474487139,       2.22474487139,      -1.0),
				new Grad3(-2.22474487139,       2.22474487139,       1.0),
				new Grad3(-1.1721513422464978,  3.0862664687972017,  0.0),
				new Grad3(-3.0862664687972017,  1.1721513422464978,  0.0),
				new Grad3(-1.0,                -2.22474487139,      -2.22474487139),
				new Grad3( 1.0,                -2.22474487139,      -2.22474487139),
				new Grad3( 0.0,                -3.0862664687972017, -1.1721513422464978),
				new Grad3( 0.0,                -1.1721513422464978, -3.0862664687972017),
				new Grad3(-1.0,                -2.22474487139,       2.22474487139),
				new Grad3( 1.0,                -2.22474487139,       2.22474487139),
				new Grad3( 0.0,                -1.1721513422464978,  3.0862664687972017),
				new Grad3( 0.0,                -3.0862664687972017,  1.1721513422464978),
				new Grad3(-1.0,                 2.22474487139,      -2.22474487139),
				new Grad3( 1.0,                 2.22474487139,      -2.22474487139),
				new Grad3( 0.0,                 1.1721513422464978, -3.0862664687972017),
				new Grad3( 0.0,                 3.0862664687972017, -1.1721513422464978),
				new Grad3(-1.0,                 2.22474487139,       2.22474487139),
				new Grad3( 1.0,                 2.22474487139,       2.22474487139),
				new Grad3( 0.0,                 3.0862664687972017,  1.1721513422464978),
				new Grad3( 0.0,                 1.1721513422464978,  3.0862664687972017),
				new Grad3( 2.22474487139,      -2.22474487139,      -1.0),
				new Grad3( 2.22474487139,      -2.22474487139,       1.0),
				new Grad3( 1.1721513422464978, -3.0862664687972017,  0.0),
				new Grad3( 3.0862664687972017, -1.1721513422464978,  0.0),
				new Grad3( 2.22474487139,      -1.0,                -2.22474487139),
				new Grad3( 2.22474487139,       1.0,                -2.22474487139),
				new Grad3( 3.0862664687972017,  0.0,                -1.1721513422464978),
				new Grad3( 1.1721513422464978,  0.0,                -3.0862664687972017),
				new Grad3( 2.22474487139,      -1.0,                 2.22474487139),
				new Grad3( 2.22474487139,       1.0,                 2.22474487139),
				new Grad3( 1.1721513422464978,  0.0,                 3.0862664687972017),
				new Grad3( 3.0862664687972017,  0.0,                 1.1721513422464978),
				new Grad3( 2.22474487139,       2.22474487139,      -1.0),
				new Grad3( 2.22474487139,       2.22474487139,       1.0),
				new Grad3( 3.0862664687972017,  1.1721513422464978,  0.0),
				new Grad3( 1.1721513422464978,  3.0862664687972017,  0.0)
		};
		Grad3[] grad3Classic = new Grad3[grad3.length];
		Grad3[] grad3XYBeforeZ = new Grad3[grad3.length];
		Grad3[] grad3XZBeforeY = new Grad3[grad3.length];
		for (int i = 0; i < grad3.length; i++) {
			grad3[i].dx /= N3; grad3[i].dy /= N3; grad3[i].dz /= N3;
			double gxr = grad3[i].dx, gyr = grad3[i].dy, gzr = grad3[i].dz;

			// Unrotated gradients for classic 3D
			double grr = (2.0 / 3.0) * (gxr + gyr + gzr);
			double dx = grr - gxr, dy = grr - gyr, dz = grr - gzr;
			grad3Classic[i] = new Grad3( grr - gxr, grr - gyr, grr - gzr );

			// Unrotated gradients for XYBeforeZ 3D
			double s2 = (gxr + gyr) * -0.211324865405187;
			double zz = gzr * 0.577350269189626;
			grad3XYBeforeZ[i] = new Grad3( gxr + s2 + zz, gyr + s2 + zz, (gzr - gxr - gyr) * 0.577350269189626 );

			// Unrotated gradients for plane-first 3D
			s2 = (gxr + gzr) * -0.211324865405187;
			double yy = gyr * 0.577350269189626;
			grad3XZBeforeY[i] = new Grad3( gxr + s2 + yy, (gyr - gxr - gzr) * 0.577350269189626, gzr + s2 + yy );
		}
		for (int i = 0; i < PSIZE; i++) {
			GRADIENTS_3D[i] = grad3[i % grad3.length];
			GRADIENTS_3D_CLASSIC[i] = grad3Classic[i % grad3Classic.length];
			GRADIENTS_3D_XY_BEFORE_Z[i] = grad3XYBeforeZ[i % grad3XYBeforeZ.length];
			GRADIENTS_3D_XZ_BEFORE_Y[i] = grad3XZBeforeY[i % grad3XZBeforeY.length];
		}
	}

	/**
	 * 4D SuperSimplex noise, classic lattice orientation.
	 */
	public double noise4_Classic(double x, double y, double z, double w) {
		
		// Get points for A4 lattice
		double s = 0.309016994374947 * (x + y + z + w);
		double xs = x + s, ys = y + s, zs = z + s, ws = w + s;
		
		return noise4_Base(xs, ys, zs, ws);
	}
	
	/**
	 * 4D SuperSimplex noise, with XY and ZW forming orthogonal triangular-based planes.
	 * Recommended for 3D terrain, where X and Y (or Z and W) are horizontal.
	 * Recommended for noise(x, y, sin(time), cos(time)) trick.
	 */
	public double noise4_XYBeforeZW(double x, double y, double z, double w) {
		
		double s2 = (x + y) * -0.28522513987434876941 + (z + w) * 0.83897065470611435718;
		double t2 = (z + w) * 0.21939749883706435719 + (x + y) * -0.48214856493302476942;
		double xs = x + s2, ys = y + s2, zs = z + t2, ws = w + t2;
		
		return noise4_Base(xs, ys, zs, ws);
	}
	
	/**
	 * 4D SuperSimplex noise, with XZ and YW forming orthogonal triangular-based planes.
	 * Recommended for 3D terrain, where X and Z (or Y and W) are horizontal.
	 */
	public double noise4_XZBeforeYW(double x, double y, double z, double w) {
		
		double s2 = (x + z) * -0.28522513987434876941 + (y + w) * 0.83897065470611435718;
		double t2 = (y + w) * 0.21939749883706435719 + (x + z) * -0.48214856493302476942;
		double xs = x + s2, ys = y + t2, zs = z + s2, ws = w + t2;
		
		return noise4_Base(xs, ys, zs, ws);
	}
	
	/**
	 * 4D SuperSimplex noise, with XYZ oriented like noise3_Classic,
	 * and W for an extra degree of freedom.
	 * Recommended for time-varied animations which texture a 3D object (W=time)
	 */
	public double noise4_XYZBeforeW(double x, double y, double z, double w) {
		
		double xyz = x + y + z;
		double ww = w * 1.118033988749894;
		double s2 = xyz * -0.16666666666666666 + ww;
		double xs = x + s2, ys = y + s2, zs = z + s2, ws = -0.5 * xyz + ww;
		
		return noise4_Base(xs, ys, zs, ws);
	}
	
	/**
	 * 4D SuperSimplex noise base.
	 * Using ultra-simple 4x4x4x4 lookup partitioning.
	 * This isn't as elegant or SIMD/GPU/etc. portable as other approaches,
	 * but it does compete performance-wise with optimized OpenSimplex1.
	 */
	private double noise4_Base(double xs, double ys, double zs, double ws) {
		double value = 0;
		
		// Get base points and offsets
		int xsb = fastFloor(xs), ysb = fastFloor(ys), zsb = fastFloor(zs), wsb = fastFloor(ws);
		double xsi = xs - xsb, ysi = ys - ysb, zsi = zs - zsb, wsi = ws - wsb;
		
		// Unskewed offsets
		double ssi = (xsi + ysi + zsi + wsi) * -0.138196601125011;
		double xi = xsi + ssi, yi = ysi + ssi, zi = zsi + ssi, wi = wsi + ssi;
			
		int index =
			  ((fastFloor(xs * 4) & 3)
			| ((fastFloor(ys * 4) & 3) << 2)
			| ((fastFloor(zs * 4) & 3) << 4)
			| ((fastFloor(ws * 4) & 3) << 6));
		
		// Point contributions
		for (LatticePoint4D c : LOOKUP_4D[index]) {
			double dx = xi + c.dx, dy = yi + c.dy, dz = zi + c.dz, dw = wi + c.dw;
			double attn = 0.8 - dx * dx - dy * dy - dz * dz - dw * dw;
			if (attn > 0) {
				attn *= attn;

				int pxm = ((int) xsb + c.xsv) & PMASK, pym = ((int) ysb + c.ysv) & PMASK;
				int pzm = ((int) zsb + c.zsv) & PMASK, pwm = ((int) wsb + c.wsv) & PMASK;
				Grad4 grad = permGrad4[perm[perm[perm[pxm] ^ pym] ^ pzm] ^ pwm];
				double extrapolation = grad.dx * dx + grad.dy * dy + grad.dz * dz + grad.dw * dw;
				
				value += attn * attn * extrapolation;
			}
		}
		return value;
	}

	/*
	 * Definitions
	 */

	private static final LatticePoint4D[][] LOOKUP_4D;
	static {
		LOOKUP_4D = new LatticePoint4D[256][];
		

		int[][] lookup4DPregen = {
			{ 0x15, 0x45, 0x51, 0x54, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA },
			{ 0x15, 0x45, 0x51, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x6A, 0x95, 0x96, 0x9A, 0xA6, 0xAA },
			{ 0x01, 0x05, 0x11, 0x15, 0x41, 0x45, 0x51, 0x55, 0x56, 0x5A, 0x66, 0x6A, 0x96, 0x9A, 0xA6, 0xAA },
			{ 0x01, 0x15, 0x16, 0x45, 0x46, 0x51, 0x52, 0x55, 0x56, 0x5A, 0x66, 0x6A, 0x96, 0x9A, 0xA6, 0xAA, 0xAB },
			{ 0x15, 0x45, 0x54, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x69, 0x6A, 0x95, 0x99, 0x9A, 0xA9, 0xAA },
			{ 0x05, 0x15, 0x45, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xAA },
			{ 0x05, 0x15, 0x45, 0x55, 0x56, 0x59, 0x5A, 0x66, 0x6A, 0x96, 0x9A, 0xAA },
			{ 0x05, 0x15, 0x16, 0x45, 0x46, 0x55, 0x56, 0x59, 0x5A, 0x66, 0x6A, 0x96, 0x9A, 0xAA, 0xAB },
			{ 0x04, 0x05, 0x14, 0x15, 0x44, 0x45, 0x54, 0x55, 0x59, 0x5A, 0x69, 0x6A, 0x99, 0x9A, 0xA9, 0xAA },
			{ 0x05, 0x15, 0x45, 0x55, 0x56, 0x59, 0x5A, 0x69, 0x6A, 0x99, 0x9A, 0xAA },
			{ 0x05, 0x15, 0x45, 0x55, 0x56, 0x59, 0x5A, 0x6A, 0x9A, 0xAA },
			{ 0x05, 0x15, 0x16, 0x45, 0x46, 0x55, 0x56, 0x59, 0x5A, 0x5B, 0x6A, 0x9A, 0xAA, 0xAB },
			{ 0x04, 0x15, 0x19, 0x45, 0x49, 0x54, 0x55, 0x58, 0x59, 0x5A, 0x69, 0x6A, 0x99, 0x9A, 0xA9, 0xAA, 0xAE },
			{ 0x05, 0x15, 0x19, 0x45, 0x49, 0x55, 0x56, 0x59, 0x5A, 0x69, 0x6A, 0x99, 0x9A, 0xAA, 0xAE },
			{ 0x05, 0x15, 0x19, 0x45, 0x49, 0x55, 0x56, 0x59, 0x5A, 0x5E, 0x6A, 0x9A, 0xAA, 0xAE },
			{ 0x05, 0x15, 0x1A, 0x45, 0x4A, 0x55, 0x56, 0x59, 0x5A, 0x5B, 0x5E, 0x6A, 0x9A, 0xAA, 0xAB, 0xAE, 0xAF },
			{ 0x15, 0x51, 0x54, 0x55, 0x56, 0x59, 0x65, 0x66, 0x69, 0x6A, 0x95, 0xA5, 0xA6, 0xA9, 0xAA },
			{ 0x11, 0x15, 0x51, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x95, 0x96, 0xA5, 0xA6, 0xAA },
			{ 0x11, 0x15, 0x51, 0x55, 0x56, 0x5A, 0x65, 0x66, 0x6A, 0x96, 0xA6, 0xAA },
			{ 0x11, 0x15, 0x16, 0x51, 0x52, 0x55, 0x56, 0x5A, 0x65, 0x66, 0x6A, 0x96, 0xA6, 0xAA, 0xAB },
			{ 0x14, 0x15, 0x54, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x95, 0x99, 0xA5, 0xA9, 0xAA },
			{ 0x15, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x95, 0x9A, 0xA6, 0xA9, 0xAA },
			{ 0x15, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x96, 0x9A, 0xA6, 0xAA, 0xAB },
			{ 0x15, 0x16, 0x55, 0x56, 0x5A, 0x66, 0x6A, 0x6B, 0x96, 0x9A, 0xA6, 0xAA, 0xAB },
			{ 0x14, 0x15, 0x54, 0x55, 0x59, 0x5A, 0x65, 0x69, 0x6A, 0x99, 0xA9, 0xAA },
			{ 0x15, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x99, 0x9A, 0xA9, 0xAA, 0xAE },
			{ 0x15, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x9A, 0xAA },
			{ 0x15, 0x16, 0x55, 0x56, 0x59, 0x5A, 0x66, 0x6A, 0x6B, 0x9A, 0xAA, 0xAB },
			{ 0x14, 0x15, 0x19, 0x54, 0x55, 0x58, 0x59, 0x5A, 0x65, 0x69, 0x6A, 0x99, 0xA9, 0xAA, 0xAE },
			{ 0x15, 0x19, 0x55, 0x59, 0x5A, 0x69, 0x6A, 0x6E, 0x99, 0x9A, 0xA9, 0xAA, 0xAE },
			{ 0x15, 0x19, 0x55, 0x56, 0x59, 0x5A, 0x69, 0x6A, 0x6E, 0x9A, 0xAA, 0xAE },
			{ 0x15, 0x1A, 0x55, 0x56, 0x59, 0x5A, 0x6A, 0x6B, 0x6E, 0x9A, 0xAA, 0xAB, 0xAE, 0xAF },
			{ 0x10, 0x11, 0x14, 0x15, 0x50, 0x51, 0x54, 0x55, 0x65, 0x66, 0x69, 0x6A, 0xA5, 0xA6, 0xA9, 0xAA },
			{ 0x11, 0x15, 0x51, 0x55, 0x56, 0x65, 0x66, 0x69, 0x6A, 0xA5, 0xA6, 0xAA },
			{ 0x11, 0x15, 0x51, 0x55, 0x56, 0x65, 0x66, 0x6A, 0xA6, 0xAA },
			{ 0x11, 0x15, 0x16, 0x51, 0x52, 0x55, 0x56, 0x65, 0x66, 0x67, 0x6A, 0xA6, 0xAA, 0xAB },
			{ 0x14, 0x15, 0x54, 0x55, 0x59, 0x65, 0x66, 0x69, 0x6A, 0xA5, 0xA9, 0xAA },
			{ 0x15, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA },
			{ 0x15, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0xA6, 0xAA },
			{ 0x15, 0x16, 0x55, 0x56, 0x5A, 0x65, 0x66, 0x6A, 0x6B, 0xA6, 0xAA, 0xAB },
			{ 0x14, 0x15, 0x54, 0x55, 0x59, 0x65, 0x69, 0x6A, 0xA9, 0xAA },
			{ 0x15, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0xA9, 0xAA },
			{ 0x15, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0xAA },
			{ 0x15, 0x16, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x6B, 0xAA, 0xAB },
			{ 0x14, 0x15, 0x19, 0x54, 0x55, 0x58, 0x59, 0x65, 0x69, 0x6A, 0x6D, 0xA9, 0xAA, 0xAE },
			{ 0x15, 0x19, 0x55, 0x59, 0x5A, 0x65, 0x69, 0x6A, 0x6E, 0xA9, 0xAA, 0xAE },
			{ 0x15, 0x19, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x6E, 0xAA, 0xAE },
			{ 0x15, 0x55, 0x56, 0x59, 0x5A, 0x66, 0x69, 0x6A, 0x6B, 0x6E, 0x9A, 0xAA, 0xAB, 0xAE, 0xAF },
			{ 0x10, 0x15, 0x25, 0x51, 0x54, 0x55, 0x61, 0x64, 0x65, 0x66, 0x69, 0x6A, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA },
			{ 0x11, 0x15, 0x25, 0x51, 0x55, 0x56, 0x61, 0x65, 0x66, 0x69, 0x6A, 0xA5, 0xA6, 0xAA, 0xBA },
			{ 0x11, 0x15, 0x25, 0x51, 0x55, 0x56, 0x61, 0x65, 0x66, 0x6A, 0x76, 0xA6, 0xAA, 0xBA },
			{ 0x11, 0x15, 0x26, 0x51, 0x55, 0x56, 0x62, 0x65, 0x66, 0x67, 0x6A, 0x76, 0xA6, 0xAA, 0xAB, 0xBA, 0xBB },
			{ 0x14, 0x15, 0x25, 0x54, 0x55, 0x59, 0x64, 0x65, 0x66, 0x69, 0x6A, 0xA5, 0xA9, 0xAA, 0xBA },
			{ 0x15, 0x25, 0x55, 0x65, 0x66, 0x69, 0x6A, 0x7A, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA },
			{ 0x15, 0x25, 0x55, 0x56, 0x65, 0x66, 0x69, 0x6A, 0x7A, 0xA6, 0xAA, 0xBA },
			{ 0x15, 0x26, 0x55, 0x56, 0x65, 0x66, 0x6A, 0x6B, 0x7A, 0xA6, 0xAA, 0xAB, 0xBA, 0xBB },
			{ 0x14, 0x15, 0x25, 0x54, 0x55, 0x59, 0x64, 0x65, 0x69, 0x6A, 0x79, 0xA9, 0xAA, 0xBA },
			{ 0x15, 0x25, 0x55, 0x59, 0x65, 0x66, 0x69, 0x6A, 0x7A, 0xA9, 0xAA, 0xBA },
			{ 0x15, 0x25, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x7A, 0xAA, 0xBA },
			{ 0x15, 0x55, 0x56, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x6B, 0x7A, 0xA6, 0xAA, 0xAB, 0xBA, 0xBB },
			{ 0x14, 0x15, 0x29, 0x54, 0x55, 0x59, 0x65, 0x68, 0x69, 0x6A, 0x6D, 0x79, 0xA9, 0xAA, 0xAE, 0xBA, 0xBE },
			{ 0x15, 0x29, 0x55, 0x59, 0x65, 0x69, 0x6A, 0x6E, 0x7A, 0xA9, 0xAA, 0xAE, 0xBA, 0xBE },
			{ 0x15, 0x55, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x6E, 0x7A, 0xA9, 0xAA, 0xAE, 0xBA, 0xBE },
			{ 0x15, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x6B, 0x6E, 0x7A, 0xAA, 0xAB, 0xAE, 0xBA, 0xBF },
			{ 0x45, 0x51, 0x54, 0x55, 0x56, 0x59, 0x65, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA },
			{ 0x41, 0x45, 0x51, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xAA },
			{ 0x41, 0x45, 0x51, 0x55, 0x56, 0x5A, 0x66, 0x95, 0x96, 0x9A, 0xA6, 0xAA },
			{ 0x41, 0x45, 0x46, 0x51, 0x52, 0x55, 0x56, 0x5A, 0x66, 0x95, 0x96, 0x9A, 0xA6, 0xAA, 0xAB },
			{ 0x44, 0x45, 0x54, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x69, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA9, 0xAA },
			{ 0x45, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xA6, 0xA9, 0xAA },
			{ 0x45, 0x55, 0x56, 0x59, 0x5A, 0x66, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xA6, 0xAA, 0xAB },
			{ 0x45, 0x46, 0x55, 0x56, 0x5A, 0x66, 0x6A, 0x96, 0x9A, 0x9B, 0xA6, 0xAA, 0xAB },
			{ 0x44, 0x45, 0x54, 0x55, 0x59, 0x5A, 0x69, 0x95, 0x99, 0x9A, 0xA9, 0xAA },
			{ 0x45, 0x55, 0x56, 0x59, 0x5A, 0x69, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xA9, 0xAA, 0xAE },
			{ 0x45, 0x55, 0x56, 0x59, 0x5A, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xAA },
			{ 0x45, 0x46, 0x55, 0x56, 0x59, 0x5A, 0x6A, 0x96, 0x9A, 0x9B, 0xAA, 0xAB },
			{ 0x44, 0x45, 0x49, 0x54, 0x55, 0x58, 0x59, 0x5A, 0x69, 0x95, 0x99, 0x9A, 0xA9, 0xAA, 0xAE },
			{ 0x45, 0x49, 0x55, 0x59, 0x5A, 0x69, 0x6A, 0x99, 0x9A, 0x9E, 0xA9, 0xAA, 0xAE },
			{ 0x45, 0x49, 0x55, 0x56, 0x59, 0x5A, 0x6A, 0x99, 0x9A, 0x9E, 0xAA, 0xAE },
			{ 0x45, 0x4A, 0x55, 0x56, 0x59, 0x5A, 0x6A, 0x9A, 0x9B, 0x9E, 0xAA, 0xAB, 0xAE, 0xAF },
			{ 0x50, 0x51, 0x54, 0x55, 0x56, 0x59, 0x65, 0x66, 0x69, 0x95, 0x96, 0x99, 0xA5, 0xA6, 0xA9, 0xAA },
			{ 0x51, 0x55, 0x56, 0x59, 0x65, 0x66, 0x6A, 0x95, 0x96, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA },
			{ 0x51, 0x55, 0x56, 0x5A, 0x65, 0x66, 0x6A, 0x95, 0x96, 0x9A, 0xA5, 0xA6, 0xAA, 0xAB },
			{ 0x51, 0x52, 0x55, 0x56, 0x5A, 0x66, 0x6A, 0x96, 0x9A, 0xA6, 0xA7, 0xAA, 0xAB },
			{ 0x54, 0x55, 0x56, 0x59, 0x65, 0x69, 0x6A, 0x95, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA },
			{ 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA },
			{ 0x15, 0x45, 0x51, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x6A, 0x95, 0x96, 0x9A, 0xA6, 0xAA, 0xAB },
			{ 0x55, 0x56, 0x5A, 0x66, 0x6A, 0x96, 0x9A, 0xA6, 0xAA, 0xAB },
			{ 0x54, 0x55, 0x59, 0x5A, 0x65, 0x69, 0x6A, 0x95, 0x99, 0x9A, 0xA5, 0xA9, 0xAA, 0xAE },
			{ 0x15, 0x45, 0x54, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x69, 0x6A, 0x95, 0x99, 0x9A, 0xA9, 0xAA, 0xAE },
			{ 0x15, 0x45, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xA6, 0xA9, 0xAA, 0xAB, 0xAE },
			{ 0x55, 0x56, 0x59, 0x5A, 0x66, 0x6A, 0x96, 0x9A, 0xA6, 0xAA, 0xAB },
			{ 0x54, 0x55, 0x58, 0x59, 0x5A, 0x69, 0x6A, 0x99, 0x9A, 0xA9, 0xAA, 0xAD, 0xAE },
			{ 0x55, 0x59, 0x5A, 0x69, 0x6A, 0x99, 0x9A, 0xA9, 0xAA, 0xAE },
			{ 0x55, 0x56, 0x59, 0x5A, 0x69, 0x6A, 0x99, 0x9A, 0xA9, 0xAA, 0xAE },
			{ 0x55, 0x56, 0x59, 0x5A, 0x6A, 0x9A, 0xAA, 0xAB, 0xAE, 0xAF },
			{ 0x50, 0x51, 0x54, 0x55, 0x65, 0x66, 0x69, 0x95, 0xA5, 0xA6, 0xA9, 0xAA },
			{ 0x51, 0x55, 0x56, 0x65, 0x66, 0x69, 0x6A, 0x95, 0x96, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA },
			{ 0x51, 0x55, 0x56, 0x65, 0x66, 0x6A, 0x95, 0x96, 0xA5, 0xA6, 0xAA },
			{ 0x51, 0x52, 0x55, 0x56, 0x65, 0x66, 0x6A, 0x96, 0xA6, 0xA7, 0xAA, 0xAB },
			{ 0x54, 0x55, 0x59, 0x65, 0x66, 0x69, 0x6A, 0x95, 0x99, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA },
			{ 0x15, 0x51, 0x54, 0x55, 0x56, 0x59, 0x65, 0x66, 0x69, 0x6A, 0x95, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA },
			{ 0x15, 0x51, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x95, 0x96, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xAB, 0xBA },
			{ 0x55, 0x56, 0x5A, 0x65, 0x66, 0x6A, 0x96, 0x9A, 0xA6, 0xAA, 0xAB },
			{ 0x54, 0x55, 0x59, 0x65, 0x69, 0x6A, 0x95, 0x99, 0xA5, 0xA9, 0xAA },
			{ 0x15, 0x54, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x95, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xAE, 0xBA },
			{ 0x15, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x9A, 0xA6, 0xA9, 0xAA },
			{ 0x15, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x96, 0x9A, 0xA6, 0xAA, 0xAB },
			{ 0x54, 0x55, 0x58, 0x59, 0x65, 0x69, 0x6A, 0x99, 0xA9, 0xAA, 0xAD, 0xAE },
			{ 0x55, 0x59, 0x5A, 0x65, 0x69, 0x6A, 0x99, 0x9A, 0xA9, 0xAA, 0xAE },
			{ 0x15, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x99, 0x9A, 0xA9, 0xAA, 0xAE },
			{ 0x15, 0x55, 0x56, 0x59, 0x5A, 0x66, 0x69, 0x6A, 0x9A, 0xAA, 0xAB, 0xAE, 0xAF },
			{ 0x50, 0x51, 0x54, 0x55, 0x61, 0x64, 0x65, 0x66, 0x69, 0x95, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA },
			{ 0x51, 0x55, 0x61, 0x65, 0x66, 0x69, 0x6A, 0xA5, 0xA6, 0xA9, 0xAA, 0xB6, 0xBA },
			{ 0x51, 0x55, 0x56, 0x61, 0x65, 0x66, 0x6A, 0xA5, 0xA6, 0xAA, 0xB6, 0xBA },
			{ 0x51, 0x55, 0x56, 0x62, 0x65, 0x66, 0x6A, 0xA6, 0xA7, 0xAA, 0xAB, 0xB6, 0xBA, 0xBB },
			{ 0x54, 0x55, 0x64, 0x65, 0x66, 0x69, 0x6A, 0xA5, 0xA6, 0xA9, 0xAA, 0xB9, 0xBA },
			{ 0x55, 0x65, 0x66, 0x69, 0x6A, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA },
			{ 0x55, 0x56, 0x65, 0x66, 0x69, 0x6A, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA },
			{ 0x55, 0x56, 0x65, 0x66, 0x6A, 0xA6, 0xAA, 0xAB, 0xBA, 0xBB },
			{ 0x54, 0x55, 0x59, 0x64, 0x65, 0x69, 0x6A, 0xA5, 0xA9, 0xAA, 0xB9, 0xBA },
			{ 0x55, 0x59, 0x65, 0x66, 0x69, 0x6A, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA },
			{ 0x15, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA },
			{ 0x15, 0x55, 0x56, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0xA6, 0xAA, 0xAB, 0xBA, 0xBB },
			{ 0x54, 0x55, 0x59, 0x65, 0x68, 0x69, 0x6A, 0xA9, 0xAA, 0xAD, 0xAE, 0xB9, 0xBA, 0xBE },
			{ 0x55, 0x59, 0x65, 0x69, 0x6A, 0xA9, 0xAA, 0xAE, 0xBA, 0xBE },
			{ 0x15, 0x55, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0xA9, 0xAA, 0xAE, 0xBA, 0xBE },
			{ 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0xAA, 0xAB, 0xAE, 0xBA, 0xBF },
			{ 0x40, 0x41, 0x44, 0x45, 0x50, 0x51, 0x54, 0x55, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA },
			{ 0x41, 0x45, 0x51, 0x55, 0x56, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xAA },
			{ 0x41, 0x45, 0x51, 0x55, 0x56, 0x95, 0x96, 0x9A, 0xA6, 0xAA },
			{ 0x41, 0x45, 0x46, 0x51, 0x52, 0x55, 0x56, 0x95, 0x96, 0x97, 0x9A, 0xA6, 0xAA, 0xAB },
			{ 0x44, 0x45, 0x54, 0x55, 0x59, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA9, 0xAA },
			{ 0x45, 0x55, 0x56, 0x59, 0x5A, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xEA },
			{ 0x45, 0x55, 0x56, 0x59, 0x5A, 0x95, 0x96, 0x99, 0x9A, 0xA6, 0xAA },
			{ 0x45, 0x46, 0x55, 0x56, 0x5A, 0x95, 0x96, 0x9A, 0x9B, 0xA6, 0xAA, 0xAB },
			{ 0x44, 0x45, 0x54, 0x55, 0x59, 0x95, 0x99, 0x9A, 0xA9, 0xAA },
			{ 0x45, 0x55, 0x56, 0x59, 0x5A, 0x95, 0x96, 0x99, 0x9A, 0xA9, 0xAA },
			{ 0x45, 0x55, 0x56, 0x59, 0x5A, 0x95, 0x96, 0x99, 0x9A, 0xAA },
			{ 0x45, 0x46, 0x55, 0x56, 0x59, 0x5A, 0x95, 0x96, 0x99, 0x9A, 0x9B, 0xAA, 0xAB },
			{ 0x44, 0x45, 0x49, 0x54, 0x55, 0x58, 0x59, 0x95, 0x99, 0x9A, 0x9D, 0xA9, 0xAA, 0xAE },
			{ 0x45, 0x49, 0x55, 0x59, 0x5A, 0x95, 0x99, 0x9A, 0x9E, 0xA9, 0xAA, 0xAE },
			{ 0x45, 0x49, 0x55, 0x56, 0x59, 0x5A, 0x95, 0x96, 0x99, 0x9A, 0x9E, 0xAA, 0xAE },
			{ 0x45, 0x55, 0x56, 0x59, 0x5A, 0x6A, 0x96, 0x99, 0x9A, 0x9B, 0x9E, 0xAA, 0xAB, 0xAE, 0xAF },
			{ 0x50, 0x51, 0x54, 0x55, 0x65, 0x95, 0x96, 0x99, 0xA5, 0xA6, 0xA9, 0xAA },
			{ 0x51, 0x55, 0x56, 0x65, 0x66, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xEA },
			{ 0x51, 0x55, 0x56, 0x65, 0x66, 0x95, 0x96, 0x9A, 0xA5, 0xA6, 0xAA },
			{ 0x51, 0x52, 0x55, 0x56, 0x66, 0x95, 0x96, 0x9A, 0xA6, 0xA7, 0xAA, 0xAB },
			{ 0x54, 0x55, 0x59, 0x65, 0x69, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xEA },
			{ 0x45, 0x51, 0x54, 0x55, 0x56, 0x59, 0x65, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xEA },
			{ 0x45, 0x51, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xAB, 0xEA },
			{ 0x55, 0x56, 0x5A, 0x66, 0x6A, 0x95, 0x96, 0x9A, 0xA6, 0xAA, 0xAB },
			{ 0x54, 0x55, 0x59, 0x65, 0x69, 0x95, 0x99, 0x9A, 0xA5, 0xA9, 0xAA },
			{ 0x45, 0x54, 0x55, 0x56, 0x59, 0x5A, 0x65, 0x69, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xAE, 0xEA },
			{ 0x45, 0x55, 0x56, 0x59, 0x5A, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xA6, 0xA9, 0xAA },
			{ 0x45, 0x55, 0x56, 0x59, 0x5A, 0x66, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xA6, 0xAA, 0xAB },
			{ 0x54, 0x55, 0x58, 0x59, 0x69, 0x95, 0x99, 0x9A, 0xA9, 0xAA, 0xAD, 0xAE },
			{ 0x55, 0x59, 0x5A, 0x69, 0x6A, 0x95, 0x99, 0x9A, 0xA9, 0xAA, 0xAE },
			{ 0x45, 0x55, 0x56, 0x59, 0x5A, 0x69, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xA9, 0xAA, 0xAE },
			{ 0x45, 0x55, 0x56, 0x59, 0x5A, 0x6A, 0x96, 0x99, 0x9A, 0xAA, 0xAB, 0xAE, 0xAF },
			{ 0x50, 0x51, 0x54, 0x55, 0x65, 0x95, 0xA5, 0xA6, 0xA9, 0xAA },
			{ 0x51, 0x55, 0x56, 0x65, 0x66, 0x95, 0x96, 0xA5, 0xA6, 0xA9, 0xAA },
			{ 0x51, 0x55, 0x56, 0x65, 0x66, 0x95, 0x96, 0xA5, 0xA6, 0xAA },
			{ 0x51, 0x52, 0x55, 0x56, 0x65, 0x66, 0x95, 0x96, 0xA5, 0xA6, 0xA7, 0xAA, 0xAB },
			{ 0x54, 0x55, 0x59, 0x65, 0x69, 0x95, 0x99, 0xA5, 0xA6, 0xA9, 0xAA },
			{ 0x51, 0x54, 0x55, 0x56, 0x59, 0x65, 0x66, 0x69, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA, 0xEA },
			{ 0x51, 0x55, 0x56, 0x65, 0x66, 0x6A, 0x95, 0x96, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA },
			{ 0x51, 0x55, 0x56, 0x5A, 0x65, 0x66, 0x6A, 0x95, 0x96, 0x9A, 0xA5, 0xA6, 0xAA, 0xAB },
			{ 0x54, 0x55, 0x59, 0x65, 0x69, 0x95, 0x99, 0xA5, 0xA9, 0xAA },
			{ 0x54, 0x55, 0x59, 0x65, 0x69, 0x6A, 0x95, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA },
			{ 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA },
			{ 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x6A, 0x95, 0x96, 0x9A, 0xA6, 0xA9, 0xAA, 0xAB },
			{ 0x54, 0x55, 0x58, 0x59, 0x65, 0x69, 0x95, 0x99, 0xA5, 0xA9, 0xAA, 0xAD, 0xAE },
			{ 0x54, 0x55, 0x59, 0x5A, 0x65, 0x69, 0x6A, 0x95, 0x99, 0x9A, 0xA5, 0xA9, 0xAA, 0xAE },
			{ 0x55, 0x56, 0x59, 0x5A, 0x65, 0x69, 0x6A, 0x95, 0x99, 0x9A, 0xA6, 0xA9, 0xAA, 0xAE },
			{ 0x55, 0x56, 0x59, 0x5A, 0x66, 0x69, 0x6A, 0x96, 0x99, 0x9A, 0xA6, 0xA9, 0xAA, 0xAB, 0xAE, 0xAF },
			{ 0x50, 0x51, 0x54, 0x55, 0x61, 0x64, 0x65, 0x95, 0xA5, 0xA6, 0xA9, 0xAA, 0xB5, 0xBA },
			{ 0x51, 0x55, 0x61, 0x65, 0x66, 0x95, 0xA5, 0xA6, 0xA9, 0xAA, 0xB6, 0xBA },
			{ 0x51, 0x55, 0x56, 0x61, 0x65, 0x66, 0x95, 0x96, 0xA5, 0xA6, 0xAA, 0xB6, 0xBA },
			{ 0x51, 0x55, 0x56, 0x65, 0x66, 0x6A, 0x96, 0xA5, 0xA6, 0xA7, 0xAA, 0xAB, 0xB6, 0xBA, 0xBB },
			{ 0x54, 0x55, 0x64, 0x65, 0x69, 0x95, 0xA5, 0xA6, 0xA9, 0xAA, 0xB9, 0xBA },
			{ 0x55, 0x65, 0x66, 0x69, 0x6A, 0x95, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA },
			{ 0x51, 0x55, 0x56, 0x65, 0x66, 0x69, 0x6A, 0x95, 0x96, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA },
			{ 0x51, 0x55, 0x56, 0x65, 0x66, 0x6A, 0x96, 0xA5, 0xA6, 0xAA, 0xAB, 0xBA, 0xBB },
			{ 0x54, 0x55, 0x59, 0x64, 0x65, 0x69, 0x95, 0x99, 0xA5, 0xA9, 0xAA, 0xB9, 0xBA },
			{ 0x54, 0x55, 0x59, 0x65, 0x66, 0x69, 0x6A, 0x95, 0x99, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA },
			{ 0x55, 0x56, 0x59, 0x65, 0x66, 0x69, 0x6A, 0x95, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA },
			{ 0x55, 0x56, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x96, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xAB, 0xBA, 0xBB },
			{ 0x54, 0x55, 0x59, 0x65, 0x69, 0x6A, 0x99, 0xA5, 0xA9, 0xAA, 0xAD, 0xAE, 0xB9, 0xBA, 0xBE },
			{ 0x54, 0x55, 0x59, 0x65, 0x69, 0x6A, 0x99, 0xA5, 0xA9, 0xAA, 0xAE, 0xBA, 0xBE },
			{ 0x55, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xAE, 0xBA, 0xBE },
			{ 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x9A, 0xA6, 0xA9, 0xAA, 0xAB, 0xAE, 0xBA },
			{ 0x40, 0x45, 0x51, 0x54, 0x55, 0x85, 0x91, 0x94, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xEA },
			{ 0x41, 0x45, 0x51, 0x55, 0x56, 0x85, 0x91, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xAA, 0xEA },
			{ 0x41, 0x45, 0x51, 0x55, 0x56, 0x85, 0x91, 0x95, 0x96, 0x9A, 0xA6, 0xAA, 0xD6, 0xEA },
			{ 0x41, 0x45, 0x51, 0x55, 0x56, 0x86, 0x92, 0x95, 0x96, 0x97, 0x9A, 0xA6, 0xAA, 0xAB, 0xD6, 0xEA, 0xEB },
			{ 0x44, 0x45, 0x54, 0x55, 0x59, 0x85, 0x94, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA9, 0xAA, 0xEA },
			{ 0x45, 0x55, 0x85, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xDA, 0xEA },
			{ 0x45, 0x55, 0x56, 0x85, 0x95, 0x96, 0x99, 0x9A, 0xA6, 0xAA, 0xDA, 0xEA },
			{ 0x45, 0x55, 0x56, 0x86, 0x95, 0x96, 0x9A, 0x9B, 0xA6, 0xAA, 0xAB, 0xDA, 0xEA, 0xEB },
			{ 0x44, 0x45, 0x54, 0x55, 0x59, 0x85, 0x94, 0x95, 0x99, 0x9A, 0xA9, 0xAA, 0xD9, 0xEA },
			{ 0x45, 0x55, 0x59, 0x85, 0x95, 0x96, 0x99, 0x9A, 0xA9, 0xAA, 0xDA, 0xEA },
			{ 0x45, 0x55, 0x56, 0x59, 0x5A, 0x85, 0x95, 0x96, 0x99, 0x9A, 0xAA, 0xDA, 0xEA },
			{ 0x45, 0x55, 0x56, 0x5A, 0x95, 0x96, 0x99, 0x9A, 0x9B, 0xA6, 0xAA, 0xAB, 0xDA, 0xEA, 0xEB },
			{ 0x44, 0x45, 0x54, 0x55, 0x59, 0x89, 0x95, 0x98, 0x99, 0x9A, 0x9D, 0xA9, 0xAA, 0xAE, 0xD9, 0xEA, 0xEE },
			{ 0x45, 0x55, 0x59, 0x89, 0x95, 0x99, 0x9A, 0x9E, 0xA9, 0xAA, 0xAE, 0xDA, 0xEA, 0xEE },
			{ 0x45, 0x55, 0x59, 0x5A, 0x95, 0x96, 0x99, 0x9A, 0x9E, 0xA9, 0xAA, 0xAE, 0xDA, 0xEA, 0xEE },
			{ 0x45, 0x55, 0x56, 0x59, 0x5A, 0x95, 0x96, 0x99, 0x9A, 0x9B, 0x9E, 0xAA, 0xAB, 0xAE, 0xDA, 0xEA, 0xEF },
			{ 0x50, 0x51, 0x54, 0x55, 0x65, 0x91, 0x94, 0x95, 0x96, 0x99, 0xA5, 0xA6, 0xA9, 0xAA, 0xEA },
			{ 0x51, 0x55, 0x91, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xE6, 0xEA },
			{ 0x51, 0x55, 0x56, 0x91, 0x95, 0x96, 0x9A, 0xA5, 0xA6, 0xAA, 0xE6, 0xEA },
			{ 0x51, 0x55, 0x56, 0x92, 0x95, 0x96, 0x9A, 0xA6, 0xA7, 0xAA, 0xAB, 0xE6, 0xEA, 0xEB },
			{ 0x54, 0x55, 0x94, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xE9, 0xEA },
			{ 0x55, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xEA },
			{ 0x55, 0x56, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xEA },
			{ 0x55, 0x56, 0x95, 0x96, 0x9A, 0xA6, 0xAA, 0xAB, 0xEA, 0xEB },
			{ 0x54, 0x55, 0x59, 0x94, 0x95, 0x99, 0x9A, 0xA5, 0xA9, 0xAA, 0xE9, 0xEA },
			{ 0x55, 0x59, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xEA },
			{ 0x45, 0x55, 0x56, 0x59, 0x5A, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xEA },
			{ 0x45, 0x55, 0x56, 0x5A, 0x95, 0x96, 0x99, 0x9A, 0xA6, 0xAA, 0xAB, 0xEA, 0xEB },
			{ 0x54, 0x55, 0x59, 0x95, 0x98, 0x99, 0x9A, 0xA9, 0xAA, 0xAD, 0xAE, 0xE9, 0xEA, 0xEE },
			{ 0x55, 0x59, 0x95, 0x99, 0x9A, 0xA9, 0xAA, 0xAE, 0xEA, 0xEE },
			{ 0x45, 0x55, 0x59, 0x5A, 0x95, 0x96, 0x99, 0x9A, 0xA9, 0xAA, 0xAE, 0xEA, 0xEE },
			{ 0x55, 0x56, 0x59, 0x5A, 0x95, 0x96, 0x99, 0x9A, 0xAA, 0xAB, 0xAE, 0xEA, 0xEF },
			{ 0x50, 0x51, 0x54, 0x55, 0x65, 0x91, 0x94, 0x95, 0xA5, 0xA6, 0xA9, 0xAA, 0xE5, 0xEA },
			{ 0x51, 0x55, 0x65, 0x91, 0x95, 0x96, 0xA5, 0xA6, 0xA9, 0xAA, 0xE6, 0xEA },
			{ 0x51, 0x55, 0x56, 0x65, 0x66, 0x91, 0x95, 0x96, 0xA5, 0xA6, 0xAA, 0xE6, 0xEA },
			{ 0x51, 0x55, 0x56, 0x66, 0x95, 0x96, 0x9A, 0xA5, 0xA6, 0xA7, 0xAA, 0xAB, 0xE6, 0xEA, 0xEB },
			{ 0x54, 0x55, 0x65, 0x94, 0x95, 0x99, 0xA5, 0xA6, 0xA9, 0xAA, 0xE9, 0xEA },
			{ 0x55, 0x65, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xEA },
			{ 0x51, 0x55, 0x56, 0x65, 0x66, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xEA },
			{ 0x51, 0x55, 0x56, 0x66, 0x95, 0x96, 0x9A, 0xA5, 0xA6, 0xAA, 0xAB, 0xEA, 0xEB },
			{ 0x54, 0x55, 0x59, 0x65, 0x69, 0x94, 0x95, 0x99, 0xA5, 0xA9, 0xAA, 0xE9, 0xEA },
			{ 0x54, 0x55, 0x59, 0x65, 0x69, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xEA },
			{ 0x55, 0x56, 0x59, 0x65, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xEA },
			{ 0x55, 0x56, 0x5A, 0x66, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xAB, 0xEA, 0xEB },
			{ 0x54, 0x55, 0x59, 0x69, 0x95, 0x99, 0x9A, 0xA5, 0xA9, 0xAA, 0xAD, 0xAE, 0xE9, 0xEA, 0xEE },
			{ 0x54, 0x55, 0x59, 0x69, 0x95, 0x99, 0x9A, 0xA5, 0xA9, 0xAA, 0xAE, 0xEA, 0xEE },
			{ 0x55, 0x59, 0x5A, 0x69, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xAE, 0xEA, 0xEE },
			{ 0x55, 0x56, 0x59, 0x5A, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xA6, 0xA9, 0xAA, 0xAB, 0xAE, 0xEA },
			{ 0x50, 0x51, 0x54, 0x55, 0x65, 0x95, 0xA1, 0xA4, 0xA5, 0xA6, 0xA9, 0xAA, 0xB5, 0xBA, 0xE5, 0xEA, 0xFA },
			{ 0x51, 0x55, 0x65, 0x95, 0xA1, 0xA5, 0xA6, 0xA9, 0xAA, 0xB6, 0xBA, 0xE6, 0xEA, 0xFA },
			{ 0x51, 0x55, 0x65, 0x66, 0x95, 0x96, 0xA5, 0xA6, 0xA9, 0xAA, 0xB6, 0xBA, 0xE6, 0xEA, 0xFA },
			{ 0x51, 0x55, 0x56, 0x65, 0x66, 0x95, 0x96, 0xA5, 0xA6, 0xA7, 0xAA, 0xAB, 0xB6, 0xBA, 0xE6, 0xEA, 0xFB },
			{ 0x54, 0x55, 0x65, 0x95, 0xA4, 0xA5, 0xA6, 0xA9, 0xAA, 0xB9, 0xBA, 0xE9, 0xEA, 0xFA },
			{ 0x55, 0x65, 0x95, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA, 0xEA, 0xFA },
			{ 0x51, 0x55, 0x65, 0x66, 0x95, 0x96, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA, 0xEA, 0xFA },
			{ 0x55, 0x56, 0x65, 0x66, 0x95, 0x96, 0xA5, 0xA6, 0xAA, 0xAB, 0xBA, 0xEA, 0xFB },
			{ 0x54, 0x55, 0x65, 0x69, 0x95, 0x99, 0xA5, 0xA6, 0xA9, 0xAA, 0xB9, 0xBA, 0xE9, 0xEA, 0xFA },
			{ 0x54, 0x55, 0x65, 0x69, 0x95, 0x99, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA, 0xEA, 0xFA },
			{ 0x55, 0x65, 0x66, 0x69, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xBA, 0xEA, 0xFA },
			{ 0x55, 0x56, 0x65, 0x66, 0x6A, 0x95, 0x96, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xAB, 0xBA, 0xEA },
			{ 0x54, 0x55, 0x59, 0x65, 0x69, 0x95, 0x99, 0xA5, 0xA9, 0xAA, 0xAD, 0xAE, 0xB9, 0xBA, 0xE9, 0xEA, 0xFE },
			{ 0x55, 0x59, 0x65, 0x69, 0x95, 0x99, 0xA5, 0xA9, 0xAA, 0xAE, 0xBA, 0xEA, 0xFE },
			{ 0x55, 0x59, 0x65, 0x69, 0x6A, 0x95, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xAE, 0xBA, 0xEA },
			{ 0x55, 0x56, 0x59, 0x5A, 0x65, 0x66, 0x69, 0x6A, 0x95, 0x96, 0x99, 0x9A, 0xA5, 0xA6, 0xA9, 0xAA, 0xAB, 0xAE, 0xBA, 0xEA },
		};
		LatticePoint4D[] latticePoints = new LatticePoint4D[256];
		for (int i = 0; i < 256; i++) {
			int cx = ((i >> 0) & 3) - 1;
			int cy = ((i >> 2) & 3) - 1;
			int cz = ((i >> 4) & 3) - 1;
			int cw = ((i >> 6) & 3) - 1;
			latticePoints[i] = new LatticePoint4D(cx, cy, cz, cw);
		}
		for (int i = 0; i < 256; i++) {
			LOOKUP_4D[i] = new LatticePoint4D[lookup4DPregen[i].length];
			for (int j = 0; j < lookup4DPregen[i].length; j++) {
				LOOKUP_4D[i][j] = latticePoints[lookup4DPregen[i][j]];
			}
		}
	}

	private static class LatticePoint4D {
		int xsv, ysv, zsv, wsv;
		double dx, dy, dz, dw;
		public LatticePoint4D(int xsv, int ysv, int zsv, int wsv) {
			this.xsv = xsv; this.ysv = ysv; this.zsv = zsv; this.wsv = wsv;
			double ssv = (xsv + ysv + zsv + wsv) * -0.138196601125011;
			this.dx = -xsv - ssv;
			this.dy = -ysv - ssv;
			this.dz = -zsv - ssv;
			this.dw = -wsv - ssv;
		}
	}
	
	/*
	 * Gradients
	 */

	static class Grad4 {
		double dx, dy, dz, dw;
		public Grad4(double dx, double dy, double dz,  double dw) {
			this.dx = dx; this.dy = dy; this.dz = dz; this.dw = dw;
		}
	}
	
	private static final double N4 = 0.11127401889945551;
	private static final Grad4[] GRADIENTS_4D;
	static {
		GRADIENTS_4D = new Grad4[PSIZE];
		Grad4[] grad4 = {
			new Grad4(-0.753341017856078,    -0.37968289875261624,  -0.37968289875261624,  -0.37968289875261624),
			new Grad4(-0.7821684431180708,   -0.4321472685365301,   -0.4321472685365301,    0.12128480194602098),
			new Grad4(-0.7821684431180708,   -0.4321472685365301,    0.12128480194602098,  -0.4321472685365301),
			new Grad4(-0.7821684431180708,    0.12128480194602098,  -0.4321472685365301,   -0.4321472685365301),
			new Grad4(-0.8586508742123365,   -0.508629699630796,     0.044802370851755174,  0.044802370851755174),
			new Grad4(-0.8586508742123365,    0.044802370851755174, -0.508629699630796,     0.044802370851755174),
			new Grad4(-0.8586508742123365,    0.044802370851755174,  0.044802370851755174, -0.508629699630796),
			new Grad4(-0.9982828964265062,   -0.03381941603233842,  -0.03381941603233842,  -0.03381941603233842),
			new Grad4(-0.37968289875261624,  -0.753341017856078,    -0.37968289875261624,  -0.37968289875261624),
			new Grad4(-0.4321472685365301,   -0.7821684431180708,   -0.4321472685365301,    0.12128480194602098),
			new Grad4(-0.4321472685365301,   -0.7821684431180708,    0.12128480194602098,  -0.4321472685365301),
			new Grad4( 0.12128480194602098,  -0.7821684431180708,   -0.4321472685365301,   -0.4321472685365301),
			new Grad4(-0.508629699630796,    -0.8586508742123365,    0.044802370851755174,  0.044802370851755174),
			new Grad4( 0.044802370851755174, -0.8586508742123365,   -0.508629699630796,     0.044802370851755174),
			new Grad4( 0.044802370851755174, -0.8586508742123365,    0.044802370851755174, -0.508629699630796),
			new Grad4(-0.03381941603233842,  -0.9982828964265062,   -0.03381941603233842,  -0.03381941603233842),
			new Grad4(-0.37968289875261624,  -0.37968289875261624,  -0.753341017856078,    -0.37968289875261624),
			new Grad4(-0.4321472685365301,   -0.4321472685365301,   -0.7821684431180708,    0.12128480194602098),
			new Grad4(-0.4321472685365301,    0.12128480194602098,  -0.7821684431180708,   -0.4321472685365301),
			new Grad4( 0.12128480194602098,  -0.4321472685365301,   -0.7821684431180708,   -0.4321472685365301),
			new Grad4(-0.508629699630796,     0.044802370851755174, -0.8586508742123365,    0.044802370851755174),
			new Grad4( 0.044802370851755174, -0.508629699630796,    -0.8586508742123365,    0.044802370851755174),
			new Grad4( 0.044802370851755174,  0.044802370851755174, -0.8586508742123365,   -0.508629699630796),
			new Grad4(-0.03381941603233842,  -0.03381941603233842,  -0.9982828964265062,   -0.03381941603233842),
			new Grad4(-0.37968289875261624,  -0.37968289875261624,  -0.37968289875261624,  -0.753341017856078),
			new Grad4(-0.4321472685365301,   -0.4321472685365301,    0.12128480194602098,  -0.7821684431180708),
			new Grad4(-0.4321472685365301,    0.12128480194602098,  -0.4321472685365301,   -0.7821684431180708),
			new Grad4( 0.12128480194602098,  -0.4321472685365301,   -0.4321472685365301,   -0.7821684431180708),
			new Grad4(-0.508629699630796,     0.044802370851755174,  0.044802370851755174, -0.8586508742123365),
			new Grad4( 0.044802370851755174, -0.508629699630796,     0.044802370851755174, -0.8586508742123365),
			new Grad4( 0.044802370851755174,  0.044802370851755174, -0.508629699630796,    -0.8586508742123365),
			new Grad4(-0.03381941603233842,  -0.03381941603233842,  -0.03381941603233842,  -0.9982828964265062),
			new Grad4(-0.6740059517812944,   -0.3239847771997537,   -0.3239847771997537,    0.5794684678643381),
			new Grad4(-0.7504883828755602,   -0.4004672082940195,    0.15296486218853164,   0.5029860367700724),
			new Grad4(-0.7504883828755602,    0.15296486218853164,  -0.4004672082940195,    0.5029860367700724),
			new Grad4(-0.8828161875373585,    0.08164729285680945,   0.08164729285680945,   0.4553054119602712),
			new Grad4(-0.4553054119602712,   -0.08164729285680945,  -0.08164729285680945,   0.8828161875373585),
			new Grad4(-0.5029860367700724,   -0.15296486218853164,   0.4004672082940195,    0.7504883828755602),
			new Grad4(-0.5029860367700724,    0.4004672082940195,   -0.15296486218853164,   0.7504883828755602),
			new Grad4(-0.5794684678643381,    0.3239847771997537,    0.3239847771997537,    0.6740059517812944),
			new Grad4(-0.3239847771997537,   -0.6740059517812944,   -0.3239847771997537,    0.5794684678643381),
			new Grad4(-0.4004672082940195,   -0.7504883828755602,    0.15296486218853164,   0.5029860367700724),
			new Grad4( 0.15296486218853164,  -0.7504883828755602,   -0.4004672082940195,    0.5029860367700724),
			new Grad4( 0.08164729285680945,  -0.8828161875373585,    0.08164729285680945,   0.4553054119602712),
			new Grad4(-0.08164729285680945,  -0.4553054119602712,   -0.08164729285680945,   0.8828161875373585),
			new Grad4(-0.15296486218853164,  -0.5029860367700724,    0.4004672082940195,    0.7504883828755602),
			new Grad4( 0.4004672082940195,   -0.5029860367700724,   -0.15296486218853164,   0.7504883828755602),
			new Grad4( 0.3239847771997537,   -0.5794684678643381,    0.3239847771997537,    0.6740059517812944),
			new Grad4(-0.3239847771997537,   -0.3239847771997537,   -0.6740059517812944,    0.5794684678643381),
			new Grad4(-0.4004672082940195,    0.15296486218853164,  -0.7504883828755602,    0.5029860367700724),
			new Grad4( 0.15296486218853164,  -0.4004672082940195,   -0.7504883828755602,    0.5029860367700724),
			new Grad4( 0.08164729285680945,   0.08164729285680945,  -0.8828161875373585,    0.4553054119602712),
			new Grad4(-0.08164729285680945,  -0.08164729285680945,  -0.4553054119602712,    0.8828161875373585),
			new Grad4(-0.15296486218853164,   0.4004672082940195,   -0.5029860367700724,    0.7504883828755602),
			new Grad4( 0.4004672082940195,   -0.15296486218853164,  -0.5029860367700724,    0.7504883828755602),
			new Grad4( 0.3239847771997537,    0.3239847771997537,   -0.5794684678643381,    0.6740059517812944),
			new Grad4(-0.6740059517812944,   -0.3239847771997537,    0.5794684678643381,   -0.3239847771997537),
			new Grad4(-0.7504883828755602,   -0.4004672082940195,    0.5029860367700724,    0.15296486218853164),
			new Grad4(-0.7504883828755602,    0.15296486218853164,   0.5029860367700724,   -0.4004672082940195),
			new Grad4(-0.8828161875373585,    0.08164729285680945,   0.4553054119602712,    0.08164729285680945),
			new Grad4(-0.4553054119602712,   -0.08164729285680945,   0.8828161875373585,   -0.08164729285680945),
			new Grad4(-0.5029860367700724,   -0.15296486218853164,   0.7504883828755602,    0.4004672082940195),
			new Grad4(-0.5029860367700724,    0.4004672082940195,    0.7504883828755602,   -0.15296486218853164),
			new Grad4(-0.5794684678643381,    0.3239847771997537,    0.6740059517812944,    0.3239847771997537),
			new Grad4(-0.3239847771997537,   -0.6740059517812944,    0.5794684678643381,   -0.3239847771997537),
			new Grad4(-0.4004672082940195,   -0.7504883828755602,    0.5029860367700724,    0.15296486218853164),
			new Grad4( 0.15296486218853164,  -0.7504883828755602,    0.5029860367700724,   -0.4004672082940195),
			new Grad4( 0.08164729285680945,  -0.8828161875373585,    0.4553054119602712,    0.08164729285680945),
			new Grad4(-0.08164729285680945,  -0.4553054119602712,    0.8828161875373585,   -0.08164729285680945),
			new Grad4(-0.15296486218853164,  -0.5029860367700724,    0.7504883828755602,    0.4004672082940195),
			new Grad4( 0.4004672082940195,   -0.5029860367700724,    0.7504883828755602,   -0.15296486218853164),
			new Grad4( 0.3239847771997537,   -0.5794684678643381,    0.6740059517812944,    0.3239847771997537),
			new Grad4(-0.3239847771997537,   -0.3239847771997537,    0.5794684678643381,   -0.6740059517812944),
			new Grad4(-0.4004672082940195,    0.15296486218853164,   0.5029860367700724,   -0.7504883828755602),
			new Grad4( 0.15296486218853164,  -0.4004672082940195,    0.5029860367700724,   -0.7504883828755602),
			new Grad4( 0.08164729285680945,   0.08164729285680945,   0.4553054119602712,   -0.8828161875373585),
			new Grad4(-0.08164729285680945,  -0.08164729285680945,   0.8828161875373585,   -0.4553054119602712),
			new Grad4(-0.15296486218853164,   0.4004672082940195,    0.7504883828755602,   -0.5029860367700724),
			new Grad4( 0.4004672082940195,   -0.15296486218853164,   0.7504883828755602,   -0.5029860367700724),
			new Grad4( 0.3239847771997537,    0.3239847771997537,    0.6740059517812944,   -0.5794684678643381),
			new Grad4(-0.6740059517812944,    0.5794684678643381,   -0.3239847771997537,   -0.3239847771997537),
			new Grad4(-0.7504883828755602,    0.5029860367700724,   -0.4004672082940195,    0.15296486218853164),
			new Grad4(-0.7504883828755602,    0.5029860367700724,    0.15296486218853164,  -0.4004672082940195),
			new Grad4(-0.8828161875373585,    0.4553054119602712,    0.08164729285680945,   0.08164729285680945),
			new Grad4(-0.4553054119602712,    0.8828161875373585,   -0.08164729285680945,  -0.08164729285680945),
			new Grad4(-0.5029860367700724,    0.7504883828755602,   -0.15296486218853164,   0.4004672082940195),
			new Grad4(-0.5029860367700724,    0.7504883828755602,    0.4004672082940195,   -0.15296486218853164),
			new Grad4(-0.5794684678643381,    0.6740059517812944,    0.3239847771997537,    0.3239847771997537),
			new Grad4(-0.3239847771997537,    0.5794684678643381,   -0.6740059517812944,   -0.3239847771997537),
			new Grad4(-0.4004672082940195,    0.5029860367700724,   -0.7504883828755602,    0.15296486218853164),
			new Grad4( 0.15296486218853164,   0.5029860367700724,   -0.7504883828755602,   -0.4004672082940195),
			new Grad4( 0.08164729285680945,   0.4553054119602712,   -0.8828161875373585,    0.08164729285680945),
			new Grad4(-0.08164729285680945,   0.8828161875373585,   -0.4553054119602712,   -0.08164729285680945),
			new Grad4(-0.15296486218853164,   0.7504883828755602,   -0.5029860367700724,    0.4004672082940195),
			new Grad4( 0.4004672082940195,    0.7504883828755602,   -0.5029860367700724,   -0.15296486218853164),
			new Grad4( 0.3239847771997537,    0.6740059517812944,   -0.5794684678643381,    0.3239847771997537),
			new Grad4(-0.3239847771997537,    0.5794684678643381,   -0.3239847771997537,   -0.6740059517812944),
			new Grad4(-0.4004672082940195,    0.5029860367700724,    0.15296486218853164,  -0.7504883828755602),
			new Grad4( 0.15296486218853164,   0.5029860367700724,   -0.4004672082940195,   -0.7504883828755602),
			new Grad4( 0.08164729285680945,   0.4553054119602712,    0.08164729285680945,  -0.8828161875373585),
			new Grad4(-0.08164729285680945,   0.8828161875373585,   -0.08164729285680945,  -0.4553054119602712),
			new Grad4(-0.15296486218853164,   0.7504883828755602,    0.4004672082940195,   -0.5029860367700724),
			new Grad4( 0.4004672082940195,    0.7504883828755602,   -0.15296486218853164,  -0.5029860367700724),
			new Grad4( 0.3239847771997537,    0.6740059517812944,    0.3239847771997537,   -0.5794684678643381),
			new Grad4( 0.5794684678643381,   -0.6740059517812944,   -0.3239847771997537,   -0.3239847771997537),
			new Grad4( 0.5029860367700724,   -0.7504883828755602,   -0.4004672082940195,    0.15296486218853164),
			new Grad4( 0.5029860367700724,   -0.7504883828755602,    0.15296486218853164,  -0.4004672082940195),
			new Grad4( 0.4553054119602712,   -0.8828161875373585,    0.08164729285680945,   0.08164729285680945),
			new Grad4( 0.8828161875373585,   -0.4553054119602712,   -0.08164729285680945,  -0.08164729285680945),
			new Grad4( 0.7504883828755602,   -0.5029860367700724,   -0.15296486218853164,   0.4004672082940195),
			new Grad4( 0.7504883828755602,   -0.5029860367700724,    0.4004672082940195,   -0.15296486218853164),
			new Grad4( 0.6740059517812944,   -0.5794684678643381,    0.3239847771997537,    0.3239847771997537),
			new Grad4( 0.5794684678643381,   -0.3239847771997537,   -0.6740059517812944,   -0.3239847771997537),
			new Grad4( 0.5029860367700724,   -0.4004672082940195,   -0.7504883828755602,    0.15296486218853164),
			new Grad4( 0.5029860367700724,    0.15296486218853164,  -0.7504883828755602,   -0.4004672082940195),
			new Grad4( 0.4553054119602712,    0.08164729285680945,  -0.8828161875373585,    0.08164729285680945),
			new Grad4( 0.8828161875373585,   -0.08164729285680945,  -0.4553054119602712,   -0.08164729285680945),
			new Grad4( 0.7504883828755602,   -0.15296486218853164,  -0.5029860367700724,    0.4004672082940195),
			new Grad4( 0.7504883828755602,    0.4004672082940195,   -0.5029860367700724,   -0.15296486218853164),
			new Grad4( 0.6740059517812944,    0.3239847771997537,   -0.5794684678643381,    0.3239847771997537),
			new Grad4( 0.5794684678643381,   -0.3239847771997537,   -0.3239847771997537,   -0.6740059517812944),
			new Grad4( 0.5029860367700724,   -0.4004672082940195,    0.15296486218853164,  -0.7504883828755602),
			new Grad4( 0.5029860367700724,    0.15296486218853164,  -0.4004672082940195,   -0.7504883828755602),
			new Grad4( 0.4553054119602712,    0.08164729285680945,   0.08164729285680945,  -0.8828161875373585),
			new Grad4( 0.8828161875373585,   -0.08164729285680945,  -0.08164729285680945,  -0.4553054119602712),
			new Grad4( 0.7504883828755602,   -0.15296486218853164,   0.4004672082940195,   -0.5029860367700724),
			new Grad4( 0.7504883828755602,    0.4004672082940195,   -0.15296486218853164,  -0.5029860367700724),
			new Grad4( 0.6740059517812944,    0.3239847771997537,    0.3239847771997537,   -0.5794684678643381),
			new Grad4( 0.03381941603233842,   0.03381941603233842,   0.03381941603233842,   0.9982828964265062),
			new Grad4(-0.044802370851755174, -0.044802370851755174,  0.508629699630796,     0.8586508742123365),
			new Grad4(-0.044802370851755174,  0.508629699630796,    -0.044802370851755174,  0.8586508742123365),
			new Grad4(-0.12128480194602098,   0.4321472685365301,    0.4321472685365301,    0.7821684431180708),
			new Grad4( 0.508629699630796,    -0.044802370851755174, -0.044802370851755174,  0.8586508742123365),
			new Grad4( 0.4321472685365301,   -0.12128480194602098,   0.4321472685365301,    0.7821684431180708),
			new Grad4( 0.4321472685365301,    0.4321472685365301,   -0.12128480194602098,   0.7821684431180708),
			new Grad4( 0.37968289875261624,   0.37968289875261624,   0.37968289875261624,   0.753341017856078),
			new Grad4( 0.03381941603233842,   0.03381941603233842,   0.9982828964265062,    0.03381941603233842),
			new Grad4(-0.044802370851755174,  0.044802370851755174,  0.8586508742123365,    0.508629699630796),
			new Grad4(-0.044802370851755174,  0.508629699630796,     0.8586508742123365,   -0.044802370851755174),
			new Grad4(-0.12128480194602098,   0.4321472685365301,    0.7821684431180708,    0.4321472685365301),
			new Grad4( 0.508629699630796,    -0.044802370851755174,  0.8586508742123365,   -0.044802370851755174),
			new Grad4( 0.4321472685365301,   -0.12128480194602098,   0.7821684431180708,    0.4321472685365301),
			new Grad4( 0.4321472685365301,    0.4321472685365301,    0.7821684431180708,   -0.12128480194602098),
			new Grad4( 0.37968289875261624,   0.37968289875261624,   0.753341017856078,     0.37968289875261624),
			new Grad4( 0.03381941603233842,   0.9982828964265062,    0.03381941603233842,   0.03381941603233842),
			new Grad4(-0.044802370851755174,  0.8586508742123365,   -0.044802370851755174,  0.508629699630796),
			new Grad4(-0.044802370851755174,  0.8586508742123365,    0.508629699630796,    -0.044802370851755174),
			new Grad4(-0.12128480194602098,   0.7821684431180708,    0.4321472685365301,    0.4321472685365301),
			new Grad4( 0.508629699630796,     0.8586508742123365,   -0.044802370851755174, -0.044802370851755174),
			new Grad4( 0.4321472685365301,    0.7821684431180708,   -0.12128480194602098,   0.4321472685365301),
			new Grad4( 0.4321472685365301,    0.7821684431180708,    0.4321472685365301,   -0.12128480194602098),
			new Grad4( 0.37968289875261624,   0.753341017856078,     0.37968289875261624,   0.37968289875261624),
			new Grad4( 0.9982828964265062,    0.03381941603233842,   0.03381941603233842,   0.03381941603233842),
			new Grad4( 0.8586508742123365,   -0.044802370851755174, -0.044802370851755174,  0.508629699630796),
			new Grad4( 0.8586508742123365,   -0.044802370851755174,  0.508629699630796,    -0.044802370851755174),
			new Grad4( 0.7821684431180708,   -0.12128480194602098,   0.4321472685365301,    0.4321472685365301),
			new Grad4( 0.8586508742123365,    0.508629699630796,    -0.044802370851755174, -0.044802370851755174),
			new Grad4( 0.7821684431180708,    0.4321472685365301,   -0.12128480194602098,   0.4321472685365301),
			new Grad4( 0.7821684431180708,    0.4321472685365301,    0.4321472685365301,   -0.12128480194602098),
			new Grad4( 0.753341017856078,     0.37968289875261624,   0.37968289875261624,   0.37968289875261624)
		};
		for (int i = 0; i < grad4.length; i++) {
			grad4[i].dx /= N4; grad4[i].dy /= N4; grad4[i].dz /= N4; grad4[i].dw /= N4;
		}
		for (int i = 0; i < PSIZE; i++) {
			GRADIENTS_4D[i] = grad4[i % grad4.length];
		}
	}
}
