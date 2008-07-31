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
 *   Jan 26, 2006 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.view;

import javax.swing.JToolTip;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class JMultiLineToolTip extends JToolTip {

    private int m_columns = 0;

    private int m_fixedwidth = 0;

    /**
     * Creates new instance of JMultiLineToolTip.
     */
    public JMultiLineToolTip() {
        updateUI();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateUI() {
        setUI(MultiLineToolTipUI.createUI(this));
    }

    /**
     * Set given number of columns.
     * 
     * @param columns number of columns to set
     */
    public void setColumns(final int columns) {
        this.m_columns = columns;
        this.m_fixedwidth = 0;
    }

    /**
     * Returns number of columns.
     * 
     * @return number of columns
     */
    public int getColumns() {
        return m_columns;
    }

    /**
     * Sets the fixed width.
     * 
     * @param width the fixed width to set
     */
    public void setFixedWidth(final int width) {
        this.m_fixedwidth = width;
        this.m_columns = 0;
    }

    /**
     * Returns the fixed width.
     * 
     * @return the fixed width
     */
    public int getFixedWidth() {
        return m_fixedwidth;
    }
}
