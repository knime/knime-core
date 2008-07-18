/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 * History
 *    26.02.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
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
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
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

    private final SortedSet<Color> m_rowColors;



    /**Constructor for class InteractiveHistogramDataModel.
     * @param exec {@link ExecutionMonitor}
     * @param data the data
     * @param noOfRows the number of rows
     * @throws CanceledExecutionException if the operation was canceled
     */
    public InteractiveHistogramDataModel(final ExecutionMonitor exec,
            final BufferedDataTable data, final int noOfRows)
    throws CanceledExecutionException {
        if (exec == null) {
            throw new NullPointerException("exec must not be null");
        }
        if (data == null) {
            throw new IllegalArgumentException(
                    "Table sholdn't be null");
        }
        ExecutionMonitor subExec = exec.createSubProgress(0.5);
        exec.setMessage("Adding rows to histogram model...");
        m_data = new DefaultDataArray(data, 1, noOfRows, subExec);
        exec.setMessage("Adding row color to histogram...");
        m_rowColors = new TreeSet<Color>(HSBColorComparator.getInstance());
        subExec = exec.createSubProgress(0.5);
        final double progressPerRow = 1.0 / noOfRows;
        double progress = 0.0;
        final CloseableRowIterator rowIterator = data.iterator();
        try {
            for (int i = 0; i < noOfRows && rowIterator.hasNext();
                i++) {
                final DataRow row = rowIterator.next();
                final Color color = m_data.getDataTableSpec().
                    getRowColor(row).getColor(false, false);
                if (!m_rowColors.contains(color)) {
                    m_rowColors.add(color);
                }
                progress += progressPerRow;
                subExec.setProgress(progress,
                        "Adding data rows to histogram...");
                subExec.checkCanceled();
            }
        } finally {
            if (rowIterator != null) {
                rowIterator.close();
            }
        }
        exec.setProgress(1.0, "Histogram finished.");

    }

    /**Constructor for class InteractiveHistogramDataModel.
     * @param array the data array
     * @param rowColors the row colors
     */
    public InteractiveHistogramDataModel(final DefaultDataArray array,
            final SortedSet<Color> rowColors) {
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
        final SortedSet<Color> rowColors = getRowColors();
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
        final SortedSet<Color> rowColors =
            new TreeSet<Color>(HSBColorComparator.getInstance());
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
    public SortedSet<Color> getRowColors() {
        return m_rowColors;
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
