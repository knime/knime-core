/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 */

package org.knime.base.node.io.database.groupby.dialog;

import java.util.List;

import javax.swing.table.TableModel;

import org.knime.base.data.aggregation.dialogutil.AggregationFunctionRow;


/**
 * {@link TableModel} that allows the displaying and editing of {@link AggregationFunctionRow}s.
 *
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @param <R> the {@link AggregationFunctionRow} implementation this {@link TableModel} operates with
 * @since 2.11
 */
public interface AggregationTableModel<R extends AggregationFunctionRow<?>> extends TableModel {

    /**
     * Removes all entries from the table.
     */
    public void removeAll();

    /**
     * @param idxs the row indices to remove
     */
    public void remove(int... idxs);

    /**
     * @param rows the {@link AggregationFunctionRow}s to add
     */
    public void add(List<R> rows);

    /**
     * @param idxs the row indices to toggle the missing cell option for
     */
    public void toggleMissingCellOption(int[] idxs);

    /**
     * This method is used to initialize the {@link TableModel} with the
     * initial selected {@link AggregationFunctionRow}s.
     * @param rows initial selected {@link AggregationFunctionRow}s
     */
    public void initialize(final List<R> rows);

    /**
     * @return the index of the missing cell option column or -1 if the table does not contain such a column
     */
    public int getMissingCellOptionColIdx();

    /**
     * @return the index of the aggregation operator settings button column
     */
    public int getSettingsButtonColIdx();


    /**
     * @param row the index of the row
     * @return the row with the given index
     */
    public R getRow(final int row);

    /**
     * @return <code>true</code> if one of the rows contains a row that requires additional settings
     */
    public boolean containsRowWithSettings();
}
