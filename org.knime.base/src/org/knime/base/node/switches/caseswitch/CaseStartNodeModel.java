/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   May 7, 2015 (wiswedel): created
 */
package org.knime.base.node.switches.caseswitch;

import java.io.File;
import java.io.IOException;

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
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;

/** Shared node model for (almost) all case switch start nodes. Port type is specified in constructor.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @noreference This class is not intended to be referenced by clients.
 */
public final class CaseStartNodeModel extends NodeModel {

    static final String CFGKEY_SELECTEDPORT = "selected output port";

    private int m_selectedOutputPort = 0;

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
        if (m_selectedOutputPort < 0 || m_selectedOutputPort >= 3) {
            // Nonsense settings
            throw new InvalidSettingsException("Selected outputport does not exists. Select a port in the range 0 - 2");
        }

        PortObjectSpec[] outSpecs = new PortObjectSpec[3];
        for (int port = 0; port < outSpecs.length; port++) {
            outSpecs[port] = InactiveBranchPortObjectSpec.INSTANCE;
        }
        outSpecs[m_selectedOutputPort] = inSpecs[0];
        return outSpecs;
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inSpecs,
            final ExecutionContext exec) throws Exception {
        if (m_selectedOutputPort < 0 || m_selectedOutputPort >= 3) {
            // Nonsense settings
            throw new InvalidSettingsException("Selected outputport does not exists. Select a port in the range 0 - 2");
        }

        PortObject[] outSpecs = new PortObject[3];
        for (int port = 0; port < outSpecs.length; port++) {
            outSpecs[port] = InactiveBranchPortObject.INSTANCE;
        }
        outSpecs[m_selectedOutputPort] = inSpecs[0];
        return outSpecs;
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_selectedOutputPort = settings.getInt(CFGKEY_SELECTEDPORT);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addInt(CFGKEY_SELECTEDPORT, m_selectedOutputPort);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        int i = settings.getInt(CFGKEY_SELECTEDPORT);
        if (!(0 <= i && i <= 2)) {
            throw new InvalidSettingsException("Selected outputport does not exists. Select a port in the range 0 - 2");
        }
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
}
