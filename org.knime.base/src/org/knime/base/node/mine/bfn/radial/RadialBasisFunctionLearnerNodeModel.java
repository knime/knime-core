/* 
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.radial;

import org.knime.base.node.mine.bfn.BasisFunctionLearnerNodeModel;
import org.knime.base.node.mine.bfn.BasisFunctionModelContent;
import org.knime.base.node.mine.bfn.BasisFunctionPortObject;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * The {@link org.knime.core.node.NodeModel} for
 * {@link RadialBasisFunctionLearnerRow}s.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class RadialBasisFunctionLearnerNodeModel 
        extends BasisFunctionLearnerNodeModel {
    
    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(RadialBasisFunctionLearnerNodeModel.class);

    /** Default theta minus: 0.2. */
    static final double THETAMINUS = 0.2;

    /** The currently set theta minus. */
    private double m_thetaMinus = THETAMINUS;

    /** Default theta plus: 0.4. */
    static final double THETAPLUS = 0.4;

    /**
     * Default theta plus value <code>0.4</code> which is the lower bound of
     * activation of non-conflicting instances.
     */
    private double m_thetaPlus = THETAPLUS;

    /** Inits a new RadialBasisFunctionFactory with one in- and one output. */
    public RadialBasisFunctionLearnerNodeModel() {
        super(RadialBasisFunctionPortObject.TYPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RadialBasisFunctionFactory getFactory(final DataTableSpec spec) {
        LOGGER.debug("theta-minus  : " + m_thetaMinus);
        LOGGER.debug("theta-plus   : " + m_thetaPlus);
        return new RadialBasisFunctionFactory(m_thetaMinus, m_thetaPlus,
                getDistance(), spec, getTargetColumns());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        // update theta minus
        m_thetaMinus = settings
                .getDouble(RadialBasisFunctionFactory.THETA_MINUS);
        // update theta plus
        m_thetaPlus = settings.getDouble(RadialBasisFunctionFactory.THETA_PLUS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        // theta minus
        settings.addDouble(RadialBasisFunctionFactory.THETA_MINUS, 
                m_thetaMinus);
        // theta plus
        settings.addDouble(RadialBasisFunctionFactory.THETA_PLUS, m_thetaPlus);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);
        // the error string
        StringBuffer errMsg = new StringBuffer();
        // update theta minus
        double thetaMinus = settings.getDouble(
                RadialBasisFunctionFactory.THETA_MINUS, -1.0);
        // update theta plus
        double thetaPlus = settings.getDouble(
                RadialBasisFunctionFactory.THETA_PLUS, -1.0);
        // theta minus
        if (thetaMinus < 0.0 || thetaMinus > 1.0) {
            errMsg.append("Theta minus invalid: " + thetaMinus + "\n");
        }
        // theta plus
        if (thetaPlus < 0.0 || thetaPlus > 1.0) {
            errMsg.append("Theta plus invalid: " + thetaPlus + "\n");
        }
        // if error message is not empty
        if (errMsg.length() > 0) {
            throw new InvalidSettingsException(errMsg.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final DataType getModelType() {
        return DoubleCell.TYPE;
   }
    
    /**
     * @return theta minus
     */
    public double getThetaMinus() {
        return m_thetaMinus;
    }
    
    /**
     * @return theta plus
     */
    public double getThetaPlus() {
        return m_thetaPlus;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BasisFunctionPortObject createPortObject(
            final BasisFunctionModelContent content) {
        return new RadialBasisFunctionPortObject(content);
    }
}
