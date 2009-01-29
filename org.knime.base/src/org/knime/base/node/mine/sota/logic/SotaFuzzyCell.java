/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   Nov 23, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.logic;

import org.knime.core.data.DataCell;
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.data.FuzzyNumberValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;


/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public final class SotaFuzzyCell implements SotaCell, FuzzyIntervalValue, 
FuzzyNumberValue {
    
    private static final String CFG_KEY_MIN_SUPP = "FuzzyMinSupp";
    private static final String CFG_KEY_MAX_SUPP = "FuzzyMaxSupp";
    private static final String CFG_KEY_MIN_CORE = "FuzzyMincore";
    private static final String CFG_KEY_MAX_CORE = "FuzzyMaxCore";
    
    private double m_minSupp;
    private double m_maxSupp;
    
    private double m_minCore;
    private double m_maxCore;
    
    /**
     * Creates new instance of SotaFuzzyCell with given min, max support and 
     * core region.
     * @param minSupp minimal support value
     * @param minCore minimal core value
     * @param maxCore maximal support value
     * @param maxSupp maximal core value
     */
    public SotaFuzzyCell(final double minSupp, final double minCore,
            final double maxCore, final double maxSupp) {
        m_minSupp = minSupp;
        m_minCore = minCore;
        m_maxCore = maxCore;
        m_maxSupp = maxSupp;
    }
    
    /**
     * {@inheritDoc}
     */
    public void adjustCell(final DataCell cell, final double learningrate) {
        if (SotaUtil.isFuzzyIntervalType(cell.getType())) {
            m_minSupp = m_minSupp + (learningrate
                    * (((FuzzyIntervalValue) cell).getMinSupport()
                            - m_minSupp));
            m_minCore = m_minCore + (learningrate
                    * (((FuzzyIntervalValue) cell).getMinCore() 
                            - m_minCore));
            m_maxSupp = m_maxSupp + (learningrate
                    * (((FuzzyIntervalValue) cell).getMaxSupport() 
                            - m_maxSupp));
            m_maxCore = m_maxCore + (learningrate
                    * (((FuzzyIntervalValue) cell).getMaxCore()
                            - m_maxCore));
        }
    }

    /**
     * {@inheritDoc}
     */
    public double getValue() {
        return getCenterOfGravity();
    }

    /**
     * {@inheritDoc}
     */
    public double getMinSupport() {
        return m_minSupp;
    }

    /**
     * {@inheritDoc}
     */
    public double getMinCore() {
        return m_minCore;
    }

    /**
     * {@inheritDoc}
     */
    public double getMaxCore() {
        return m_maxCore;
    }

    /**
     * {@inheritDoc}
     */
    public double getMaxSupport() {
        return m_maxSupp;
    }

    /**
     * {@inheritDoc}
     */
    public double getCenterOfGravity() {
        return (m_maxCore + m_minCore) / 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SotaCell clone() {
        return new SotaFuzzyCell(m_minSupp, m_minCore, m_maxCore, m_maxSupp);
    }
    

    /**
     * {@inheritDoc}
     */
    public void loadFrom(final ModelContentRO modelContent)
            throws InvalidSettingsException {
        m_minSupp = modelContent.getDouble(CFG_KEY_MIN_SUPP);
        m_maxSupp = modelContent.getDouble(CFG_KEY_MAX_SUPP);
        m_minCore = modelContent.getDouble(CFG_KEY_MIN_CORE);
        m_maxCore = modelContent.getDouble(CFG_KEY_MAX_CORE);
    }

    /**
     * {@inheritDoc}
     */
    public void saveTo(final ModelContentWO modelContent) {
        modelContent.addDouble(CFG_KEY_MIN_SUPP, m_minSupp);
        modelContent.addDouble(CFG_KEY_MAX_SUPP, m_maxSupp);
        modelContent.addDouble(CFG_KEY_MIN_CORE, m_minCore);
        modelContent.addDouble(CFG_KEY_MAX_CORE, m_maxCore);
    }

    /**
     * {@inheritDoc}
     */
    public String getType() {
        return SotaCellFactory.FUZZY_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    public double getCore() {
        return getCenterOfGravity();
    }
}
