package com.drillgon200.shooter.util;

public class Quat4f {

	//From both javax vecmath's Quat4f and lwjgl 2's quaternion

	final static double EPS = 0.000001;
	
	public float x, y, z, w;

	public Quat4f() {
		x = 0;
		y = 0;
		z = 0;
		w = 1;
	}

	public Quat4f(Quat4f q) {
		this.x = q.x;
		this.y = q.y;
		this.z = q.z;
		this.w = q.w;
	}

	public Quat4f(float x, float y, float z, float w) {
		float mag = (float) (1.0 / Math.sqrt(x * x + y * y + z * z + w * w));
		this.x = x * mag;
		this.y = y * mag;
		this.z = z * mag;
		this.w = w * mag;
	}

	/**
	 * Sets the value of this quaternion to the conjugate of quaternion q1.
	 * 
	 * @param q1
	 *            the source vector
	 */
	public final void conjugate(Quat4f q1) {
		this.x = -q1.x;
		this.y = -q1.y;
		this.z = -q1.z;
		this.w = q1.w;
	}

	/**
	 * Sets the value of this quaternion to the conjugate of itself.
	 */
	public final void conjugate() {
		this.x = -this.x;
		this.y = -this.y;
		this.z = -this.z;
	}

	/**
	 * Sets the value of this quaternion to the quaternion product of
	 * quaternions q1 and q2 (this = q1 * q2). Note that this is safe for
	 * aliasing (e.g. this can be q1 or q2).
	 * 
	 * @param q1
	 *            the first quaternion
	 * @param q2
	 *            the second quaternion
	 */
	public final void mul(Quat4f q1, Quat4f q2) {
		if(this != q1 && this != q2) {
			this.w = q1.w * q2.w - q1.x * q2.x - q1.y * q2.y - q1.z * q2.z;
			this.x = q1.w * q2.x + q2.w * q1.x + q1.y * q2.z - q1.z * q2.y;
			this.y = q1.w * q2.y + q2.w * q1.y - q1.x * q2.z + q1.z * q2.x;
			this.z = q1.w * q2.z + q2.w * q1.z + q1.x * q2.y - q1.y * q2.x;
		} else {
			float x, y, w;

			w = q1.w * q2.w - q1.x * q2.x - q1.y * q2.y - q1.z * q2.z;
			x = q1.w * q2.x + q2.w * q1.x + q1.y * q2.z - q1.z * q2.y;
			y = q1.w * q2.y + q2.w * q1.y - q1.x * q2.z + q1.z * q2.x;
			this.z = q1.w * q2.z + q2.w * q1.z + q1.x * q2.y - q1.y * q2.x;
			this.w = w;
			this.x = x;
			this.y = y;
		}
	}

	/**
	 * Sets the value of this quaternion to the quaternion product of itself and
	 * q1 (this = this * q1).
	 * 
	 * @param q1
	 *            the other quaternion
	 */
	public final void mul(Quat4f q1) {
		float x, y, w;

		w = this.w * q1.w - this.x * q1.x - this.y * q1.y - this.z * q1.z;
		x = this.w * q1.x + q1.w * this.x + this.y * q1.z - this.z * q1.y;
		y = this.w * q1.y + q1.w * this.y - this.x * q1.z + this.z * q1.x;
		this.z = this.w * q1.z + q1.w * this.z + this.x * q1.y - this.y * q1.x;
		this.w = w;
		this.x = x;
		this.y = y;
	}

	/**
	 * Multiplies quaternion q1 by the inverse of quaternion q2 and places the
	 * value into this quaternion. The value of both argument quaternions is
	 * preservered (this = q1 * q2^-1).
	 * 
	 * @param q1
	 *            the first quaternion
	 * @param q2
	 *            the second quaternion
	 */
	public final void mulInverse(Quat4f q1, Quat4f q2) {
		Quat4f tempQuat = new Quat4f(q2);

		tempQuat.inverse();
		this.mul(q1, tempQuat);
	}

	/**
	 * Multiplies this quaternion by the inverse of quaternion q1 and places the
	 * value into this quaternion. The value of the argument quaternion is
	 * preserved (this = this * q^-1).
	 * 
	 * @param q1
	 *            the other quaternion
	 */
	public final void mulInverse(Quat4f q1) {
		Quat4f tempQuat = new Quat4f(q1);

		tempQuat.inverse();
		this.mul(tempQuat);
	}

	/**
	 * Sets the value of this quaternion to quaternion inverse of quaternion q1.
	 * 
	 * @param q1
	 *            the quaternion to be inverted
	 */
	public final void inverse(Quat4f q1) {
		float norm;

		norm = 1.0f / (q1.w * q1.w + q1.x * q1.x + q1.y * q1.y + q1.z * q1.z);
		this.w = norm * q1.w;
		this.x = -norm * q1.x;
		this.y = -norm * q1.y;
		this.z = -norm * q1.z;
	}

	/**
	 * Sets the value of this quaternion to the quaternion inverse of itself.
	 */
	public final void inverse() {
		float norm;

		norm = 1.0f / (this.w * this.w + this.x * this.x + this.y * this.y + this.z * this.z);
		this.w *= norm;
		this.x *= -norm;
		this.y *= -norm;
		this.z *= -norm;
	}

