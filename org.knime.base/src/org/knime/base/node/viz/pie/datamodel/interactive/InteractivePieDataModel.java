/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *    18.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.datamodel.interactive;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.pie.datamodel.PieDataModel;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * This is the interactive implementation of the {@link PieDataModel} which
 * allows hiliting and column changing.
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractivePieDataModel extends PieDataModel
    implements Iterable<DataRow> {

    private static final String CFG_DATA_FILE = "dataFile.xml.gz";
    private static final String CFG_SETTING_FILE = "settingFile.xml.gz";
    private static final String CFG_SETTING = "fixedPieDataModel";
    private static final String CFG_HILITING = "hiliting";
    private static final String CFG_DETAILS = "details";

    private final DataArray m_data;

    /**Constructor for class InteractivePieDataModel.
     * @param exec the {@link ExecutionMonitor}
     * @param table the data table
     * @param noOfRows the optional number of rows to initialize the row array
     * @param detailsAvailable <code>true</code> if details are available
     * @throws CanceledExecutionException if the progress was canceled
     */
    public InteractivePieDataModel(final ExecutionMonitor exec,
            final DataTable table, final int noOfRows,
            final boolean detailsAvailable) throws CanceledExecutionException {
        super(true, detailsAvailable);
        m_data = new DefaultDataArray(table, 1, noOfRows, exec);
    }

    private InteractivePieDataModel(final DataArray array,
            final boolean detailsAvailable, final boolean supportHiliting) {
        super(supportHiliting, detailsAvailable);
        m_data = array;
    }

    /**
     * @param dataDir the data directory to write to
     * @param exec the {@link ExecutionMonitor}
     * @throws IOException if the output file could not be created
     * @throws CanceledExecutionException if the saving was canceled
     */
    public void save2File(final File dataDir, final ExecutionMonitor exec)
    throws IOException, CanceledExecutionException {
        final File settingFile = new File(dataDir, CFG_SETTING_FILE);
        final FileOutputStream os = new FileOutputStream(settingFile);
        final GZIPOutputStream settingOS = new GZIPOutputStream(os);
        final Config config = new NodeSettings(CFG_SETTING);
        config.addBoolean(CFG_HILITING, supportsHiliting());
        config.addBoolean(CFG_DETAILS, detailsAvailable());
        config.saveToXML(settingOS);
        exec.checkCanceled();

        final File dataFile = new File(dataDir, CFG_DATA_FILE);
        DataContainer.writeToZip(m_data, dataFile, exec);
    }

    /**
     * @param dataDir the data directory to read from
     * @param exec {@link ExecutionMonitor}
     * @return the {@link InteractivePieDataModel}
     * @throws IOException if the file could not be read
     * @throws InvalidSettingsException if a setting wasn't present
     * @throws CanceledExecutionException if the operation was canceled
     */
    public static InteractivePieDataModel loadFromFile(final File dataDir,
            final ExecutionMonitor exec) throws IOException,
            InvalidSettingsException, CanceledExecutionException {
        final File settingFile = new File(dataDir, CFG_SETTING_FILE);
        final FileInputStream is = new FileInputStream(settingFile);
        final GZIPInputStream inData = new GZIPInputStream(is);
        final ConfigRO config = NodeSettings.loadFromXML(inData);
        final boolean supportHiliting = config.getBoolean(CFG_HILITING);
        final boolean detailsAvailable = config.getBoolean(CFG_DETAILS);
        exec.checkCanceled();

        final File dataFile = new File(dataDir, CFG_DATA_FILE);
        final ContainerTable table = DataContainer.readFromZip(dataFile);
        final int rowCount = table.getRowCount();
        final DefaultDataArray dataArray =
            new DefaultDataArray(table, 1, rowCount, exec);
        return new InteractivePieDataModel(dataArray, detailsAvailable,
                supportHiliting);
    }

    /**
     * @return all data rows
     */
    public Iterator<DataRow> getDataRows() {

        return m_data.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<DataRow> iterator() {
        return getDataRows();
    }

    /**
     * @return the {@link DataTableSpec}
     */
    public DataTableSpec getDataTableSpec() {
        return m_data.getDataTableSpec();
    }

    /**
     * @param colName the column name to get the spec for
     * @return the {@link DataColumnSpec} or <code>null</code> if the name is
     * not in the spec
     */
    public DataColumnSpec getColSpec(final String colName) {
        return getDataTableSpec().getColumnSpec(colName);
    }

    /**
     * @param colName the column name to get the index for
     * @return the index of the given column name
     */
    public int getColIndex(final String colName) {
        return getDataTableSpec().findColumnIndex(colName);
    }

    /**
     * @param row the row to get the color for
     * @return the color of this row
     */
    public Color getRowColor(final DataRow row) {
        return getDataTableSpec().getRowColor(row).getColor(false, false);
    }
}
