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
 * ---------------------------------------------------------------------
 *
 * Created on 22.08.2012 by kilian
 */
package org.knime.base.node.image.tablerowtoimage;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.image.ImageContent;
import org.knime.core.data.image.ImageValue;
import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.image.ImagePortObject;
import org.knime.core.node.port.image.ImagePortObjectSpec;

/**
 *
 * @author Kilian Thiel, KNIME.com AG, Zurich
 * @since 2.7
 */
class TableRowToImageNodeModel extends NodeModel {

    private SettingsModelString m_imageColSettingsModel =
            TableRowToImageNodeDialog.getImageColumnSettingsModel();

    private static final ImagePortObjectSpec OUTSPEC = new ImagePortObjectSpec(
            PNGImageContent.TYPE);

    /**
     * New node model with on image port input and a data table output.
     */
    public TableRowToImageNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
                new PortType[]{ImagePortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec inSpec = (DataTableSpec)inSpecs[0];

        String column = m_imageColSettingsModel.getStringValue();
        int columnIndex = inSpec.findColumnIndex(column);
        if (columnIndex < 0) {
            columnIndex = findImageColumnIndex(inSpec);
            if (columnIndex >= 0) {
                setWarningMessage("Found image column '"
                        + inSpec.getColumnSpec(columnIndex).getName() + "'.");
            }
        }

        if (columnIndex < 0) {
            throw new InvalidSettingsException(
                    "No image column in input table.");
        }

        return new PortObjectSpec[]{OUTSPEC};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable inTable = (BufferedDataTable)inObjects[0];
        // check for empty table
        if (inTable.getRowCount() == 0) {
            throw new IllegalArgumentException("Input table is empty.");
        }
        // warn if more than one row
        if (inTable.getRowCount() > 1) {
            setWarningMessage("Input data table has more than one rows! "
                    + "Using first row only.");
        }

        String column = m_imageColSettingsModel.getStringValue();
        DataTableSpec inSpec = inTable.getDataTableSpec();
        int columnIndex = inSpec.findColumnIndex(column);
        if (columnIndex < 0) {
            columnIndex = findImageColumnIndex(inSpec);
        }

        final RowIterator it = inTable.iterator();
        while (it.hasNext()) {
            DataRow row = it.next();
            DataCell cell = row.getCell(columnIndex);
            if (!cell.isMissing()) {
                ImageContent ic = ((ImageValue)cell).getImageContent();
                return new PortObject[]{new ImagePortObject(ic, OUTSPEC)};
            } else {
                setWarningMessage("Found missing image cell, skipping it...");
            }
        }
        throw new IllegalArgumentException(
                "Input table contains only missing cells.");
    }

    private static int findImageColumnIndex(final DataTableSpec spec) {
        // find first image column
        for (int i = 0; i < spec.getNumColumns(); i++) {
            if (spec.getColumnSpec(i).getType().isCompatible(ImageValue.class))
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_imageColSettingsModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_imageColSettingsModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_imageColSettingsModel.loadSettingsFrom(settings);
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
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Nothing to do ...
    }
}
