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
 *    21.08.2008 (Tobias Koetter): created
 */

package org.knime.base.node.mine.bayes.naivebayes.port;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import org.knime.base.node.mine.bayes.naivebayes.datamodel.NaiveBayesModel;


/**
 * The Naive Bayes specific port object implementation.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class NaiveBayesPortObject extends AbstractSimplePortObject {

    /**The {@link PortType}.*/
    public static final PortType TYPE =
        new PortType(NaiveBayesPortObject.class);

    private static final String CNFG_MODEL = "naiveBayesModel";

    private NaiveBayesModel m_model;

    private PortObjectSpec m_portSpec;

    /**Constructor for class NaiveBayesPortObject.
     */
    public NaiveBayesPortObject() {
        // needed for loading
    }

    /**Constructor for class NaiveBayesPortObject.
     * @param trainingDataSpec the {@link DataTableSpec} of the training table
     * @param model the {@link NaiveBayesModel}
     */
    public NaiveBayesPortObject(final DataTableSpec trainingDataSpec,
            final NaiveBayesModel model) {
        if (trainingDataSpec == null) {
            throw new NullPointerException("trainingDataSpec must not be null");
        }
        if (model == null) {
            throw new NullPointerException("model must not be null");
        }
        m_model = model;
        m_portSpec = new NaiveBayesPortObjectSpec(trainingDataSpec,
                trainingDataSpec.getColumnSpec(m_model.getClassColumnName()));
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec,
            final ExecutionMonitor exec) throws InvalidSettingsException {
        final Config modelConfig = model.getConfig(CNFG_MODEL);
        m_model = new NaiveBayesModel(modelConfig);
        m_portSpec = spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model,
            final ExecutionMonitor exec) {
        final Config modelConfig = model.addConfig(CNFG_MODEL);
        m_model.savePredictorParams(modelConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec getSpec() {
        return m_portSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        return m_model.getSummary();
    }
    /**
     * @return the {@link NaiveBayesModel}
     */
    public NaiveBayesModel getModel() {
        return m_model;
    }

}
