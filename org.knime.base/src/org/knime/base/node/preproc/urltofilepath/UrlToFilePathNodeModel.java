/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   16.05.2012 (kilian): created
 */
package org.knime.base.node.preproc.urltofilepath;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The node model of the url to file path node. Converting url strings into
 * file paths, consisting of four elements: the complete file path, the parent 
 * folder of the file, the file name, and the file extension. The node appends 
 * four columns one for each element. It can be specified if node fails or not, 
 * if an input url string is not valid or a file does not exist.
 * 
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 */
class UrlToFilePathNodeModel extends NodeModel {

    /**
     * Default selected column name.
     */
    static final String DEF_COLNAME = "URL";
    
    /**
     * Default fail on invalid syntax setting.
     */
    static final boolean DEF_FAIL_ON_INVALID_SYNTAX = false;
    
    /**
     * Default fail on invalid location setting.
     */
    static final boolean DEF_FAIL_ON_INVALID_LOCATION = false;    
    
    /**
     * Default name for column containing parent folders.
     */
    static final String DEF_COLNAME_PARENTFOLDER = "Parent folder";

    /**
     * Default name for column containing file names.
     */
    static final String DEF_COLNAME_FILENAME = "File name";

    /**
     * Default name for column containing file extension.
     */    
    static final String DEF_COLNAME_FILEEXTENSION = "File extension";

    /**
     * Default name for column containing the complete file path.
     */    
    static final String DEF_COLNAME_FILEPATH = "File path";
    
    private SettingsModelString m_stringColMode = 
        UrlToFilePathNodeDialog.getStringColModel();
    
    private SettingsModelBoolean m_failOnInvalidSyntaxModel = 
        UrlToFilePathNodeDialog.getFailOnInvalidSyntaxModel();

    private SettingsModelBoolean m_failOnInvalidLocationModel = 
        UrlToFilePathNodeDialog.getFailOnInvalidLocationModel();
    
    /**
     * Creates new instance of <code>UrlToFilePathNodeModel</code>.
     */
    UrlToFilePathNodeModel() {
        super(1, 1);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec inSpec = inSpecs[0];        
        return new DataTableSpec[] {createRearranger(inSpec).createSpec()};
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable data = inData[0];
        DataTableSpec dataSpec = data.getDataTableSpec();
        ColumnRearranger cR = createRearranger(dataSpec);
        return new BufferedDataTable[] {exec.createColumnRearrangeTable(data, 
                cR, exec)};
    }
    
    /**
     * Creates and returns the <code>ColumnRearranger</code> used to create the 
     * output spec as well as the output data table.
     * 
     * @param dataSpec The original input data table spec.
     * @return The column rearranger used to create the output spec
     * as well as the output data table.
     * @throws InvalidSettingsException If the input spec contains no columns
     * containing string cells, or if the selected column is not contained in 
     * the input spec.
     */
    private ColumnRearranger createRearranger(final DataTableSpec dataSpec) 
    throws InvalidSettingsException {
        //
        /// SPEC CHECKS
        //
        // check for at least one string column in input data table spec
        int noStringCols = 0;
        for (int i = 0; i < dataSpec.getNumColumns(); i++) {
            if (dataSpec.getColumnSpec(i).getType().isCompatible(
                    StringValue.class)) {
                noStringCols++;
            }
        }
        if (noStringCols <= 0) {
            throw new InvalidSettingsException(
                    "No columns containing string values in input table!");
        }
        // check if all included columns are available in the spec
        String selectedColName = m_stringColMode.getStringValue();
        if (!dataSpec.containsName(selectedColName)) {
                throw new InvalidSettingsException("Column \"" 
                        + selectedColName + "\" is not available!");
        }
        
        //
        /// CREATE COLUMN REARRANGER
        //
        // parameters
        final boolean failOnInvalidSyntax = 
            m_failOnInvalidSyntaxModel.getBooleanValue();
        final boolean failOnInvalidLocation = 
            m_failOnInvalidLocationModel.getBooleanValue();
        int selectedColIndex = getSelectedColIndex(dataSpec);
        
        ColumnRearranger cR = new ColumnRearranger(dataSpec);
        // create spec of new output columns
        DataColumnSpec[] newColsSpecs = getNewColSpecs(dataSpec);
        
        // Pass all necessary parameters to the cell factory, which converts
        // the values and creates new cells to replace or append.
        UrlToFilePathCellFactory cellFac = new UrlToFilePathCellFactory(
                selectedColIndex, failOnInvalidSyntax, failOnInvalidLocation, 
                newColsSpecs);
        
        cR.append(cellFac);
        
        return cR;
    }    

    /**
     * Creates and returns the index of the selected column in the given
     * table spec.
     * 
     * @param dataSpec The input data table spec.
     * @return The index of the selected column.
     */
    private int getSelectedColIndex(final DataTableSpec dataSpec) {
        String selectedColumn = m_stringColMode.getStringValue();

        for (int i = 0; i < dataSpec.getNumColumns(); i++) {
            String currColName = dataSpec.getColumnSpec(i).getName();
            if (currColName.equals(selectedColumn)) {
                return i;
            }
        }
        return -1;
    }    
    
    /**
     * Creates and returns new specs of the four columns to be appended, which
     * are the complete file path, parent folder, file name, and file 
     * extension.
     * 
     * @param origInSpec The original spec of the input data table.
     * @return The specs of the four columns to be appended.
     */
    private static final DataColumnSpec[] getNewColSpecs(
            final DataTableSpec origInSpec) {
        DataColumnSpec[] appColumnSpecs = new DataColumnSpec[4];
        
        // create string cell for parent folder
        String name = DataTableSpec.getUniqueColumnName(origInSpec, 
                DEF_COLNAME_PARENTFOLDER);
        appColumnSpecs[0] = new DataColumnSpecCreator(name, 
                StringCell.TYPE).createSpec();

        // create string cell for file name
        name = DataTableSpec.getUniqueColumnName(origInSpec, 
                DEF_COLNAME_FILENAME);
        appColumnSpecs[1] = new DataColumnSpecCreator(name, 
                StringCell.TYPE).createSpec();

        // create string cell for file extension
        name = DataTableSpec.getUniqueColumnName(origInSpec, 
                DEF_COLNAME_FILEEXTENSION);
        appColumnSpecs[2] = new DataColumnSpecCreator(name, 
                StringCell.TYPE).createSpec();        

        // create string cell for complete file path
        name = DataTableSpec.getUniqueColumnName(origInSpec, 
                DEF_COLNAME_FILEPATH);
        appColumnSpecs[3] = new DataColumnSpecCreator(name, 
                StringCell.TYPE).createSpec();      
        
        return appColumnSpecs;
    }    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_failOnInvalidLocationModel.saveSettingsTo(settings);
        m_failOnInvalidSyntaxModel.saveSettingsTo(settings);
        m_stringColMode.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_failOnInvalidLocationModel.validateSettings(settings);
        m_failOnInvalidSyntaxModel.validateSettings(settings);
        m_stringColMode.validateSettings(settings);        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_failOnInvalidLocationModel.loadSettingsFrom(settings);
        m_failOnInvalidSyntaxModel.loadSettingsFrom(settings);
        m_stringColMode.loadSettingsFrom(settings);
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
