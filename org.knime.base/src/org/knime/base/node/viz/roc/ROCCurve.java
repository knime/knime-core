/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *   26.05.2015 (Alexander): created
 */
package org.knime.base.node.viz.roc;


/**
 * Small container class for a single ROC curve.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.12
 */
public class ROCCurve {
    private String m_name;

    private double m_area;

    private double[] m_x, m_y;

    private int m_maxPoints;

    /**
     * Creates a new ROC curve container. The data points may be downsampled by providing a maximum number
     * of points. This should speed up the view for curves with many data points
     *
     * @param name the curve's name
     * @param x the curve's x-values
     * @param y the curve's y-values
     * @param area the curve's area
     * @param maxPoints the maximum number of points to store when downsampling the curve; -1 disabled downsampling
     */
    public ROCCurve(final String name, final double[] x, final double[] y,
            final double area, final int maxPoints) {
        m_name = name;
        m_x = sample(x, maxPoints);
        m_y = sample(y, maxPoints);
        m_area = area;
        m_maxPoints = maxPoints;
    }

    /**
     *
     */
    protected ROCCurve() {
        // TODO Auto-generated constructor stub
    }

    private static double[] sample(final double[] in, final int maxPoints) {
        if ((maxPoints == -1) || (in.length <= maxPoints)) {
            return in;
        }

        double[] out = new double[maxPoints];
        out[0] = in[0];
        out[out.length - 1] = in[in.length - 1];

        final double factor = in.length / (double) maxPoints;
        double x = factor;

        for (int i = 1; i < out.length - 1; i++) {
            out[i] = in[(int) Math.round(x)];
            x += factor;
        }

        return out;
    }

    /**
     * @return the maxPoints
     */
    public int getMaxPoints() {
        return m_maxPoints;
    }

    /**
     * @param name the name to set
     */
    protected void setName(final String name) {
        m_name = name;
    }

    /**
     * @param area the area to set
     */
    protected void setArea(final double area) {
        m_area = area;
    }

    /**
     * @param x the x to set
     */
    protected void setX(final double[] x) {
        m_x = x;
    }

    /**
     * @param y the y to set
     */
    protected void setY(final double[] y) {
        m_y = y;
    }

    /**
     * Returns the curve's name.
     *
     * @return the curve's name.
     */
    public String getName() {
        return m_name;
    }

    /**
     * Returns the curve's area.
     *
     * @return the curve's area.
     */
    public final double getArea() {
        return m_area;
    }

    /**
     * Returns the curve's x-values.
     *
     * @return the curve's x-values.
     */
    public final double[] getX() {
        return m_x;
    }

    /**
     * Returns the curve's y-values.
     *
     * @return the curve's y-values.
     */
    public final double[] getY() {
        return m_y;
    }
}