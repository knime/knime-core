/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Feb 25, 2008 (sellien): created
 */
package org.knime.base.node.viz.condbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.plotter.box.BoxPlotDataProvider;
import org.knime.base.node.viz.plotter.box.BoxPlotNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * Class for the model of a conditional box plot.
 *
 * @author Stephan Sellien, University of Konstanz
 *
 */
public class ConditionalBoxPlotNodeModel extends NodeModel implements
        BoxPlotDataProvider {

    private final ConditionalBoxPlotSettings m_settings =
            new ConditionalBoxPlotSettings();

    private Map<DataColumnSpec, double[]> m_statistics;

    private Map<String, Map<Double, Set<RowKey>>> m_mildOutliers;

    private Map<String, Map<Double, Set<RowKey>>> m_extremeOutliers;

    private DataArray m_dataArray;

    private HiLiteHandler m_hiLiteHandler = new HiLiteHandler();

    /** The spec of the numeric column. Necessary for passing the
     * column's domain to the view to allow a normalized visualization
     * respecting the domain. */
    private DataColumnSpec m_numColSpec;

    /**
     * Creates a conditional box plot node model.
     */
    protected ConditionalBoxPlotNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        String warning = "";
        String nominalCol = m_settings.nominalColumn();
        if (nominalCol == null) {
            for (int i = 0; i < inSpecs[0].getNumColumns(); i++) {
                DataColumnSpec nomColSpec = inSpecs[0].getColumnSpec(i);
                if (nomColSpec.getType().isCompatible(NominalValue.class)
                        && nomColSpec.getDomain().hasValues()) {
                    nominalCol = inSpecs[0].getColumnSpec(i).getName();
                    m_settings.nominalColumn(nominalCol);
                    warning = "No nominal column selected. Column "
                            + nominalCol + " chosen.";
                    break;
                }
            }
            if (nominalCol == null) {
                throw new InvalidSettingsException(
                        "No nominal column with domain values found.\n"
                                + "Domain values can be created by the node:"
                                + " Domain Calculator");
            }
        }
        if (inSpecs[0].getColumnSpec(nominalCol) == null) {
            throw new InvalidSettingsException(
                    "Selected nominal column does not exist");
        }
        if (!inSpecs[0].getColumnSpec(nominalCol).getType().isCompatible(
                NominalValue.class)) {
            throw new InvalidSettingsException(
                    "Selected nominal column is not nominal.");
        }
        if (!inSpecs[0].getColumnSpec(nominalCol).getDomain().hasValues()) {
            warning += "\nSelected nominal column has no domain values.\n"
                    + "Use Domain Calculator before.";
            throw new InvalidSettingsException(
                    "Selected nominal column has no domain values.");
        }

        String numericCol = m_settings.numericColumn();
        if (numericCol == null) {
            for (int i = 0; i < inSpecs[0].getNumColumns(); i++) {
                if (inSpecs[0].getColumnSpec(i).getType().isCompatible(
                        DoubleValue.class)) {
                    numericCol = inSpecs[0].getColumnSpec(i).getName();
                    m_settings.numericColumn(numericCol);
                    warning += "\nNo numeric column selected. Column "
                            + numericCol + " chosen.";
                    break;
                }
            }
            if (numericCol == null) {
                throw new InvalidSettingsException("No numeric column found.");
            }
        }
        if (inSpecs[0].getColumnSpec(numericCol) == null) {
            throw new InvalidSettingsException(
                    "Selected numeric column does not exists");
        }
        if (!inSpecs[0].getColumnSpec(numericCol).getType().isCompatible(
                DoubleValue.class)) {
            throw new InvalidSettingsException(
                    "Selected numeric column is not numeric.");
        }

        if (warning.length() > 0) {
            setWarningMessage(warning.trim());
        }
        return new DataTableSpec[]{createOutputSpec(inSpecs[0])};
    }

    private DataTableSpec createOutputSpec(final DataTableSpec tableSpec) {
        DataColumnSpec spec =
                tableSpec.getColumnSpec(m_settings.nominalColumn());

        return new DataTableSpec(createColumnSpec(spec, false));
    }

    private DataColumnSpec[] createColumnSpec(final DataColumnSpec spec,
            final boolean ignoreMissingValues) {
        int size = spec.getDomain().getValues().size();
        if (m_settings.showMissingValues() && !ignoreMissingValues) {
            size++;
        }
        DataColumnSpec[] colSpec = new DataColumnSpec[size];
        List<String> values = new ArrayList<String>();
        for (DataCell value : spec.getDomain().getValues()) {
            values.add(value.toString());
        }
        if (m_settings.showMissingValues() && !ignoreMissingValues) {
            values.add(DataType.getMissingCell().toString());
        }

        int emptyCounter = 0;
        for (int i = 0; i < values.size(); i++) {
            if ("".equals(values.get(i))) {
                String replacementName;
                do {
                    replacementName = "<empty_" + emptyCounter++ + ">";
                } while (values.contains(replacementName));
                values.set(i, replacementName);
            }
        }

        Collections.sort(values);
        DataColumnSpecCreator creator;
        int index = 0;
        for (String value : values) {
            creator = new DataColumnSpecCreator(replaceSpaces(value),
                    DoubleCell.TYPE);
            colSpec[index++] = creator.createSpec();
        }
        return colSpec;
    }

    private DataContainer createOutputTable(final DataTableSpec tableSpec,
            final DataColumnSpec[] colSpecs) {
        DataContainer cont = new DataContainer(createOutputSpec(tableSpec));

        RowKey[] rowKeys = new RowKey[BoxPlotNodeModel.SIZE];
        rowKeys[BoxPlotNodeModel.MIN] = new RowKey("Minimum");
        rowKeys[BoxPlotNodeModel.LOWER_WHISKER] = new RowKey("Lower Whisker");
        rowKeys[BoxPlotNodeModel.LOWER_QUARTILE] = new RowKey("Lower Quartile");
        rowKeys[BoxPlotNodeModel.MEDIAN] = new RowKey("Median");
        rowKeys[BoxPlotNodeModel.UPPER_QUARTILE] = new RowKey("Upper Quartile");
        rowKeys[BoxPlotNodeModel.UPPER_WHISKER] = new RowKey("Upper Whisker");
        rowKeys[BoxPlotNodeModel.MAX] = new RowKey("Maximum");

        for (int row = 0; row < rowKeys.length; row++) {
            DataCell[] cells =
                    new DataCell[cont.getTableSpec().getNumColumns()];

            for (int i = 0; i < cells.length; i++) {
                double[] stats = m_statistics.get(colSpecs[i]);
                if (stats == null) {
                    cells[i] = DataType.getMissingCell();
                } else {
                    cells[i] = new DoubleCell(stats[row]);
                }
            }

            cont.addRowToTable(new DefaultRow(rowKeys[row], cells));
        }
        cont.close();
        return cont;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        m_statistics = new LinkedHashMap<DataColumnSpec, double[]>();
        m_mildOutliers = new LinkedHashMap<String, Map<Double, Set<RowKey>>>();
        m_extremeOutliers =
                new LinkedHashMap<String, Map<Double, Set<RowKey>>>();
        double nrRows = inData[0].getRowCount();
        int rowCount = 0;
        int numericIndex =
                inData[0].getDataTableSpec().findColumnIndex(
                        m_settings.numericColumn());
        int nominalIndex =
                inData[0].getDataTableSpec().findColumnIndex(
                        m_settings.nominalColumn());
        Map<String, Map<Double, Set<RowKey>>> data =
                new LinkedHashMap<String, Map<Double, Set<RowKey>>>();

        // some default values .. if one column only has missing values.
        for (DataCell d : inData[0].getDataTableSpec().getColumnSpec(
                nominalIndex).getDomain().getValues()) {
            String name = ((StringValue)d).getStringValue();
            m_mildOutliers.put(name, new HashMap<Double, Set<RowKey>>());
            m_extremeOutliers.put(name, new HashMap<Double, Set<RowKey>>());
        }

        for (DataRow r : inData[0]) {
            exec.checkCanceled();
            exec.setProgress(rowCount++ / nrRows, "Separating...");
            if (!m_settings.showMissingValues()) {
                if (r.getCell(nominalIndex).isMissing()) {
                    // missing cell in nominal values is unwanted?
                    continue;
                }
            }
            String nominal = replaceSpaces(r.getCell(nominalIndex).toString());
            if (r.getCell(numericIndex).isMissing()) {
                // ignore missing cells in numeric column
                continue;
            }
            DoubleValue numeric = (DoubleValue)r.getCell(numericIndex);
            Map<Double, Set<RowKey>> map = data.get(nominal);
            if (map == null) {
                map = new LinkedHashMap<Double, Set<RowKey>>();
            }
            Set<RowKey> set = map.get(numeric.getDoubleValue());
            if (set == null) {
                set = new HashSet<RowKey>();
            }
            set.add(r.getKey());
            map.put(numeric.getDoubleValue(), set);
            data.put(nominal, map);
        }
        List<String> keys = new ArrayList<String>(data.keySet());
        boolean ignoreMissingValues = false;
        if (m_settings.showMissingValues()
                && !keys.contains(DataType.getMissingCell().toString())) {
            // we promised to create data for missing values..
            // if there aren't any.. we have to create them ourselves
            setWarningMessage("No missing values found.");
            ignoreMissingValues = true;
        }
        Collections.sort(keys);
        DataColumnSpec[] colSpecs =
                createColumnSpec(inData[0].getDataTableSpec().getColumnSpec(
                        nominalIndex), ignoreMissingValues);
        if (keys.size() == 0) {
            setWarningMessage("All classes are empty.");
        }
        int dataSetNr = 0;
        // for (String d : keys) {
        for (DataColumnSpec dcs : colSpecs) {
            String d = dcs.getName();
            if (data.get(d) == null || keys.size() == 0) {
                dataSetNr++;
                continue;
            }

            exec.checkCanceled();
            exec.setProgress(dataSetNr / keys.size(), "Creating statistics");

            Map<Double, Set<RowKey>> extremeOutliers =
                    new LinkedHashMap<Double, Set<RowKey>>();
            Map<Double, Set<RowKey>> mildOutliers =
                    new LinkedHashMap<Double, Set<RowKey>>();

            double[] stats =
                    calculateStatistic(data.get(d), mildOutliers,
                            extremeOutliers);

            double minimum = stats[BoxPlotNodeModel.MIN];
            double maximum = stats[BoxPlotNodeModel.MAX];

            DataColumnSpecCreator creator =
                    new DataColumnSpecCreator(colSpecs[dataSetNr]);
            creator.setDomain(new DataColumnDomainCreator(new DoubleCell(
                    minimum), new DoubleCell(maximum)).createDomain());
            colSpecs[dataSetNr] = creator.createSpec();

            m_statistics.put(colSpecs[dataSetNr], stats);
            m_mildOutliers.put(d, mildOutliers);
            m_extremeOutliers.put(d, extremeOutliers);

            dataSetNr++;

        }
        DataTableSpec dts = new DataTableSpec("MyTempTable", colSpecs);
        DataContainer cont = new DataContainer(dts);
        cont.close();
        m_dataArray = new DefaultDataArray(cont.getTable(), 1, 2);

        if (ignoreMissingValues) {
            DataColumnSpec[] temp = new DataColumnSpec[colSpecs.length + 1];
            DataColumnSpec missing =
                    new DataColumnSpecCreator(DataType.getMissingCell()
                            .toString(), DataType.getMissingCell().getType())
                            .createSpec();
            int i = 0;
            while (missing.getName().compareTo(colSpecs[i].getName()) > 0) {
                temp[i] = colSpecs[i];
                i++;
            }
            temp[i++] = missing;
            while (i < temp.length) {
                temp[i] = colSpecs[i - 1];
                i++;
            }
            colSpecs = temp;
        }
         /* Save inSpec of the numeric column to provide the view a way to
         * consider the input domain for normalization. */
        m_numColSpec = inData[0].getDataTableSpec().getColumnSpec(numericIndex);

        return new BufferedDataTable[]{exec.createBufferedDataTable(
                createOutputTable(inData[0].getDataTableSpec(), colSpecs)
                        .getTable(), exec)};

    }

    /**
     * Put the string in parentheses if it contains leading or trailing spaces.
     *
     * @param string the string to be modified
     * @return the string starting and ending with parentheses and the appended
     *          modification string if applicable
     */
    private String replaceSpaces(final String string) {
        StringBuffer sb = new StringBuffer(string);
        if (string.startsWith(" ")  || string.endsWith(" ")) {
            // replace the first leading space
            sb = sb.insert(0, '(');
            sb = sb.append(')');
            sb.append("#with_blanks");
        }
        return sb.toString();
    }

    private double[] calculateStatistic(final Map<Double, Set<RowKey>> map,
            final Map<Double, Set<RowKey>> mildOutliers,
            final Map<Double, Set<RowKey>> extremeOutliers) {
        List<Double> values = new LinkedList<Double>();
        for (Map.Entry<Double, Set<RowKey>> entry : map.entrySet()) {
            for (int i = 0; i < entry.getValue().size(); i++) {
                values.add(entry.getKey());
            }
        }
        Collections.sort(values);
        int size = values.size();
        if (size == 0) {
            return null;
        }
        double median = 0.0;
        if (size % 2 == 0) {
            median = (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
        } else {
            median = values.get(size / 2);
        }
        double q1 = 0.0;
        if (size % 4 == 0) {
            q1 = (values.get(size / 4 - 1) + values.get(size / 4)) / 2.0;
        } else {
            q1 = values.get(size / 4);
        }
        double q3 = 0.0;
        if ((3 * size) % 4 == 0) {
            double v1 = values.get(3 * size / 4 - 1);
            double v2 = values.get(3 * size / 4);
            q3 = (v1 + v2) / 2.0;
        } else {
            q3 = values.get(3 * size / 4);
        }
        double iqr = q3 - q1;
        double min = values.get(0);
        double max = values.get(size - 1);
        double lowerWhisker = min;
        double upperWhisker = max;
        double upperWhiskerFence = q3 + (1.5 * iqr);
        double lowerWhiskerFence = q1 - (1.5 * iqr);
        double lowerFence = q1 - (3 * iqr);
        double upperFence = q3 + (3 * iqr);

        if (lowerWhisker < lowerWhiskerFence
                || upperWhisker > upperWhiskerFence) {
            for (int i = 0; i < size; i++) {
                double value = values.get(i);
                Set<RowKey> rowKey = map.get(value);
                if (value < lowerFence) {
                    extremeOutliers.put(value, rowKey);
                } else if (value < lowerWhiskerFence) {
                    mildOutliers.put(value, rowKey);
                } else if (lowerWhisker < lowerWhiskerFence
                        && value >= lowerWhiskerFence) {
                    lowerWhisker = value;
                } else if (value <= upperWhiskerFence) {
                    upperWhisker = value;
                } else if (value > upperFence) {
                    extremeOutliers.put(value, rowKey);
                } else if (value > upperWhiskerFence) {
                    mildOutliers.put(value, rowKey);
                }
            }
        }
        double minimum = values.get(0);
        double maximum = values.get(size - 1);
        double[] stats = new double[BoxPlotNodeModel.SIZE];
        stats[BoxPlotNodeModel.MIN] = minimum;
        stats[BoxPlotNodeModel.LOWER_WHISKER] = lowerWhisker;
        stats[BoxPlotNodeModel.LOWER_QUARTILE] = q1;
        stats[BoxPlotNodeModel.MEDIAN] = median;
        stats[BoxPlotNodeModel.UPPER_QUARTILE] = q3;
        stats[BoxPlotNodeModel.UPPER_WHISKER] = upperWhisker;
        stats[BoxPlotNodeModel.MAX] = maximum;

        return stats;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        try {
            File f = new File(nodeInternDir, "conditionalBoxPlotInternals");
            FileInputStream fis = new FileInputStream(f);
            NodeSettingsRO settings = NodeSettings.loadFromXML(fis);
            fis.close();
            m_statistics = new LinkedHashMap<DataColumnSpec, double[]>();
            m_mildOutliers =
                    new LinkedHashMap<String, Map<Double, Set<RowKey>>>();
            m_extremeOutliers =
                    new LinkedHashMap<String, Map<Double, Set<RowKey>>>();

           /* Load the numerical column spec if available.*/
           if (settings.containsKey("numColSpec")) {
                m_numColSpec = DataColumnSpec.load(
                        settings.getConfig("numColSpec"));
           }

            int nrOfCols = settings.getInt("nrOfCols");

            for (int i = 0; i < nrOfCols; i++) {
                DataColumnSpec spec =
                        DataColumnSpec.load(settings.getConfig("col" + i));
                String colName = spec.getName();
                double[] stats = settings.getDoubleArray("stats" + colName);
                m_statistics.put(spec, stats);

                double[] mild = settings.getDoubleArray("mild" + colName);
                Map<Double, Set<RowKey>> mildmap =
                        new LinkedHashMap<Double, Set<RowKey>>();
                for (int j = 0; j < mild.length; j++) {
                    Set<RowKey> set = new HashSet<RowKey>();
                    String[] mildKeys =
                            settings.getStringArray("mildKeys" + colName + j);
                    for (int k = 0; k < mildKeys.length; k++) {
                        set.add(new RowKey(mildKeys[k]));
                    }
                    mildmap.put(mild[j], set);
                }
                m_mildOutliers.put(colName, mildmap);

                double[] extr = settings.getDoubleArray("extreme" + colName);

                Map<Double, Set<RowKey>> extrmap =
                        new LinkedHashMap<Double, Set<RowKey>>();
                for (int j = 0; j < extr.length; j++) {
                    Set<RowKey> set = new HashSet<RowKey>();
                    String[] extrKeys =
                            settings.getStringArray("extremeKeys" + colName
                                    + j);
                    for (int k = 0; k < extrKeys.length; k++) {

                        set.add(new RowKey(extrKeys[k]));
                    }
                    extrmap.put(extr[j], set);
                }
                m_extremeOutliers.put(colName, extrmap);
            }

            File dataFile =
                    new File(nodeInternDir, "conditionalBoxPlotDataFile");
            ContainerTable table = DataContainer.readFromZip(dataFile);
            m_dataArray = new DefaultDataArray(table, 1, 2, exec);
        } catch (Exception e) {
            throw new IOException(
                    "Unable to load internals: " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_extremeOutliers = null;
        m_mildOutliers = null;
        m_statistics = null;
        m_dataArray = null;
        m_numColSpec = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        try {
            NodeSettings settings =
                    new NodeSettings("conditionalBoxPlotInternals");
            settings.addInt("nrOfCols", m_statistics.size());
            int i = 0;
            for (DataColumnSpec spec : m_statistics.keySet()) {
                NodeSettings colSetting =
                        (NodeSettings)settings.addConfig("col" + (i++));
                spec.save(colSetting);
            }
            if (m_numColSpec != null) {
                m_numColSpec.save(settings.addConfig("numColSpec"));
            }
            for (Map.Entry<DataColumnSpec, double[]> entry : m_statistics
                    .entrySet()) {
                String colName = entry.getKey().getName();
                settings.addDoubleArray("stats" + colName, entry.getValue());
                Map<Double, Set<RowKey>> mildOutliers =
                        m_mildOutliers.get(colName);
                double[] mild = new double[mildOutliers.size()];
                int mildIndex = 0;
                for (Map.Entry<Double, Set<RowKey>> mEnt : mildOutliers
                        .entrySet()) {
                    RowKey[] keys =
                            mEnt.getValue().toArray(
                                    new RowKey[mEnt.getValue().size()]);
                    String[] mildKeys = new String[keys.length];
                    mild[mildIndex] = mEnt.getKey();
                    for (int j = 0; j < keys.length; j++) {
                        mildKeys[j] = keys[j].getString();
                    }
                    settings.addStringArray("mildKeys" + colName + mildIndex,
                            mildKeys);
                    mildIndex++;
                }
                settings.addDoubleArray("mild" + colName, mild);

                Map<Double, Set<RowKey>> extremeOutliers =
                        m_extremeOutliers.get(colName);
                double[] extr = new double[extremeOutliers.size()];

                int extrIndex = 0;
                for (Map.Entry<Double, Set<RowKey>> eEnt : extremeOutliers
                        .entrySet()) {
                    RowKey[] keys =
                            eEnt.getValue().toArray(
                                    new RowKey[eEnt.getValue().size()]);
                    String[] extrKeys = new String[keys.length];
                    extr[extrIndex] = eEnt.getKey();
                    for (int j = 0; j < keys.length; j++) {
                        extrKeys[j] = keys[j].getString();
                    }
                    settings.addStringArray("extremeKeys" + colName
                            + extrIndex, extrKeys);
                    extrIndex++;
                }

                settings.addDoubleArray("extreme" + colName, extr);
            }
            File f = new File(nodeInternDir, "conditionalBoxPlotInternals");
            FileOutputStream fos = new FileOutputStream(f);
            settings.saveToXML(fos);
            File dataFile =
                    new File(nodeInternDir, "conditionalBoxPlotDataFile");
            DataContainer.writeToZip(m_dataArray, dataFile, exec);
        } catch (Exception e) {
            throw new IOException(
                    "Unable to save internals: " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        ConditionalBoxPlotSettings s = new ConditionalBoxPlotSettings();
        s.loadSettings(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_hiLiteHandler;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Map<Double, Set<RowKey>>> getExtremeOutliers() {
        return m_extremeOutliers;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Map<Double, Set<RowKey>>> getMildOutliers() {
        return m_mildOutliers;
    }

    /**
     * {@inheritDoc}
     */
    public Map<DataColumnSpec, double[]> getStatistics() {
        return m_statistics;
    }

    /**
     * {@inheritDoc}
     */
    public DataArray getDataArray(final int index) {
        return m_dataArray;
    }

    /**
     * @return the numColSpec
     */
    public DataColumnSpec getNumColSpec() {
        return m_numColSpec;
    }

    /**
     * @return true if the spec of the numerical column is present
     */
    public boolean hasNumColSpec() {
        return m_numColSpec != null;
    }

    /**
     * @return the settings
     */
    public ConditionalBoxPlotSettings getSettings() {
        return m_settings;
    }
}
