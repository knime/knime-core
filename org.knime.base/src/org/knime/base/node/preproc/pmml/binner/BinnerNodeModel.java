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
package org.knime.base.node.preproc.pmml.binner;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.preproc.pmml.binner.BinnerColumnFactory.Bin;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortObjectOutput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableOperator;

/**
 * Bins numeric columns into intervals which are then returned as string-type
 * columns.
 *
 * @author Thomas Gabriel, University of Konstanz
 * @since 3.1
 */
public class BinnerNodeModel extends NodeModel {
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
    static final int DATA_INPORT = 0;

    /** Keeps index of the optional model port which is 1. */
    static final int MODEL_INPORT = 1;

    /** Keeps index of the output port which is 0. */
    static final int OUTPORT = 0;

    private final boolean m_pmmlInEnabled;

    private final boolean m_pmmlOutEnabled;

    /** Creates a new binner. */
  BinnerNodeModel() {
      this(true, true);
  }

    /** Creates a new binner.
     * @param pmmlInEnabled
     * @param pmmlOutEnabled */
   protected BinnerNodeModel(final boolean pmmlInEnabled, final boolean pmmlOutEnabled) {
        super(pmmlInEnabled ? new PortType[] {BufferedDataTable.TYPE,
                PMMLPortObject.TYPE_OPTIONAL} : new PortType[] {BufferedDataTable.TYPE},
            pmmlOutEnabled ? new PortType[] {BufferedDataTable.TYPE, PMMLPortObject.TYPE} : new PortType[] {BufferedDataTable.TYPE});
        m_pmmlInEnabled = pmmlInEnabled;
        m_pmmlOutEnabled = pmmlOutEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inPorts,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable inData = (BufferedDataTable)inPorts[DATA_INPORT];
        DataTableSpec spec = inData.getDataTableSpec();
        ColumnRearranger colReg = createColumnRearranger(spec);
        BufferedDataTable buf = exec.createColumnRearrangeTable(inData,
                colReg, exec);

        if (!m_pmmlOutEnabled) {
            return new PortObject[]{buf};
        }

        // handle the optional PMML in port (can be null)
        PMMLPortObject inPMMLPort = m_pmmlInEnabled ? (PMMLPortObject)inPorts[1] : null;
        PMMLPortObject outPMMLPort = createPMMLModel(inPMMLPort, spec, buf.getDataTableSpec());

        return new PortObject[]{buf, outPMMLPort};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
        throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec) throws Exception {
                ColumnRearranger colre = createColumnRearranger((DataTableSpec)inSpecs[0]);
                colre.createStreamableFunction(0, 0).runFinal(inputs, outputs, exec);

                if (m_pmmlOutEnabled) {
                    // handle the optional PMML in port (can be null)
                    PMMLPortObject inPMMLPort =
                        m_pmmlInEnabled ? (PMMLPortObject)((PortObjectInput)inputs[1]).getPortObject() : null;
                    PMMLPortObject outPMMLPort = createPMMLModel(inPMMLPort, (DataTableSpec) inSpecs[0], colre.createSpec());
                    ((PortObjectOutput) outputs[1]).setPortObject(outPMMLPort);
                }
            }
        };
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        InputPortRole[] roles = new InputPortRole[getNrInPorts()];
        roles[0] = InputPortRole.DISTRIBUTED_STREAMABLE;
        if(m_pmmlOutEnabled) {
            roles[0] = InputPortRole.NONDISTRIBUTED_STREAMABLE;
        }
        if(m_pmmlInEnabled) {
            roles[1] = InputPortRole.NONDISTRIBUTED_NONSTREAMABLE;
        }
        return roles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        OutputPortRole[] roles = new OutputPortRole[getNrOutPorts()];
        roles[0] = OutputPortRole.DISTRIBUTED;
        if(m_pmmlOutEnabled) {
            roles[1] = OutputPortRole.NONDISTRIBUTED;
        }
        return roles;
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
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec inDataSpec = (DataTableSpec)inSpecs[DATA_INPORT];
        PMMLPortObjectSpec inModelSpec
                = m_pmmlInEnabled ? (PMMLPortObjectSpec)inSpecs[MODEL_INPORT] : null;
        for (String columnKey : m_columnToBins.keySet()) {
            assert m_columnToAppended.containsKey(columnKey) : columnKey;
            if (!inDataSpec.containsName(columnKey)) {
                throw new InvalidSettingsException("Binner: column \"" + columnKey
                    + "\" not found in spec.");
            }
            if (!inDataSpec.getColumnSpec(columnKey).getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Binner: column \"" + columnKey
                    + "\" not compatible with double-type.");
            }
            String appended = m_columnToAppended.get(columnKey);
            if (appended != null) {
                if (inDataSpec.containsName(appended)) {
                    throw new InvalidSettingsException("Binner: duplicate "
                            + "appended column \"" + appended + "\" in spec.");
                }
            }
        }
        // set warning when no binning is defined
        if (m_columnToBins.isEmpty()) {
            super.setWarningMessage("No column select for binning.");
        }
        // generate numeric binned table spec
        DataTableSpec outDataSpec = createColumnRearranger(inDataSpec).createSpec();
        if (!m_pmmlOutEnabled) {
            return new PortObjectSpec[]{outDataSpec};
        }
        PMMLPortObjectSpecCreator pmmlSpecCreator
            = new PMMLPortObjectSpecCreator(inModelSpec, outDataSpec);
        return new PortObjectSpec[]{outDataSpec, pmmlSpecCreator.createSpec()};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec) {
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
     * Creates the pmml port object.
     * @param the in-port pmml object. Can be <code>null</code> (optional in-port)
     */
    private PMMLPortObject createPMMLModel(final PMMLPortObject inPMMLPort, final DataTableSpec inSpec, final DataTableSpec outSpec) {
        PMMLBinningTranslator trans =
            new PMMLBinningTranslator(m_columnToBins, m_columnToAppended, new DerivedFieldMapper(inPMMLPort));
        PMMLPortObjectSpecCreator pmmlSpecCreator = new PMMLPortObjectSpecCreator(inPMMLPort, outSpec);
        PMMLPortObject outPMMLPort = new PMMLPortObject(pmmlSpecCreator.createSpec(), inPMMLPort, inSpec);
        outPMMLPort.addGlobalTransformations(trans.exportToTransDict());
        return outPMMLPort;
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
