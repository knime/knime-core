/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 * 
 * History
 *   17.05.2012 (kilian): created
 */
package org.knime.base.node.preproc.urltofilepathvariable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.knime.base.node.preproc.urltofilepath.UrlToFileUtil;
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
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.util.FileUtil;

/**
 * The node model of the url to file path variable node. Converting url strings 
 * into file paths, consisting of four elements: the complete file path, the 
 * parent folder of the file, the file name, and the file extension. The node 
 * expects flow variables as input, with one string flow variable containing 
 * the url string. The file path, parent folder, file name, and file extension 
 * are pushed as flow variables as well. It can be specified if the node fails 
 * or not, if an input url string is not valid or a file does not exist.
 * 
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 */
class UrlToFilePathVariableNodeModel extends NodeModel {

    /**
     * Default selected column name.
     */
    static final String DEF_VARNAME = "URL";
    
    /**
     * Default fail on invalid syntax setting.
     */
    static final boolean DEF_FAIL_ON_INVALID_SYNTAX = false;
    
    /**
     * Default fail on invalid location setting.
     */
    static final boolean DEF_FAIL_ON_INVALID_LOCATION = false;

    /**
     * Default add prefix to variable value (<code>false</code>).
     */
    static final boolean DEF_ADD_PREFIX_TO_VARIABLE = false;    
    
    /**
     * Default name for column containing parent folders.
     */
    static final String DEF_VARNAME_PARENTFOLDER = "parent_folder";

    /**
     * Default name for column containing file names.
     */
    static final String DEF_VARNAME_FILENAME = "file_name";

    /**
     * Default name for column containing file extension.
     */    
    static final String DEF_VARNAME_FILEEXTENSION = "file_extension";

    /**
     * Default name for column containing file extension.
     */    
    static final String DEF_VARNAME_FILEPATH = "file_path";
    
    /**
     * Default "missing value" for flow variable parent folder.
     */
    static final String DEF_FLOWVAR_PARENTFOLDER_MISSING = "";

    /**
     * Default "missing value" for flow variable file name.
     */
    static final String DEF_FLOWVAR_FILENAME_MISSING = "";
    
    /**
     * Default "missing value" for flow variable file extension.
     */
    static final String DEF_FLOWVAR_FILEEXTENSION_MISSING = "";
    
    /**
     * Default "missing value" for flow variable file path.
     */
    static final String DEF_FLOWVAR_FILEPATH_MISSING = "";
    
    
    private SettingsModelString m_flowVarNameModel = 
        UrlToFilePathVaribaleNodeDialog.getFlowVariableModel();
    
    private SettingsModelBoolean m_failOnInvalidUrlModel =
        UrlToFilePathVaribaleNodeDialog.getFailOnInvalidSyntaxModel();
    
    private SettingsModelBoolean m_failOnInvalidLocationModel =
        UrlToFilePathVaribaleNodeDialog.getFailOnInvalidLocationModel();
    
    private SettingsModelBoolean m_addPrefixToVarModel =
        UrlToFilePathVaribaleNodeDialog.getAddPrefixToVariableModel();
    
    /**
     * Creates new instance of <code>UrlToFilePathVariableNodeModel</code>.
     */
    UrlToFilePathVariableNodeModel() {
        super(new PortType[]{FlowVariablePortObject.TYPE}, 
              new PortType[]{FlowVariablePortObject.TYPE});
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        convert();
        return new PortObjectSpec[] {FlowVariablePortObjectSpec.INSTANCE};
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, 
            final ExecutionContext exec) throws Exception {
        // convert is not called since it is called already on configure
        return new PortObject[] {FlowVariablePortObject.INSTANCE};
    }
    
