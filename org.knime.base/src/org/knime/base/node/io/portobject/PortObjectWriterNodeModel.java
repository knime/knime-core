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
 * History
 *   29.10.2005 (mb): created
 */
package org.knime.base.node.io.portobject;

import java.io.File;
import java.io.IOException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortUtil;


/**
 * Write ModelContent object into file.
 * 
 * @author M. Berthold, University of Konstanz
 */
class PortObjectWriterNodeModel extends NodeModel {

    /** key for filename entry in config object. */
    static final String FILENAME = "filename";

    /** Config identifier for overwrite OK. */
    static final String CFG_OVERWRITE_OK = "overwriteOK";
    
    private final SettingsModelString m_fileName = 
        new SettingsModelString(FILENAME, null);

    private final SettingsModelBoolean m_overwriteOK =
        new SettingsModelBoolean(CFG_OVERWRITE_OK, false);
    
    /**
     * Constructor: Create new NodeModel with only one Model Input Port.
     * @param writeType The type of the input port.
     */
    PortObjectWriterNodeModel(final PortType writeType) {
        super(new PortType[]{writeType}, new PortType[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_fileName.saveSettingsTo(settings);
        m_overwriteOK.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        SettingsModelString fileNameModel = 
            m_fileName.createCloneWithValidatedValue(settings);
        String fileName = fileNameModel.getStringValue();
        if (fileName == null || fileName.length() == 0) {
            throw new InvalidSettingsException("No output file specified");
        }
        // must not verify overwriteOK (added in v2.1)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_fileName.loadSettingsFrom(settings);
        try {
            // property added in v2.1 -- if missing (old flow), set it to true
            m_overwriteOK.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            m_overwriteOK.setBooleanValue(true);
        }
    }

    /**
     * Writes model as ModelContent to file.
     * 
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] portObject,
            final ExecutionContext exec) throws Exception {
        String fileName = m_fileName.getStringValue();
        checkFileAccess(fileName);
        File realFile = new File(fileName);
        if (realFile.exists()) {
            realFile.delete();
        }
        try {
            PortUtil.writeObjectToFile(portObject[0], realFile, exec);
        } catch (Exception e) {
            realFile.delete();
            throw e;
        }
        return new PortObject[0];
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
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        String fileName = m_fileName.getStringValue();
        checkFileAccess(fileName);
        if (new File(fileName).exists()) {
            // here it exists and we can write it: warn user!
            setWarningMessage("Selected output file \"" + fileName + "\"" 
                    + " exists and will be overwritten!");
        }
        return new PortObjectSpec[0];
    }

    /**
     * Helper that checks some properties for the file argument.
     * 
     * @param fileName The file to check
     * @throws InvalidSettingsException If that fails.
     */
    private void checkFileAccess(final String fileName)
            throws InvalidSettingsException {
        if (fileName == null) {
            throw new InvalidSettingsException("No output file specified.");
        }
        File file = new File(fileName);
        if (file.isDirectory()) {
            throw new InvalidSettingsException("\"" + file.getAbsolutePath()
                    + "\" is a directory.");
        }
        if (file.exists()) {
            if (!file.canWrite()) {
                throw new InvalidSettingsException("Cannot write to file \""
                    + file.getAbsolutePath() + "\".");
            }
        }
        if (file.exists() && !m_overwriteOK.getBooleanValue()) {
            throw new InvalidSettingsException("File exists and can't be "
                    + "overwritten, check dialog settings");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) throws IOException, 
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) throws IOException, 
            CanceledExecutionException {
        // nothing to do here
    }
 
}
