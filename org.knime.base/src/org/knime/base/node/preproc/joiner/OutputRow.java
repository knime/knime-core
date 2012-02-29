/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   01.12.2009 (Heiko Hofer): created
 */
package org.knime.base.node.preproc.joiner;

import java.util.Comparator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;



/**
 * Stores information of a inner or outer join in a DataRow. Stored are the
 * cells of the right row, its rowkey and the indices of the left and the
 * right row. This class provides static method to retrieve the stored data.
 *
 * @author Heiko Hofer
 */
final class OutputRow {

    private OutputRow() {
        // private constructor to prevent instantiation.
    }

    /**
     * Create a {@link DataRow} that stores information of a inner or
     * outer joins.
     *
     * @param index The index of this row.
     * @param leftIndex The index of the left row.
     * @param rightIndex The index of the right row.
     * @param rightDataRow The right row.
     * @param settings The common settings object.
     * @return New instance of {@link DataRow} populated with the given
     * information.
     */
    static DataRow createDataRow(final int index,
            final int leftIndex, final int rightIndex,
            final DataRow rightDataRow,
            final OutputRow.Settings settings) {

        int[] survivors = settings.getSurvivors();

        DataCell[] cells =
                new DataCell[survivors.length + 3];
        int c = 0;
        for (int i = 0; i < survivors.length; i++) {
            cells[c] = rightDataRow.getCell(survivors[i]);
            c++;
        }
        cells[c] = new StringCell(rightDataRow.getKey().getString());
        c++;
        cells[c] = new IntCell(leftIndex);
        c++;
        cells[c] = new IntCell(rightIndex);
        RowKey rowID = new RowKey(Integer.toString(index));
        return  new DefaultRow(rowID, cells);
    }

    /**
     * Create a {@link DataRow} that stores information of a right outer join.
     *
     * @param index The index of this row.
     * @param leftIndex The index of the left row.
     * @param rightIndex The index of the right row.
     * @param settings The common settings object.
     * @return New instance of {@link DataRow} populated with the given
     * information.
     */
    static DataRow createDataRow(final int index,
            final int leftIndex, final int rightIndex,
            final OutputRow.Settings settings) {

        int[] survivors = settings.getSurvivors();

        DataCell[] cells =
                new DataCell[survivors.length + 3];
        int c = 0;
        for (int i = 0; i < survivors.length; i++) {
            cells[c] = DataType.getMissingCell();
            c++;
        }
        cells[c] = DataType.getMissingCell();
        c++;
        cells[c] = new IntCell(leftIndex);
        c++;
        cells[c] = new IntCell(rightIndex);
        RowKey rowID = new RowKey(Integer.toString(index));
        return new DefaultRow(rowID, cells);
    }


    /**
     * @param row A {@link DataRow} created by the factory methods of this
     * class.
     * @return The index of the left row.
     */
    static int getLeftIndex(final DataRow row) {
        IntCell cell = (IntCell)row.getCell(row.getNumCells() - 2);
        return cell.getIntValue();
    }

    /**
     * @param row A {@link DataRow} created by the factory methods of this
     * class.
     * @return The index of the right row.
     */
    static int getRightIndex(final DataRow row) {
        IntCell cell = (IntCell)row.getCell(row.getNumCells() - 1);
        return cell.getIntValue();
    }

    /**
     * @param row A {@link DataRow} created by the factory methods of this
     * class.
     * @return the row ID of the right row or null if missing (left outer join)
     */
    static RowKey getRightKey(final DataRow row) {
        DataCell cell = row.getCell(row.getNumCells() - 3);
        if (!cell.isMissing()) {
            return new RowKey(((StringCell)cell).getStringValue());
        } else {
            return null;
        }
    }

    /**
     * Creates a comparator which can be used to order {@link DataRow}s
     * created by the factory methods of this class.
     * @return The comparator.
     */
    static Comparator<DataRow> createRowComparator() {
        return new Comparator<DataRow>() {
            @Override
            public int compare(final DataRow o1, final DataRow o2) {
                int diff = OutputRow.getLeftIndex(o1)
                    - OutputRow.getLeftIndex(o2);
                if (diff != 0) {
                    return diff;
                } else {
                    return OutputRow.getRightIndex(o1)
                            - OutputRow.getRightIndex(o2);
                }
            }
        };
    }

    /**
     * A class that stores the common settings for all {@link OutputRow}s.
     *
     * @author Heiko Hofer
     */
    static class Settings {
        private DataTableSpec m_spec;
        private int[] m_rightTableSurvivors;

        /**
         * @param rightTableSpec The spec of the right table.
         * @param rightTableSurvivors The survivors of the right table.
         */
        public Settings(
                final DataTableSpec rightTableSpec,
                final int[] rightTableSurvivors) {
            m_rightTableSurvivors = rightTableSurvivors;

            DataColumnSpec[] cspec =
                new DataColumnSpec[rightTableSurvivors.length + 3];
            Integer c = 0;
            for (int i = 0; i < rightTableSurvivors.length; i++) {
                cspec[c] = new DataColumnSpecCreator(c.toString(),
                        rightTableSpec.getColumnSpec(
                        rightTableSurvivors[i]).getType()).createSpec();
                c++;
            }
            cspec[c] = new DataColumnSpecCreator(c.toString(),
                    StringCell.TYPE).createSpec();
            c++;
            cspec[c] = new DataColumnSpecCreator(c.toString(),
                    IntCell.TYPE).createSpec();
            c++;
            cspec[c] = new DataColumnSpecCreator(c.toString(),
                    IntCell.TYPE).createSpec();

            m_spec = new DataTableSpec(cspec);
        }

        /**
         * @return The survivors of the right table.
         */
        public int[] getSurvivors() {
            return m_rightTableSurvivors;
        }

        /**
         * The spec of an {@link OutputRow}.
         *
         * @return the spec
         */
        public DataTableSpec getSpec() {
            return m_spec;
        }
    }


}
