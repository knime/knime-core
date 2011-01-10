/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   Sept 17 2008 (mb): created (from wiswedel's TableToVariableNode)
 */
package org.knime.base.node.switches.endcase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.data.append.row.AppendedRowsIterator;
import org.knime.base.data.append.row.AppendedRowsIterator.RuntimeCanceledExecutionException;
import org.knime.base.data.append.row.AppendedRowsTable;
import org.knime.base.node.io.pmml.write.PMMLWriterNodeModel;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.inactive.InactiveBranchConsumer;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteManager;
import org.knime.core.node.property.hilite.HiLiteTranslator;

/**
 * End of an IF Statement. Takes the data from one or both input ports
 * (whereever it is available. If both input ports carry data the specs must
 * match and the two tables will be concatenated.
 *
 * @author M. Berthold, University of Konstanz
 */
public class EndcaseNodeModel extends NodeModel
implements InactiveBranchConsumer {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(PMMLWriterNodeModel.class);

    /** NodeSettings key if to append suffix. If false, skip the rows. */
    static final String CFG_APPEND_SUFFIX = "append_suffix";

    /** NodeSettings key: suffix to append. */
    static final String CFG_SUFFIX = "suffix";

    /** NodeSettings key: enable hiliting. */
    static final String CFG_HILITING = "enable_hiliting";

    private boolean m_isAppendSuffix = true;
    private String m_suffix = "_dup";
    private boolean m_enableHiliting;

    /** Hilite manager that summarizes both input handlers into one. */
    private final HiLiteManager m_hiliteManager = new HiLiteManager();
    /** Hilite translator for duplicate row keys. */
    private final HiLiteTranslator m_hiliteTranslator = new HiLiteTranslator();
    /** Default hilite handler used if hilite translation is disabled. */
    private final HiLiteHandler m_dftHiliteHandler = new HiLiteHandler();

    /**
     * Two inputs, one output.
     */
    protected EndcaseNodeModel() {
        super(createInTypes(3), new PortType[] {BufferedDataTable.TYPE});
    }

    private static PortType[] createInTypes(final int nrIns) {
        PortType[] types = new PortType[nrIns];
        Arrays.fill(types, new PortType(BufferedDataTable.class, true));
        types[0] = BufferedDataTable.TYPE;
        return types;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        Vector<DataTableSpec> specs = new Vector<DataTableSpec>();
        for (int i = 0; i < getNrInPorts(); i++) {
            if (inSpecs[i] != null) {
                // if connected...
                if (!(inSpecs[i] instanceof InactiveBranchPortObjectSpec)) {
                    // ...and active, add it:
                    specs.add((DataTableSpec)inSpecs[i]);
                }
            }
        }
        if (specs.size() == 0) {
            // all inactive or not connected, return first spec (which must
            // be connected!)
            assert inSpecs[0] instanceof InactiveBranchPortObjectSpec;
            return new PortObjectSpec[]{inSpecs[0]};
        }
        assert specs.size() > 0;
        // check compatibility of specs against first spec in list
        for (int i = 1; i < specs.size(); i++) {
            if (!(specs.get(0).equalStructure(specs.get(i)))) {
                // incompatible - refuse to configure
                throw new InvalidSettingsException("The table structures of"
                        + " active ports are not compatible.");
            }
        }
        // all ok, return first spec:
        return new PortObjectSpec[]{specs.get(0)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        Vector<BufferedDataTable> tables = new Vector<BufferedDataTable>();
        for (int i = 0; i < getNrInPorts(); i++) {
            if (inData[i] != null) {
                // if connected...
                if (!(inData[i] instanceof InactiveBranchPortObject)) {
                    // ...and active, add it:
                    tables.add((BufferedDataTable)inData[i]);
                }
            }
        }
        if (tables.size() == 0) {
            // all inactive or not connected, return first PO (which must
            // be connected!)
            assert inData[0] instanceof InactiveBranchPortObject;
            if (m_enableHiliting) {
                // create empty hilite translation map (so we correctly
                // handle the internals).
                Map<RowKey, Set<RowKey>> map =
                        new HashMap<RowKey, Set<RowKey>>();
                m_hiliteTranslator.setMapper(new DefaultHiLiteMapper(map));
            }
            return new PortObject[]{inData[0]};
        }
        assert tables.size() > 0;
        // check compatibility of specs against first spec in list
        for (int i = 1; i < tables.size(); i++) {
            if (!(tables.get(0).getSpec().equalStructure(
                    tables.get(i).getSpec()))) {
                // incompatible - refuse to execute
                throw new Exception("The data table structures of the active "
                        + "ports are not compatible.");
            }
        }

        int totalRowCount = 0;
        DataTable[] dtables = new DataTable[tables.size()];
        int i = 0;
        for (BufferedDataTable t : tables) {
            totalRowCount += t.getRowCount();
            dtables[i] = t;
            i++;
        }

        AppendedRowsTable out = new AppendedRowsTable(
                (m_isAppendSuffix ? m_suffix : null), dtables);
        // note, this iterator throws runtime exceptions when canceled.
        AppendedRowsIterator it = out.iterator(exec, totalRowCount);
        BufferedDataContainer c =
            exec.createDataContainer(out.getDataTableSpec());
        try {
            while (it.hasNext()) {
                // may throw exception, also sets progress
                c.addRowToTable(it.next());
            }
        } catch (RuntimeCanceledExecutionException rcee) {
            throw rcee.getCause();
        } finally {
            c.close();
        }
        if (it.getNrRowsSkipped() > 0) {
            setWarningMessage("Filtered out " + it.getNrRowsSkipped()
                    + " duplicate row id(s).");
        }
        if (m_enableHiliting) {
            // create hilite translation map
            Map<RowKey, Set<RowKey>> map = new HashMap<RowKey, Set<RowKey>>();
            // map of all RowKeys and duplicate RowKeys in the resulting table
            Map<RowKey, RowKey> dupMap = it.getDuplicateNameMap();
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
            m_hiliteTranslator.setMapper(new DefaultHiLiteMapper(map));
        }
        return new BufferedDataTable[]{c.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_APPEND_SUFFIX, m_isAppendSuffix);
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
        boolean appendSuffix = settings.getBoolean(CFG_APPEND_SUFFIX);
        if (appendSuffix) {
            String suffix = settings.getString(CFG_SUFFIX);
            if (suffix == null || suffix.equals("")) {
                throw new InvalidSettingsException("Invalid suffix: " + suffix);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
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
}
