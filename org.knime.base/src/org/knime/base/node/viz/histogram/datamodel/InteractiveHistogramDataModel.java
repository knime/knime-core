/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *    26.02.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * This data model holds all information (DataRows, DataTableSpec) to provide
 * the flexibility.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveHistogramDataModel implements Iterable<DataRow> {

    private static final String CFG_DATA_FILE = "dataFile.xml.gz";
    private static final String CFG_SETTING_FILE = "settingFile.xml.gz";
    private static final String CFG_SETTING = "interactiveHistogramDataModel";
    private static final String CFG_COLOR_COLS = "rowColors";
    private static final String CFG_ROW_COLOR_COUNTER = "rowColorCounter";
    private static final String CFG_ROW_COLOR = "rowColor_";

    private final DataArray m_data;

    private final List<Color> m_rowColors;

    /**Constructor for class InteractiveHistogramDataModel.
     * @param array the data array
     * @param rowColors the row colors
     */
    public InteractiveHistogramDataModel(final DataArray array,
            final List<Color> rowColors) {
        m_data = array;
        m_rowColors = rowColors;
    }

    /**
     * @param dataDir the data directory to write to
     * @param exec the {@link ExecutionMonitor}
     * @throws IOException if the file can't be created
     * @throws CanceledExecutionException if the process was canceled
     */
    public void save2File(final File dataDir, final ExecutionMonitor exec)
    throws IOException, CanceledExecutionException {
        final File settingFile = new File(dataDir, CFG_SETTING_FILE);
        final FileOutputStream os = new FileOutputStream(settingFile);
        final GZIPOutputStream dataOS = new GZIPOutputStream(os);
        final Config config = new NodeSettings(CFG_SETTING);
        final List<Color> rowColors = getRowColors();
        final ConfigWO colorColsConf = config.addConfig(CFG_COLOR_COLS);
        colorColsConf.addInt(CFG_ROW_COLOR_COUNTER, rowColors.size());
        int idx = 0;
        for (final Color color : rowColors) {
            colorColsConf.addInt(CFG_ROW_COLOR + idx++, color.getRGB());
        }
        config.saveToXML(dataOS);
        exec.checkCanceled();

        final File dataFile = new File(dataDir, CFG_DATA_FILE);
        DataContainer.writeToZip(m_data, dataFile, exec);
    }

    /**
     * @param dataDir the data directory to read from
     * @param exec the {@link ExecutionMonitor}
     * @return the {@link InteractiveHistogramDataModel}
     * @throws IOException if the file is invalid
     * @throws InvalidSettingsException if a setting is invalid
     * @throws CanceledExecutionException if the process was canceled
     */
    public static InteractiveHistogramDataModel loadFromFile(final File dataDir,
            final ExecutionMonitor exec) throws IOException,
            InvalidSettingsException, CanceledExecutionException {
        final File settingFile = new File(dataDir, CFG_SETTING_FILE);
        final FileInputStream is = new FileInputStream(settingFile);
        final GZIPInputStream inData = new GZIPInputStream(is);
        final ConfigRO config = NodeSettings.loadFromXML(inData);
        final ConfigRO colorColsConf = config.getConfig(CFG_COLOR_COLS);
        final int counter = colorColsConf.getInt(CFG_ROW_COLOR_COUNTER);
        final List<Color> rowColors =
            new ArrayList<Color>();
        for (int i = 0; i < counter; i++) {
            rowColors.add(new Color(colorColsConf.getInt(CFG_ROW_COLOR + i)));
        }
        exec.checkCanceled();

        final File dataFile = new File(dataDir, CFG_DATA_FILE);
        final ContainerTable table = DataContainer.readFromZip(dataFile);
        final int rowCount = table.getRowCount();
        final DefaultDataArray dataArray =
            new DefaultDataArray(table, 1, rowCount, exec);
        return new InteractiveHistogramDataModel(dataArray, rowColors);
    }


    /**
     * @return the rowColors
     */
    public List<Color> getRowColors() {
        return Collections.unmodifiableList(m_rowColors);
    }

    /**
     * @param idx the index of the column
     * @return the {@link DataColumnSpec} of the column with the given index
     */
    public DataColumnSpec getColumnSpec(final int idx) {
        return m_data.getDataTableSpec().getColumnSpec(idx);
    }

    /**
     * @return the data rows to display
     */
    public List<DataRow> getDataRows() {
        final List<DataRow> rows = new ArrayList<DataRow>(m_data.size());
        for (final DataRow row : m_data) {
            rows.add(row);
        }
        return rows;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<DataRow> iterator() {
        return m_data.iterator();
    }
}
