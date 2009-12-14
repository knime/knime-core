/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   23.05.2006 (gabriel): created
 */
package org.knime.base.node.viz.property.color;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.viewproperty.ColorHandlerPortObject;
import org.knime.core.node.port.viewproperty.ViewPropertyPortObject;

/**
 * Node model to append color settings to a column selected in the dialog.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColorAppender2NodeModel extends NodeModel {
    
    private final SettingsModelString m_column = 
        ColorAppender2NodeDialogPane.createColumnModel();
    
    /**
     * Create a new color appender model.
     */
    public ColorAppender2NodeModel() {
        super(new PortType[]{
                ColorHandlerPortObject.TYPE, BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec modelSpec = (DataTableSpec)inSpecs[0];
        DataTableSpec dataSpec = (DataTableSpec)inSpecs[1];
        DataTableSpec out = createOutputSpec(modelSpec, dataSpec);
        return new DataTableSpec[]{out};
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec modelSpec = ((ViewPropertyPortObject)inData[0]).getSpec();
        DataTableSpec dataSpec = ((BufferedDataTable)inData[1]).getSpec();
        DataTableSpec outSpec = createOutputSpec(modelSpec, dataSpec);
        BufferedDataTable table = exec.createSpecReplacerTable(
                (BufferedDataTable)inData[1], outSpec);
        return new BufferedDataTable[]{table};
    }

    private DataTableSpec createOutputSpec(final DataTableSpec modelSpec, 
            final DataTableSpec dataSpec) throws InvalidSettingsException {
        if (modelSpec == null || dataSpec == null) {
            throw new InvalidSettingsException("Invalid input.");
        }
        if (modelSpec.getNumColumns() < 1) {
            throw new InvalidSettingsException("No color information in input");
        }
        DataColumnSpec col = modelSpec.getColumnSpec(0);
        ColorHandler colorHandler = col.getColorHandler();
        if (col.getColorHandler() == null) {
            throw new InvalidSettingsException("No color information in input");
        }
        String column = m_column.getStringValue();
        if (column == null) { // auto-configuration/guessing
            // TODO check for nominal/range coloring and guess last
            // suitable column (while setting a warning message)
            if (dataSpec.containsName(col.getName())) {
                column = col.getName();
            }
        }
        if (column == null) {
            throw new InvalidSettingsException("Not configured.");
        }
        if (!dataSpec.containsName(column)) {
            throw new InvalidSettingsException("Column \"" + column 
                    + "\" not available.");
        }
        DataTableSpec spec = ColorManager2NodeModel.getOutSpec(
                dataSpec, column, colorHandler);
        return spec;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_column.loadSettingsFrom(settings);
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
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_column.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_column.validateSettings(settings);
    }
   
}
