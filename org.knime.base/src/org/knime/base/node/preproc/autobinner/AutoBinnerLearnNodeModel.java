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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.preproc.autobinner;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.knime.base.node.preproc.autobinner.apply.AutoBinnerApply;
import org.knime.base.node.preproc.autobinner.pmml.PMMLDiscretizePreprocPortObject;
import org.knime.base.node.preproc.autobinner.pmml.PMMLPreprocDiscretize;
import org.knime.core.data.DataTableSpec;
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
import org.knime.core.node.port.pmml.preproc.PMMLPreprocPortObject;

/**
 * Performs a mapping from continuous to discrete values.
 *
 * @author Heiko Hofer
 */
final class AutoBinnerLearnNodeModel extends NodeModel {
    private final AutoBinnerLearnSettings m_settings;

    /** Creates a new binner. */
    AutoBinnerLearnNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE,
                    PMMLPreprocPortObject.TYPE});
        m_settings = new AutoBinnerLearnSettings();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
      throws InvalidSettingsException {
        DataTableSpec dataSpec = (DataTableSpec)inSpecs[0];

        AutoBinner binner = new AutoBinner(m_settings);

        return binner.getOutputSpec(dataSpec);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable data = (BufferedDataTable)inObjects[0];

        AutoBinner binner = new AutoBinner(m_settings);
        BufferedDataTable inData = binner.calcDomainBoundsIfNeccessary(data,
                exec, Arrays.asList(m_settings.getTargetColumn()));

        PMMLPreprocDiscretize op = binner.execute(inData, exec);

        AutoBinnerApply applier = new AutoBinnerApply();
        BufferedDataTable outData = applier.execute(op, inData, exec);
        return new PortObject[]{
                outData,
                new PMMLDiscretizePreprocPortObject(op)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to reset
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
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        AutoBinnerLearnSettings s = new AutoBinnerLearnSettings();
        s.loadSettings(settings);
        AutoBinner.validateSettings(s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Node has no internal data.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Node has no internal data.
    }
}
