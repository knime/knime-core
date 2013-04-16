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
 * History
 *   29.10.2010 (meinl): created
 */
package org.knime.base.node.image.imagecolwriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.image.ImageContent;
import org.knime.core.data.image.ImageValue;
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
 * This is the model for the Image writer node. It takes an image column from
 * the input table and writes each cell into a separate file in the output
 * directory.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Sebastian Peter, University of Konstanz
 */
public class ImageColumnWriterNodeModel extends NodeModel {
    private final SettingsModelString m_imageColumn = new SettingsModelString(
            "imageColumn", null);

    private final SettingsModelString m_directory = new SettingsModelString(
            "directory", null);

    private final SettingsModelBoolean m_overwrite = new SettingsModelBoolean(
            "overwrite", false);

    /**
     * Creates a new model with one input port and not output port.
     */
    public ImageColumnWriterNodeModel() {
        super(1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_directory.getStringValue() == null) {
            throw new InvalidSettingsException("No output directory selected");
        }

        File dir = new File(m_directory.getStringValue());
        if (!dir.exists()) {
            throw new InvalidSettingsException("Directory '"
                    + dir.getAbsolutePath() + "' does not exist");
        }
        if (!dir.isDirectory()) {
            throw new InvalidSettingsException("'" + dir.getAbsolutePath()
                    + "' is not a directory");
        }

        if (m_imageColumn.getStringValue() == null) {
            for (DataColumnSpec cs : inSpecs[0]) {
                if (cs.getType().isCompatible(ImageValue.class)) {
                    m_imageColumn.setStringValue(cs.getName());
                    break;
                }
            }
            if (m_imageColumn.getStringValue() == null) {
                throw new InvalidSettingsException(
                        "Input table does not contain an Image column");
            }
        }

        int colIndex =
                inSpecs[0].findColumnIndex(m_imageColumn.getStringValue());
        if (colIndex == -1) {
            throw new InvalidSettingsException("Image column '"
                    + m_imageColumn.getStringValue() + "' does not exist");
        }
        if (!inSpecs[0].getColumnSpec(colIndex).getType()
                .isCompatible(ImageValue.class)) {
            throw new InvalidSettingsException("Column '"
                    + m_imageColumn.getStringValue()
                    + "' does not contain images");
        }

        return new DataTableSpec[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        double max = inData[0].getRowCount();
        int count = 0;

        File dir = new File(m_directory.getStringValue());

        final int colIndex =
                inData[0].getDataTableSpec().findColumnIndex(
                        m_imageColumn.getStringValue());

        for (DataRow row : inData[0]) {
            exec.checkCanceled();
            ImageValue v = (ImageValue)row.getCell(colIndex);
            final String ext = v.getImageExtension();
            exec.setProgress(count++ / max, "Writing " + row.getKey() + "."
                    + ext);

            File imageFile = new File(dir, row.getKey() + "." + ext);
            if (!m_overwrite.getBooleanValue() && imageFile.exists()) {
                throw new IOException("File '" + imageFile.getAbsolutePath()
                        + "' already exists");
            }
            ImageContent content = v.getImageContent();
            OutputStream os = new FileOutputStream(imageFile);
            content.save(os);
            os.close();
        }

        return new BufferedDataTable[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_imageColumn.saveSettingsTo(settings);
        m_directory.saveSettingsTo(settings);
        m_overwrite.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_imageColumn.validateSettings(settings);
        m_directory.validateSettings(settings);
        m_overwrite.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_imageColumn.loadSettingsFrom(settings);
        m_directory.loadSettingsFrom(settings);
        m_overwrite.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }
}
