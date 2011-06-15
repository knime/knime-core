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

import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.def.StringCell;

/**
 * This class is a container for a DataRow and an index. The index will be used
 * to define an order on the InputDataRow.
 *
 * @author Heiko Hofer
 */
class InputDataRow implements Comparable<InputDataRow> {

    private DataRow m_row;

    private Settings.InDataPort m_port;

    private int m_index;

    private Settings m_settings;

    /**
     * @param row A DataRow
     * @param index The index of row
     * @param port The DataPort of the row, either Left or Right
     * @param settings The settings common for all InputDataRow Objects.
     */
    InputDataRow(final DataRow row, final int index,
            final Settings.InDataPort port, final Settings settings) {
        m_row = row;
        m_port = port;
        m_index = index;
        m_settings = settings;
    }

    /**
     * @return the index
     */
    int getIndex() {
        return m_index;
    }

    /**
     * @return the JoinTuples of this row.
     */
    JoinTuple[] getJoinTuples() {
        List<Integer> indices = null;
        indices = m_settings.getJoiningIndices(m_port);
        if (!m_settings.getMultipleMatchCanOccur()) {
            int numJoinAttributes = indices.size();
            DataCell[] cells = new DataCell[numJoinAttributes];
            for (int i = 0; i < numJoinAttributes; i++) {
                int index = indices.get(i);
                if (index >= 0) {
                    cells[i] = m_row.getCell(index);
                } else {
                    // create a StringCell since row IDs may match
                    // StringCell's
                    cells[i] = new StringCell(m_row.getKey().getString());
                }
            }
            return new JoinTuple[]{new JoinTuple(cells)};
        } else {
            int numJoinAttributes = indices.size();
            JoinTuple[] joinCells = new JoinTuple[numJoinAttributes];

            for (int i = 0; i < numJoinAttributes; i++) {
                int index = indices.get(i);
                DataCell[] cells = new DataCell[numJoinAttributes];
                for (int k = 0; k < numJoinAttributes; k++) {
                    cells[k] = new WildCardCell();
                }
                if (index >= 0) {
                    cells[i] = m_row.getCell(index);
                } else {
                    // create a StringCell since row IDs may match
                    // StringCell's
                    cells[i] = new StringCell(m_row.getKey().getString());
                }
                joinCells[i] = new JoinTuple(cells);
            }
            return joinCells;
        }
    }


    /**
     * {@inheritDoc}
     */
    public int compareTo(final InputDataRow that) {
        return this.m_index - that.m_index;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_port.toString() + ": (" + Integer.toString(m_index) + ") | "
                + m_row.toString();
    }

    /**
     * Common settings for all InputDataRows.
     *
     * @author Heiko Hofer
     */
    static class Settings {
        /**
         * A InputDataRow belongs either to the left input table or to the right
         * input table.
         *
         * @author Heiko Hofer
         */
        enum InDataPort {
            /** DataRow belongs to the left input table. */
            Left,
            /** DataRow belongs to the right input table. */
            Right;
        }


        private Map<InDataPort, List<Integer>> m_joiningIndices;

        private boolean m_multipleMatchCanOccur;


        /**
         * @param joiningIndices The joining indices of the input tables
         * @param multipleMatchCanOccur Whether rows can match more often than
         *            one
         */
        Settings(
                final Map<InDataPort, List<Integer>> joiningIndices,
                final boolean multipleMatchCanOccur) {
            m_joiningIndices = joiningIndices;
            m_multipleMatchCanOccur = multipleMatchCanOccur;
        }


        /**
         * @param port {@link InDataPort} either left or right.
         * @return the joining indices for the given port.
         */
        List<Integer> getJoiningIndices(final InDataPort port) {
            return m_joiningIndices.get(port);
        }

        /**
         * @return the multipleMatchCanOccur
         */
        boolean getMultipleMatchCanOccur() {
            return m_multipleMatchCanOccur;
        }
    }

   /**
    * Used in the match any case.
    * @author Heiko Hofer
    */
   static class WildCardCell extends DataCell {
       /**
        * {@inheritDoc}
        */
       @Override
       public int hashCode() {
           return 0;
       }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            throw new UnsupportedOperationException(
                    "This method should never be called.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean equalsDataCell(final DataCell dc) {
            throw new UnsupportedOperationException(
                    "This method should never be called.");
        }
   }
}
