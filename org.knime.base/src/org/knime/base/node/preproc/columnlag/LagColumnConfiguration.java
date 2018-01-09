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
 * ---------------------------------------------------------------------
 *
 * Created on Mar 17, 2013 by wiswedel
 */
package org.knime.base.node.preproc.columnlag;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Config proxy of node.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class LagColumnConfiguration {

    private String m_column;
    private int m_lagInterval = 1;
    private int m_lag = 1;
    private boolean m_skipInitialIncompleteRows;
    private boolean m_skipLastIncompleteRows;

    /**
     * @return the column
     */
    String getColumn() {
        return m_column;
    }
    /**
     * @param column the column to set
     */
    void setColumn(final String column) {
        m_column = column;
    }
    /**
     * @return the lag interval
     */
    int getLagInterval() {
        return m_lagInterval;
    }
    /**
     * @param lagInterval the lag interval to set
     * @throws InvalidSettingsException If argument <=0.
     */
    void setLagInterval(final int lagInterval) throws InvalidSettingsException {
        if (lagInterval <= 0) {
            throw new InvalidSettingsException("Lag interval must be greater than 0: " + lagInterval);
        }
        m_lagInterval = lagInterval;
    }
    /**
     * @return the lag
     */
    int getLag() {
        return m_lag;
    }
    /**
     * @param lag the lag to set
     * @throws InvalidSettingsException If lag <=0.
     */
    void setLag(final int lag) throws InvalidSettingsException {
        if (lag <= 0) {
            throw new InvalidSettingsException("Lag must be greater than 0: " + lag);
        }
        m_lag = lag;
    }
    /**
     * @return the skipInitialIncompleteRows
     */
    boolean isSkipInitialIncompleteRows() {
        return m_skipInitialIncompleteRows;
    }
    /**
     * @param value the skipInitialIncompleteRows to set
     */
    void setSkipInitialIncompleteRows(final boolean value) {
        m_skipInitialIncompleteRows = value;
    }
    /**
     * @return the skipLastIncompleteRows
     */
    boolean isSkipLastIncompleteRows() {
        return m_skipLastIncompleteRows;
    }
    /**
     * @param value the skipLastIncompleteRows to set
     */
    void setSkipLastIncompleteRows(final boolean value) {
        m_skipLastIncompleteRows = value;
    }

    /** Called from model's load and validate settings method.
     * @param settings Arg settings.
     * @throws InvalidSettingsException If incomplete
     */
    void loadSettingsInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_column = settings.getString("column");
        setLag(settings.getInt("lag"));
        setLagInterval(settings.getInt("lag_interval")); // range check
        m_skipInitialIncompleteRows = settings.getBoolean("skipInitialIncompleteRows");
        m_skipLastIncompleteRows = settings.getBoolean("skipLastIncompleteRows");
    }

    /** Called from dialog's load method.
     * @param settings Arg settings.
     * @param spec Input table spec
     */
    void loadSettingsInDialog(final NodeSettingsRO settings, final DataTableSpec spec) {
        String defColumn = null;
        boolean isTimeColumnAssigned = false;
        // take last column as default, favor time value columns if present
        for (DataColumnSpec s : spec) {
            if (s.getType().isCompatible(DateAndTimeValue.class)) {
                isTimeColumnAssigned = true;
                defColumn = s.getName();
            } else if (isTimeColumnAssigned) {
                // don't reassign
            } else {
                defColumn = s.getName();
            }
        }
        m_column = settings.getString("column", defColumn);
        m_lag = Math.max(1, settings.getInt("lag", 1));
        m_lagInterval = Math.max(1, settings.getInt("lag_interval", 1));
        m_skipInitialIncompleteRows = settings.getBoolean("skipInitialIncompleteRows", false);
        m_skipLastIncompleteRows = settings.getBoolean("skipLastIncompleteRows", true);
    }

    /** Called from dialog's and model's save method.
     * @param settings Arg settings.
     */
    void saveSettings(final NodeSettingsWO settings) {
        settings.addString("column", m_column);
        settings.addInt("lag", m_lag);
        settings.addInt("lag_interval", m_lagInterval);
        settings.addBoolean("skipInitialIncompleteRows", m_skipInitialIncompleteRows);
        settings.addBoolean("skipLastIncompleteRows", m_skipLastIncompleteRows);
    }
}
