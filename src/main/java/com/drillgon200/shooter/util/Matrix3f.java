package com.drillgon200.shooter.util;

import java.nio.FloatBuffer;

public class Matrix3f implements Cloneable {
	//Again, LWJGL 3 doesn't have a util package so this is mostly copied from LWJGL 2.

	public float m00, m01, m02, m10, m11, m12, m20, m21, m22;

	private static final double EPS = 1.0E-8;

	public Matrix3f() {
		setIdentity();
	}

	public Matrix3f(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22) {
		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;

		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;

		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;
	}
	
	/**
     * Retrieves the value at the specified row and column of this
     * matrix.
     * @param row the row number to be retrieved (zero indexed)
     * @param column the column number to be retrieved (zero indexed)
     * @return the value at the indexed element.
     */
    public final float getElement(int row, int column)
    {
	switch (row) 
	  {
	  case 0:
	    switch(column)
	      {
	      case 0:
		return(this.m00);
	      case 1:
		return(this.m01);
	      case 2:
		return(this.m02);
	      default:
                break;
	      }
	    break;
	  case 1:
	    switch(column) 
	      {
	      case 0:
		return(this.m10);
	      case 1:
		return(this.m11);
	      case 2:
		return(this.m12);
	      default:
                break;
	      }
	    break;
	  
	  case 2:
	    switch(column) 
	      {
	      case 0:
		return(this.m20);
	      case 1:
		return(this.m21);
	      case 2:
		return(this.m22);
	      default:
                break;
	      }
	    break;
	    
	  default:
            break;
	  }
       throw new ArrayIndexOutOfBoundsException("Bad row or column index");
    }

	/**
	 * Set this matrix to be the identity matrix.
	 * 
	 * @return this
	 */
	public Matrix3f setIdentity() {
		return setIdentity(this);
	}

	/**
	 * Set the matrix to be the identity matrix.
	 * 
	 * @param m
	 *            The matrix to be set to the identity
	 * @return m
	 */
	public static Matrix3f setIdentity(Matrix3f m) {
		m.m00 = 1.0f;
		m.m01 = 0.0f;
		m.m02 = 0.0f;
		m.m10 = 0.0f;
		m.m11 = 1.0f;
		m.m12 = 0.0f;
		m.m20 = 0.0f;
		m.m21 = 0.0f;
		m.m22 = 1.0f;
		return m;
	}

	/**
	 * Set this matrix to 0.
	 * 
	 * @return this
	 */
	public Matrix3f setZero() {
		return setZero(this);
	}

	/**
	 * Set the matrix matrix to 0.
	 * 
	 * @param m
	 *            The matrix to be set to 0
	 * @return m
	 */
	public static Matrix3f setZero(Matrix3f m) {
		m.m00 = 0.0f;
		m.m01 = 0.0f;
		m.m02 = 0.0f;
		m.m10 = 0.0f;
		m.m11 = 0.0f;
		m.m12 = 0.0f;
		m.m20 = 0.0f;
		m.m21 = 0.0f;
		m.m22 = 0.0f;
		return m;
	}

	/**
	 * Sets the value of this matrix to the matrix conversion of the (single
	 * precision) axis and angle argument.
	 * 
	 * @param a1
	 *            the axis and angle to be converted
	 */
	public final void set(AxisAngle4f a1) {
		float mag = (float) Math.sqrt(a1.x * a1.x + a1.y * a1.y + a1.z * a1.z);
		if(mag < EPS) {
			m00 = 1.0f;
			m01 = 0.0f;
			m02 = 0.0f;

			m10 = 0.0f;
			m11 = 1.0f;
			m12 = 0.0f;

			m20 = 0.0f;
			m21 = 0.0f;
			m22 = 1.0f;
		} else {
			mag = 1.0f / mag;
			float ax = a1.x * mag;
			float ay = a1.y * mag;
			float az = a1.z * mag;

			float sinTheta = (float) Math.sin((float) a1.angle);
			float cosTheta = (float) Math.cos((float) a1.angle);
			float t = (float) 1.0 - cosTheta;

			float xz = ax * az;
			float xy = ax * ay;
			float yz = ay * az;

			m00 = t * ax * ax + cosTheta;
			m01 = t * xy - sinTheta * az;
			m02 = t * xz + sinTheta * ay;

			m10 = t * xy + sinTheta * az;
			m11 = t * ay * ay + cosTheta;
			m12 = t * yz - sinTheta * ax;

			m20 = t * xz - sinTheta * ay;
			m21 = t * yz + sinTheta * ax;
			m22 = t * az * az + cosTheta;
		}

	}

