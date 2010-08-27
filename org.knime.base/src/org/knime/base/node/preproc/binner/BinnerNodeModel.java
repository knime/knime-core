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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.preproc.binner;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.preproc.binner.BinnerColumnFactory.Bin;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Bins numeric columns into intervals which are then returned as string-type
 * columns.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class BinnerNodeModel extends NodeModel {
    // private static final NodeLogger LOGGER =
    // NodeLogger.getLogger(BinnerNodeModel.class);

    /** Key for binned columns. */
    static final String NUMERIC_COLUMNS = "binned_columns";

    /** Key if new column is appended. */
    static final String IS_APPENDED = "_is_appended";

    /** Selected columns for binning. */
    private final Map<String, Bin[]> m_columnToBins = 
        new HashMap<String, Bin[]>();

    private final Map<String, String> m_columnToAppended = 
        new HashMap<String, String>();

    /** Keeps index of the input port which is 0. */
    static final int INPORT = 0;

    /** Keeps index of the output port which is 0. */
    static final int OUTPORT = 0;

    /** Creates a new binner. */
    BinnerNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException {
        assert (data != null && data.length == 1 && data[INPORT] != null);
        DataTableSpec spec = data[INPORT].getDataTableSpec();
        BufferedDataTable buf = exec.createColumnRearrangeTable(data[INPORT],
                createColReg(spec), exec);
        return new BufferedDataTable[]{buf};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * Passes the input spec to the output.
     * 
     * @param inSpecs The input spec.
     * @return The generated output specs.
     * @throws InvalidSettingsException If column to bin cannot be identified.
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        for (String columnKey : m_columnToBins.keySet()) {
            assert m_columnToAppended.containsKey(columnKey) : columnKey;
            if (!inSpecs[INPORT].containsName(columnKey)) {
                throw new InvalidSettingsException("Binner: column "
                        + columnKey + " not found in spec.");
            }
            String appended = m_columnToAppended.get(columnKey);
            if (appended != null) {
                if (inSpecs[INPORT].containsName(appended)) {
                    throw new InvalidSettingsException("Binner: duplicate "
                            + "appended column " + appended + " in spec.");
                }
            }
        }
        // generate numeric binned table spec
        DataTableSpec spec = createColReg(inSpecs[INPORT]).createSpec();
        return new DataTableSpec[]{spec};
    }

    private ColumnRearranger createColReg(final DataTableSpec spec) {
        ColumnRearranger colreg = new ColumnRearranger(spec);
        for (String columnKey : m_columnToBins.keySet()) {
            Bin[] bins = m_columnToBins.get(columnKey);
            String appended = m_columnToAppended.get(columnKey);
            int columnIdx = spec.findColumnIndex(columnKey);
            if (appended == null) {
                BinnerColumnFactory binColumn = new BinnerColumnFactory(
                        columnIdx, bins, columnKey, false);
                colreg.replace(binColumn, columnIdx);
            } else {
                BinnerColumnFactory binColumn = new BinnerColumnFactory(
                        columnIdx, bins, appended, true);
                colreg.append(binColumn);
            }
            // set warning message when same bin names are used
            Set<String> hashBinNames = new HashSet<String>();
            for (Bin b : bins) {
                if (hashBinNames.contains(b.getBinName())) {
                    setWarningMessage("Bin name \"" + b.getBinName() 
                            + "\" is used for different intervals.");
                }
                hashBinNames.add(b.getBinName());
            }
        }
        return colreg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnToBins.clear();
        m_columnToAppended.clear();
        String[] columns = settings.getStringArray(NUMERIC_COLUMNS,
                new String[0]);
        for (int i = 0; i < columns.length; i++) {
            NodeSettingsRO column = settings.getNodeSettings(columns[i]
                    .toString());
            Set<String> bins = column.keySet();
            Bin[] binnings = new Bin[bins.size()];
            int s = 0;
            for (String binKey : bins) {
                NodeSettingsRO bin = column.getNodeSettings(binKey);
                binnings[s] = new NumericBin(bin);
                s++;
            }
            m_columnToBins.put(columns[i], binnings);
            String appended = settings.getString(columns[i].toString()
                    + IS_APPENDED, null);
            m_columnToAppended.put(columns[i], appended);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        for (String columnKey : m_columnToBins.keySet()) {
            NodeSettingsWO column = settings.addNodeSettings(columnKey);
            if (m_columnToAppended.get(columnKey) != null) {
                settings.addString(columnKey + IS_APPENDED, m_columnToAppended
                        .get(columnKey));
            } else {
                settings.addString(columnKey + IS_APPENDED, null);
            }
            Bin[] bins = m_columnToBins.get(columnKey);
            for (int b = 0; b < bins.length; b++) {
                NodeSettingsWO bin = column.addNodeSettings(bins[b]
                        .getBinName() + "_" + b);
                bins[b].saveToSettings(bin);
            }
        }
        settings.addStringArray(NUMERIC_COLUMNS, m_columnToAppended.keySet()
                .toArray(new String[0]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        StringBuffer sb = new StringBuffer();
        String[] columns = settings.getStringArray(NUMERIC_COLUMNS,
                new String[0]);
        if (columns == null) {
            sb.append("Numeric column array can't be 'null'\n");
        } else {
            for (int i = 0; i < columns.length; i++) {
                // appended or replaced
                settings.getString(columns[i].toString() + IS_APPENDED, null);
                double old = Double.NEGATIVE_INFINITY;
                if (columns[i] == null) {
                    sb.append("Column can't be 'null': " + i + "\n");
                    continue;
                }
                NodeSettingsRO set = settings.getNodeSettings(columns[i]
                        .toString());
                for (String binKey : set.keySet()) {
                    NodeSettingsRO bin = set.getNodeSettings(binKey);
                    NumericBin theBin = null;
                    try {
                        theBin = new NumericBin(bin);
                    } catch (InvalidSettingsException ise) {
                        sb.append(columns[i] + ": " + ise.getMessage() + "\n");
                        continue;
                    }
                    String binName = theBin.getBinName();
                    double l = theBin.getLeftValue();
                    if (l != old) {
                        sb.append(columns[i] + ": " + binName
                                + " check interval: " + "left=" + l
                                + ",oldright=" + old + "\n");
                    }
                    double r = theBin.getRightValue();
                    boolean lOpen = theBin.isLeftOpen();
                    boolean rOpen = theBin.isRightOpen();

                    if (r < l) {
                        sb.append(columns[i] + ": " + binName
                                + " check interval: " + "left=" + l + ",right="
                                + r + "\n");
                    } else {
                        if (r == l && !(!lOpen && !rOpen)) {
                            sb.append(columns[i] + ": " + binName
                                    + " check borders: " + "left=" + l
                                    + ",right=" + r + "\n");
                        }
                    }
                    old = r;
                }
                if (old != Double.POSITIVE_INFINITY) {
                    sb.append(columns[i] + ": check last right interval value="
                            + old + "\n");
                }
            }
        }

        if (sb.length() > 0) {
            throw new InvalidSettingsException(sb.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // No need to store anything.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Nothing to load.
    }
}
