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
 *   Nov 21, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.logic;

import java.io.Serializable;

import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;


/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaDoubleCell implements SotaCell, DoubleValue, Serializable {
    
    private static final String CFG_KEY_VALUE = "DoubleValue";
    
    private double m_value;

    /**
     * Creates new instance of SotaCell with given value.
     * 
     * @param value value to set
     */
    public SotaDoubleCell(final double value) {
        m_value = value;
    }

    /**
     * {@inheritDoc}
     */
    public double getDoubleValue() {
        return m_value;
    }

    /**
     * {@inheritDoc}
     */
    public void adjustCell(final DataCell cell, final double learningrate) {
        if (SotaUtil.isNumberType(cell.getType())) {
            m_value = m_value + (learningrate * (((DoubleValue)cell)
                    .getDoubleValue() - m_value));
        }
    }

    /**
     * {@inheritDoc}
     */
    public double getValue() {
        return getDoubleValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SotaCell clone() {
        return new SotaDoubleCell(m_value);
    }
    

    /**
     * {@inheritDoc}
     */
    public void loadFrom(final ModelContentRO modelContent)
            throws InvalidSettingsException {
        m_value = modelContent.getDouble(CFG_KEY_VALUE);
    }

    /**
     * {@inheritDoc}
     */
    public void saveTo(final ModelContentWO modelContent) {
        modelContent.addDouble(CFG_KEY_VALUE, m_value);
    }

    /**
     * {@inheritDoc}
     */
    public String getType() {
        return SotaCellFactory.DOUBLE_TYPE;
    }    
}
