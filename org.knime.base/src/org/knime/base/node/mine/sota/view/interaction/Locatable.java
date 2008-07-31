/* 
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
 *   Dec 5, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.view.interaction;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public interface Locatable {

    /**
     * Default X coordinate.
     */
    public static final int X = -1;

    /**
     * Default Y coordinate.
     */
    public static final int Y = -1;

    /**
     * Returns the objects start X coordinate.
     * 
     * @return the objects start X coordinate
     */
    public int getStartX();

    /**
     * Returns the objects start Y coordinate.
     * 
     * @return the objects start Y coordinate
     */
    public int getStartY();

    /**
     * Returns the objects end X coordinate.
     * 
     * @return the objects end X coordinate
     */
    public int getEndX();

    /**
     * Returns the objects end Y coordinate.
     * 
     * @return the objects end Y coordinate
     */
    public int getEndY();

    /**
     * Sets the given start x coordinate.
     * 
     * @param x the start x coordinate to set
     */
    public void setStartX(int x);

    /**
     * Sets the given start y coordinate.
     * 
     * @param y the start y coordinate to set
     */
    public void setStartY(int y);

    /**
     * Sets the given end x coordinate.
     * 
     * @param x the end x coordinate to set
     */
    public void setEndX(int x);

    /**
     * Sets the given end y coordinate.
     * 
     * @param y the end y coordinate to set
     */
    public void setEndY(int y);
}
