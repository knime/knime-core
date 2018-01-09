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
 * ------------------------------------------------------------------------
 *
 * History
 *   Dec 28, 2011 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.RowKey;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class TreeNominalColumnDataCreator implements TreeAttributeColumnDataCreator {

    private final DataColumnSpec m_columnSpec;

    private final Map<String, NominalValueRepresentation> m_string2NomValRepMap;

    private final List<NominalTuple> m_tuples;

    private final List<Integer> m_indicesInColumnMissings;

    private int m_numInstances = 0;

    TreeNominalColumnDataCreator(final DataColumnSpec colSpec) {
        m_columnSpec = colSpec;
        m_string2NomValRepMap = new HashMap<String, NominalValueRepresentation>();
        m_tuples = new ArrayList<NominalTuple>();
        m_indicesInColumnMissings = new ArrayList<Integer>();
    }

    /** {@inheritDoc} */
    @Override
    public boolean acceptsMissing() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void add(final RowKey rowKey, final DataCell cell) {
        if (cell.isMissing()) {
            //            throw new IllegalStateException("Missing vals not supported yet.");
            m_indicesInColumnMissings.add(m_numInstances++);
        } else {
            String str = cell.toString();
            int assignedValue;
            NominalValueRepresentation rep = m_string2NomValRepMap.get(str);
            if (rep == null) {
                assignedValue = m_string2NomValRepMap.size();
                m_string2NomValRepMap.put(str, new NominalValueRepresentation(str, assignedValue, 1.0));
            } else {
                assignedValue = rep.getAssignedInteger();
                rep.addToFrequency(1.0);
            }
            NominalTuple t = new NominalTuple();
            t.m_value = assignedValue;
            t.m_indexInOriginal = m_numInstances++;
            m_tuples.add(t);
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getNrAttributes() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override
    public TreeNominalColumnData createColumnData(final int attributeIndex,
        final TreeEnsembleLearnerConfiguration configuration) {
        assert attributeIndex == 0;

        final boolean containsMissingValues = !m_indicesInColumnMissings.isEmpty();

        final NominalTuple[] tuples = m_tuples.toArray(new NominalTuple[m_tuples.size()]);
        Arrays.sort(tuples);
        final int nomValListLength =
            containsMissingValues ? m_string2NomValRepMap.size() + 1 : m_string2NomValRepMap.size();
        NominalValueRepresentation[] nominalValueList = new NominalValueRepresentation[nomValListLength];
        for (NominalValueRepresentation e : m_string2NomValRepMap.values()) {
            int assignedInteger = e.getAssignedInteger();
            nominalValueList[assignedInteger] = e;
        }
        if (containsMissingValues) {
            int assignedValue = m_string2NomValRepMap.size();
            NominalValueRepresentation missingValueRepresentation = new NominalValueRepresentation(
                NominalValueRepresentation.MISSING_VALUE, assignedValue, m_indicesInColumnMissings.size());
            nominalValueList[nomValListLength - 1] = missingValueRepresentation;
        }
        assert!Arrays.asList(nominalValueList).contains(null);
        final int[] valueCounts = new int[nominalValueList.length];
        final int nonMissingNum = tuples.length;
        final int totalNum = nonMissingNum + m_indicesInColumnMissings.size();
        final int[] originalIndexInColumnList = new int[totalNum];
        for (int i = 0; i < nonMissingNum; i++) {
            NominalTuple t = tuples[i];
            valueCounts[t.m_value] += 1;
            originalIndexInColumnList[i] = t.m_indexInOriginal;
        }
        if (containsMissingValues) {
            valueCounts[valueCounts.length - 1] = m_indicesInColumnMissings.size();
            for (int i = nonMissingNum; i < totalNum; i++) {
                originalIndexInColumnList[i] = m_indicesInColumnMissings.get(i - nonMissingNum);
            }
        }
        TreeNominalColumnMetaData metaData = new TreeNominalColumnMetaData(m_columnSpec.getName(), nominalValueList);
        return new TreeNominalColumnData(metaData, configuration, valueCounts, originalIndexInColumnList,
            containsMissingValues);
    }

    private static class NominalTuple implements Comparable<NominalTuple> {
        private int m_value;

        private int m_indexInOriginal;

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + m_indexInOriginal;
            result = prime * result + m_value;
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            NominalTuple other = (NominalTuple)obj;
            if (m_indexInOriginal != other.m_indexInOriginal) {
                return false;
            }
            if (m_value != other.m_value) {
                return false;
            }
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(final NominalTuple o) {
            int diff = m_value - o.m_value;
            if (diff == 0) {
                return m_indexInOriginal - o.m_indexInOriginal;
            }
            return diff;
        }
    }
}
