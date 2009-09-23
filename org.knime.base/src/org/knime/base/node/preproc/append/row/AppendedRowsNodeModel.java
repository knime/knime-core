/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 */
package org.knime.base.node.preproc.append.row;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.data.append.row.AppendedRowsIterator;
import org.knime.base.data.append.row.AppendedRowsTable;
import org.knime.base.data.append.row.AppendedRowsIterator.RuntimeCanceledExecutionException;
import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteManager;
import org.knime.core.node.property.hilite.HiLiteTranslator;

/**
 * {@link org.knime.core.node.NodeModel} that concatenates its two input
 * table to one output table.
 *
 * @see AppendedRowsTable
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AppendedRowsNodeModel extends NodeModel {

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

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTable[] corrected;
        int totalRowCount = 0;
        for (BufferedDataTable t : inData) {
            totalRowCount += t.getRowCount();
        }
        if (m_isIntersection) {
            DataTableSpec[] inSpecs = new DataTableSpec[inData.length];
            for (int i = 0; i < inData.length; i++) {
                inSpecs[i] = inData[i].getDataTableSpec();
            }
            corrected = new DataTable[inData.length];
            String[] intersection = getIntersection(inSpecs);
            for (int i = 0; i < inData.length; i++) {
                corrected[i] = new FilterColumnTable(inData[i], intersection);
            }
        } else {
            corrected = inData;
        }

        AppendedRowsTable out = new AppendedRowsTable(
                (m_isAppendSuffix ? m_suffix : null), corrected);
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
                    + " duplicate row(s).");
        }
        if (m_enableHiliting) {
            // create hilite translation
            Map<RowKey, Set<RowKey>> map = new HashMap<RowKey, Set<RowKey>>();
            // set of all keys in the resulting table
            Set<RowKey> dupHash = it.getDuplicateHash();
            for (RowKey key : dupHash) {
                Set<RowKey> set = new HashSet<RowKey>(1);
                // if a duplicate key
                if (key.getString().endsWith(m_suffix)) {
                    String str = key.getString();
                    str = str.substring(0, str.lastIndexOf(m_suffix));
                    set.add(new RowKey(str));
                    // put duplicate key and original key into map
                    map.put(key, set);
                } else {
                    // skip duplicate keys
                    if (!dupHash.contains(new RowKey(
                            key.getString() + m_suffix))) {
                        set.add(key);
                        map.put(key, set);
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec[] corrected;
        if (m_isIntersection) {
            corrected = new DataTableSpec[inSpecs.length];
            String[] intersection = getIntersection(inSpecs);
            for (int i = 0; i < inSpecs.length; i++) {
                corrected[i] = FilterColumnTable.createFilterTableSpec(
                        inSpecs[i], intersection);
            }
        } else {
            corrected = inSpecs;
        }
        DataTableSpec[] outSpecs = new DataTableSpec[1];
        outSpecs[0] = AppendedRowsTable.generateDataTableSpec(corrected);
        return outSpecs;
    }

    /**
     * Determines the names of columns that appear in all specs.
     *
     * @param specs specs to check
     * @return column names that appear in all columns
     */
    static String[] getIntersection(final DataTableSpec... specs) {
        LinkedHashSet<String> hash = new LinkedHashSet<String>();
        for (DataColumnSpec c : specs[0]) {
            hash.add(c.getName());
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
    protected void saveSettingsTo(final NodeSettingsWO settings) {
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
        m_isIntersection = settings.getBoolean(CFG_INTERSECT_COLUMNS);
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
