/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.append.row;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.data.filter.column.FilterColumnRowInput;
import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedRowsIterator;
import org.knime.core.data.append.AppendedRowsRowInput;
import org.knime.core.data.append.AppendedRowsTable;
import org.knime.core.data.append.AppendedRowsTable.DuplicatePolicy;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteManager;
import org.knime.core.node.property.hilite.HiLiteTranslator;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;

/**
 * {@link org.knime.core.node.NodeModel} that concatenates its two input
 * table to one output table.
 *
 * @see AppendedRowsTable
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AppendedRowsNodeModel extends NodeModel {

    /** NodeSettings key flag if to fail on duplicate ids . This option was
     * added in v2.3 and will overrule the append suffix/skip flags if true. */
    static final String CFG_FAIL_ON_DUPLICATES = "fail_on_duplicates";

    /**
     * NodeSettings key if to append suffix (boolean). If false, skip the rows.
     */
    static final String CFG_APPEND_SUFFIX = "append_suffix";

    /** NodeSettings key: suffix to append. */
    static final String CFG_SUFFIX = "suffix";

    /** NodeSettings key: enable hiliting. */
    static final String CFG_HILITING = "enable_hiliting";

    /** NodeSettings key: Use only the intersection of columns. */
    static final String CFG_INTERSECT_COLUMNS = "intersection_of_columns";

    private boolean m_isFailOnDuplicate = false;

    private boolean m_isAppendSuffix = true;

    private String m_suffix = "_dup";

    private boolean m_isIntersection;

    private boolean m_enableHiliting;

    /** Hilite manager that summarizes both input handlers into one. */
    private final HiLiteManager m_hiliteManager = new HiLiteManager();
    /** Hilite translator for duplicate row keys. */
    private final HiLiteTranslator m_hiliteTranslator = new HiLiteTranslator();
    /** Default hilite handler used if hilite translation is disabled. */
    private final HiLiteHandler m_dftHiliteHandler = new HiLiteHandler();

    /**
     * Creates new node model with two inputs and one output.
     */
    public AppendedRowsNodeModel() {
        super(2, 1);
    }

    /** Create new node with given number of inputs. All inputs except the first
     * one are declared as optional.
     * @param nrIns Nr inputs, must be >=1.
     */
    AppendedRowsNodeModel(final int nrIns) {
        super(getInPortTypes(nrIns), new PortType[] {BufferedDataTable.TYPE});
    }

    private static final PortType[] getInPortTypes(final int nrIns) {
        if (nrIns < 1) {
            throw new IllegalArgumentException("invalid input count: " + nrIns);
        }
        PortType[] result = new PortType[nrIns];
        Arrays.fill(result, BufferedDataTable.TYPE_OPTIONAL);
        result[0] = BufferedDataTable.TYPE;
        return result;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] rawInData,
        final ExecutionContext exec) throws Exception {

        // remove all null tables first (optional input data)
        BufferedDataTable[] noNullArray = noNullArray(rawInData);
        DataTableSpec[] noNullSpecs = new DataTableSpec[noNullArray.length];
        for (int i = 0; i < noNullArray.length; i++) {
            noNullSpecs[i] = noNullArray[i].getDataTableSpec();
        }

        //table can only be wrapped if a suffix is to be append or the node fails in case of duplicate row ID's
        if (m_isAppendSuffix || m_isFailOnDuplicate) {
            //just wrap the tables virtually instead of traversing it and copying the rows

            //virtually create the concatenated table (no traverse necessary)
            Optional<String> suffix = m_isAppendSuffix ? Optional.of(m_suffix) : Optional.empty();
            BufferedDataTable concatTable = exec.createConcatenateTable(exec, suffix, m_isFailOnDuplicate, noNullArray);
            if (m_isIntersection) {
                //wrap the table and filter the non-intersecting columns
                DataTableSpec actualOutSpec = getOutputSpec(noNullSpecs);
                DataTableSpec currentOutSpec = concatTable.getDataTableSpec();
                String[] intersectCols = getIntersection(actualOutSpec, currentOutSpec);
                ColumnRearranger cr = new ColumnRearranger(currentOutSpec);
                cr.keepOnly(intersectCols);
                concatTable = exec.createColumnRearrangeTable(concatTable, cr, exec);
            }
            if (m_enableHiliting) {
                AppendedRowsTable tmp = new AppendedRowsTable(DuplicatePolicy.Fail, null, noNullArray);
                Map<RowKey, Set<RowKey>> map =
                    createHiliteTranslationMap(createDuplicateMap(tmp, exec, m_suffix == null ? "" : m_suffix));
                m_hiliteTranslator.setMapper(new DefaultHiLiteMapper(map));
            }
            return new BufferedDataTable[]{concatTable};
        } else {
            //traverse the table and copy the rows
            long totalRowCount = 0L;
            RowInput[] inputs = new RowInput[noNullArray.length];
            for (int i = 0; i < noNullArray.length; i++) {
                totalRowCount += noNullArray[i].size();
                inputs[i] = new DataTableRowInput(noNullArray[i]);
            }
            DataTableSpec outputSpec = getOutputSpec(noNullSpecs);
            BufferedDataTableRowOutput output = new BufferedDataTableRowOutput(exec.createDataContainer(outputSpec));
            run(inputs, output, exec, totalRowCount);
            return new BufferedDataTable[]{output.getDataTable()};
        }

    }

    private static BufferedDataTable[] noNullArray(final BufferedDataTable[] rawInData) {
        List<BufferedDataTable> nonNullList = new ArrayList<BufferedDataTable>();
        for (BufferedDataTable t : rawInData) {
            if (t != null) {
                nonNullList.add(t);
            }
        }
        return nonNullList.toArray(new BufferedDataTable[nonNullList.size()]);
    }

    private static RowInput[] noNullArray(final RowInput[] rawInData) {
        List<RowInput> nonNullList = new ArrayList<RowInput>();
        for (RowInput t : rawInData) {
            if (t != null) {
                nonNullList.add(t);
            }
        }
        return nonNullList.toArray(new RowInput[nonNullList.size()]);
    }

    void run(final RowInput[] inputs, final RowOutput output,
        final ExecutionMonitor exec, final long totalRowCount) throws Exception {
        RowInput[] corrected;
        if (m_isIntersection) {
            final RowInput[] noNullArray = noNullArray(inputs);
            corrected = new RowInput[noNullArray.length];
            DataTableSpec[] inSpecs = new DataTableSpec[noNullArray.length];
            for (int i = 0; i < noNullArray.length; i++) {
                inSpecs[i] = noNullArray[i].getDataTableSpec();
            }
            String[] intersection = getIntersection(inSpecs);
            for (int i = 0; i < noNullArray.length; i++) {
                corrected[i] = new FilterColumnRowInput(noNullArray[i], intersection);
            }
        } else {
            corrected = inputs;
        }

        AppendedRowsTable.DuplicatePolicy duplPolicy;
        if (m_isFailOnDuplicate) {
            duplPolicy = AppendedRowsTable.DuplicatePolicy.Fail;
        } else if (m_isAppendSuffix) {
            duplPolicy = AppendedRowsTable.DuplicatePolicy.AppendSuffix;
        } else {
            duplPolicy = AppendedRowsTable.DuplicatePolicy.Skip;
        }
        AppendedRowsRowInput appendedInput = AppendedRowsRowInput.create(corrected,
            duplPolicy, m_suffix, exec, totalRowCount);
        try {
            DataRow next;
            // note, this iterator throws runtime exceptions when canceled.
            while ((next = appendedInput.poll()) != null) {
                // may throw exception, also sets progress
                output.push(next);
            }
        } catch (AppendedRowsIterator.RuntimeCanceledExecutionException rcee) {
            throw rcee.getCause();
        } finally {
            output.close();
        }
        if (appendedInput.getNrRowsSkipped() > 0) {
            setWarningMessage("Filtered out " + appendedInput.getNrRowsSkipped() + " duplicate row(s).");
        }
        if (m_enableHiliting) {
            Map<RowKey, Set<RowKey>> map = createHiliteTranslationMap(appendedInput.getDuplicateNameMap());
            m_hiliteTranslator.setMapper(new DefaultHiLiteMapper(map));
        }
    }

    private DataTableSpec getOutputSpec(final DataTableSpec[] nonNullInSpecs) throws InvalidSettingsException {
        DataTableSpec[] corrected;
        if (m_isIntersection) {
            corrected = new DataTableSpec[nonNullInSpecs.length];
            String[] intersection = getIntersection(nonNullInSpecs);
            for (int i = 0; i < nonNullInSpecs.length; i++) {
                corrected[i] = FilterColumnTable.createFilterTableSpec(
                    nonNullInSpecs[i], intersection);
            }
        } else {
            corrected = nonNullInSpecs;
        }
        return AppendedRowsTable.generateDataTableSpec(corrected);
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] rawInSpecs) throws InvalidSettingsException {
        List<DataTableSpec> noNullSpecList = new ArrayList<>();
        for (DataTableSpec s : rawInSpecs) {
            if (s != null) {
                noNullSpecList.add(s);
            }
        }
        DataTableSpec[] noNullSpecs = noNullSpecList.toArray(new DataTableSpec[noNullSpecList.size()]);
        DataTableSpec outputSpec = getOutputSpec(noNullSpecs);
        return new DataTableSpec[] {outputSpec};
    }

    /**
     * Determines the names of columns that appear in all specs.
     *
     * @param specs specs to check
     * @return column names that appear in all columns
     */
    static String[] getIntersection(final DataTableSpec... specs) {
        LinkedHashSet<String> hash = new LinkedHashSet<String>();
        if (specs.length > 0) {
            for (DataColumnSpec c : specs[0]) {
                hash.add(c.getName());
            }
        }
        LinkedHashSet<String> hash2 = new LinkedHashSet<String>();
        for (int i = 1; i < specs.length; i++) {
            hash2.clear();
            for (DataColumnSpec c : specs[i]) {
                hash2.add(c.getName());
            }
            hash.retainAll(hash2);
        }
        return hash.toArray(new String[hash.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
            return new StreamableOperator() {
                @Override
                public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                    throws Exception {
                    List<RowInput> noNullList = new ArrayList<RowInput>();
                    for (PortInput p : inputs) {
                        if (p != null) {
                            noNullList.add((RowInput)p);
                        }
                    }
                    RowInput[] rowInputs = noNullList.toArray(new RowInput[noNullList.size()]);
                    run(rowInputs, (RowOutput)outputs[0], exec, -1);
                }
            };
    }

    /** {@inheritDoc} */
    @Override
    public InputPortRole[] getInputPortRoles() {
        InputPortRole[] result = new InputPortRole[getNrInPorts()];
        Arrays.fill(result, InputPortRole.NONDISTRIBUTED_STREAMABLE);
        return result;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // added in v2.3
        settings.addBoolean(CFG_FAIL_ON_DUPLICATES, m_isFailOnDuplicate);
        settings.addBoolean(CFG_APPEND_SUFFIX, m_isAppendSuffix);
        settings.addBoolean(CFG_INTERSECT_COLUMNS, m_isIntersection);
        if (m_suffix != null) {
            settings.addString(CFG_SUFFIX, m_suffix);
        }
        settings.addBoolean(CFG_HILITING, m_enableHiliting);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getBoolean(CFG_INTERSECT_COLUMNS);
        // added v2.3
        boolean isFailOnDuplicate =
            settings.getBoolean(CFG_FAIL_ON_DUPLICATES, false);
        boolean appendSuffix = settings.getBoolean(CFG_APPEND_SUFFIX);
        if (isFailOnDuplicate) {
            // ignore suffix
        } else if (appendSuffix) {
            String suffix = settings.getString(CFG_SUFFIX);
            if (suffix == null || suffix.equals("")) {
                throw new InvalidSettingsException("Invalid suffix: " + suffix);
            }
        } else { // skip duplicates
            // ignore suffix
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_isIntersection = settings.getBoolean(CFG_INTERSECT_COLUMNS);
        // added in v2.3
        m_isFailOnDuplicate =
            settings.getBoolean(CFG_FAIL_ON_DUPLICATES, false);
        m_isAppendSuffix = settings.getBoolean(CFG_APPEND_SUFFIX);
        if (m_isAppendSuffix) {
            m_suffix = settings.getString(CFG_SUFFIX);
        } else {
            // may be in there, but must not necessarily
            m_suffix = settings.getString(CFG_SUFFIX, m_suffix);
        }
        m_enableHiliting = settings.getBoolean(CFG_HILITING, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_hiliteManager.removeAllToHiliteHandlers();
        m_hiliteManager.addToHiLiteHandler(
                m_hiliteTranslator.getFromHiLiteHandler());
        m_hiliteManager.addToHiLiteHandler(getInHiLiteHandler(0));
        m_hiliteTranslator.removeAllToHiliteHandlers();
        m_hiliteTranslator.addToHiLiteHandler(getInHiLiteHandler(1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        if (m_enableHiliting) {
            final NodeSettingsRO config = NodeSettings.loadFromXML(
                    new GZIPInputStream(new FileInputStream(
                    new File(nodeInternDir, "hilite_mapping.xml.gz"))));
            try {
                m_hiliteTranslator.setMapper(DefaultHiLiteMapper.load(config));
            } catch (final InvalidSettingsException ex) {
                throw new IOException(ex.getMessage());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        if (m_enableHiliting) {
            final NodeSettings config = new NodeSettings("hilite_mapping");
            ((DefaultHiLiteMapper) m_hiliteTranslator.getMapper()).save(config);
            config.saveToXML(new GZIPOutputStream(new FileOutputStream(new File(
                    nodeInternDir, "hilite_mapping.xml.gz"))));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex,
            final HiLiteHandler hiLiteHdl) {
        super.setInHiLiteHandler(inIndex, hiLiteHdl);
        if (inIndex == 0) {
            m_hiliteManager.removeAllToHiliteHandlers();
            m_hiliteManager.addToHiLiteHandler(
                    m_hiliteTranslator.getFromHiLiteHandler());
            m_hiliteManager.addToHiLiteHandler(hiLiteHdl);
        } else if (inIndex == 1) {
            m_hiliteTranslator.removeAllToHiliteHandlers();
            m_hiliteTranslator.addToHiLiteHandler(hiLiteHdl);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        if (m_enableHiliting) {
            return m_hiliteManager.getFromHiLiteHandler();
        } else {
            return m_dftHiliteHandler;
        }
    }

    private Map<RowKey, RowKey> createDuplicateMap(final DataTable table, final ExecutionContext exec, final String suffix) throws CanceledExecutionException {
        Map<RowKey, RowKey> duplicateMap = new HashMap<RowKey, RowKey>();

        RowIterator it = table.iterator();
        DataRow row;
        while (it.hasNext()) {
            row = it.next();
            RowKey origKey = row.getKey();
            RowKey key = origKey;
            while (duplicateMap.containsKey(key)) {
                exec.checkCanceled();
                String newId = key.toString() + suffix;
                key = new RowKey(newId);
            }
            duplicateMap.put(key, origKey);
        }
        return duplicateMap;
    }

    private Map<RowKey, Set<RowKey>> createHiliteTranslationMap(final Map<RowKey, RowKey> dupMap) {
     // create hilite translation map
        Map<RowKey, Set<RowKey>> map = new HashMap<RowKey, Set<RowKey>>();
        // map of all RowKeys and duplicate RowKeys in the resulting table
        for (Map.Entry<RowKey, RowKey> e : dupMap.entrySet()) {
            // if a duplicate key
            if (!e.getKey().equals(e.getValue())) {
                Set<RowKey> set = Collections.singleton(e.getValue());
                // put duplicate key and original key into map
                map.put(e.getKey(), set);
            } else {
                // skip duplicate keys
                if (!dupMap.containsKey(new RowKey(e.getKey().getString()
                        + m_suffix))) {
                    Set<RowKey> set = Collections.singleton(e.getValue());
                    map.put(e.getKey(), set);
                }
            }
        }
        return map;
    }
}