	/**
	 * Sets the value of this quaternion to the normalized value of quaternion
	 * q1.
	 * 
	 * @param q1
	 *            the quaternion to be normalized.
	 */
	public final void normalize(Quat4f q1) {
		float norm;

		norm = (q1.x * q1.x + q1.y * q1.y + q1.z * q1.z + q1.w * q1.w);

		if(norm > 0.0f) {
			norm = 1.0f / (float) Math.sqrt(norm);
			this.x = norm * q1.x;
			this.y = norm * q1.y;
			this.z = norm * q1.z;
			this.w = norm * q1.w;
		} else {
			this.x = (float) 0.0;
			this.y = (float) 0.0;
			this.z = (float) 0.0;
			this.w = (float) 0.0;
		}
	}

	/**
	 * Normalizes the value of this quaternion in place.
	 */
	public final void normalize() {
		float norm;

		norm = (this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w);

		if(norm > 0.0f) {
			norm = 1.0f / (float) Math.sqrt(norm);
			this.x *= norm;
			this.y *= norm;
			this.z *= norm;
			this.w *= norm;
		} else {
			this.x = (float) 0.0;
			this.y = (float) 0.0;
			this.z = (float) 0.0;
			this.w = (float) 0.0;
		}
	}

	public void setFromMat(Matrix3f mat) {
		setFromMat(this, mat);
	}

	public static void setFromMat(Quat4f q, Matrix3f mat) {
		setFromMat(q, mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
	}

	public static void setFromMat(Quat4f q, float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22) {

		float s;
		float tr = m00 + m11 + m22;
		if(tr >= 0.0) {
			s = (float) Math.sqrt(tr + 1.0);
			q.w = s * 0.5f;
			s = 0.5f / s;
			q.x = (m21 - m12) * s;
			q.y = (m02 - m20) * s;
			q.z = (m10 - m01) * s;
		} else {
			float max = Math.max(Math.max(m00, m11), m22);
			if(max == m00) {
				s = (float) Math.sqrt(m00 - (m11 + m22) + 1.0);
				q.x = s * 0.5f;
				s = 0.5f / s;
				q.y = (m01 + m10) * s;
				q.z = (m20 + m02) * s;
				q.w = (m21 - m12) * s;
			} else if(max == m11) {
				s = (float) Math.sqrt(m11 - (m22 + m00) + 1.0);
				q.y = s * 0.5f;
				s = 0.5f / s;
				q.z = (m12 + m21) * s;
				q.x = (m01 + m10) * s;
				q.w = (m02 - m20) * s;
			} else {
				s = (float) Math.sqrt(m22 - (m00 + m11) + 1.0);
				q.z = s * 0.5f;
				s = 0.5f / s;
				q.x = (m20 + m02) * s;
				q.y = (m12 + m21) * s;
				q.w = (m10 - m01) * s;
			}
		}
	}

	/**
	 * Performs a great circle interpolation between this quaternion and the
	 * quaternion parameter and places the result into this quaternion.
	 * 
	 * @param q1
	 *            the other quaternion
	 * @param alpha
	 *            the alpha interpolation parameter
	 */
	public final void interpolate(Quat4f q1, float alpha) {
		// From "Advanced Animation and Rendering Techniques"
		// by Watt and Watt pg. 364, function as implemented appeared to be 
		// incorrect.  Fails to choose the same quaternion for the double
		// covering. Resulting in change of direction for rotations.
		// Fixed function to negate the first quaternion in the case that the
		// dot product of q1 and this is negative. Second case was not needed. 

		double dot, s1, s2, om, sinom;

		dot = x * q1.x + y * q1.y + z * q1.z + w * q1.w;

		if(dot < 0) {
			// negate quaternion
			q1.x = -q1.x;
			q1.y = -q1.y;
			q1.z = -q1.z;
			q1.w = -q1.w;
			dot = -dot;
		}

		if((1.0 - dot) > EPS) {
			om = Math.acos(dot);
			sinom = Math.sin(om);
			s1 = Math.sin((1.0 - alpha) * om) / sinom;
			s2 = Math.sin(alpha * om) / sinom;
		} else {
			s1 = 1.0 - alpha;
			s2 = alpha;
		}

		w = (float) (s1 * w + s2 * q1.w);
		x = (float) (s1 * x + s2 * q1.x);
		y = (float) (s1 * y + s2 * q1.y);
		z = (float) (s1 * z + s2 * q1.z);
	}

	public Matrix3f matrixFromQuat() {
		return matrixFromQuat(new Matrix3f());
	}

	public Matrix3f matrixFromQuat(Matrix3f m) {
		m.m00 = 1 - 2 * y * y - 2 * z * z;
		m.m01 = 2 * x * y - 2 * z * w;
		m.m02 = 2 * x * z + 2 * y * w;

		m.m10 = 2 * x * y + 2 * z * w;
		m.m11 = 1 - 2 * x * x - 2 * z * z;
		m.m12 = 2 * y * z - 2 * x * w;

		m.m20 = 2 * x * z - 2 * y * w;
		m.m21 = 2 * y * z + 2 * x * w;
		m.m22 = 1 - 2 * x * x - 2 * y * y;

		return m;
	}
}
