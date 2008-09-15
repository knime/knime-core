/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 10, 2008 (sellien): created
 */
package org.knime.base.node.viz.liftchart;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.knime.base.data.sort.SortedTable;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The node model of a lift chart.
 *
 * @author Stephan Sellien, University of Konstanz
 */
public class LiftChartNodeModel extends NodeModel implements DataProvider {

    private static final String RESPONSE_COLUMN = "responseColumn";

    private static final String PROBABILITY_COLUMN = "probabilityColumn";

    private static final String DATA_FILE = "LiftChartNodeDataFile";

    private static final String RESPONSE_LABEL = "responseLabel";

    private static final String INTERVAL_WIDTH = "intervalWidth";

    private final SettingsModelString m_responseColumn =
            createResponseColumnModel();

    private final SettingsModelString m_probabilityColumn =
            createProbabilityColumnModel();

    private final SettingsModelString m_responseLabel =
            createResponseLabelModel();

    private final SettingsModelString m_intervalWidth =
            createIntervalWidthModel();

    private DataArray[] m_dataArray;

    /**
     * Creates a new lift chart node model.
     */
    protected LiftChartNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_responseColumn.getStringValue() == null
                || m_responseColumn.getStringValue().trim().length() == 0
                || !inSpecs[0].containsName(m_responseColumn.getStringValue())
                || inSpecs[0].getColumnSpec(
                        m_responseColumn.getStringValue()) == null) {
            for (int i = 0; i < inSpecs[0].getNumColumns(); i++) {
                DataColumnSpec cs = inSpecs[0].getColumnSpec(i);
                if (cs.getType().isCompatible(NominalValue.class)
                        && cs.getDomain().hasValues()) {
                    m_responseColumn.setStringValue(cs.getName());
                    setWarningMessage("Auto-selected column " + cs.getName()
                            + " as response column.");
                    break;
                }
            }
        }
        if (inSpecs[0].getColumnSpec(m_responseColumn.getStringValue()) == null
                || !inSpecs[0].getColumnSpec(m_responseColumn.getStringValue())
                .getType().isCompatible(NominalValue.class)) {
            // auto-configure makes no sense here, since guessing the true label
            // is maybe a bit too much 
            throw new InvalidSettingsException(
                    "Selected response column contains no string values or"
                            + " domain is not set."
                            + " You might have to use a domain calculator.");
        }
        if (m_probabilityColumn.getStringValue() == null
                || m_probabilityColumn.getStringValue().trim().length() == 0
                || !inSpecs[0].containsName(
                        m_probabilityColumn.getStringValue())
                || inSpecs[0].getColumnSpec(
                        m_probabilityColumn.getStringValue()) == null) {
            for (int i = 0; i < inSpecs[0].getNumColumns(); i++) {
                if (inSpecs[0].getColumnSpec(i).getType().isCompatible(
                        DoubleValue.class)) {
                    m_probabilityColumn.setStringValue(inSpecs[0]
                            .getColumnSpec(i).getName());
                    setWarningMessage("Auto-selected column "
                            + inSpecs[0].getColumnSpec(i).getName()
                            + " as probability column.");
                    break;
                }
            }
        }
        if (!inSpecs[0].getColumnSpec(m_probabilityColumn.getStringValue())
                .getType().isCompatible(DoubleValue.class)) {
            throw new InvalidSettingsException(
                    "Selected probability column contains no double values.");
        }

        if (m_responseLabel.getStringValue() == null) {
            throw new InvalidSettingsException("Response label must be set.");
        }
        return inSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        m_dataArray = new DataArray[2];

        int predColIndex =
                inData[0].getDataTableSpec().findColumnIndex(
                        m_responseColumn.getStringValue());

        List<String> inclList = new LinkedList<String>();

        inclList.add(m_probabilityColumn.getStringValue());

        boolean[] order = new boolean[]{false};

        SortedTable st = new SortedTable(inData[0], inclList, order, exec);

        long totalResponses = 0;

        double partWidth = Double.parseDouble(m_intervalWidth.getStringValue());

        int nrParts = (int)Math.ceil(100.0 / partWidth);

        List<Integer> positiveResponses = new LinkedList<Integer>();

        int rowIndex = 0;
        for (DataRow row : st) {
            if (row.getCell(predColIndex).isMissing()) {
                setWarningMessage("There are missing values."
                        + " Please check your data.");
                continue;
            }

            String response =
                    ((StringValue)row.getCell(predColIndex)).getStringValue()
                            .trim();

            if (response.equalsIgnoreCase(m_responseLabel.getStringValue())) {
                totalResponses++;
                positiveResponses.add(rowIndex);
            }

            rowIndex++;
        }

        int[] counter = new int[nrParts];
        int partWidthAbsolute = (int)Math.ceil(rowIndex / (double)nrParts);

        double avgResponse = (double)positiveResponses.size() / rowIndex;

        for (int rIndex : positiveResponses) {
            int index = rIndex / partWidthAbsolute;
            counter[index]++;
        }

        DataColumnSpec[] colSpec = new DataColumnSpec[3];

        colSpec[0] =
                new DataColumnSpecCreator("Lift", DoubleCell.TYPE).createSpec();
        colSpec[1] =
                new DataColumnSpecCreator("Baseline", DoubleCell.TYPE)
                        .createSpec();
        colSpec[2] =
                new DataColumnSpecCreator("Cumulative Lift", DoubleCell.TYPE)
                        .createSpec();

        DataTableSpec tableSpec = new DataTableSpec(colSpec);

        DataContainer cont = new DataContainer(tableSpec);

        colSpec = new DataColumnSpec[2];

        colSpec[0] =
                new DataColumnSpecCreator("Actual", DoubleCell.TYPE)
                        .createSpec();
        colSpec[1] =
                new DataColumnSpecCreator("Baseline", DoubleCell.TYPE)
                        .createSpec();

        tableSpec = new DataTableSpec(colSpec);

        DataContainer responseCont = new DataContainer(tableSpec);

        long cumulativeCounter = 0;

        double lifts = 0;

        responseCont.addRowToTable(new DefaultRow(new RowKey("0"), 0.0, 0.0));

        for (int i = 0; i < counter.length; i++) {
            cumulativeCounter += counter[i];
            double responseRate = (double)counter[i] / partWidthAbsolute;
            double lift = responseRate / avgResponse;

            double cumResponseRate = (double)cumulativeCounter / totalResponses;

            long number = partWidthAbsolute * (i + 1);

            // well.. rounding problems
            if (number > rowIndex) {
                number = rowIndex;
            }

            double cumulativeLift =
            // (double)cumulativeCounter / (partWidthAbsolute * (i + 1));
                    (double)cumulativeCounter / number;
            cumulativeLift /= avgResponse;

            lifts += lift;

            // cumulativeLift = lifts / (i+1);

            double rowKey = ((i + 1) * partWidth);
            if (rowKey > 100) {
                rowKey = 100;
            }
            cont.addRowToTable(new DefaultRow(new RowKey("" + rowKey), lift,
                    1.0, cumulativeLift));

            double cumBaseline = (i + 1) * partWidth;

            if (cumBaseline > 100) {
                cumBaseline = 100;
            }
            responseCont.addRowToTable(new DefaultRow(new RowKey("" + rowKey),
                    cumResponseRate * 100, cumBaseline));
        }

        cont.close();
        responseCont.close();

        m_dataArray[0] = new DefaultDataArray(cont.getTable(), 1, cont.size());
        m_dataArray[1] =
                new DefaultDataArray(responseCont.getTable(), 1, responseCont
                        .size());

        return new BufferedDataTable[]{st.getBufferedDataTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File dataFile1 = new File(nodeInternDir, DATA_FILE + "1");
        File dataFile2 = new File(nodeInternDir, DATA_FILE + "2");
        ContainerTable dataCont1 = DataContainer.readFromZip(dataFile1);
        ContainerTable dataCont2 = DataContainer.readFromZip(dataFile2);
        m_dataArray = new DataArray[2];
        m_dataArray[0] =
                new DefaultDataArray(dataCont1, 1, dataCont1.getRowCount(),
                        exec.createSubProgress(0.5));
        m_dataArray[1] =
                new DefaultDataArray(dataCont2, 1, dataCont2.getRowCount(),
                        exec.createSubProgress(0.5));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_responseColumn.loadSettingsFrom(settings);
        m_probabilityColumn.loadSettingsFrom(settings);
        m_responseLabel.loadSettingsFrom(settings);
        m_intervalWidth.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // do nothing
        m_responseColumn.setStringValue(null);
        m_probabilityColumn.setStringValue(null);
        m_responseLabel.setStringValue(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File dataFile1 = new File(nodeInternDir, DATA_FILE + "1");
        DataContainer.writeToZip(m_dataArray[0], dataFile1, exec
                .createSubProgress(0.5));
        File dataFile2 = new File(nodeInternDir, DATA_FILE + "2");
        DataContainer.writeToZip(m_dataArray[1], dataFile2, exec
                .createSubProgress(0.5));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_responseColumn.saveSettingsTo(settings);
        m_probabilityColumn.saveSettingsTo(settings);
        m_responseLabel.saveSettingsTo(settings);
        m_intervalWidth.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_responseColumn.validateSettings(settings);
        m_probabilityColumn.validateSettings(settings);
        m_intervalWidth.validateSettings(settings);
        m_responseLabel.validateSettings(settings);
        if (settings.getString(RESPONSE_LABEL) == null
                || settings.getString(RESPONSE_LABEL).length() == 0) {
            throw new InvalidSettingsException("Invalid response label.");
        }
    }

    /**
     * Creates the model for selecting the column with response values.
     *
     * @return a {@link SettingsModelString}
     */
    public static SettingsModelString createResponseColumnModel() {
        return new SettingsModelString(RESPONSE_COLUMN, "");
    }

    /**
     * Creates the model for selecting the column with predicted values.
     *
     * @return a {@link SettingsModelString}
     */
    public static SettingsModelString createProbabilityColumnModel() {
        return new SettingsModelString(PROBABILITY_COLUMN, "");
    }

    /**
     * Creates the model for the response label.
     *
     * @return a {@link SettingsModelString}
     */
    public static SettingsModelString createResponseLabelModel() {
        return new SettingsModelString(RESPONSE_LABEL, null);
    }

    /**
     * Creates the model for the interval width.
     *
     * @return a {@link SettingsModelIntegerBounded}
     */
    public static SettingsModelString createIntervalWidthModel() {
        return new SettingsModelString(INTERVAL_WIDTH, "10");
    }

    /**
     * {@inheritDoc}
     */
    public DataArray getDataArray(final int index) {
        if (index == 0 || index == 1) {
            return m_dataArray[index];
        } else {
            return m_dataArray[0];
        }
    }
}
