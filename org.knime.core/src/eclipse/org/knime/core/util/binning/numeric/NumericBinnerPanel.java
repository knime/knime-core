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
 * ------------------------------------------------------------------------
 */
package org.knime.core.util.binning.numeric;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Binner dialog used to group numeric columns (int or double) into intervals.
 *
 * @author Mor Kalla
 * @since 3.6
 */
public class NumericBinnerPanel extends JPanel {

    private static final long serialVersionUID = -8262649169401727101L;

    private static final Dimension DIMENSION_NUMERIC_PANEL = new Dimension(200, 155);

    private static final Dimension DIMENSION_NUM_INTERVAL_PANEL = new Dimension(500, 300);

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NumericBinnerPanel.class);

    private final JList<DataColumnSpec> m_numericColumnList;

    private final DefaultListModel<DataColumnSpec> m_numericColumnModel;

    private final JPanel m_numericIntervalsPanel;

    private final LinkedHashMap<String, IntervalPanel> m_numericIntervals;

    /**
     * Constructs a {@link NumericBinnerPanel}.
     */
    public NumericBinnerPanel() {
        m_numericIntervals = new LinkedHashMap<>();

        // numeric panel in tab
        setLayout(new GridLayout(1, 1));

        // numeric column list
        m_numericColumnModel = new DefaultListModel<>();
        m_numericColumnList = new JList<>(m_numericColumnModel);

        m_numericColumnList.setCellRenderer(new BinnerListCellRenderer(m_numericIntervals));
        m_numericColumnList.addListSelectionListener(e -> {
            columnChanged();
            validate();
            repaint();
        });
        m_numericColumnList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        final JScrollPane numScroll = new JScrollPane(m_numericColumnList);
        numScroll.setMinimumSize(DIMENSION_NUMERIC_PANEL);
        numScroll.setBorder(BorderFactory.createTitledBorder(" Select Column "));

        // numeric column intervals
        m_numericIntervalsPanel = new JPanel(new GridLayout(1, 1));
        m_numericIntervalsPanel.setBorder(BorderFactory.createTitledBorder(" "));
        m_numericIntervalsPanel.setMinimumSize(DIMENSION_NUM_INTERVAL_PANEL);
        m_numericIntervalsPanel.setPreferredSize(DIMENSION_NUM_INTERVAL_PANEL);
        final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, numScroll, m_numericIntervalsPanel);
        add(split);
    }

    private void columnChanged() {
        m_numericIntervalsPanel.removeAll();
        final DataColumnSpec spec = m_numericColumnList.getSelectedValue();
        if (spec == null) {
            m_numericIntervalsPanel.setBorder(BorderFactory.createTitledBorder(" Select Column "));
        } else {
            m_numericIntervalsPanel.setBorder(null);
            m_numericIntervalsPanel.add(createIntervalPanel(spec));
        }
    }

    private IntervalPanel createIntervalPanel(final DataColumnSpec cspec) {
        final String columnName = cspec.getName();
        final IntervalPanel intervalPanel;
        if (m_numericIntervals.containsKey(columnName)) {
            intervalPanel = m_numericIntervals.get(columnName);
        } else {
            intervalPanel =
                new IntervalPanel(columnName, null, m_numericColumnList, cspec.getType(), m_numericIntervalsPanel);
            m_numericIntervals.put(columnName, intervalPanel);
        }
        intervalPanel.validate();
        intervalPanel.repaint();
        return intervalPanel;
    }

    /**
     * Loads settings for dialog.
     *
     * @param settings the settings to load
     * @param specs the input table specs
     * @throws NotConfigurableException if settings are invalid
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {

        final DataTableSpec spec = (DataTableSpec)specs[0];
        m_numericIntervals.clear();
        m_numericColumnModel.removeAllElements();
        storeNumericColumnsToModel(spec);

        final String[] columns = settings.getStringArray(DBNumericBinnerConfig.CFG_NUMERIC_COLUMNS, (String[])null);
        // if numeric columns in settings, select first
        if (columns != null && columns.length > 0) {
            for (String column : columns) {
                if (!spec.containsName(column)) {
                    continue;
                }
                final DataType type = spec.getColumnSpec(column).getType();
                if (!type.isCompatible(DoubleValue.class)) {
                    continue;
                }

                final NodeSettingsRO col;
                try {
                    col = settings.getNodeSettings(column);
                } catch (InvalidSettingsException ise) {
                    LOGGER.warn("NodeSettings not available for column: " + column);
                    continue;
                }

                String appendedColumn = null;
                if (settings.containsKey(column + DBNumericBinnerConfig.CFG_IS_APPENDED)) {
                    appendedColumn = settings.getString(column + DBNumericBinnerConfig.CFG_IS_APPENDED, null);
                }
                final IntervalPanel intervalPanel =
                    new IntervalPanel(column, appendedColumn, m_numericColumnList, type, m_numericIntervalsPanel);
                m_numericIntervals.put(column, intervalPanel);

                final Iterator<String> colIterator = col.iterator();
                while (colIterator.hasNext()) {

                    final String binId = colIterator.next();
                    final NumericBin theBin;

                    try {
                        theBin = new NumericBin(col.getNodeSettings(binId));
                    } catch (InvalidSettingsException ise) {
                        LOGGER.warn("NodeSettings not available for " + "interval bin: " + binId);
                        continue;
                    }

                    intervalPanel.addIntervalItem(new IntervalItemPanel(intervalPanel, theBin.isLeftOpen(),
                        theBin.getLeftValue(), theBin.isRightOpen(), theBin.getRightValue(), theBin.getBinName(), type,
                        m_numericIntervalsPanel));

                }
                final DataColumnSpec cspec = spec.getColumnSpec(column);
                // select column and scroll to position
                m_numericColumnList.setSelectedValue(cspec, true);
            }
        }
        validate();
        repaint();
    }

    private void storeNumericColumnsToModel(final DataTableSpec spec) {
        for (int i = 0; i < spec.getNumColumns(); i++) {
            final DataColumnSpec cspec = spec.getColumnSpec(i);
            if (cspec.getType().isCompatible(DoubleValue.class)) {
                m_numericColumnModel.addElement(cspec);
            }
        }
    }

    /**
     * Saves the settings.
     *
     * @param settings the settings from dialog
     * @throws InvalidSettingsException if settings are invalid
     */
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final LinkedHashSet<String> colList = new LinkedHashSet<>();
        for (Entry<String, IntervalPanel> entry : m_numericIntervals.entrySet()) {
            final String cellKey = entry.getKey();
            final IntervalPanel intervalPanel = entry.getValue();
            // only if at least 1 bin is defined
            if (intervalPanel.getNumericIntervalSize() > 0) {
                colList.add(cellKey);
                final NodeSettingsWO set = settings.addNodeSettings(cellKey);
                if (intervalPanel.isAppendedColumn()) {
                    final String appendedName = intervalPanel.getColumnName();
                    Enumeration<DataColumnSpec> e = m_numericColumnModel.elements();
                    while (e.hasMoreElements()) {
                        final DataColumnSpec cspec = e.nextElement();
                        if (cspec.getName().equals(appendedName)) {
                            throw new InvalidSettingsException(
                                "New appended column " + appendedName + " matches other column.");
                        }
                    }
                    settings.addString(cellKey + DBNumericBinnerConfig.CFG_IS_APPENDED, appendedName);
                } else {
                    settings.addString(cellKey + DBNumericBinnerConfig.CFG_IS_APPENDED, null);
                }
                for (int j = 0; j < intervalPanel.getNumericIntervalSize(); j++) {
                    final IntervalItemPanel item = intervalPanel.getInterval(j);
                    final String binName = item.getBin();
                    if (binName == null || binName.length() == 0) {
                        throw new InvalidSettingsException("Name for bin " + j + " not set: " + item);
                    }
                    final NodeSettingsWO bin = set.addNodeSettings(binName + "_" + j);
                    final NumericBin theBin = new NumericBin(binName, item.isLeftOpen(), item.getLeftValue(false),
                        item.isRightOpen(), item.getRightValue(false));
                    theBin.saveToSettings(bin);
                }
            }
        }
        // add binned columns
        final String[] columns = colList.toArray(new String[0]);
        settings.addStringArray(DBNumericBinnerConfig.CFG_NUMERIC_COLUMNS, columns);
    }
}
