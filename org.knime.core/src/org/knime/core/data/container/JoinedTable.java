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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 * 
 * History
 *   Feb 15, 2007 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;

/**
 * Class that realizes a join table of two {@link BufferedDataTable} arguments.
 * <p><b>This class is not intended to be used in any node implementation, it
 * is public only because some KNIME framework classes access it.</b>
 * <p>This class is used to represent the {@link BufferedDataTable} that is
 * returned by the {@link org.knime.core.node.ExecutionContext}s 
 * {@link org.knime.core.node.ExecutionContext#createJoinedTable(
 * BufferedDataTable, BufferedDataTable, ExecutionMonitor)}
 * method.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class JoinedTable implements KnowsRowCountTable {
    
    private final BufferedDataTable m_leftTable;
    private final BufferedDataTable m_rightTable;
    private final DataTableSpec m_spec;
    /** We use the {@link JoinTableIterator}, which needs a map and flags
     * arguments. */
    private final int[] m_map;
    private final boolean[] m_flags;
    
    /**
     * Creates new object. No checks are done.
     * @param left The left table.
     * @param right The right table.
     * @param spec The proper spec.
     */
    private JoinedTable(final BufferedDataTable left, 
            final BufferedDataTable right, final DataTableSpec spec) {
        m_leftTable = left;
        m_rightTable = right;
        final int colsLeft = m_leftTable.getDataTableSpec().getNumColumns();
        final int colsRight = m_rightTable.getDataTableSpec().getNumColumns();
        m_map = new int[colsLeft + colsRight];
        m_flags = new boolean[colsLeft + colsRight];
        for (int i = 0; i < colsLeft; i++) {
            m_map[i] = i;
            m_flags[i] = true;
        }
        for (int i = 0; i < colsRight; i++) {
            m_map[i + colsLeft] = i;
            m_flags[i + colsLeft] = false;
        }
        m_spec = spec;
    }

    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }
    
    /**
     * {@inheritDoc}
     */
    public CloseableRowIterator iterator() {
        return new JoinTableIterator(m_leftTable.iterator(), 
                m_rightTable.iterator(), m_map, m_flags);
    }
    
    /**
     * Does nothing. 
     * {@inheritDoc}
     */
    public void clear() {
        // left empty, it's up to the node to clear our underlying tables.
    }
    
    /** Internal use. 
     * {@inheritDoc} */
    public void ensureOpen() {
        Node.invokeEnsureOpen(m_leftTable);
        Node.invokeEnsureOpen(m_rightTable);
    }

    /**
     * {@inheritDoc}
     */
    public BufferedDataTable[] getReferenceTables() {
        return new BufferedDataTable[]{m_leftTable, m_rightTable};
    }

    /**
     * {@inheritDoc}
     */
    public int getRowCount() {
        return m_leftTable.getRowCount();
    }

    /**
     * {@inheritDoc}
     */
    public void putIntoTableRepository(
            final HashMap<Integer, ContainerTable> rep) {
    }

    /**
     * {@inheritDoc}
     */
    public void removeFromTableRepository(
            final HashMap<Integer, ContainerTable> rep) {
    }
    
    private static final String CFG_INTERNAL_META = "meta_internal";
    private static final String CFG_LEFT_TABLE_ID = "leftTableID";
    private static final String CFG_RIGHT_TABLE_ID = "rightTableID";

    /**
     * {@inheritDoc}
     */
    public void saveToFile(final File f, final NodeSettingsWO settings,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        NodeSettingsWO internals = settings.addNodeSettings(CFG_INTERNAL_META);
        internals.addInt(CFG_LEFT_TABLE_ID, m_leftTable.getBufferedTableId());
        internals.addInt(CFG_RIGHT_TABLE_ID, m_rightTable.getBufferedTableId());
    }
    
    /** Method being called when the workflow is restored and the table shall
     * recreated.
     * @param s The settings object, contains tables ids.
     * @param spec The final spec.
     * @param tblRep The table repository
     * @return The restored table.
     * @throws InvalidSettingsException If the settings can't be read.
     */
    public static JoinedTable load(final NodeSettingsRO s, 
            final DataTableSpec spec, 
            final Map<Integer, BufferedDataTable> tblRep) 
        throws InvalidSettingsException {
        NodeSettingsRO subSettings = s.getNodeSettings(CFG_INTERNAL_META);
        int leftID = subSettings.getInt(CFG_LEFT_TABLE_ID);
        int rightID = subSettings.getInt(CFG_RIGHT_TABLE_ID);
        BufferedDataTable leftTable = 
            BufferedDataTable.getDataTable(tblRep, leftID);
        BufferedDataTable rightTable = 
            BufferedDataTable.getDataTable(tblRep, rightID);
        return new JoinedTable(leftTable, rightTable, spec);
    }
    
    /**
     * Creates new join table, does the sanity checks. Called from the 
     * {@link org.knime.core.node.ExecutionContext#createJoinedTable(
     * BufferedDataTable, BufferedDataTable, ExecutionMonitor)} method.
     * @param left The left table.
     * @param right The right table.
     * @param prog For progress/cancel.
     * @return A joined table.
     * @throws CanceledExecutionException When canceled.
     * @throws IllegalArgumentException If row keys don't match or there are
     * duplicate columns.
     */
    public static JoinedTable create(final BufferedDataTable left,
            final BufferedDataTable right, final ExecutionMonitor prog)
            throws CanceledExecutionException {
        if (left.getRowCount() != right.getRowCount()) {
            throw new IllegalArgumentException("Tables can't be joined, non "
                    + "matching row counts: " + left.getRowCount() + " vs. "
                    + right.getRowCount());
        }
        // throws exception when duplicates encountered.
        DataTableSpec joinSpec = new DataTableSpec(
                left.getDataTableSpec(), right.getDataTableSpec());
        // check if rows come in same order
        RowIterator leftIt = left.iterator();
        RowIterator rightIt = right.iterator();
        int rowIndex = 0;
        while (leftIt.hasNext()) {
            prog.checkCanceled();
            RowKey leftKey = leftIt.next().getKey();
            RowKey rightKey = rightIt.next().getKey();
            if (!leftKey.equals(rightKey)) {
                throw new IllegalArgumentException(
                        "Tables contain non-matching rows or are sorted "
                        + "differently, keys in row " + rowIndex 
                        + " do not match: \"" + leftKey 
                        + "\" vs. \"" + rightKey + "\"");
            }
            rowIndex++;
        }
        return new JoinedTable(left, right, joinSpec);
    }


}