	/**
	 * Sets the value of this matrix to the value of the Matrix3f argument.
	 * 
	 * @param m1
	 *            the source matrix3f
	 */
	public final Matrix3f set(Matrix3f m1) {

		this.m00 = m1.m00;
		this.m01 = m1.m01;
		this.m02 = m1.m02;

		this.m10 = m1.m10;
		this.m11 = m1.m11;
		this.m12 = m1.m12;

		this.m20 = m1.m20;
		this.m21 = m1.m21;
		this.m22 = m1.m22;

		return this;
	}

	/**
	 * Sets the value of this matrix to the matrix sum of matrices m1 and m2.
	 * 
	 * @param m1
	 *            the first matrix
	 * @param m2
	 *            the second matrix
	 */
	public final void add(Matrix3f m1, Matrix3f m2) {
		this.m00 = m1.m00 + m2.m00;
		this.m01 = m1.m01 + m2.m01;
		this.m02 = m1.m02 + m2.m02;

		this.m10 = m1.m10 + m2.m10;
		this.m11 = m1.m11 + m2.m11;
		this.m12 = m1.m12 + m2.m12;

		this.m20 = m1.m20 + m2.m20;
		this.m21 = m1.m21 + m2.m21;
		this.m22 = m1.m22 + m2.m22;
	}

	/**
	 * Sets the value of this matrix to the matrix sum of itself and matrix m1.
	 * 
	 * @param m1
	 *            the other matrix
	 */
	public final void add(Matrix3f m1) {
		this.m00 += m1.m00;
		this.m01 += m1.m01;
		this.m02 += m1.m02;

		this.m10 += m1.m10;
		this.m11 += m1.m11;
		this.m12 += m1.m12;

		this.m20 += m1.m20;
		this.m21 += m1.m21;
		this.m22 += m1.m22;
	}

	/**
	 * Sets the value of this matrix to the matrix difference of matrices m1 and
	 * m2.
	 * 
	 * @param m1
	 *            the first matrix
	 * @param m2
	 *            the second matrix
	 */
	public final void sub(Matrix3f m1, Matrix3f m2) {
		this.m00 = m1.m00 - m2.m00;
		this.m01 = m1.m01 - m2.m01;
		this.m02 = m1.m02 - m2.m02;

		this.m10 = m1.m10 - m2.m10;
		this.m11 = m1.m11 - m2.m11;
		this.m12 = m1.m12 - m2.m12;

		this.m20 = m1.m20 - m2.m20;
		this.m21 = m1.m21 - m2.m21;
		this.m22 = m1.m22 - m2.m22;
	}

	/**
	 * Sets the value of this matrix to the matrix difference of itself and
	 * matrix m1 (this = this - m1).
	 * 
	 * @param m1
	 *            the other matrix
	 */
	public final void sub(Matrix3f m1) {
		this.m00 -= m1.m00;
		this.m01 -= m1.m01;
		this.m02 -= m1.m02;

		this.m10 -= m1.m10;
		this.m11 -= m1.m11;
		this.m12 -= m1.m12;

		this.m20 -= m1.m20;
		this.m21 -= m1.m21;
		this.m22 -= m1.m22;
	}

	/**
	 * Transform a Vector by a matrix and return the result in a destination
	 * vector.
	 * 
	 * @param left
	 *            The left matrix
	 * @param right
	 *            The right vector
	 * @param dest
	 *            The destination vector, or null if a new one is to be created
	 * @return the destination vector
	 */
	public static Vec3f transform(Matrix3f left, Vec3f vec) {
		float x = left.m00 * vec.x + left.m10 * vec.y + left.m20 * vec.z;
		float y = left.m01 * vec.x + left.m11 * vec.y + left.m21 * vec.z;
		float z = left.m02 * vec.x + left.m12 * vec.y + left.m22 * vec.z;

		return new Vec3f(x, y, z);
	}

	/**
	 * Transpose this matrix
	 * 
	 * @return this
	 */
	public Matrix3f transpose() {
		return transpose(this, this);
	}

	/**
	 * Transpose this matrix and place the result in another matrix
	 * 
	 * @param dest
	 *            The destination matrix or null if a new matrix is to be
	 *            created
	 * @return the transposed matrix
	 */
	public Matrix3f transpose(Matrix3f dest) {
		return transpose(this, dest);
	}

	/**
	 * Transpose the source matrix and place the result into the destination
	 * matrix
	 * 
	 * @param src
	 *            The source matrix to be transposed
	 * @param dest
	 *            The destination matrix or null if a new matrix is to be
	 *            created
	 * @return the transposed matrix
	 */
	public static Matrix3f transpose(Matrix3f src, Matrix3f dest) {
		if(dest == null)
			dest = new Matrix3f();
		float m00 = src.m00;
		float m01 = src.m10;
		float m02 = src.m20;
		float m10 = src.m01;
		float m11 = src.m11;
		float m12 = src.m21;
		float m20 = src.m02;
		float m21 = src.m12;
		float m22 = src.m22;

		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;
		return dest;
	}

