/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   17.05.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.knime.core.data.DataRow;
import org.knime.core.node.BufferedDataTable;

/**
 * {@link TrainingData} implementation that holds all the data in memory.
 *
 * @author Adrian Nembach, KNIME.com
 * @param <T>
 */
public class InMemoryData <T extends TrainingRow> extends AbstractTrainingData<T> {

    private final List<T> m_rows;

    /**
     * Instantiates a {@link TrainingData} object that holds all data in memory.
     *
     * @param data the {@link BufferedDataTable} that contains the data to learn on
     * @param seed used to generate pseudo random numbers
     * @param rowBuilder used to create {@link TrainingRow} objects form {@link DataRow} objects
     *
     */
    public InMemoryData(final BufferedDataTable data, final Long seed, final TrainingRowBuilder<T> rowBuilder) {
        super(data, seed, rowBuilder);
        int idCounter = 0;
        m_rows = new ArrayList<T>(getRowCount());
        for (DataRow row : data) {
            T trainingRow = rowBuilder.build(row, idCounter++);
            m_rows.add(trainingRow);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<T> iterator() {
        return m_rows.iterator();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public T getRandomRow() {
        int randomIdx = getRandomDataGenerator().nextInt(m_rows.size());
        return m_rows.get(randomIdx);
    }

}
