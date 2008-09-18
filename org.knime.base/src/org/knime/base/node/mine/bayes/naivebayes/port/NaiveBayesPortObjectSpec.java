/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *    25.08.2008 (Tobias Koetter): created
 */

package org.knime.base.node.mine.bayes.naivebayes.port;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;


/**
 * The Naive Bayes specific port object specification implementation.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class NaiveBayesPortObjectSpec extends AbstractSimplePortObjectSpec {

    private static final String CNFG_CLASS_COL = "classCol";

    private static final String CNFG_SPEC = "trainingTableSpec";

    private DataTableSpec m_tableSpec;

    private DataColumnSpec m_classColumn;

    /**Constructor for class NaiveBayesPortObjectSpec.
     */
    public NaiveBayesPortObjectSpec() {
        // needed for loading
    }

    /**Constructor for class NaiveBayesPortObjectSpec.
     * @param traingDataSpec the {@link DataTableSpec} of the training data
     * table
     * @param classColumn the name of the class column
     */
    public NaiveBayesPortObjectSpec(final DataTableSpec traingDataSpec,
            final DataColumnSpec classColumn) {
        if (traingDataSpec == null) {
            throw new NullPointerException("traingDataSpec must not be null");
        }
        if (classColumn == null) {
            throw new NullPointerException("classColumn must not be null");
        }
        m_tableSpec = traingDataSpec;
        m_classColumn = classColumn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model)
    throws InvalidSettingsException {
        final Config specModel = model.getConfig(CNFG_SPEC);
        m_tableSpec = DataTableSpec.load(specModel);
        final ModelContentRO classColModel =
            model.getModelContent(CNFG_CLASS_COL);
        m_classColumn = DataColumnSpec.load(classColModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model) {
        final Config specModel = model.addConfig(CNFG_SPEC);
        m_tableSpec.save(specModel);
        final ModelContentWO classColModel =
            model.addModelContent(CNFG_CLASS_COL);
        m_classColumn.save(classColModel);
    }


    /**
     * @return the tableSpec of the training data
     */
    public DataTableSpec getTableSpec() {
        return m_tableSpec;
    }


    /**
     * @return the column that contained the classes
     */
    public DataColumnSpec getClassColumn() {
        return m_classColumn;
    }
}
