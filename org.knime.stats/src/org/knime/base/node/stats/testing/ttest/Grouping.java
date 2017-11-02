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
 * ------------------------------------------------------------------------
 *
 * History
 *   26.06.2012 (hofer): created
 */
package org.knime.base.node.stats.testing.ttest;

import java.util.Map;

import org.knime.core.data.DataCell;


/**
 *
 * @author Heiko Hofer
 */
public abstract class Grouping {

    /**
     * Defines two groups.
     *
     * @author Heiko Hofer
     */
    public enum Group {
        /** The first group */
        GroupX,
        /** The second group */
        GroupY
    }

    private String m_column;

    /**
     * A grouping that is based on the values of the given column.
     * @param column the grouping column
     */
    public Grouping(final String column) {
        super();
        m_column = column;
    }

    /**
     * Return the grouping column.
     * @return the grouping column
     */
    public String getColumn() {
        return m_column;
    }

    /**
     * Get the group for the given value. The given cell is supposed to be
     * from the grouping column.
     * @param cell the cell from the grouping column
     * @return the group for the given cell
     */
    public abstract Group getGroup(final DataCell cell);

    /**
     * Get labels for each group.
     * @return the group lables
     */
    public abstract Map<Group, String> getGroupLabels();


}
