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
 *   03.06.2012 (kilian): created
 */
package org.knime.base.node.util.extracttablespec;

import java.io.File;
import java.io.IOException;

import org.knime.base.data.util.DataTableSpecExtractor;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;

/**
 * The node model of the extract table spec node. The node extracts information
 * from the spec of the input table, such as column names, types, lower and
 * upper bounds etc.
 * 
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 */
class ExtractTableSpecNodeModel extends NodeModel {

    /**
     * Default value of the extract property handler settings.
     */
    public static final boolean DEF_EXTRACT_PROPERTY_HANDLERS = false;
    
    /**
     * Default value of the possible value as collection value.
     */
    public static final boolean DEF_POSSIBLE_VALUES_AS_COLLECTION = false;
    
    private final SettingsModelBoolean m_extractPropertyHandlersModel =
        ExtractTableSpecNodeDialog.getExtractPropertyHandlersModel();
    
    private final SettingsModelBoolean m_possibleValuesAsCollection = 
        ExtractTableSpecNodeDialog.getPossibleValuesAsCollectionModel();
    
    /**
     * Constructor of <code>ExtractTableSpecNodeModel</code>.
     */
    public ExtractTableSpecNodeModel() {
        super(1, 1);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[] {
                getExtractedDataTable(inSpecs[0]).getDataTableSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        return new BufferedDataTable[] {exec.createBufferedDataTable(
                getExtractedDataTable(inData[0].getDataTableSpec()), exec)};
    }
    
    /**
     * Creates a data table with the meta information extracted from the given
     * spec as data rows/cols.
     * 
     * @param spec The table spec to extract the meta information from.
     * @return The data table containing the meta information from the given 
     * spec.
     */
    private DataTable getExtractedDataTable(final DataTableSpec spec) {
        DataTableSpecExtractor extractor = new DataTableSpecExtractor();
        extractor.setExtractPossibleValuesAsCollection(
                m_possibleValuesAsCollection.getBooleanValue());
        extractor.setExtractPropertyHandlers(
                m_extractPropertyHandlersModel.getBooleanValue());
        return extractor.extract(spec);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_extractPropertyHandlersModel.saveSettingsTo(settings);
        m_possibleValuesAsCollection.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_extractPropertyHandlersModel.validateSettings(settings);
        m_possibleValuesAsCollection.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_extractPropertyHandlersModel.loadSettingsFrom(settings);
        m_possibleValuesAsCollection.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Nothing to do ...
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
