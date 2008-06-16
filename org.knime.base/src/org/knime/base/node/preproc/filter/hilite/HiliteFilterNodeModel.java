/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   20.11.2006 (thiel): created
 */
package org.knime.base.node.preproc.filter.hilite;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.preproc.filter.row.RowFilterTable;
import org.knime.base.node.preproc.filter.row.rowfilter.EndOfTableException;
import org.knime.base.node.preproc.filter.row.rowfilter.IncludeFromNowOn;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.core.data.DataRow;
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
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * 
 * @author thiel, University of Konstanz
 */
public class HiliteFilterNodeModel extends NodeModel {

    /**
     * Creates an instance of HiliteFilterNodeModel.
     */
    public HiliteFilterNodeModel() {
        super(1, 2);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        assert inSpecs.length == 1;
        return new DataTableSpec[]{inSpecs[0], inSpecs[0]};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTable[] filteredTables = new DataTable[2];
        
        HiLiteHandler hdl = this.getInHiLiteHandler(0);
               
        synchronized (hdl) {
            filteredTables[0] = new RowFilterTable(inData[0], 
                    new HilightOnlyRowFilter(hdl));
            filteredTables[1] = new RowFilterTable(inData[0], 
                    new NotHilightedRowFilter(hdl));
        }
        return exec.createBufferedDataTables(filteredTables, exec);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
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
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
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
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }
    
    
    /**
     * Row filter that filters non-hilited rows - it's the most convenient way
     * to write only the hilited rows.
     * 
     * @author Bernd Wiswedel, University of Konstanz
     */
    private static final class HilightOnlyRowFilter extends RowFilter {

        private final HiLiteHandler m_handler;

        /**
         * Creates new instance given a hilight handler.
         * 
         * @param handler the handler to get the hilite info from
         */
        public HilightOnlyRowFilter(final HiLiteHandler handler) {
            m_handler = handler;
        }

        @Override
        public DataTableSpec configure(final DataTableSpec inSpec)
                throws InvalidSettingsException {
            throw new IllegalStateException("Not intended for permanent usage");
        }

        @Override
        public void loadSettingsFrom(final NodeSettingsRO cfg)
                throws InvalidSettingsException {
            throw new IllegalStateException("Not intended for permanent usage");
        }

        @Override
        protected void saveSettings(final NodeSettingsWO cfg) {
            throw new IllegalStateException("Not intended for permanent usage");
        }

        @Override
        public boolean matches(final DataRow row, final int rowIndex)
                throws EndOfTableException, IncludeFromNowOn {
            return m_handler.isHiLit(row.getKey());
        }
    }
    
    /**
     * Row filter that filters hilited rows - it's the most convenient way
     * to write only the not hilited rows.
     * 
     * @author Kilian Thiel, University of Konstanz
     */
    private static final class NotHilightedRowFilter extends RowFilter {

        private final HiLiteHandler m_handler;

        /**
         * Creates new instance given a hilight handler.
         * 
         * @param handler the handler to get the hilite info from
         */
        public NotHilightedRowFilter(final HiLiteHandler handler) {
            m_handler = handler;
        }

        @Override
        public DataTableSpec configure(final DataTableSpec inSpec)
                throws InvalidSettingsException {
            throw new IllegalStateException("Not intended for permanent usage");
        }

        @Override
        public void loadSettingsFrom(final NodeSettingsRO cfg)
                throws InvalidSettingsException {
            throw new IllegalStateException("Not intended for permanent usage");
        }

        @Override
        protected void saveSettings(final NodeSettingsWO cfg) {
            throw new IllegalStateException("Not intended for permanent usage");
        }

        @Override
        public boolean matches(final DataRow row, final int rowIndex)
                throws EndOfTableException, IncludeFromNowOn {
            return !m_handler.isHiLit(row.getKey());
        }
    }
}