    /**
     * Converts url string from specified flow variable into file path, 
     * consisting of four parts: file path, parent folder, file name, and file 
     * extension. Each part is pushed as a string flow variable if conversion is
     * successful. If url is not valid or file does not exist an exception
     * will be throw if specified, otherwise empty string will be set for each
     * output variable.
     * 
     * @throws InvalidSettingsException If url is invalid or file location does 
     * not exist and specified in the settings.
     */
    private void convert() throws InvalidSettingsException {
        String inputVarname = m_flowVarNameModel.getStringValue();
        // only if specified variable is available
        if (getAvailableFlowVariables().keySet().contains(inputVarname)) {
            
            // get variable
            String urlStr =
                    peekFlowVariableString(inputVarname);
            // default values (fall back)
            String parentFolder = DEF_FLOWVAR_PARENTFOLDER_MISSING;
            String fileName = DEF_FLOWVAR_FILENAME_MISSING;
            String fileExtension = DEF_FLOWVAR_FILEEXTENSION_MISSING;
            String filePath = DEF_FLOWVAR_FILEPATH_MISSING;

            URL url = null;
            boolean error = false;
            try {
                url = new URL(urlStr);
            } catch (MalformedURLException e) {
                // if url string is not valid fail if specified
                if (m_failOnInvalidUrlModel.getBooleanValue()) {
                    throw new InvalidSettingsException("URL " + urlStr
                            + " is not valid!");
                }
                // file can not exist since url is invalid 
                // => throw exception if fail is specified
                if (m_failOnInvalidLocationModel.getBooleanValue()) {
                    throw new InvalidSettingsException("File at " + urlStr
                            + " does not exist!");
                }                
                error = true;
            }
            if (!error) {
                File file = FileUtil.getFileFromURL(url);
                if (!file.exists()) {
                    // if file does not exists fail if specified
                    if (m_failOnInvalidLocationModel.getBooleanValue()) {
                        throw new InvalidSettingsException("File "
                                + file.getAbsolutePath() + " does not exist!");
                    }
                    error = true;
                }
                
                // if url is valid and file exists, get corresponding values
                if (!error) {
                    parentFolder = UrlToFileUtil.getParentFolder(file);
                    fileName = UrlToFileUtil.getFileName(file);
                    fileExtension = UrlToFileUtil.getFileExtension(file);
                    filePath = file.getAbsolutePath();
                }
            }
            
            // push all file path values as variables
            String varnameParentfolder = DEF_VARNAME_PARENTFOLDER;
            String varnameFilename = DEF_VARNAME_FILENAME;
            String varnameFileextension = DEF_VARNAME_FILEEXTENSION;
            String varnameFilepath = DEF_VARNAME_FILEPATH;
            if (m_addPrefixToVarModel.getBooleanValue()) {
                varnameParentfolder = inputVarname + "_" 
                                        + varnameParentfolder;
                varnameFilename = inputVarname + "_" + varnameFilename;
                varnameFileextension = inputVarname + "_" 
                                        + varnameFileextension;
                varnameFilepath = inputVarname + "_" + varnameFilepath;
            }
            
            pushFlowVariableString(varnameParentfolder, parentFolder);
            pushFlowVariableString(varnameFilename, fileName);
            pushFlowVariableString(varnameFileextension, fileExtension);
            pushFlowVariableString(varnameFilepath, filePath);
        } else {
            throw new InvalidSettingsException("Specified flow variable " 
                    + m_flowVarNameModel.getStringValue() + " does not exist!");
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_flowVarNameModel.saveSettingsTo(settings);
        m_failOnInvalidLocationModel.saveSettingsTo(settings);
        m_failOnInvalidUrlModel.saveSettingsTo(settings);
        m_addPrefixToVarModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_flowVarNameModel.validateSettings(settings);
        m_failOnInvalidLocationModel.validateSettings(settings);
        m_failOnInvalidUrlModel.validateSettings(settings);
        m_addPrefixToVarModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_flowVarNameModel.loadSettingsFrom(settings);
        m_failOnInvalidLocationModel.loadSettingsFrom(settings);
        m_failOnInvalidUrlModel.loadSettingsFrom(settings);
        m_addPrefixToVarModel.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Nothing to reset ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
    throws IOException, CanceledExecutionException {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
    throws IOException, CanceledExecutionException {
        // Nothing to do ...
    }
}
