package com.utyf.pmetro.util;

import android.graphics.Path;
import android.graphics.PointF;

/*
 * Created by Utyf on 21.02.2015.
 *
 */

public class ExtPath extends Path {
    private PointF[] firstControlPoints;
    private PointF[] secondControlPoints;

    public void Spline(PointF[] pnts)  {
        if (!GetCurveControlPoints(pnts)) return;
        moveTo(pnts[0].x, pnts[0].y);
        for( int i=1; i<pnts.length; i++ )
            cubicTo(firstControlPoints[i-1].x,firstControlPoints[i-1].y, secondControlPoints[i-1].x,secondControlPoints[i-1].y, pnts[i].x,pnts[i].y);
    }

    /* <summary>
       Get open-ended Bezier Spline Control Points.
       </summary>
       <param name="knots">Input Knot Bezier spline points.</param>
       <param name="firstControlPoints">Output First Control points array of knots.Length - 1 length.</param>
       <param name="secondControlPoints">Output Second Control points array of knots.Length - 1 length.</param>
       <exception cref="ArgumentNullException"><paramref name="knots"/> parameter must be not null.</exception>
       <exception cref="ArgumentException"><paramref name="knots"/> array must containg at least two points.</exception>
    */
    private boolean GetCurveControlPoints(PointF[] knots)
    {
        if (knots == null) return false; // throw new Error("knots");
        int n = knots.length - 1;
        if (n < 1) return false; //         throw new Error("At least two knot points required knots");

        if (n == 1)
        { // Special case: Bezier curve should be a straight line.
            firstControlPoints = new PointF[1];
            firstControlPoints[0] = new PointF();
            // 3P1 = 2P0 + P3
            firstControlPoints[0].x = (2 * knots[0].x + knots[1].x) / 3;
            firstControlPoints[0].y = (2 * knots[0].y + knots[1].y) / 3;

            secondControlPoints = new PointF[1];
            secondControlPoints[0] = new PointF();
            // P2 = 2P1 – P0
            secondControlPoints[0].x = 2 * firstControlPoints[0].x - knots[0].x;
            secondControlPoints[0].y = 2 * firstControlPoints[0].y - knots[0].y;
            return true;
        }

        // Calculate first Bezier control points
        // Right hand side vector
        float[] rhs = new float[n];

        // Set right hand side X values
        for (int i = 1; i < n - 1; ++i)
            rhs[i] = 4 * knots[i].x + 2 * knots[i + 1].x;
        rhs[0] = knots[0].x + 2 * knots[1].x;
        rhs[n - 1] = (8 * knots[n - 1].x + knots[n].x) / 2.0f;
        // Get first control points X-values
        float[] x = GetFirstControlPoints(rhs);

        // Set right hand side Y values
        for (int i = 1; i < n - 1; ++i)
            rhs[i] = 4 * knots[i].y + 2 * knots[i + 1].y;
        rhs[0] = knots[0].y + 2 * knots[1].y;
        rhs[n - 1] = (8 * knots[n - 1].y + knots[n].y) / 2.0f;
        // Get first control points Y-values
        float[] y = GetFirstControlPoints(rhs);

        // Fill output arrays.
        firstControlPoints = new PointF[n];
        secondControlPoints = new PointF[n];
        for (int i = 0; i < n; ++i)
        {
            // First control point
            firstControlPoints[i] = new PointF(x[i], y[i]);
            // Second control point
            if (i < n - 1)
                secondControlPoints[i] = new PointF(2 * knots[i + 1].x - x[i + 1], 2 * knots[i + 1].y - y[i + 1]);
            else
                secondControlPoints[i] = new PointF((knots[n].x + x[n - 1]) / 2, (knots[n].y + y[n - 1]) / 2);
        }
        return true;
    }

    /// <summary>
    /// Solves a tridiagonal system for one of coordinates (x or y) of first Bezier control points.
    /// </summary>
    /// <param name="rhs">Right hand side vector.</param>
    /// <returns>Solution vector.</returns>
    private float[] GetFirstControlPoints(float[] rhs)
    {
        int n = rhs.length;
        float[] x = new float[n]; // Solution vector.
        float[] tmp = new float[n]; // Temp workspace.

        float b = 2.0f;
        x[0] = rhs[0] / b;
        for (int i = 1; i < n; i++) // Decomposition and forward substitution.
        {
            tmp[i] = 1 / b;
            b = (i < n - 1 ? 4.0f : 3.5f) - tmp[i];
            x[i] = (rhs[i] - x[i - 1]) / b;
        }
        for (int i = 1; i < n; i++)
            x[n - i - 1] -= tmp[n - i] * x[n - i]; // Backsubstitution.

        return x;
    }
}
