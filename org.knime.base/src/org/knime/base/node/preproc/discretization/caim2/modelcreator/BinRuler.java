/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 14, 2006 (sieb): created
 */
package org.knime.base.node.preproc.discretization.caim2.modelcreator;

import java.awt.Point;

/**
 * A <code>BinRuler</code> is the visual representation of a column binning.
 * The ruler contains just display coordinates such that it can be painted
 * directly into a drawing pane; i.e. it is the visual model of the binning.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class BinRuler {

    /**
     * The left coordinate point of the ruler where it starts.
     */
    private Point m_leftStartPoint;

    /**
     * The horizontal length (width) of the ruler.
     */
    private int m_width;

    /**
     * The y coordinates of the bin boundaries.
     */
    private int[] m_binPositions;

    /**
     * The value of the corresponding bin position used as a label.
     */
    private String[] m_binPosValue;

    /**
     * The name to display for this ruler.
     */
    private String m_name;

    /**
     * Creates a <code>BinRuler</code> from its left starting point, the width
     * of the ruler, its binning boundary positions and the value labels for the
     * boundaries.
     * 
     * @param leftStartPoint the left starting point of the ruler
     * @param width the width of the ruler
     * @param binPositions binning boundary positions
     * @param binPosValues the value labels for the boundaries
     * @param name the name for this ruler; normaly the column name
     */
    public BinRuler(final Point leftStartPoint, final int width,
            final int[] binPositions, final String[] binPosValues,
            final String name) {
        m_leftStartPoint = leftStartPoint;
        m_width = width;
        m_binPositions = binPositions;
        m_binPosValue = binPosValues;
        m_name = name;
    }

    /**
     * Returns the bin positions.
     * 
     * @return the bin positions
     */
    public int[] getBinPositions() {
        return m_binPositions;
    }

    /**
     * Returns the bin positions label values.
     * 
     * @return the bin positions label values
     */
    public String[] getBinPosValue() {
        return m_binPosValue;
    }

    /**
     * Returns the left start point of the ruler.
     * 
     * @return the left start point of the ruler
     */
    public Point getLeftStartPoint() {
        return m_leftStartPoint;
    }

    /**
     * Returns the width of the ruler.
     * 
     * @return the width of the ruler
     */
    public int getWidth() {
        return m_width;
    }

    /**
     * Returns the name for this ruler.
     * 
     * @return the name for this ruler
     */
    public String getName() {
        return m_name;
    }
}
