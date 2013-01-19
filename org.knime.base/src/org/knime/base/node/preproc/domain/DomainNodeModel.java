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
 * -------------------------------------------------------------------
 *
 * History
 *   Aug 12, 2006 (wiswedel): created
 */
package org.knime.base.node.preproc.domain;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.NominalValue;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class DomainNodeModel extends NodeModel {

    /** Config identifier for columns for which possible values
     * must be determined. */
    static final String CFG_POSSVAL_COLS = "possible_values_columns";

    /** Config identifier whether possible value domain should be retained
     * for non-selected columns (will otherwise be dropped). */
    static final String CFG_POSSVAL_RETAIN_UNSELECTED =
        "possible_values_unselected_retain";

    /** Config identifier for columns for which min and max values
     * must be determined. */
    static final String CFG_MIN_MAX_COLS = "min_max_columns";

    /** Config identifier whether min/max values should be retained
     * for non-selected columns (will otherwise be dropped). */
    static final String CFG_MIN_MAX_RETAIN_UNSELECTED =
        "min_max_unselected_retain";

    /** Config identifier for columns for which min and max values
     * must be determined. */
    static final String CFG_MAX_POSS_VALUES = "max_poss_values";

    private String[] m_possValCols;
    private boolean m_possValRetainUnselected = true; // added in 2.1.2
    private String[] m_minMaxCols;
    private boolean m_minMaxRetainUnselected = true;  // added in 2.1.2
    private int m_maxPossValues;

    /** Constructor, inits one input, one output. */
    public DomainNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final DataTableSpec oldSpec = inData[0].getDataTableSpec();
        final int colCount = oldSpec.getNumColumns();
        HashSet<String> possValColsHash =
            new HashSet<String>(Arrays.asList(m_possValCols));
        HashSet<String> minMaxColsHash =
            new HashSet<String>(Arrays.asList(m_minMaxCols));
        @SuppressWarnings("unchecked")
        LinkedHashSet<DataCell>[] possVals = new LinkedHashSet[colCount];
        DataCell[] mins = new DataCell[colCount];
        DataCell[] maxs = new DataCell[colCount];
        DataValueComparator[] comparators = new DataValueComparator[colCount];
        for (int i = 0; i < colCount; i++) {
            DataColumnSpec col = oldSpec.getColumnSpec(i);
            if (possValColsHash.contains(col.getName())) {
                possVals[i] = new LinkedHashSet<DataCell>();
            } else {
                possVals[i] = null;
            }
            if (minMaxColsHash.contains(col.getName())) {
                mins[i] = DataType.getMissingCell();
                maxs[i] = DataType.getMissingCell();
                comparators[i] = col.getType().getComparator();
            } else {
                mins[i] = null;
                maxs[i] = null;
                comparators[i] = null;
            }
        }

        int row = 0;
        final double rowCount = inData[0].getRowCount();
        for (RowIterator it = inData[0].iterator(); it.hasNext(); row++) {
            DataRow r = it.next();
            for (int i = 0; i < colCount; i++) {
                DataCell c = r.getCell(i);
                if (!c.isMissing() && possVals[i] != null) {
                    possVals[i].add(c);
                    if (m_maxPossValues >= 0
                            && possVals[i].size() > m_maxPossValues) {
                        possVals[i] = null;
                    }
                }
                if (!c.isMissing() && mins[i] != null) {
                    if (mins[i].isMissing()) {
                        mins[i] = c;
                        maxs[i] = c;
                        continue; // it was the first row with a valid value
                    }
                    if (comparators[i].compare(c, mins[i]) < 0) {
                        mins[i] = c;
                    }
                    if (comparators[i].compare(c, maxs[i]) > 0) {
                        maxs[i] = c;
                    }
                }
            }
            exec.checkCanceled();
            exec.setProgress(row / rowCount, "Processed row #"
                    + (row + 1) + " (\"" + r.getKey() + "\")");
        }
        DataColumnSpec[] colSpec = new DataColumnSpec[colCount];
        for (int i = 0; i < colSpec.length; i++) {
            DataColumnSpec original = oldSpec.getColumnSpec(i);
            String name = original.getName();
            DataColumnDomainCreator domainCreator =
                new DataColumnDomainCreator(original.getDomain());

            if (possValColsHash.contains(name)) {
                domainCreator.setValues(possVals[i]);
            } else if (m_possValRetainUnselected) {
                // use old one (already set in creator)
            } else {
                domainCreator.setValues(null);
            }

            if (minMaxColsHash.contains(name)) {
                DataCell min = !mins[i].isMissing() ? mins[i] : null;
                DataCell max = !maxs[i].isMissing() ? maxs[i] : null;
                domainCreator.setLowerBound(min);
                domainCreator.setUpperBound(max);
            } else if (m_minMaxRetainUnselected) {
                // use old one (already set in creator)
            } else {
                domainCreator.setLowerBound(null);
                domainCreator.setUpperBound(null);
            }

            DataColumnSpecCreator specCreator =
                new DataColumnSpecCreator(original);
            specCreator.setDomain(domainCreator.createDomain());
            colSpec[i] = specCreator.createSpec();
        }
        DataTableSpec newSpec = new DataTableSpec(oldSpec.getName(), colSpec);
        BufferedDataTable o = exec.createSpecReplacerTable(inData[0], newSpec);
        return new BufferedDataTable[]{o};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
    throws InvalidSettingsException {
        DataTableSpec oldSpec = inSpecs[0];
        if (m_minMaxCols == null) {
            setWarningMessage(
                    "No configuration available, using auto-configuration.");
            m_minMaxCols = getAllCols(BoundedValue.class, oldSpec);
            m_possValCols = getAllCols(NominalValue.class, oldSpec);
            m_maxPossValues = 60;
        }
        Set<String> possValColSet =
            new HashSet<String>(Arrays.asList(m_possValCols));
        Set<String> minMaxColSet =
            new HashSet<String>(Arrays.asList(m_minMaxCols));
        int colCount = oldSpec.getNumColumns();
        DataColumnSpec[] colSpec = new DataColumnSpec[colCount];
        for (int i = 0; i < colSpec.length; i++) {
            DataColumnSpec original = oldSpec.getColumnSpec(i);
            String name = original.getName();
            DataColumnSpecCreator specCreator =
                new DataColumnSpecCreator(original);
            DataColumnDomainCreator domainCreator =
                new DataColumnDomainCreator(original.getDomain());
            if (possValColSet.contains(name)) {
                // will be set to concrete values in execute
                domainCreator.setValues(null);
            } else if (m_possValRetainUnselected) {
                // use old one (already set in creator)
            } else {
                domainCreator.setValues(null);
            }

            if (minMaxColSet.contains(name)) {
                // will be set to concrete values in execute
                domainCreator.setLowerBound(null);
                domainCreator.setUpperBound(null);
            } else if (m_minMaxRetainUnselected) {
                // use old one (already set in creator)
            } else {
                domainCreator.setLowerBound(null);
                domainCreator.setUpperBound(null);
            }
            specCreator.setDomain(domainCreator.createDomain());
            colSpec[i] = specCreator.createSpec();
        }
        DataTableSpec newSpec = new DataTableSpec(oldSpec.getName(), colSpec);
        return new DataTableSpec[]{newSpec};
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
        if (m_possValCols != null) {
            settings.addStringArray(CFG_POSSVAL_COLS, m_possValCols);
            settings.addStringArray(CFG_MIN_MAX_COLS, m_minMaxCols);
            settings.addInt(CFG_MAX_POSS_VALUES, m_maxPossValues);
            settings.addBoolean(
                    CFG_POSSVAL_RETAIN_UNSELECTED, m_possValRetainUnselected);
            settings.addBoolean(
                    CFG_MIN_MAX_RETAIN_UNSELECTED, m_minMaxRetainUnselected);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getStringArray(CFG_POSSVAL_COLS);
        settings.getStringArray(CFG_MIN_MAX_COLS);
        settings.getInt(CFG_MAX_POSS_VALUES);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_possValCols = settings.getStringArray(CFG_POSSVAL_COLS);
        m_minMaxCols = settings.getStringArray(CFG_MIN_MAX_COLS);
        m_maxPossValues = settings.getInt(CFG_MAX_POSS_VALUES);
        // fields added in 2.1.2 (default is "false" to imitate old behavior
        // in nodes saved in 2.1.1)
        m_minMaxRetainUnselected =
            settings.getBoolean(CFG_MIN_MAX_RETAIN_UNSELECTED, false);
        m_possValRetainUnselected =
            settings.getBoolean(CFG_POSSVAL_RETAIN_UNSELECTED, false);
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

    /** Finds all columns in a spec whose type is compatible to cl.
     * @param cl The value to be compatible to.
     * @param spec The spec to query.
     * @return The identified columns.
     */
    static String[] getAllCols(
            final Class<? extends DataValue> cl, final DataTableSpec spec) {
        ArrayList<String> result = new ArrayList<String>();
        for (DataColumnSpec c : spec) {
            if (c.getType().isCompatible(cl)) {
                result.add(c.getName());
            }
        }
        return result.toArray(new String[result.size()]);
    }

}

