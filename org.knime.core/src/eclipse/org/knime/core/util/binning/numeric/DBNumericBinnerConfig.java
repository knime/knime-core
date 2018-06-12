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
 * History
 *   May 18, 2018 (Mor Kalla): created
 */
package org.knime.core.util.binning.numeric;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Configuration class for database numeric binner node.
 *
 * @author Mor Kalla
 */
public class DBNumericBinnerConfig {

    /**
     * Config key for numeric columns.
     */
    public static final String CFG_NUMERIC_COLUMNS = "binned_columns";

    /**
     * Config key postfix for appended columns.
     */
    public static final String CFG_IS_APPENDED = "_is_appended";

    private final Map<String, Bin[]> m_columnToBins = new HashMap<>();

    private final Map<String, String> m_columnToAppended = new HashMap<>();

    /**
     * Gets the included columns.
     *
     * @return the columnToBins
     */
    public Map<String, Bin[]> getColumnToBins() {
        return m_columnToBins;
    }

    /**
     * Gets the additional columns.
     *
     * @return the columnToAppended
     */
    public Map<String, String> getColumnToAppended() {
        return m_columnToAppended;
    }

    /**
     * Saves settings into the {@link NodeSettings}.
     *
     * @param settings the {@link NodeSettings} object
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        for (Entry<String, Bin[]> entry : m_columnToBins.entrySet()) {
            final String columnKey = entry.getKey();
            final NodeSettingsWO column = settings.addNodeSettings(columnKey);
            if (m_columnToAppended.get(columnKey) != null) {
                settings.addString(columnKey + CFG_IS_APPENDED, m_columnToAppended.get(columnKey));
            } else {
                settings.addString(columnKey + CFG_IS_APPENDED, null);
            }
            final Bin[] bins = m_columnToBins.get(columnKey);
            for (int b = 0; b < bins.length; b++) {
                final NodeSettingsWO bin = column.addNodeSettings(bins[b].getBinName() + "_" + b);
                bins[b].saveToSettings(bin);
            }
        }
        settings.addStringArray(CFG_NUMERIC_COLUMNS, m_columnToAppended.keySet().toArray(new String[0]));
    }

    /**
     * Validates settings before loading from the {@link NodeSettings}.
     *
     * @param settings the {@link NodeSettings} object
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

        final StringBuilder builder = new StringBuilder();
        final String[] columns = settings.getStringArray(CFG_NUMERIC_COLUMNS, new String[0]);
        if (columns == null) {
            builder.append("Numeric column array can't be 'null'\n");
        } else {
            for (int i = 0; i < columns.length; i++) {
                // appended or replaced
                settings.getString(columns[i] + CFG_IS_APPENDED, null);
                double oldValue = Double.NEGATIVE_INFINITY;
                if (columns[i] == null) {
                    builder.append("Column can't be 'null': ").append(i).append("\n");
                    continue;
                }
                final NodeSettingsRO set = settings.getNodeSettings(columns[i]);
                for (String binKey : set.keySet()) {
                    final NodeSettingsRO bin = set.getNodeSettings(binKey);
                    final NumericBin theBin;
                    try {
                        theBin = new NumericBin(bin);
                    } catch (InvalidSettingsException ise) {
                        builder.append(columns[i]).append(": ").append(ise.getMessage()).append("\n");
                        continue;
                    }
                    final String binName = theBin.getBinName();
                    final double leftValue = theBin.getLeftValue();
                    if (leftValue != oldValue) {
                        builder.append(getMessage(columns[i], binName, leftValue, oldValue));
                    }
                    final double rightValue = theBin.getRightValue();
                    final boolean leftOpen = theBin.isLeftOpen();
                    final boolean rightOpen = theBin.isRightOpen();

                    if (rightValue < leftValue) {
                        builder.append(getMessage(columns[i], binName, leftValue, rightValue));
                    } else if (rightValue == leftValue && !(!leftOpen && !rightOpen)) {
                        builder.append(getMessage(columns[i], binName, leftValue, rightValue));
                    }

                    oldValue = rightValue;
                }
                if (oldValue != Double.POSITIVE_INFINITY) {
                    builder.append(columns[i] + ": check last right interval value=" + oldValue + "\n");
                }
            }
        }

        if (builder.length() > 0) {
            throw new InvalidSettingsException(builder.toString());
        }
    }

    private static String getMessage(final String columnName, final String binName, final double leftValue,
        final double rightValue) {
        return columnName + ": " + binName + " check interval: " + "left=" + leftValue + ",right=" + rightValue + "\n";
    }

    /**
     * Loads validated settings from the {@link NodeSettings}.
     *
     * @param settings the {@link NodeSettings} object
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnToBins.clear();
        m_columnToAppended.clear();
        final String[] columns = settings.getStringArray(CFG_NUMERIC_COLUMNS, new String[0]);
        for (int i = 0; i < columns.length; i++) {
            final NodeSettingsRO column = settings.getNodeSettings(columns[i]);
            final String[] bins = column.keySet().toArray(new String[0]);
            final NumericBin[] binnings = new NumericBin[bins.length];

            for (int j = 0; j < bins.length; j++) {
                final NodeSettingsRO bin = column.getNodeSettings(bins[j]);
                binnings[j] = new NumericBin(bin);
            }

            m_columnToBins.put(columns[i], binnings);
            final String appended = settings.getString(columns[i] + CFG_IS_APPENDED, null);
            m_columnToAppended.put(columns[i], appended);
        }
    }

}
