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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.fuzzy;

import org.knime.base.node.mine.bfn.BasisFunctionFactory;
import org.knime.base.node.mine.bfn.BasisFunctionLearnerNodeModel;
import org.knime.base.node.mine.bfn.BasisFunctionModelContent;
import org.knime.base.node.mine.bfn.fuzzy.norm.Norm;
import org.knime.base.node.mine.bfn.fuzzy.shrink.Shrink;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.FuzzyIntervalCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;

/**
 * The fuzzy basis function model training
 * {@link org.knime.base.node.mine.bfn.fuzzy.FuzzyBasisFunctionLearnerRow}s.
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
        super(FuzzyBasisFunctionPortObject.TYPE);
    }

    /**
     * Starts the learning algorithm in the learner.
     * 
     * @param data the input training data at index 0
     * @param exec the execution monitor
     * @return the output fuzzy rule model
     * @throws CanceledExecutionException if the training was canceled
     */
    @Override
    public PortObject[] execute(final PortObject[] data,
            final ExecutionContext exec) throws CanceledExecutionException {
        return super.execute(data, exec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BasisFunctionFactory getFactory(final DataTableSpec spec) {
        LOGGER.debug("fuzzy_norm   : " + Norm.NORMS[m_norm]);
        LOGGER.debug("shrink       : " + Shrink.SHRINKS[m_shrink]);
        return new FuzzyBasisFunctionFactory(m_norm, m_shrink, spec,
                getTargetColumns(), getDistance());
    } 

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec[] configure(final PortObjectSpec[] ins)
            throws InvalidSettingsException {
        return super.configure(ins);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        // shrink procedure
        m_shrink = settings.getInt(Shrink.SHRINK_KEY);
        // fuzzy norm
        m_norm = settings.getInt(Norm.NORM_KEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        // shrink procedure
        settings.addInt(Shrink.SHRINK_KEY, m_shrink);
        // fuzzy norm
        settings.addInt(Norm.NORM_KEY, m_norm);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings)
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
    public final DataType getModelType() {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public FuzzyBasisFunctionPortObject createPortObject(
            final BasisFunctionModelContent content) {
        return new FuzzyBasisFunctionPortObject(content);
    }
}
