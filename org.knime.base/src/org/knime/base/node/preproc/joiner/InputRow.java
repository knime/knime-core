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

import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.def.StringCell;

/**
 * This class is a container for a DataRow and an index. The index will be used
 * to define an order on the InputRow.
 *
 * @author Heiko Hofer
 */
class InputRow implements Comparable<InputRow> {

    private DataRow m_row;

    private Settings.InDataPort m_port;

    private int m_index;

    private Settings m_settings;

    /**
     * @param row A DataRow
     * @param index The index of row
     * @param port The DataPort of the row, either Left or Right
     * @param settings The settings common for all InputRow Objects.
     */
    InputRow(final DataRow row, final int index,
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
     * Create the join tuples for this input row. Note, more than one
     * join tuple is returned in the match any case only. In this case a
     * pairwise comparison will always be false, so that an insertion in a
     * HashSet will always lead to as many new entries as there are columns
     * in the tuple.
     *
     * @return the JoinTuples of this row.
     */
    JoinTuple[] getJoinTuples() {
        List<Integer> indices = null;
        indices = m_settings.getJoiningIndices(m_port);
        if (!m_settings.getMatchAny()) {
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
                    cells[k] = WildCardCell.getDefault();
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
    @Override
    public int compareTo(final InputRow that) {
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
         * A InputRow belongs either to the left input table or to the right
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

        private boolean m_matchAny;

        /**
         * @param joiningIndices The joining indices of the input tables
         * @param matchAny Whether rows can match more often than one time
         */
        Settings(final Map<InDataPort, List<Integer>> joiningIndices,
                final boolean matchAny) {
            m_joiningIndices = joiningIndices;
            m_matchAny = matchAny;
        }

        /**
         * @param port {@link InDataPort} either left or right.
         * @return the joining indices for the given port.
         */
        List<Integer> getJoiningIndices(final InDataPort port) {
            return m_joiningIndices.get(port);
        }

        /**
         * @return the matchAny property
         */
        boolean getMatchAny() {
            return m_matchAny;
        }
    }

    /**
     * Used in the match any case.
     *
     * @author Heiko Hofer
     */
    @SuppressWarnings("serial")
    static final class WildCardCell extends DataCell {
        private static final WildCardCell INSTANCE = new WildCardCell();

        /**
         * prevent the creation of instances.
         */
        private WildCardCell() {
            // do nothing;
        }

        /**
         * Get the singleton value.
         * @return the singleton value.
         */
        public static WildCardCell getDefault() {
            return INSTANCE;
        }

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
            return "<wildcard>";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean equalsDataCell(final DataCell dc) {
            // singleton
            return this == dc;
        }

        /** Serialization method, throws exception as it's not to be used.
         * @param out ignored */
        private void writeObject(final ObjectOutputStream out) {
            throw new IllegalStateException("WildcardCell not "
                    + "supposed to be serialized");
        }

    }
}
