/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
