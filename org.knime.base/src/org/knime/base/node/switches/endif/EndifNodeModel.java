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
 * ---------------------------------------------------------------------
 *
 * History
 *   Sept 17 2008 (mb): created (from wiswedel's TableToVariableNode)
 */
package org.knime.base.node.switches.endif;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.data.append.row.AppendedRowsIterator;
import org.knime.base.data.append.row.AppendedRowsIterator.RuntimeCanceledExecutionException;
import org.knime.base.data.append.row.AppendedRowsTable;
import org.knime.base.node.io.pmml.write.PMMLWriterNodeModel;
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
public class EndifNodeModel extends NodeModel
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
    protected EndifNodeModel() {
        super(2, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        if (inSpecs[0] instanceof InactiveBranchPortObjectSpec) {
            return new PortObjectSpec[]{inSpecs[1]};
        }
        if (inSpecs[1] instanceof InactiveBranchPortObjectSpec) {
            return new PortObjectSpec[]{inSpecs[0]};
        }
        // no inactive branch - check compatibility of specs - which in
        // this case must be BFT Specs!
        DataTableSpec spec0 = (DataTableSpec)inSpecs[0];
        DataTableSpec spec1 = (DataTableSpec)inSpecs[1];
        if (spec0.equalStructure(spec1)) {
            return new PortObjectSpec[]{inSpecs[0]};
        }
        // incompatible - refuse to configure
        throw new InvalidSettingsException("The structures of both active input"
                + " data tables are not compatible.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] rawInData,
            final ExecutionContext exec) throws Exception {
        if (m_enableHiliting) {
            // create empty hilite translation map (so we correctly
            // handle the internals even if we return with a IBPO:
            Map<RowKey, Set<RowKey>> map = new HashMap<RowKey, Set<RowKey>>();
            m_hiliteTranslator.setMapper(new DefaultHiLiteMapper(map));
        }
        if (rawInData[0] instanceof InactiveBranchPortObject) {
            return new PortObject[]{rawInData[1]};
        }
        if (rawInData[1] instanceof InactiveBranchPortObject) {
            return new PortObject[]{rawInData[0]};
        }
        // no inactive branch - check compatibility of specs - which in
        // this case must be BFT Specs!
        DataTableSpec spec0 = (DataTableSpec)(rawInData[0].getSpec());
        DataTableSpec spec1 = (DataTableSpec)(rawInData[1].getSpec());
        if (spec0.equalStructure(spec1)) {
            // concatenate tables and return result
            BufferedDataTable[] inData = new BufferedDataTable[2];
            inData[0] = (BufferedDataTable)rawInData[0];
            inData[1] = (BufferedDataTable)rawInData[1];
            int totalRowCount = 0;
            for (BufferedDataTable t : inData) {
                totalRowCount += t.getRowCount();
            }

            AppendedRowsTable out = new AppendedRowsTable(
                    (m_isAppendSuffix ? m_suffix : null), inData);
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
        throw new Exception("Both input ports have data but the tables "
                + "have incompatible specs");
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
