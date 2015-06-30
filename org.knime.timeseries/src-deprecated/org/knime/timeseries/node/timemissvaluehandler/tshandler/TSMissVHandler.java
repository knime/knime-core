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
 * ------------------------------------------------------------------------
 */
package org.knime.timeseries.node.timemissvaluehandler.tshandler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
/**
 * The abstract class for handling missing values.
 *
 * @author Iris Adae, University of Konstanz
 * @author Marcel Hanser, University of Konstanz
 * @deprecated See new missing node that incorporates time series handling in package
 * org.knime.base.node.preproc.pmml.missingval
 *
 */
@Deprecated
public abstract class TSMissVHandler {

    // this list contains the row keys of the data cells which are missing in the original
    // and are waiting for an correction.
    private List<RowKey> m_waitingMissingsRowkeyList;
    // This list contains the corrected values for missings.
    private Map<RowKey, DataCell> m_detectedMissings;

    /**
     * Constructor.
     */
    public TSMissVHandler() {
           m_detectedMissings = new HashMap<RowKey, DataCell>();
           m_waitingMissingsRowkeyList = new LinkedList<RowKey>();
    }

    /**
     * Gets the DataCell which should be inserted for the missing
     * one in row with RowKey rk.
     *
     * This method is used in the second run, to actually replace the missing
     * cells.
     * @param rk the rowkey
     * @return the DataCell to be replaced, if such a cell was put
     * to the detected list. other wise a missing cell.
     */
    public DataCell getandRemove(final RowKey rk) {
        if (m_detectedMissings.containsKey(rk)) {
            return m_detectedMissings.get(rk);
        }
        return DataType.getMissingCell();
    }

    /**
     * Call this method for each row in the data.
     * If it's missing, or used to replaced missings has to be decided in
     * in the method.
     * @param key the rowkey of the cell
     * @param newCell the current cell.
     */
    public abstract void incomingValue(RowKey key, DataCell newCell);

    /**
     * Closes the handler, to prepare for the inserting step.
     */
    public void close() {
        for (RowKey k : m_waitingMissingsRowkeyList) {
            m_detectedMissings.put(k , DataType.getMissingCell());
        }
    }


    /**
     * @return a sorted list of rowkeys of the waiting cells (needed for the linear aggregation)
     */
    protected List<RowKey> getWaitingList() {
        return m_waitingMissingsRowkeyList;
    }


    /**
     * @param key the RowKey of the new waiting cell
     * @param newCell the waiting cell
     */
    protected void addToWaiting(final RowKey key, final DataCell newCell) {
        m_waitingMissingsRowkeyList.add(key);

    }
    /**
     * Deletes all entries from the waiting list.
     */
    protected void clearWaiting() {
       m_waitingMissingsRowkeyList.clear();

    }

    /**
     * @param key the RowKey of the new waiting cell
     * @param newCell the waiting cell
     */
    protected void addToDetected(final RowKey key, final DataCell newCell) {
        m_detectedMissings.put(key, newCell);

    }
}
