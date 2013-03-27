/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 *
 * History
 *   04.01.2011 (thiel): created
 */
package org.knime.base.node.mine.sota.view.interaction;

public class SotaTreeCellLocations implements Locatable {

    // Related to interface Locatable
    private int m_startY = Locatable.X;

    private int m_startX = Locatable.Y;

    private int m_endY = Locatable.X;

    private int m_endX = Locatable.Y;

    public SotaTreeCellLocations() { }

    /**
     * {@inheritDoc}
     */
    public int getStartX() {
        return m_startX;
    }

    /**
     * {@inheritDoc}
     */
    public int getStartY() {
        return m_startY;
    }

    /**
     * {@inheritDoc}
     */
    public int getEndX() {
        return m_endX;
    }

    /**
     * {@inheritDoc}
     */
    public int getEndY() {
        return m_endY;
    }

    /**
     * {@inheritDoc}
     */
    public void setStartX(final int x) {
        m_startX = x;
    }

    /**
     * {@inheritDoc}
     */
    public void setStartY(final int y) {
        m_startY = y;
    }

    /**
     * {@inheritDoc}
     */
    public void setEndX(final int x) {
        m_endX = x;
    }

    /**
     * {@inheritDoc}
     */
    public void setEndY(final int y) {
        m_endY = y;
    }
}
