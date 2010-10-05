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
 * ---------------------------------------------------------------------
 *
 * History
 *   27.02.2008 (thor): created
 */
package org.knime.base.node.meta.feature.backwardelim;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
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
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.util.Pair;

/**
 * This class is the model for the feature elimination filter node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class BWElimFilterNodeModel extends NodeModel {
    private final BWElimFilterSettings m_settings = new BWElimFilterSettings();

    /**
     * Creates a new model with one input and two output ports.
     */
    public BWElimFilterNodeModel() {
        super(new PortType[]{BWElimModel.TYPE, BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        BWElimModel model = (BWElimModel)inSpecs[0];
        if (model == null) {
            throw new InvalidSettingsException("No model available");
        }

        if (m_settings.nrOfFeatures() < 1) {
            throw new InvalidSettingsException("No features selected yet");
        }

        DataTableSpec tSpec = (DataTableSpec)inSpecs[1];

        int missing = 0;
        HashSet<String> allColumns = new HashSet<String>();
        for (Pair<Double, Collection<String>> p : model.featureLevels()) {
            allColumns.addAll(p.getSecond());
        }
        for (String s : allColumns) {
            if (!tSpec.containsName(s)) {
                missing++;
            }
        }
        if (missing >= allColumns.size()) {
            throw new InvalidSettingsException("Input table does not contain "
                    + "any of the columns used in the feature elimination ");
        }


        missing = 0;
        String missingColumns = ", ";
        Collection<String> incFeatures = m_settings.includedColumns(model);
        for (String s : incFeatures) {
            if (!tSpec.containsName(s)) {
                missing++;
                missingColumns += s + ", ";
            }
        }

        if (missing > 0) {
            setWarningMessage("The following columns used in the selected "
                    + " level are missing in the input table: "
                    + missingColumns.substring(0, missingColumns.length() - 2));
        }
        ColumnRearranger crea = createRearranger(tSpec, incFeatures);

        return new DataTableSpec[]{crea.createSpec()};
    }


    private ColumnRearranger createRearranger(final DataTableSpec inSpec,
            final Collection<String> includedColumns) {
        ColumnRearranger crea = new ColumnRearranger(inSpec);

        for (DataColumnSpec cs : inSpec) {
            if (!includedColumns.contains(cs.getName())) {
                crea.remove(cs.getName());
            }
        }

        return crea;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        BWElimModel model = (BWElimModel)inData[0];
        BufferedDataTable table = (BufferedDataTable)inData[1];

        ColumnRearranger crea =
                createRearranger(((DataTable)inData[1]).getDataTableSpec(),
                        m_settings.includedColumns(model));

        return new PortObject[]{exec.createColumnRearrangeTable(table, crea,
                exec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        BWElimFilterSettings s = new BWElimFilterSettings();
        s.loadSettings(settings);
    }
}
