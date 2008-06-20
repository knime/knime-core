/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   20.06.2008 (mb): created
 */
package org.knime.base.node.io.filereader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.workflow.LoopStartNode;
import org.knime.core.node.workflow.ScopeVariable;

/**
 * 
 * @author mb, University of Konstanz
 */
public class LoopingFileReaderNodeModel extends FileReaderNodeModel implements
        LoopStartNode {

    private int m_currentIteration = 0;

    private final SettingsModelString m_stackVarName =
            LoopingFileReaderNodeDialog.createVariableNameModel();

    private final SettingsModelString m_fileList =
            LoopingFileReaderNodeDialog.createVariableValuesModel();

    private String[] m_fileArray = null;

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // we need to put the counts on the stack for the loop's tail to see:
        assert m_currentIteration == 0;
        pushScopeVariable(new ScopeVariable("currentIteration",
                m_currentIteration));
        pushScopeVariable(new ScopeVariable("maxIterations",
                m_fileArray.length + 1));
        // we push the filename on the stack if a variable name is provided
        if (!m_stackVarName.getStringValue().isEmpty()) {
            if (m_currentIteration <= 1) {
                // first file read is the one from the dialog URL
                pushScopeVariable(new ScopeVariable(m_stackVarName
                        .getStringValue(), getFileReaderSettings()
                        .getDataFileLocation().getFile()));
            } else {
                // push the filename of the next file on the stack
                pushScopeVariable(new ScopeVariable(m_stackVarName
                       .getStringValue(), m_fileArray[m_currentIteration - 2]));
            }
        }
        return super.configure(inSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException,
            InvalidSettingsException {
        if (this.getLoopTailNode() == null) {
            // start of loop
            assert m_currentIteration == 0;
        } else {
            assert m_currentIteration > 0;
            try {
                // get file name from list
                URL nextFile =
                        getFileFromList(m_currentIteration - 1,
                                getFileReaderSettings().getDataFileLocation());
                // push new filename into FileReader
                getFileReaderSettings().setDataFileLocationAndUpdateTableName(
                        nextFile);
            } catch (MalformedURLException mfue) {
                throw new InvalidSettingsException("Couldn't create URL for "
                        + "specified file: "
                        + m_fileArray[m_currentIteration - 1]);
            } catch (URISyntaxException mfue) {
                throw new InvalidSettingsException("Couldn't create URL for "
                        + "specified file: "
                        + m_fileArray[m_currentIteration - 1]);
            }
        }
        // push current numbers on stack
        pushScopeVariable(new ScopeVariable("currentIteration",
                m_currentIteration));
        pushScopeVariable(new ScopeVariable("maxIterations",
                m_fileArray.length + 1));
        // next time we are in the next iteration (configure will return the
        // new index)
        m_currentIteration++;
        return super.execute(data, exec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_currentIteration = 0;
        super.reset();
    }
    /**
     * Returns the URL to the file in the list with the provided index. If the
     * path in the file list is relative (i.e. doesn't start with a slash,
     * backslash or colon) it will add the path of the previous file to it.
     * 
     * @param index the index of the file from the file list to return
     * @param lastURL path to last file read. For relative file paths
     * @return the URL to the file at the specified index.
     */
    private URL getFileFromList(final int index, final URL lastURL)
            throws MalformedURLException, URISyntaxException {

        String nextFile = m_fileArray[index];

        // relative path??
        if (nextFile.length() <= 1 || ( nextFile.charAt(0) != '\\'
                && nextFile.charAt(0) != '/' && nextFile.charAt(1) != ':')) {
            String dir = new File(lastURL.toURI()).getParent();
            nextFile = dir + "/" + nextFile;
        }

        return FileReaderNodeDialog.textToURL(nextFile);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_stackVarName.loadSettingsFrom(settings);
        m_fileList.loadSettingsFrom(settings);
        if (m_fileList.getStringValue() != null) {
            // from the \n separated list create the array
            m_fileArray = m_fileList.getStringValue().split("\n");
        } else {
            m_fileArray = new String[0];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);
        m_stackVarName.validateSettings(settings);
        m_fileList.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_stackVarName.saveSettingsTo(settings);
        m_fileList.saveSettingsTo(settings);
    }

}
