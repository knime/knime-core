/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   Aug 7, 2010 (wiswedel): created
 */
package org.knime.base.node.preproc.columnheaderextract;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
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
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.DataValueColumnFilter;

/**
 * This is the model implementation of ColumnHeaderExtractor.
 *
 *
 * @author Bernd Wiswedel
 */
@SuppressWarnings("unchecked")
public class ColumnHeaderExtractorNodeModel extends NodeModel {

    /** Selected column type. */
    static enum ColType {
        /** All columns. */
        All(DataValue.class),
        /** String columns. */
        String(StringValue.class),
        /** Integer columns. */
        Integer(IntValue.class),
        /** Double columns. */
        Double(DoubleValue.class);

        private final ColumnFilter m_filter;

        private ColType(final Class<? extends DataValue>... cl) {
            m_filter = new DataValueColumnFilter(cl);
        }

        /** @return associated filter. */
        public ColumnFilter getFilter() {
            return m_filter;
        }
    }

    private final HiLiteHandler m_hiliteHandler = new HiLiteHandler();

    private final SettingsModelBoolean m_replaceColHeader;

    private final SettingsModelString m_unifyHeaderPrefix;

    private final SettingsModelString m_colTypeFilter;

    /**
     * Constructor for the node model.
     */
    protected ColumnHeaderExtractorNodeModel() {
        super(1, 2);
        m_replaceColHeader = createReplaceColHeader();
        m_unifyHeaderPrefix = createUnifyHeaderPrefix(m_replaceColHeader);
        m_colTypeFilter = createColTypeFilter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec inSpec = inData[0].getDataTableSpec();
        DataTableSpec spec0 = createOutSpecPort0(inSpec);
        DataTableSpec spec1 = createOutSpecPort1(inSpec);
        BufferedDataContainer cont = exec.createDataContainer(spec0);
        List<String> origColNames = new ArrayList<String>();
        ColumnFilter filter = getColType().getFilter();
        for (DataColumnSpec c : inSpec) {
            if (filter.includeColumn(c)) {
                origColNames.add(c.getName());
            }
        }
        DefaultRow row =
                new DefaultRow("Column Header", origColNames
                        .toArray(new String[origColNames.size()]));
        cont.addRowToTable(row);
        cont.close();
        BufferedDataTable table0 = cont.getTable();
        BufferedDataTable table1 =
                exec.createSpecReplacerTable(inData[0], spec1);

        return new BufferedDataTable[]{table0, table1};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec inSpec = inSpecs[0];
        DataTableSpec spec0 = createOutSpecPort0(inSpec);
        DataTableSpec spec1 = createOutSpecPort1(inSpec);
        return new DataTableSpec[]{spec0, spec1};
    }

    private DataTableSpec createOutputSpecInternal(final DataTableSpec in,
            final boolean includeIgnoredCols) throws InvalidSettingsException {
        ColType colType = getColType();
        ColumnFilter filter = colType.getFilter();
        String namePrefix = m_unifyHeaderPrefix.getStringValue();

        HashSet<String> usedNames = new HashSet<String>();
        for (DataColumnSpec c : in) {
            if (!filter.includeColumn(c)) {
                // only add remaining columns
                usedNames.add(c.getName());
            }
        }

        List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>();
        for (DataColumnSpec c : in) {
            if (filter.includeColumn(c)) {
                int index = 0;
                String newName;
                do {
                    newName = namePrefix + (index++);
                } while (!usedNames.add(newName));
                DataColumnSpecCreator newSpecCreator =
                        new DataColumnSpecCreator(c);
                newSpecCreator.setName(newName);
                colSpecs.add(newSpecCreator.createSpec());
            } else if (includeIgnoredCols) {
                colSpecs.add(c);
            }
        }
        return new DataTableSpec(in.getName(), colSpecs
                .toArray(new DataColumnSpec[colSpecs.size()]));
    }

    private DataTableSpec createOutSpecPort0(final DataTableSpec spec)
            throws InvalidSettingsException {
        DataTableSpec temp;
        if (m_replaceColHeader.getBooleanValue()) {
            temp = createOutputSpecInternal(spec, false);
        } else {
            ColumnFilter filter = getColType().getFilter();
            List<String> includes = new ArrayList<String>();
            for (DataColumnSpec colSpec : spec) {
                if (filter.includeColumn(colSpec)) {
                    includes.add(colSpec.getName());
                }
            }
            temp =
                    FilterColumnTable.createFilterTableSpec(spec, includes
                            .toArray(new String[includes.size()]));
        }
        DataColumnSpec[] cols = new DataColumnSpec[temp.getNumColumns()];
        for (int i = 0; i < cols.length; i++) {
            // don't use input as basis here, throw away handlers etc.
            DataColumnSpecCreator creator =
                    new DataColumnSpecCreator(temp.getColumnSpec(i).getName(),
                            StringCell.TYPE);
            cols[i] = creator.createSpec();
        }
        return new DataTableSpec("Column Headers", cols);
    }

    private DataTableSpec createOutSpecPort1(final DataTableSpec spec)
            throws InvalidSettingsException {
        if (m_replaceColHeader.getBooleanValue()) {
            return createOutputSpecInternal(spec, true);
        } else {
            return spec;
        }
    }

    /**
     * @return
     * @throws InvalidSettingsException
     */
    private ColType getColType() throws InvalidSettingsException {
        ColType colType;
        try {
            colType = ColType.valueOf(m_colTypeFilter.getStringValue());
        } catch (Exception e) {
            throw new InvalidSettingsException("Unable to get col type for \""
                    + m_colTypeFilter.getStringValue() + "\"");
        }
        return colType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_replaceColHeader.saveSettingsTo(settings);
        m_unifyHeaderPrefix.saveSettingsTo(settings);
        m_colTypeFilter.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_replaceColHeader.loadSettingsFrom(settings);
        m_unifyHeaderPrefix.loadSettingsFrom(settings);
        m_colTypeFilter.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_replaceColHeader.validateSettings(settings);
        m_unifyHeaderPrefix.validateSettings(settings);
        m_colTypeFilter.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        switch (outIndex) {
        case 0:
            return m_hiliteHandler;
        case 1:
            return super.getOutHiLiteHandler(0);
        default:
            throw new IndexOutOfBoundsException("Invalid port: " + outIndex);
        }
    }

    /** @return new settings model for replace col header property. */
    static SettingsModelBoolean createReplaceColHeader() {
        return new SettingsModelBoolean("replaceColHeader", true);
    }

    /** @param replaceColHeader column header model (enable/disable listener)
     * @return new settings model for prefix of new header */
    static SettingsModelString createUnifyHeaderPrefix(
            final SettingsModelBoolean replaceColHeader) {
        final SettingsModelString result =
                new SettingsModelString("unifyHeaderPrefix", "Column ");
        replaceColHeader.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent chEvent) {
                result.setEnabled(replaceColHeader.getBooleanValue());
            }
        });
        return result;
    }

    /** @return new settings model for column filter type. */
    static SettingsModelString createColTypeFilter() {
        return new SettingsModelString("coltype", ColType.All.toString());
    }

}
