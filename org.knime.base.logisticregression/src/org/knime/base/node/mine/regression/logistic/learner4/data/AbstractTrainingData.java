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
 *   22.05.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.data;

import java.util.Random;

import org.knime.core.node.BufferedDataTable;

/**
 * Holds members that are common to different implementations of {@link TrainingData}.
 *
 * @author Adrian Nembach, KNIME.com
 */
abstract class AbstractTrainingData <T extends TrainingRow> implements TrainingData<T> {

    private final int m_rowCount;
    private final Random m_randomGenerator;
    private final TrainingRowBuilder<T> m_rowBuilder;

    /**
     * @param data the {@link BufferedDataTable} that contains the input data
     * @param seed used to generate pseudo random numbers
     * @param rowBuilder used to create {@link TrainingRow} objects from the rows of <b>data</b>
     *
     */
    public AbstractTrainingData(final BufferedDataTable data, final Long seed, final TrainingRowBuilder<T> rowBuilder) {
        if (data.size() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("The data table contains too many rows.");
        }
        m_rowCount = (int)data.size();
        if (seed == null) {
            m_randomGenerator = new Random();
        } else {
            m_randomGenerator = new Random(seed);
        }
        m_rowBuilder = rowBuilder;

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getFeatureCount() {
        return m_rowBuilder.getFeatureCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return m_rowCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTargetDimension() {
        return m_rowBuilder.getTargetDimension();
    }

    protected TrainingRowBuilder<T> getRowBuilder() {
        return m_rowBuilder;
    }

    protected Random getRandomDataGenerator() {
        return m_randomGenerator;
    }

}
