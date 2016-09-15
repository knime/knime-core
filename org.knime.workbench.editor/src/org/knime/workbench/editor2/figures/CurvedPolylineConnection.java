/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Jul 14, 2016 (hornm): created
 */
package org.knime.workbench.editor2.figures;

import java.awt.geom.CubicCurve2D;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Geometry;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.widgets.Display;

/**
 * A {@link ProgressPolylineConnection} that optionally can be rendered as a curved line, i.e. compose of possibly
 * multiple bezier curves.
 *
 * @author Martin Horn
 */
public class CurvedPolylineConnection extends ProgressPolylineConnection {

    /**
     * Tolerance for checking whether a point lies on the polyline (see {@link #containsPoint(int, int)}.
     * A bigger tolerance helps the user to select a connection more easily.
     */
    private static final int TOLERANCE = 6;

    /**
     * The two control points for each bezier curve segments are moved by this amount (e.g. one third) to the right from
     * start point and to left from the end point. The higher, the more curvy. If 0, the segments are just straight
     * lines.
     */
    private static final double RELATIVE_CONTROL_POINT_PLACEMENT = 0.3;

    private static final Rectangle LINEBOUNDS = Rectangle.SINGLETON;


    /**
     * Keeps track of all bezier curves as list of lines approximating it. It helps to speed-up the {@link #containsPoint(int, int)} calculation.
     * At the same time it serves as a flag whether to draw a curved polyline (m_approxCurve!=null) or a straight polyline (m_approxCurve==null).
     */
    private PointList m_approxCurve = null;

    /**
     * The curve to be drawn (in case of a curved connection).
     */
    private Path m_path = null;

    /**
     * @param curved whether the connections should be rendered curved (<code>true</code>) or straight (
     *            <code>false</code>)
     */
    public CurvedPolylineConnection(final boolean curved) {
        super();
        setCurved(curved);
    }

    /**
     * @param curved whether the connections should be rendered curved (<code>true</code>) or straight (
     *            <code>false</code>)
     */
    public void setCurved(final boolean curved) {
        if(curved) {
            if(m_approxCurve == null) {
                calcCurve();
            }
        } else {
            m_approxCurve = null;
            m_path = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void outlineShape(final Graphics g) {
        if (m_approxCurve != null) {
            if (m_state < 0) {
                setLineStyle(SWT.LINE_SOLID);
            } else {
                g.setLineDash(DASHES[m_state]);
            }
            // set node connection color
            g.setForegroundColor(getForegroundColor());
            g.drawPath(m_path);

            //for debugging to check whether the lines correctly approximate the curve
            //g.drawPolyline(m_approxCurve);
        } else {
            super.outlineShape(g);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsPoint(final int x, final int y) {
        if (m_approxCurve != null) {
            int tolerance = (int)Math.max(getLineWidthFloat() / 2.0f, TOLERANCE);
            LINEBOUNDS.setBounds(getBounds());
            LINEBOUNDS.expand(0, tolerance);
            //shrink the horizontal bounds a bit - otherwise it'll be difficult to draw a connection from a already connected node (since the connection line will get selected first)
            LINEBOUNDS.shrink(10, 0);
            if (!LINEBOUNDS.contains(x, y)) {
                return false;
            }
            return Geometry.polylineContainsPoint(m_approxCurve, x, y, TOLERANCE) || childrenContainsPoint(x, y);
        } else {
            int tolerance = (int)Math.max(getLineWidthFloat() / 2.0f, TOLERANCE);
            LINEBOUNDS.setBounds(getBounds());
            LINEBOUNDS.expand(0, tolerance);
            //shrink the horizontal bounds a bit - otherwise it'll be difficult to draw a connection from a already connected node (since the connection line will get selected first)
            LINEBOUNDS.shrink(10, 0);
            if (!LINEBOUNDS.contains(x, y)) {
                return false;
            }
            return Geometry.polylineContainsPoint(getPoints(), x, y, TOLERANCE) || childrenContainsPoint(x, y);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Rectangle getBounds() {
        if (m_approxCurve != null) {
            if (bounds == null) {
                int expand = (int)(getLineWidthFloat() / 2.0f) + 10;
                bounds = m_approxCurve.getBounds().getExpanded(expand, expand);
            }
            return bounds;
        } else {
            return super.getBounds();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fireFigureMoved() {
        super.fireFigureMoved();
        if(m_approxCurve != null) {
        	bounds = null;
            calcCurve();
        }
    }

    private void calcCurve() {
        //redraw the path and re-determine the curve approximation
        if (m_approxCurve != null) {
            m_approxCurve.removeAllPoints();
        } else {
            m_approxCurve = new PointList();
        }

        PointList points = getPoints();
        m_path = new Path(Display.getDefault());
        m_path.moveTo(points.getFirstPoint().x, points.getFirstPoint().y);
        m_approxCurve.addPoint(points.getFirstPoint().x, points.getFirstPoint().y);
        Point lastPoint = points.getFirstPoint();
        for (int i = 1; i < points.size(); i++) {
            int x = points.getPoint(i).x;
            int y = points.getPoint(i).y;
            double dist = Math.sqrt((x - lastPoint.x) * (x - lastPoint.x) + (y - lastPoint.y) * (y - lastPoint.y));

            //control pts
            int cp1x = lastPoint.x + (int)(RELATIVE_CONTROL_POINT_PLACEMENT * dist);
            int cp1y = lastPoint.y;
            int cp2x = x - (int)(RELATIVE_CONTROL_POINT_PLACEMENT * dist);
            int cp2y = y;
            m_path.cubicTo(cp1x, cp1y, cp2x, cp2y, x, y);

            CubicCurve2D cc = new CubicCurve2D.Float(lastPoint.x, lastPoint.y, cp1x, cp1y, cp2x, cp2y, x, y);
            FlatteningPathIterator fpi = new FlatteningPathIterator(cc.getPathIterator(null), 3, 5);
            float[] coords = new float[6];
            while (!fpi.isDone()) {
                int type = fpi.currentSegment(coords);
                switch (type) {
                    case PathIterator.SEG_LINETO:
                        m_approxCurve.addPoint((int)coords[0], (int)coords[1]);
                        break;
                }
                fpi.next();
            }
            lastPoint = new Point(x, y);
        }
    }
}
