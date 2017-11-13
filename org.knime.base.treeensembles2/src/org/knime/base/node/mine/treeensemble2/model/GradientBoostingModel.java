/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 * ---------------------------------------------------------------------
 *
 * History
 *   18.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.model;

import java.io.DataOutputStream;
import java.io.IOException;

import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.data.TreeMetaData;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class GradientBoostingModel extends AbstractGradientBoostingModel {

    private double[] m_coefficients;

    /**
     * @param config
     * @param metaData
     * @param models
     * @param treeType
     * @param coefficients
     * @param initialValue
     */
    public GradientBoostingModel(final TreeEnsembleLearnerConfiguration config, final TreeMetaData metaData,
        final TreeModelRegression[] models, final TreeType treeType, final double initialValue, final double[] coefficients) {
        super(config, metaData, models, treeType, initialValue);
        m_coefficients = coefficients;
    }

    /**
     * @param metaData
     * @param models
     * @param type
     * @param containsClassDistribution
     */
    protected GradientBoostingModel(final TreeMetaData metaData, final AbstractTreeModel[] models, final TreeType type,
        final boolean containsClassDistribution) {
        super(metaData, models, type, containsClassDistribution);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double predict(final PredictorRecord record) {
        double prediction = getInitialValue();
        for (int i = 0; i < getNrModels(); i++) {
            prediction += m_coefficients[i] * getTreeModelRegression(i).findMatchingNode(record).getMean();
        }
        return prediction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveData(final DataOutputStream dataOutput) throws IOException {
        super.saveData(dataOutput);
        dataOutput.writeInt(m_coefficients.length);
        for (double coefficient : m_coefficients) {
            dataOutput.writeDouble(coefficient);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadData(final TreeModelDataInputStream input) throws IOException {
        super.loadData(input);
        m_coefficients = new double[input.readInt()];
        for (int i = 0; i < m_coefficients.length; i++) {
            m_coefficients[i] = input.readDouble();
        }
    }

}
