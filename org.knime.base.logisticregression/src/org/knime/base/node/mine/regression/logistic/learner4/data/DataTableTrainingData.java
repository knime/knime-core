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
 *   23.05.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.knime.core.data.DataRow;
import org.knime.core.node.BufferedDataTable;

/**
 *
 * @author Adrian Nembach, KNIME.com
 * @param <T>
 */
public class DataTableTrainingData <T extends TrainingRow> extends AbstractTrainingData<T> {

    private final BufferedDataTable m_data;
    private final List<T> m_cachedRows;
    private final int m_cacheSize;
    private Iterator<DataRow> m_rowIterator;
    private int m_idCounter;
    private int m_sampleCounter;

    /**
     * @param data
     * @param seed
     * @param rowBuilder
     * @param cacheSize
     *
     */
    public DataTableTrainingData(final BufferedDataTable data, final Long seed, final TrainingRowBuilder<T> rowBuilder,
        final int cacheSize) {
        super(data, seed, rowBuilder);
        m_data = data;
        m_cacheSize = cacheSize;
        m_cachedRows = new ArrayList<>(m_cacheSize);
        m_rowIterator = data.iterator();
        m_idCounter = 0;
        m_sampleCounter = 0;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public T getRandomRow() {
        if (m_sampleCounter >= m_cacheSize) {
            refillCache();
            m_sampleCounter = 0;
        }
        m_sampleCounter++;
        return drawFromCache();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<T> iterator() {
        return new DataTableIterator<>(m_data.iterator(), getRowBuilder());
    }

    private T drawFromCache() {
        int randomIdx = getRandomDataGenerator().nextInt(m_cacheSize);
        return m_cachedRows.get(randomIdx);
    }

    private void refillCache() {
        m_cachedRows.clear();
        for (int i = 0; i < m_cacheSize; i++) {
            addRowToCache();
        }
    }

    private void addRowToCache() {
        if (m_rowIterator.hasNext()) {
            T row = getRowBuilder().build(m_rowIterator.next(), m_idCounter++);
            m_cachedRows.add(row);
        } else {
            m_rowIterator = m_data.iterator();
            m_idCounter = 0;
            addRowToCache();
        }
    }

    private static class DataTableIterator <T extends TrainingRow> implements Iterator<T> {

        private TrainingRowBuilder<T> m_rowBuilder;
        private Iterator<DataRow> m_rowIterator;
        private int m_idCounter = 0;

        /**
         * @param rowIterator
         * @param rowBuilder
         *
         */
        public DataTableIterator(final Iterator<DataRow> rowIterator, final TrainingRowBuilder<T> rowBuilder) {
            m_rowBuilder = rowBuilder;
            m_rowIterator = rowIterator;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return m_rowIterator.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public T next() {
            return m_rowBuilder.build(m_rowIterator.next(), m_idCounter++);
        }
    }

}
