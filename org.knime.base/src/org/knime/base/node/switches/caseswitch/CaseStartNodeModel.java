/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *   May 7, 2015 (wiswedel): created
 */
package org.knime.base.node.switches.caseswitch;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;

/** Shared node model for (almost) all case switch start nodes. Port type is specified in constructor.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @noreference This class is not intended to be referenced by clients.
 */
public final class CaseStartNodeModel extends NodeModel {

    private final SettingsModelIntegerBounded m_selectedPortModel = createSelectedPortModel();
    private final SettingsModelBoolean m_activateAllOutputsDuringConfigureModel =
            createActivateAllOutputsDuringConfigureModel();

    /** One in, three out.
     * @param portType Type of ins and outs.
     */
    protected CaseStartNodeModel(final PortType portType) {
        super(new PortType[]{portType}, new PortType[]{portType, portType, portType});
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        int selectedOutputPort = m_selectedPortModel.getIntValue();
        PortObjectSpec[] outSpecs = new PortObjectSpec[3];
        PortObjectSpec defOutput = m_activateAllOutputsDuringConfigureModel.getBooleanValue()
                ? inSpecs[0] : InactiveBranchPortObjectSpec.INSTANCE;
        Arrays.fill(outSpecs, defOutput);
        outSpecs[selectedOutputPort] = inSpecs[0];
        return outSpecs;
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inSpecs,
            final ExecutionContext exec) throws Exception {
        int selectedOutputPort = m_selectedPortModel.getIntValue();

        PortObject[] outObjects = new PortObject[3];
        Arrays.fill(outObjects, InactiveBranchPortObject.INSTANCE);
        outObjects[selectedOutputPort] = inSpecs[0];
        return outObjects;
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_selectedPortModel.loadSettingsFrom(settings);
        m_activateAllOutputsDuringConfigureModel.loadSettingsFrom(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_selectedPortModel.saveSettingsTo(settings);
        m_activateAllOutputsDuringConfigureModel.saveSettingsTo(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_selectedPortModel.validateSettings(settings);
        m_activateAllOutputsDuringConfigureModel.validateSettings(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    static SettingsModelIntegerBounded createSelectedPortModel() {
        return new SettingsModelIntegerBounded("active_outport", 0, 0, 2);
    }

    static SettingsModelBoolean createActivateAllOutputsDuringConfigureModel() {
        return new SettingsModelBoolean("activate_all_outputs_during_configure", true);
    }
}
