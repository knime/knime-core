/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
package org.knime.base.node.mine.bfn.fuzzy;

import org.knime.base.node.mine.bfn.BasisFunctionFactory;
import org.knime.base.node.mine.bfn.BasisFunctionLearnerNodeModel;
import org.knime.base.node.mine.bfn.fuzzy.norm.Norm;
import org.knime.base.node.mine.bfn.fuzzy.shrink.Shrink;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.FuzzyIntervalCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * The fuzzy basis function model training
 * {@link FuzzyBasisFunctionLearnerRow}s.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class FuzzyBasisFunctionLearnerNodeModel extends
        BasisFunctionLearnerNodeModel {
    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(FuzzyBasisFunctionLearnerNodeModel.class);

    /** The selected shrink, 0 default. */
    private int m_shrink = 0;

    /** The selected norm, 0 default. */
    private int m_norm = 0;

    /** Inits a new model for fuzzy basisfunctions. */
    public FuzzyBasisFunctionLearnerNodeModel() {
        super();
    }

    /**
     * Starts the learning algorithm in the learner.
     * 
     * @param data the input trainings data at index 0
     * @param exec the execution monitor
     * @return the ouput fuzzy rule model
     * @throws CanceledExecutionException if the training was canceled
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException {
        return super.execute(data, exec);
    }

    /**
     * @see BasisFunctionLearnerNodeModel
     *      #getFactory(org.knime.core.data.DataTableSpec)
     */
    @Override
    protected BasisFunctionFactory getFactory(final DataTableSpec spec) {
        LOGGER.debug("fuzzy_norm   : " + Norm.NORMS[m_norm]);
        LOGGER.debug("shrink       : " + Shrink.SHRINKS[m_shrink]);
        return new FuzzyBasisFunctionFactory(m_norm, m_shrink, spec,
                getTargetColumn(), getDistance());
    }

    /**
     * @see #configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] ins)
            throws InvalidSettingsException {
        return super.configure(ins);
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        // shrink procedure
        m_shrink = settings.getInt(Shrink.SHRINK_KEY);
        // fuzzy norm
        m_norm = settings.getInt(Norm.NORM_KEY);
    }

    /**
     * @see org.knime.core.node.NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        // shrink procedure
        settings.addInt(Shrink.SHRINK_KEY, m_shrink);
        // fuzzy norm
        settings.addInt(Norm.NORM_KEY, m_norm);
    }

    /**
     * @see org.knime.core.node.NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);
        StringBuffer msg = new StringBuffer();
        // shrink function
        int shrink = 0;
        try {
            shrink = settings.getInt(Shrink.SHRINK_KEY);
        } catch (InvalidSettingsException ise) {
            // use default
        }
        if (shrink < 0 || shrink >= Shrink.SHRINKS.length) {
            msg.append("Unknown shrink type: " + shrink + ".\n");
        }
        int norm = 0;
        try {
            norm = settings.getInt(Norm.NORM_KEY);
        } catch (InvalidSettingsException ise) {
            // use default
        }
        // fuzzy norm
        if (norm < 0 || norm >= Norm.NORMS.length) {
            msg.append("Unknown norm choice: " + norm + ".");
        }
        // if message length contains chars
        if (msg.length() > 0) {
            throw new InvalidSettingsException(msg.toString());
        }
    }

    /**
     * @return {@link FuzzyIntervalCell#TYPE}
     */
    @Override
    protected final DataType getModelType() {
        return FuzzyIntervalCell.TYPE;
    }

    /**
     * @return shrink function for conflict avoidance
     */
    public final int getShrink() {
        return m_shrink;
    }

    /**
     * @return fuzzy norm
     */
    public final int getNorm() {
        return m_norm;
    }
}
