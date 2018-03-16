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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.base.node.preproc.groupby.GroupKey;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;

/**
 * Counts the number of members for each column and group combination.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class MemberCounter {

    /**
     *
     */
    private static final String GROUP_VAL_KEY = "group-val";

    /**
     *
     */
    private static final String GROUP_KEY_KEY = "group-key";

    /**
     *
     */
    private static final String GROUP_COUNT_KEY = "group-count";

    /**
     *
     */
    private static final String GROUP_COUNTS_KEY = "group-counts";

    /**
     *
     */
    private static final String OUT_COL_NAME_KEY = "outlier-column-name";

    private static final String OUT_COL_KEY = "outlier-column_";

    /** Map storing the number of members for each column respective the different groups. */
    final Map<String, Map<GroupKey, Integer>> m_groupCounts;

    /**
     * Constructor.
     */
    MemberCounter() {
        m_groupCounts = new LinkedHashMap<>();
    }

    /**
     * Constructs the member counter by merging various counters.
     *
     * @param array of member counters to be merged
     */
    static MemberCounter merge(final MemberCounter[] counters) {
        final MemberCounter mCounter = new MemberCounter();
        for (final MemberCounter counter : counters) {
            for (Entry<String, Map<GroupKey, Integer>> oEntry : counter.m_groupCounts.entrySet()) {
                for (Entry<GroupKey, Integer> cEntry : oEntry.getValue().entrySet()) {
                    mCounter.incrementMemberCount(oEntry.getKey(), cEntry.getKey(), cEntry.getValue());
                }
            }
        }
        return mCounter;
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
     * Increments the member count for the given outlier column - key pair by one.
     *
     * @param outlierColName the outlier column name
     * @param key the key of the group whose count needs to be incremented
     */
    void incrementMemberCount(final String outlierColName, final GroupKey key) {
        incrementMemberCount(outlierColName, key, 1);
    }

    /**
     * Increments the member count for the given outlier column - key pair by the provided value.
     *
     * @param outlierColName the outlier column name
     * @param key the key of the group whose count needs to be incremented
     * @param increment the increment value
     */
    private void incrementMemberCount(final String outlierColName, final GroupKey key, final int increment) {
        if (!m_groupCounts.containsKey(outlierColName)) {
            m_groupCounts.put(outlierColName, new HashMap<GroupKey, Integer>());
        }
        final Map<GroupKey, Integer> map = m_groupCounts.get(outlierColName);
        // if key not contained initialize by 0
        if (!map.containsKey(key)) {
            map.put(key, 0);
        }
        // increment the value
        map.put(key, map.get(key) + increment);
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

    /**
     * Saves the member counter to the provided model content.
     *
     * @param model the model content to save to
     */
    void saveModel(final ModelContentWO model) {
        int pos = 0;
        for (Entry<String, Map<GroupKey, Integer>> entry : m_groupCounts.entrySet()) {
            final ModelContentWO colSettings = model.addModelContent(OUT_COL_KEY + pos++);
            colSettings.addString(OUT_COL_NAME_KEY, entry.getKey());
            final ModelContentWO groupCounts = colSettings.addModelContent(GROUP_COUNTS_KEY);
            for (Entry<GroupKey, Integer> gCountEntry : entry.getValue().entrySet()) {
                final ModelContentWO groupCount = groupCounts.addModelContent(GROUP_COUNT_KEY);
                groupCount.addDataCellArray(GROUP_KEY_KEY, gCountEntry.getKey().getGroupVals());
                groupCount.addInt(GROUP_VAL_KEY, gCountEntry.getValue());
            }
        }
    }

    /**
     * Load a member counter from the provided model content.
     *
     * @param model the model content
     * @return the proper initialized member counter
     * @throws InvalidSettingsException if the input settings cannot be parsed
     */
    @SuppressWarnings("unchecked")
    static MemberCounter loadInstance(final ModelContentRO model) throws InvalidSettingsException {
        final MemberCounter counter = new MemberCounter();
        final Enumeration<ModelContentRO> colSettings = model.children();
        while (colSettings.hasMoreElements()) {
            final ModelContentRO colSetting = colSettings.nextElement();
            final String outlierColName = colSetting.getString(OUT_COL_NAME_KEY);
            final Enumeration<ModelContentRO> groupCounts = colSetting.getModelContent(GROUP_COUNTS_KEY).children();
            while (groupCounts.hasMoreElements()) {
                final ModelContentRO groupCount = groupCounts.nextElement();
                final GroupKey key = new GroupKey(groupCount.getDataCellArray(GROUP_KEY_KEY));
                final int count = groupCount.getInt(GROUP_VAL_KEY);
                counter.incrementMemberCount(outlierColName, key, count);
            }
        }
        return counter;
    }

}