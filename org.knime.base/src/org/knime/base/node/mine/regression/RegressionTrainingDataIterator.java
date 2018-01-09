/*
 * ------------------------------------------------------------------------
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * Created on 04.11.2013 by hofer
 */
package org.knime.base.node.mine.regression;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;

/**
 * This is a decorator for a iterator over DataRows.
 *
 * @author Heiko Hofer
 * @since 2.9
 */
public  class RegressionTrainingDataIterator implements Iterator<RegressionTrainingRow> {
    private Iterator<DataRow> m_iter;

    private int m_target;
    private int m_parameterCount;
    private List<Integer> m_learningCols;
    private Map<Integer, Boolean> m_isNominal;
    private Map<Integer, List<DataCell>> m_domainValues;
    /** If true an exception is thrown when a missing cell is observed. */
    private boolean m_failOnMissing;

    private Map<? extends Integer, ? extends Integer> m_vectorLengths;

    /**
     * @param iter the underlying iterator
     * @param parameterCount number of parameters which will be generated
     * from the learning columns
     * @param learningCols indices of the learning columns
     * @param isNominal whether a learning column is nominal
     * @param domainValues the domain values of the nominal learning columns
     * @param target the index of the target value
     * @param failOnMissing when true an exception is thrown when a missing cell is observed
     */
    public RegressionTrainingDataIterator(final Iterator<DataRow> iter,
            final int target,
            final int parameterCount,
            final List<Integer> learningCols,
            final Map<Integer, Boolean> isNominal,
            final Map<Integer, List<DataCell>> domainValues,
            final boolean failOnMissing) {
        this(iter, target, parameterCount, learningCols, isNominal, domainValues, Collections.emptyMap(), failOnMissing);
    }
    /**
     * @param iter the underlying iterator
     * @param parameterCount number of parameters which will be generated
     * from the learning columns
     * @param learningCols indices of the learning columns
     * @param isNominal whether a learning column is nominal
     * @param domainValues the domain values of the nominal learning columns
     * @param target the index of the target value
     * @param vectorLengths the vector (maximal) lengths for the vector columns.
     * @param failOnMissing when true an exception is thrown when a missing cell is observed
     * @since 3.1
     */
    public RegressionTrainingDataIterator(final Iterator<DataRow> iter,
        final int target,
        final int parameterCount,
        final List<Integer> learningCols,
        final Map<Integer, Boolean> isNominal,
        final Map<Integer, List<DataCell>> domainValues,
        final Map<? extends Integer, ? extends Integer> vectorLengths, final boolean failOnMissing) {
        m_iter = iter;
        m_target = target;
        m_parameterCount = parameterCount;
        m_learningCols = learningCols;
        m_isNominal = isNominal;
        m_domainValues = domainValues;
        m_vectorLengths = Collections.unmodifiableMap(new HashMap<>(vectorLengths));
        m_failOnMissing = failOnMissing;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return m_iter.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RegressionTrainingRow next() {
        return new RegressionTrainingRow(m_iter.next(), m_target,
                m_parameterCount, m_learningCols,
                m_isNominal, m_domainValues, m_vectorLengths, m_failOnMissing);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
