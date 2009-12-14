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
