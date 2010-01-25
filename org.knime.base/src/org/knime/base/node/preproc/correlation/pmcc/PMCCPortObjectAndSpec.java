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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 * 
 * History
 *   18.02.2007 (wiswedel): created
 */
package org.knime.base.node.preproc.correlation.pmcc;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import javax.swing.JComponent;

import org.knime.base.util.HalfDoubleMatrix;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;

/**
 * PortObject and PortObjectSpec of the model that's passed between the 
 * correlation nodes.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class PMCCPortObjectAndSpec implements PortObject, PortObjectSpec {

    /** Serializer required by the {@link PortObject} interface.
     * @return Such a serializer.
     */
    public static PortObjectSerializer<PMCCPortObjectAndSpec> 
        getPortObjectSerializer() {
        return new PortObjectSerializer<PMCCPortObjectAndSpec>() {

            @Override
            public PMCCPortObjectAndSpec loadPortObject(
                    final PortObjectZipInputStream in, 
                    final PortObjectSpec spec,
                    final ExecutionMonitor exec) throws IOException,
                    CanceledExecutionException {
                return (PMCCPortObjectAndSpec)spec;
            }

            @Override
            public void savePortObject(final PMCCPortObjectAndSpec portObject,
                    final PortObjectZipOutputStream out, 
                    final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
            }
        };
    }
    
    /** Serializer required by the {@link PortObjectSpec} interface.
     * @return Such a serializer.
     */
    public static PortObjectSpecSerializer<PMCCPortObjectAndSpec> 
        getPortObjectSpecSerializer() {
        return new PortObjectSpecSerializer<PMCCPortObjectAndSpec>() {

            @Override
            public PMCCPortObjectAndSpec loadPortObjectSpec(
                    final PortObjectSpecZipInputStream in) throws IOException {
                ModelContentRO cont = ModelContent.loadFromXML(in);
                try {
                    return load(cont);
                } catch (InvalidSettingsException e) {
                    throw new IOException("Can't parse content", e);
                }
            }

            @Override
            public void savePortObjectSpec(
                    final PMCCPortObjectAndSpec portObjectSpec,
                    final PortObjectSpecZipOutputStream out) 
                throws IOException {
                ModelContent cont = new ModelContent("correlation");
                portObjectSpec.save(cont);
                cont.saveToXML(out);
            }
            
        };
    }
    
    /** Convenience access field for the port type. */
    static final PortType TYPE = new PortType(PMCCPortObjectAndSpec.class);
    
    private final String[] m_colNames;
    private final HalfDoubleMatrix m_correlations;
    
    /** Values smaller than this are considered to be 0, used to avoid
     * round-off errors. */
    static final double ROUND_ERROR_OK = 1e-8; 
    
    /** Creates new object, whereby no correlation values are available.
     * @param includes The columns being analyzed.
     */
    PMCCPortObjectAndSpec(final String[] includes) {
        if (includes == null || Arrays.asList(includes).contains(null)) {
            throw new NullPointerException("Arg must not be null or " 
                    + "contain null elements");
        }
        m_colNames = includes;
        m_correlations = null;
    }
    
    /** Creates new object with content. Used in the execute method.
     * @param includes The names of the columns.
     * @param cors The correlation values
     * @throws InvalidSettingsException If cor-values don't match the columns
     * or are out of range.
     */
    PMCCPortObjectAndSpec(final String[] includes, final HalfDoubleMatrix cors) 
        throws InvalidSettingsException {
        final int l = includes.length;
        if (cors.getRowCount() != l) {
            throw new InvalidSettingsException("Correlations array has wrong "
                    + "size, expected " + l + " got " + cors.getRowCount());
        }
        for (int i = 0; i < l; i++) {
            for (int j = i + 1; j < l; j++) {
                double d = cors.get(i, j);
                if (d < -1.0) {
                    if (d < -1.0 - ROUND_ERROR_OK) {
                        throw new InvalidSettingsException(
                                "Correlation measure is out of range: " + d);
                    } else {
                        cors.set(i, j, -1.0);
                    }
                } else if (d > 1.0) {
                    if (d > 1.0 + ROUND_ERROR_OK) {
                        throw new InvalidSettingsException(
                                "Correlation measure is out of range: " + d);
                    } else {
                        cors.set(i, j, 1.0);
                    }
                }
            }
        }
        m_colNames = includes;
        m_correlations = cors;
    }
    
    /** {@inheritDoc} */
    @Override
    public PortObjectSpec getSpec() {
        return this;
    }
    
    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        return "Correlation values on " + m_colNames.length + " columns";
    }
    
    /** @return If correlation values are available. */
    boolean hasData() {
        return m_correlations != null;
    }
    
    /**
     * Get set of column names that would be in the output table if a given
     * correlation threshold is applied. 
     * @param threshold The threshold, in [0, 1]
     * @return The set of string suggested as "survivors"
     */
    String[] getReducedSet(final double threshold) {
        if (!hasData()) {
            throw new IllegalStateException("No data available");
        }
        final int l = m_colNames.length;
        boolean[] hideFlags = new boolean[l];
        int[] countsAboveThreshold = new int[l];
        HashSet<String> excludes = new HashSet<String>();
        if (Thread.currentThread().isInterrupted()) {
            return new String[0];
        }
        int bestIndex; // index of next column to include
        do {
            Arrays.fill(countsAboveThreshold, 0);
            for (int i = 0; i < l; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    return new String[0];
                }
                if (hideFlags[i]) {
                    continue;
                }
                for (int j = i + 1; j < l; j++) {
                    if (hideFlags[j]) {
                        continue;
                    }
                    double d = m_correlations.get(i, j);
                    if (Math.abs(d) >= threshold) {
                        countsAboveThreshold[i] += 1;
                        countsAboveThreshold[j] += 1;
                    }
                }
            }
            int max = -1;
            bestIndex = -1;
            for (int i = 0; i < l; i++) {
                if (hideFlags[i] || countsAboveThreshold[i] == 0) {
                    continue;
                }
                if (countsAboveThreshold[i] > max) {
                    bestIndex = i;
                    max = countsAboveThreshold[i];
                }
            }
            if (bestIndex >= 0) {
                if (Thread.currentThread().isInterrupted()) {
                    return new String[0];
                }
                for (int i = 0; i < l; i++) {
                    if (hideFlags[i] || i == bestIndex) {
                        continue;
                    }
                    double d = m_correlations.get(bestIndex, i);
                    if (i == bestIndex || Math.abs(d) >= threshold) {
                        hideFlags[i] = true;
                        excludes.add(m_colNames[i]);
                    }
                }
                hideFlags[bestIndex] = true;
            }
        } while (bestIndex >= 0);
        String[] result = new String[l - excludes.size()];
        int last = 0;
        for (int i = 0; i < l; i++) {
            if (!excludes.contains(m_colNames[i])) {
                result[last++] = m_colNames[i];
            }
        }
        return result;
    }

    /**
     * Creates the correlation table, used in the view and as output table.
     * @param con For progress info/cancelation
     * @return The correlation table
     * @throws CanceledExecutionException If canceled.
     */
    BufferedDataTable createCorrelationMatrix(final ExecutionContext con) 
        throws CanceledExecutionException {
        BufferedDataContainer cont = 
            con.createDataContainer(createOutSpec(m_colNames));
        return (BufferedDataTable)createCorrelationMatrix(cont, con);
        
    }
    
    private DataTable createCorrelationMatrix(final DataContainer cont, 
            final ExecutionMonitor mon) 
        throws CanceledExecutionException {
        if (!hasData()) {
            throw new IllegalStateException("No data available");
        }
        final int l = m_colNames.length;
        for (int i = 0; i < l; i++) {
            RowKey key = new RowKey(m_colNames[i]);
            DataCell[] cells = new DataCell[l];
            for (int j = 0; j < l; j++) {
                if (i == j) {
                    cells[i] = MAX_VALUE_CELL;
                } else {
                    double corr = m_correlations.get(i, j);
                    if (Double.isNaN(corr)) {
                        cells[j] = DataType.getMissingCell();
                    } else {
                        cells[j] = new DoubleCell(corr);
                    }
                }
            }
            mon.checkCanceled();
            cont.addRowToTable(new DefaultRow(key, cells));
            mon.setProgress(i / (double)l, "Added row " + i);
        }
        cont.close();
        return cont.getTable();
    }
    
    private static final DataCell MIN_VALUE_CELL = new DoubleCell(-1.0);
    private static final DataCell MAX_VALUE_CELL = new DoubleCell(1.0);
    
    /** Creates output spec for correlation table.
     * @param names the column names being analyzed.
     * @return The new output spec.
     */
    static DataTableSpec createOutSpec(final String[] names) {
        DataColumnSpec[] colSpecs = new DataColumnSpec[names.length];
        for (int i = 0; i < colSpecs.length; i++) {
            DataColumnSpecCreator c = 
                new DataColumnSpecCreator(names[i], DoubleCell.TYPE);
            c.setDomain(new DataColumnDomainCreator(
                    MIN_VALUE_CELL, MAX_VALUE_CELL).createDomain());
            colSpecs[i] = c.createSpec();
        }
        return new DataTableSpec("Correlation values", colSpecs);
    }

    private static final String CFG_INTERNAL = "pmcc_model";
    private static final String CFG_NAMES = "names";
    private static final String CFG_VALUES = "correlation_values";
    private static final String CFG_CONTAINS_VALUES = "contains_values";

    /** Saves this object to a config.
     * @param m To save to.
     */
    public void save(final ConfigWO m) {
        ConfigWO sub = m.addConfig(CFG_INTERNAL);
        sub.addStringArray(CFG_NAMES, m_colNames);
        sub.addBoolean(CFG_CONTAINS_VALUES, m_correlations != null);
        if (m_correlations != null) {
            m_correlations.save(sub.addConfig(CFG_VALUES));
        }
    }
    
    /** Factory method to load from config. 
     * @param m to load from.
     * @return new object loaded from argument
     * @throws InvalidSettingsException If that fails.
     */
    public static PMCCPortObjectAndSpec load(final ConfigRO m) 
        throws InvalidSettingsException {
        ConfigRO sub = m.getConfig(CFG_INTERNAL);
        String[] names = sub.getStringArray(CFG_NAMES);
        if (names == null) {
            throw new InvalidSettingsException("Column names array is null.");
        }
        if (sub.getBoolean(CFG_CONTAINS_VALUES)) {
            HalfDoubleMatrix corrMatrix = 
                new HalfDoubleMatrix(sub.getConfig(CFG_VALUES));
            return new PMCCPortObjectAndSpec(names, corrMatrix);
        } else {
            return new PMCCPortObjectAndSpec(names);
        }
    }

    /**
     * @return the colNames
     */
    String[] getColNames() {
        return m_colNames;
    }
    
    /** {@inheritDoc} */
    @Override
    public JComponent[] getViews() {
        return null;
    }
    
    
}
