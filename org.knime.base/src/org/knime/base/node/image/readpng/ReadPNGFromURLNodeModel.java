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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.image.readpng;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/** Model for Read PNG node.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class ReadPNGFromURLNodeModel extends NodeModel {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(ReadPNGFromURLNodeModel.class);
    private ReadPNGFromURLConfig m_config = new ReadPNGFromURLConfig();

    /** One in, one out. */
    public ReadPNGFromURLNodeModel() {
        super(1, 1);
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        ColumnRearranger rearranger = createColumnRearranger(
                inSpecs[0], new AtomicInteger());
        DataTableSpec out = rearranger.createSpec();
        return new DataTableSpec[] {out};
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec spec = inData[0].getDataTableSpec();
        AtomicInteger failCount = new AtomicInteger();
        ColumnRearranger rearranger = createColumnRearranger(spec, failCount);
        BufferedDataTable out = exec.createColumnRearrangeTable(
                inData[0], rearranger, exec);
        int rowCount = out.getRowCount();
        int fail = failCount.get();
        if (rowCount > 0 && rowCount == fail) {
            throw new Exception("None of the URLs could be read "
                    + "as PNG (see log for details)");
        } else if (fail > 0) {
            setWarningMessage("Failed to read " + fail + "/"
                    + rowCount + " files");
        }
        return new BufferedDataTable[] {out};
    }

    private ColumnRearranger createColumnRearranger(
            final DataTableSpec in, final AtomicInteger failCounter)
        throws InvalidSettingsException {
        String colName = m_config.getUrlColName();
        if (colName == null) {
            m_config.guessDefaults(in); // throws ISE
            colName = m_config.getUrlColName();
            setWarningMessage("Auto-configuration: Guessing column \""
                    + colName + "\" to contain locations");
        }
        final int colIndex = in.findColumnIndex(colName);
        if (colIndex < 0) {
            throw new InvalidSettingsException(
                    "No such column in input: " + colName);
        }
        DataColumnSpec colSpec = in.getColumnSpec(colIndex);
        if (!colSpec.getType().isCompatible(StringValue.class)) {
            throw new InvalidSettingsException("Selected column \""
                    + colName + "\" is not string-compatible");
        }
        final String newColName = m_config.getNewColumnName();
        DataColumnSpecCreator colSpecCreator;
        if (newColName != null) {
            String newName =
                DataTableSpec.getUniqueColumnName(in, newColName);
            colSpecCreator = new DataColumnSpecCreator(
                    newName, PNGImageContent.TYPE);
        } else {
            colSpecCreator = new DataColumnSpecCreator(colSpec);
            colSpecCreator.setType(PNGImageContent.TYPE);
            colSpecCreator.removeAllHandlers();
            colSpecCreator.setDomain(null);
        }
        DataColumnSpec outColumnSpec = colSpecCreator.createSpec();
        ColumnRearranger rearranger = new ColumnRearranger(in);
        CellFactory fac = new SingleCellFactory(outColumnSpec) {
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(colIndex);
                if (cell.isMissing()) {
                    return DataType.getMissingCell();
                } else {
                    String url = ((StringValue)cell).getStringValue();
                    try {
                        return toPNGCell(url);
                    } catch (Exception e) {
                        if (m_config.isFailOnInvalid()) {
                            if (e instanceof RuntimeException) {
                                throw (RuntimeException)e;
                            } else {
                                throw new RuntimeException(e.getMessage(), e);
                            }
                        } else {
                            String message = "Failed to read png content from "
                                + "\"" + url + "\": " + e.getMessage();
                            LOGGER.warn(message, e);
                            failCounter.incrementAndGet();
                            return DataType.getMissingCell();
                        }
                    }
                }
            }
        };
        if (newColName == null) {
            rearranger.replace(fac, colIndex);
        } else {
            rearranger.append(fac);
        }
        return rearranger;
    }

    /** Read image from URL.
     * @param urlValue The URL
     * @return A new image cell
     * @throws IOException
     * @throws IllegalArgumentException If the image is invalid
     * @see PNGImageContent#PNGImageContent(InputStream)
     */
    private DataCell toPNGCell(final String urlValue) throws IOException {
        URL url = new URL(urlValue);
        InputStream in = url.openStream();
        try {
            PNGImageContent pngImageContent = new PNGImageContent(in);
            return pngImageContent.toImageCell();
        } finally {
            try {
                in.close();
            } catch (IOException ioe) {
                // ignore
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new ReadPNGFromURLConfig().loadInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        ReadPNGFromURLConfig cfg = new ReadPNGFromURLConfig();
        cfg.loadInModel(settings);
        m_config = cfg;
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_config.getUrlColName() != null) {
            m_config.save(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals
    }

}
