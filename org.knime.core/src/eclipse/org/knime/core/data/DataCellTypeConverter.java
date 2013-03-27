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
 * Created on 01.11.2012 by meinl
 */
package org.knime.core.data;

import java.util.concurrent.atomic.AtomicInteger;

import org.knime.core.node.NodeLogger;

/**
 * Interface for converters from one cell type to another.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.7
 */
public abstract class DataCellTypeConverter {

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());
    private AtomicInteger m_nrErrors = new AtomicInteger(0);
    private final boolean m_processConcurrently;

    /**
     *
     */
    public DataCellTypeConverter() {
        this(false);
    }

    public DataCellTypeConverter(final boolean processConcurrently) {
        m_processConcurrently = processConcurrently;
    }

    /**
     * Converts one data cell into another data cell. The type of the returned cell must be the same as the one returned
     * by {@link #getOutputType()}.
     *
     * @param source the cell that should be converted
     * @return the converted cell
     * @throws Exception if an error during the conversion occurs
     */
    public abstract DataCell convert(DataCell source) throws Exception;

    /**
     * Returns the type of the cells created by {@link #convert(DataCell)}.
     *
     * @return a data type
     */
    public abstract DataType getOutputType();

    /** Called when {@link #convert(DataCell)} throws an exception. The default implementation logs the error to
     * the system logger.
     * @param source The cell that caused the problem.
     * @param e The exception as thrown by {@link #convert(DataCell)}.
     */
    public void onConvertException(final DataCell source, final Exception e) {
        int max = 10;
        int errorCount = m_nrErrors.incrementAndGet();
        if (errorCount <= max) {
            m_logger.error("Failed to convert cell " + e.getMessage(), e);
            if (errorCount == max) {
                m_logger.debug("Suppressing further warnings");
            }
        }
    }

    /** Called by framework to invoke {@link #convert(DataCell)} (and {@link #onConvertException(DataCell, Exception)}
     * if needed). Not to be used by clients.
     * @param cell The argument cell.
     * @return The converted cell or missing.
     */
    public final DataCell callConvert(final DataCell cell) {
      try {
          return convert(cell);
      } catch (Exception e) {
          onConvertException(cell, e);
          return new MissingCell(e.getMessage());
      }
    }

    /** Get the number of error that occurred (how often did {@link #convert(DataCell)} throw an exception).
     * @return the nrErrorss
     */
    public final AtomicInteger getNrErrors() {
        return m_nrErrors;
    }

    /** @return the processConcurrently as passed in constructor (default is false). */
    public final boolean isProcessConcurrently() {
        return m_processConcurrently;
    }
}
