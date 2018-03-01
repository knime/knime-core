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
 * History
 *   Feb 28, 2018 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.algorithms.outlier;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.base.node.preproc.groupby.GroupKey;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.IntCell.IntCellFactory;

/**
 * Counts the number of members for each column and group combination.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class GroupsMemberCounterPerColumn {

    /** Map storing the number of members for each column respective the different groups. */
    final Map<String, Map<GroupKey, Integer>> m_groupCounts;

    /**
     * Constructor.
     */
    GroupsMemberCounterPerColumn() {
        m_groupCounts = new LinkedHashMap<>();
    }

    /**
     * Tells whether or not the counter is empty.
     *
     * @return <code>True</code> if the counter is empty, <code>False</code> otherwise
     */
    boolean isEmpty() {
        return m_groupCounts.isEmpty();
    }

    /**
     * Returns the set of group keys.
     *
     * @return the set of group keys
     */
    Set<GroupKey> getGroupKeys() {
        return m_groupCounts.values().stream()//
            .map(data -> data.keySet())//
            .flatMap(keys -> keys.stream())//
            .collect(Collectors.toSet());
    }

    /**
     * Increment the member count for the given outlier column - key pair.
     *
     * @param outlierColName the outlier column name
     * @param key the key of the group whose count needs to be incremented
     */
    void incrementMemberCount(final String outlierColName, final GroupKey key) {
        if (!m_groupCounts.containsKey(outlierColName)) {
            m_groupCounts.put(outlierColName, new HashMap<GroupKey, Integer>());
        }
        final Map<GroupKey, Integer> map = m_groupCounts.get(outlierColName);
        // if key not contained initialize by 0
        if (!map.containsKey(key)) {
            map.put(key, 0);
        }
        // increment the value
        map.put(key, map.get(key) + 1);
    }

    /**
     * Returns the member count for the given outlier column - key pair.
     *
     * @param outlierColName the outlier column name
     * @param key the key of the group whose count needs to be returned
     * @return the member count for the given index key pair
     */
    DataCell getCount(final String outlierColName, final GroupKey key) {
        if (m_groupCounts.containsKey(outlierColName)) {
            final Map<GroupKey, Integer> map = m_groupCounts.get(outlierColName);
            if (map.containsKey(key)) {
                return IntCellFactory.create(map.get(key));
            }
        }
        return IntCellFactory.create(0);
    }

}