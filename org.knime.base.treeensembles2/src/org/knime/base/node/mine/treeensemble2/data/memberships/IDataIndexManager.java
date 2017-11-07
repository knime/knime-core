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
 *   22.07.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.data.memberships;

import org.knime.base.node.mine.treeensemble2.data.TreeAttributeColumnData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetColumnData;

/**
 * Classes that implement this interface are used in the learning process of random forests. <br>
 * They keep track which position in the original table (and in the {@link TreeTargetColumnData}) corresponds
 * to which position in the individual {@link TreeAttributeColumnData} and vice versa. <br>
 * For each execution of a Learner node there is only one {@link IDataIndexManager}. <br>
 * <br>
 * We use index to refer to columns and positions to refer to positions of records in columns or the original input table.
 *
 *
 * @author Adrian Nembach, KNIME.com
 */
public interface IDataIndexManager {

    /**
     * @param colIndex the index of the column, for which the mapping should be returned
     * @return a mapping from the original positions to the positions in the column with <b>colIndex</b>
     */
    public int[] getPositionsInColumn(final int colIndex);

    /**
     * @param colIndex the index of the column, for which the mapping should be returned
     * @return a mapping form the positions in the column with <b>colIndex</b> to the original positions
     */
    public int[] getOriginalPositions(final int colIndex);

    /**
     * @param colIndex the index of the column we want to map <b>originalIndex</b> to
     * @param originalPosition the original position of a record in the input table
     * @return the index of the record with <b>originalPosition</b> in the column corresponding to <b>colIndex</b>
     */
    public int getPositionInColumn(final int colIndex, final int originalPosition);

    /**
     * @param colIndex the index of the column from which we want to map <b>indexInColumn</b> to the original index in the input table
     * @param positionInColumn the position of a record in the column with index <b>colIndex</b>
     * @return the original position of the record with <b>positionInColumn</b> in the column identified by <b>colIndex</b>
     */
    public int getOriginalPosition(final int colIndex, final int positionInColumn);
}
