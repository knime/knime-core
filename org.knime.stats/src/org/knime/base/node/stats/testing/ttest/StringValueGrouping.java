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
 *   26.06.2012 (hofer): created
 */
package org.knime.base.node.stats.testing.ttest;

import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.StringValue;


/**
 * An implementation of grouping where values of GroupX are of the same value.
 *
 * @author Heiko Hofer
 */
public class StringValueGrouping extends Grouping {
    private Map<Group, String> m_groups;

    /**
     * @param column the grouping column
     * @param groups the groups mapped to the value
     */
    public StringValueGrouping(final String column,
            final Map<Group, String> groups) {
        super(column);
        m_groups = groups;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Group getGroup(final DataCell cell) {
        if (cell.isMissing()) {
            return null;
        }
        for (Group group : m_groups.keySet()) {
            String label = m_groups.get(group);
            String value = cell.getType().isCompatible(StringValue.class)
                ? ((StringValue)cell).getStringValue() : cell.toString();
            if (label.equals(value)) {
                return group;
            }
        }
        // cell is not in a group
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Group, String> getGroupLabels() {
        return m_groups;
    }

}
