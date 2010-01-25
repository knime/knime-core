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
 * -------------------------------------------------------------------
 * 
 * History
 *   May 19, 2006 (wiswedel): created
 */
package org.knime.base.node.io.table.read;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.util.NonClosableInputStream;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * NodeMode for table that reads the file as written from the
 * Write table node.
 * @author wiswedel, University of Konstanz
 */
public class ReadTableNodeModel extends NodeModel {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(ReadTableNodeModel.class);
    
    /** Identifier for the node settings object. */
    static final String CFG_FILENAME = "filename";
    
    /** The extension of the files to store, \".knime\". */
    public static final String PREFERRED_FILE_EXTENSION = ".table";
    
    private final SettingsModelString m_fileName = 
        new SettingsModelString(CFG_FILENAME, null);

    /**
     * Creates new model with no inputs, one output.
     */
    public ReadTableNodeModel() {
        super(0, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_fileName.getStringValue() != null) {
            m_fileName.saveSettingsTo(settings);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_fileName.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_fileName.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        File f = new File(m_fileName.getStringValue());
        DataTable table = DataContainer.readFromZip(f);
        BufferedDataTable out = exec.createBufferedDataTable(table, exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_fileName.getStringValue() == null) {
            throw new InvalidSettingsException("No file set.");
        }
        try {
            File f = new File(m_fileName.getStringValue());
            if (!f.isFile()) {
                throw new InvalidSettingsException(
                        "No such file: " + m_fileName.getStringValue());
            }
            DataTableSpec spec = peekDataTableSpec(f);
            if (spec == null) { // if written with 1.3.x and before
                LOGGER.debug("Table spec is not first entry in input file, " 
                        + "need to deflate entire file");
                DataTable outTable = DataContainer.readFromZip(f);
                spec = outTable.getDataTableSpec();
            }
            return new DataTableSpec[]{spec};
        } catch (IOException ioe) {
            String message = ioe.getMessage();
            if (message == null) {
                message = "Unable to read spec from file, "
                    + "no detailed message available.";
            }
            throw new InvalidSettingsException(message);
            
        }
    }
    
    /** Opens the zip file and checks whether the first entry is the spec. If
     * so, the spec is parsed and returned. Otherwise null is returned.
     * 
     * <p> This method is used to fix bug #1141: Dialog closes very slowly.
     * @param file To read from.
     * @return The spec or null (null will be returned when the file was
     * written with a version prior 2.0)
     * @throws IOException If that fails for any reason.
     */
    private DataTableSpec peekDataTableSpec(final File file) 
        throws IOException {
        // must not use ZipFile here as it is known to have memory problems
        // on large files, see e.g. 
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5077277
        ZipInputStream zipIn = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(file)));
        ZipEntry entry = zipIn.getNextEntry();
        try {
            // hardcoded constants here as we do not want additional 
            // functionality to DataContainer ... at least not yet.
            if ("spec.xml".equals(entry != null ? entry.getName() : "")) {
                NodeSettingsRO settings = NodeSettings.loadFromXML(
                        new NonClosableInputStream.Zip(zipIn));
                try {
                    NodeSettingsRO specSettings = 
                        settings.getNodeSettings("table.spec");
                    return DataTableSpec.load(specSettings);
                } catch (InvalidSettingsException ise) {
                    IOException ioe = new IOException(
                    "Unable to read spec from file");
                    ioe.initCause(ise);
                    throw ioe;
                }
            } else {
                return null;
            }
        } finally {
            zipIn.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to load
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // not internals to save
    }
}
