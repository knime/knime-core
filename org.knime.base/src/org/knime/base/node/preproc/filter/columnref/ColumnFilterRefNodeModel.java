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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 * History
 *   06.05.2008 (gabriel): created
 */
package org.knime.base.node.preproc.filter.columnref;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
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
 *
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColumnFilterRefNodeModel extends NodeModel {

    /** Settings model to included or exclude columns. */
    private final SettingsModelString m_inexcudeColumns =
        ColumnFilterRefNodeDialogPane.createInExcludeModel();

    /** Settings model to check column type compatibility. */
    private final SettingsModelBoolean m_typeComp =
        ColumnFilterRefNodeDialogPane.createTypeModel();

    /**
     * Creates a new node model of the Reference Column Filter node with two
     * inputs and one output.
     */
    public ColumnFilterRefNodeModel() {
        super(2, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{
                createRearranger(inSpecs[0], inSpecs[1]).createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        ColumnRearranger cr =
            createRearranger(inData[0].getSpec(), inData[1].getSpec());
        BufferedDataTable out =
            exec.createColumnRearrangeTable(inData[0], cr, exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * Creates a <code>ColumnRearranger</code> that is a filter on the input 
     * table spec.
     * @param oSpec original table spec to filter
     * @param filterSpec the reference table spec
     * @return a rearranger object that filters the original spec
     */
    private ColumnRearranger createRearranger(final DataTableSpec oSpec,
            final DataTableSpec filterSpec) {
        ColumnRearranger cr = new ColumnRearranger(oSpec);

        boolean exclude = m_inexcudeColumns.getStringValue().equals(
                ColumnFilterRefNodeDialogPane.EXCLUDE);

        for (DataColumnSpec cspec : oSpec) {
            String name = cspec.getName();
            if (exclude) {
                if (filterSpec.containsName(name)) {
                    DataType fType = filterSpec.getColumnSpec(name).getType();
                    if (!m_typeComp.getBooleanValue()
                            || cspec.getType().isASuperTypeOf(fType)) {
                        cr.remove(name);
                    }
                }
            } else {
                if (!filterSpec.containsName(name)) {
                    cr.remove(name);
                } else {
                    DataType fType = filterSpec.getColumnSpec(name).getType();
                    if (m_typeComp.getBooleanValue()
                            && !cspec.getType().isASuperTypeOf(fType)) {
                        cr.remove(name);
                    }
                }
            }
        }
        return cr;

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
        m_inexcudeColumns.loadSettingsFrom(settings);
        m_typeComp.loadSettingsFrom(settings);
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
        m_inexcudeColumns.saveSettingsTo(settings);
        m_typeComp.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_inexcudeColumns.validateSettings(settings);
        m_typeComp.validateSettings(settings);
    }

}