	/**
	 * @return the determinant of the matrix
	 */
	public float determinant() {
		float f = m00 * (m11 * m22 - m12 * m21) + m01 * (m12 * m20 - m10 * m22) + m02 * (m10 * m21 - m11 * m20);
		return f;
	}

	/**
	 * Returns a string representation of this matrix
	 */
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(m00).append(' ').append(m10).append(' ').append(m20).append(' ').append('\n');
		buf.append(m01).append(' ').append(m11).append(' ').append(m21).append(' ').append('\n');
		buf.append(m02).append(' ').append(m12).append(' ').append(m22).append(' ').append('\n');
		return buf.toString();
	}

	/**
	 * Invert this matrix
	 * 
	 * @return this if successful, null otherwise
	 */
	public Matrix3f invert() {
		return invert(this, this);
	}

	/**
	 * Invert the source matrix and put the result into the destination matrix
	 * 
	 * @param src
	 *            The source matrix to be inverted
	 * @param dest
	 *            The destination matrix, or null if a new one is to be created
	 * @return The inverted matrix if successful, null otherwise
	 */
	public static Matrix3f invert(Matrix3f src, Matrix3f dest) {
		float determinant = src.determinant();

		if(determinant != 0) {
			if(dest == null)
				dest = new Matrix3f();
			/* do it the ordinary way
			 *
			 * inv(A) = 1/det(A) * adj(T), where adj(T) = transpose(Conjugate Matrix)
			 *
			 * m00 m01 m02
			 * m10 m11 m12
			 * m20 m21 m22
			 */
			float determinant_inv = 1f / determinant;

			// get the conjugate matrix
			float t00 = src.m11 * src.m22 - src.m12 * src.m21;
			float t01 = -src.m10 * src.m22 + src.m12 * src.m20;
			float t02 = src.m10 * src.m21 - src.m11 * src.m20;
			float t10 = -src.m01 * src.m22 + src.m02 * src.m21;
			float t11 = src.m00 * src.m22 - src.m02 * src.m20;
			float t12 = -src.m00 * src.m21 + src.m01 * src.m20;
			float t20 = src.m01 * src.m12 - src.m02 * src.m11;
			float t21 = -src.m00 * src.m12 + src.m02 * src.m10;
			float t22 = src.m00 * src.m11 - src.m01 * src.m10;

			dest.m00 = t00 * determinant_inv;
			dest.m11 = t11 * determinant_inv;
			dest.m22 = t22 * determinant_inv;
			dest.m01 = t10 * determinant_inv;
			dest.m10 = t01 * determinant_inv;
			dest.m20 = t02 * determinant_inv;
			dest.m02 = t20 * determinant_inv;
			dest.m12 = t21 * determinant_inv;
			dest.m21 = t12 * determinant_inv;
			return dest;
		} else
			return null;
	}

	/**
	 * Multiplies each element of this matrix by a scalar.
	 * 
	 * @param scalar
	 *            the scalar multiplier
	 */
	public final void mul(float scalar) {
		m00 *= scalar;
		m01 *= scalar;
		m02 *= scalar;

		m10 *= scalar;
		m11 *= scalar;
		m12 *= scalar;

		m20 *= scalar;
		m21 *= scalar;
		m22 *= scalar;
	}

	/**
	 * Sets the value of this matrix to the result of multiplying itself with
	 * matrix m1.
	 * 
	 * @param m1
	 *            the other matrix
	 */
	public final void mul(Matrix3f m1) {
		float m00, m01, m02, m10, m11, m12, m20, m21, m22;

		m00 = this.m00 * m1.m00 + this.m01 * m1.m10 + this.m02 * m1.m20;
		m01 = this.m00 * m1.m01 + this.m01 * m1.m11 + this.m02 * m1.m21;
		m02 = this.m00 * m1.m02 + this.m01 * m1.m12 + this.m02 * m1.m22;

		m10 = this.m10 * m1.m00 + this.m11 * m1.m10 + this.m12 * m1.m20;
		m11 = this.m10 * m1.m01 + this.m11 * m1.m11 + this.m12 * m1.m21;
		m12 = this.m10 * m1.m02 + this.m11 * m1.m12 + this.m12 * m1.m22;

		m20 = this.m20 * m1.m00 + this.m21 * m1.m10 + this.m22 * m1.m20;
		m21 = this.m20 * m1.m01 + this.m21 * m1.m11 + this.m22 * m1.m21;
		m22 = this.m20 * m1.m02 + this.m21 * m1.m12 + this.m22 * m1.m22;

		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;
		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;
		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;
	}

	/**
	 * Multiply the right matrix by the left and place the result in a third
	 * matrix.
	 * 
	 * @param left
	 *            The left source matrix
	 * @param right
	 *            The right source matrix
	 * @param dest
	 *            The destination matrix, or null if a new one is to be created
	 * @return the destination matrix
	 */
	public static Matrix3f mul(Matrix3f left, Matrix3f right, Matrix3f dest) {
		if(dest == null)
			dest = new Matrix3f();

		float m00 = left.m00 * right.m00 + left.m10 * right.m01 + left.m20 * right.m02;
		float m01 = left.m01 * right.m00 + left.m11 * right.m01 + left.m21 * right.m02;
		float m02 = left.m02 * right.m00 + left.m12 * right.m01 + left.m22 * right.m02;
		float m10 = left.m00 * right.m10 + left.m10 * right.m11 + left.m20 * right.m12;
		float m11 = left.m01 * right.m10 + left.m11 * right.m11 + left.m21 * right.m12;
		float m12 = left.m02 * right.m10 + left.m12 * right.m11 + left.m22 * right.m12;
		float m20 = left.m00 * right.m20 + left.m10 * right.m21 + left.m20 * right.m22;
		float m21 = left.m01 * right.m20 + left.m11 * right.m21 + left.m21 * right.m22;
		float m22 = left.m02 * right.m20 + left.m12 * right.m21 + left.m22 * right.m22;

		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;

		return dest;
	}

	/**
	 * Load from another matrix
	 * 
	 * @param src
	 *            The source matrix
	 * @return this
	 */
	public Matrix3f load(Matrix3f src) {
		return load(src, this);
	}

	/**
	 * Copy source matrix to destination matrix
	 * 
	 * @param src
	 *            The source matrix
	 * @param dest
	 *            The destination matrix, or null of a new matrix is to be
	 *            created
	 * @return The copied matrix
	 */
	public static Matrix3f load(Matrix3f src, Matrix3f dest) {
		if(dest == null)
			dest = new Matrix3f();

		dest.m00 = src.m00;
		dest.m10 = src.m10;
		dest.m20 = src.m20;
		dest.m01 = src.m01;
		dest.m11 = src.m11;
		dest.m21 = src.m21;
		dest.m02 = src.m02;
		dest.m12 = src.m12;
		dest.m22 = src.m22;

		return dest;
	}

	/**
	 * Load from a float buffer. The buffer stores the matrix in column major
	 * (OpenGL) order.
	 *
	 * @param buf
	 *            A float buffer to read from
	 * @return this
	 */
	public Matrix3f load(FloatBuffer buf) {

		m00 = buf.get();
		m01 = buf.get();
		m02 = buf.get();
		m10 = buf.get();
		m11 = buf.get();
		m12 = buf.get();
		m20 = buf.get();
		m21 = buf.get();
		m22 = buf.get();

		return this;
	}

	/**
	 * Load from a float buffer. The buffer stores the matrix in row major
	 * (maths) order.
	 *
	 * @param buf
	 *            A float buffer to read from
	 * @return this
	 */
	public Matrix3f loadTranspose(FloatBuffer buf) {

		m00 = buf.get();
		m10 = buf.get();
		m20 = buf.get();
		m01 = buf.get();
		m11 = buf.get();
		m21 = buf.get();
		m02 = buf.get();
		m12 = buf.get();
		m22 = buf.get();

		return this;
	}

	/**
	 * Store this matrix in a float buffer. The matrix is stored in column major
	 * (openGL) order.
	 * 
	 * @param buf
	 *            The buffer to store this matrix in
	 */
	public Matrix3f store(FloatBuffer buf) {
		buf.put(m00);
		buf.put(m01);
		buf.put(m02);
		buf.put(m10);
		buf.put(m11);
		buf.put(m12);
		buf.put(m20);
		buf.put(m21);
		buf.put(m22);
		return this;
	}

	/**
	 * Store this matrix in a float buffer. The matrix is stored in row major
	 * (maths) order.
	 * 
	 * @param buf
	 *            The buffer to store this matrix in
	 */
	public Matrix3f storeTranspose(FloatBuffer buf) {
		buf.put(m00);
		buf.put(m10);
		buf.put(m20);
		buf.put(m01);
		buf.put(m11);
		buf.put(m21);
		buf.put(m02);
		buf.put(m12);
		buf.put(m22);
		return this;
	}

	@Override
	public Object clone() {
		return new Matrix3f().set(this);
	}
}
