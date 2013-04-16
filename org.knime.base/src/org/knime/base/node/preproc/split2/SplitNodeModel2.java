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
 *   06.09.2005 (bernd): created
 */
package org.knime.base.node.preproc.split2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 * NodeModel with one input, two outputs. It splits the in table (column-based)
 * into a top and a bottom table.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class SplitNodeModel2 extends NodeModel {
    
    private DataColumnSpecFilterConfiguration m_conf;
    
    /** Config key for the filter column settings. */
    public static final String CFG_FILTERCOLS = "Filter Column Settings";

    /** Split node model with one data in-port and two data out-ports.  */
    public SplitNodeModel2() {
        super(1, 2);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_conf == null) {
            m_conf = createColFilterConf();
        }
        m_conf.saveConfiguration(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        DataColumnSpecFilterConfiguration conf = createColFilterConf();
        conf.loadConfigurationInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        DataColumnSpecFilterConfiguration conf = createColFilterConf();
        conf.loadConfigurationInModel(settings);
        m_conf = conf;
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        if (m_conf == null) {
            m_conf = createColFilterConf();
        }
        
        BufferedDataTable in = inData[0];
        DataTableSpec inSpec = in.getDataTableSpec();
        ColumnRearranger[] a = createColumnRearrangers(inSpec);
        BufferedDataTable[] outs = new BufferedDataTable[2];
        // exec won't be used here! It's just a wrapper.
        outs[0] = exec.createColumnRearrangeTable(in, a[0], exec);
        outs[1] = exec.createColumnRearrangeTable(in, a[1], exec);
        return outs;
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // no op
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no op
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no op
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec in = inSpecs[0];
        
        if (m_conf == null) {
            m_conf = createColFilterConf();
            m_conf.loadDefaults(in, false);
            setWarningMessage("No settings available, "
                    + "passing all columns to top output port.");            
        }        
        final FilterResult filter = m_conf.applyTo(in);
        String[] top = filter.getExcludes();
        String[] bottom = filter.getIncludes();
        
        for (int i = 0; i < top.length; i++) {
            if (!in.containsName(top[i])) {
                throw new InvalidSettingsException("No such column: "
                        + top[i]);
            }
        }
        for (int i = 0; i < bottom.length; i++) {
            if (!in.containsName(bottom[i])) {
                throw new InvalidSettingsException("No such column: "
                        + bottom[i]);
            }
        }
        ColumnRearranger[] a = createColumnRearrangers(in);
        return new DataTableSpec[]{a[0].createSpec(), a[1].createSpec()};
    }

    private ColumnRearranger[] createColumnRearrangers(final DataTableSpec s) {
        // apply spec to settings to retrieve filter instance
        final FilterResult filter = m_conf.applyTo(s);
        final StringBuilder warn = new StringBuilder();
        int maxToReport = 3;
        final String[] unknownsIncl = filter.getRemovedFromIncludes();
        final String[] unknownsExcl = filter.getRemovedFromExcludes();
        final ArrayList<String> unknowns = new ArrayList<String>();
        unknowns.addAll(Arrays.asList(unknownsIncl));
        unknowns.addAll(Arrays.asList(unknownsExcl));
        if (unknowns.size() > 0) {
            warn.append("Some columns are no longer available: ");
            for (int i = 0; i < unknowns.size(); i++) {
                warn.append(i > 0 ? ", " : "");
                if (i < maxToReport) {
                    warn.append("\"").append(unknowns.get(i)).append("\"");
                } else {
                    warn.append("...<").append(unknowns.size() - maxToReport).append(" more>");
                    break;
                }
            }
        }        
        final String[] incls = filter.getIncludes();
        final String[] excls = filter.getExcludes();
        if (incls.length == 0) {
            if (excls.length > 0) {
                if (warn.length() == 0) {
                    warn.append("All columns in top partition.");
                } else {
                    warn.append("; all columns in top partition.");
                }
            }
        } else {
            if (excls.length == 0) {
                if (warn.length() == 0) {
                    warn.append("All columns in bottom partition.");
                } else {
                    warn.append("; all columns in bottom partition.");
                }
            }
        }
        
        // set warn message, if there is one
        if (warn.length() > 0) {
            setWarningMessage(warn.toString());
        }
        
        ColumnRearranger topArrange = new ColumnRearranger(s);
        topArrange.keepOnly(filter.getExcludes());
        ColumnRearranger bottomArrange = new ColumnRearranger(s);
        bottomArrange.keepOnly(filter.getIncludes());
        return new ColumnRearranger[]{topArrange, bottomArrange};
    }
    
    /**
     * @return creates and returns configuration instance for column filter 
     * panel.
     */
    private DataColumnSpecFilterConfiguration createColFilterConf() {
        return new DataColumnSpecFilterConfiguration(
                SplitNodeModel2.CFG_FILTERCOLS);
    }     
}
