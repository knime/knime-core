/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 14, 2006 (sieb): created
 */
package org.knime.base.node.preproc.discretization.caim.modelcreator;

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
