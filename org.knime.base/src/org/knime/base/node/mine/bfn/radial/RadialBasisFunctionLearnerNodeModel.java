/* 
 * --------------------------------------------------------------------- *
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
                getDistance(), spec, getDataColumns(), getTargetColumns());
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
