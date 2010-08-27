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
 *   24.02.2009 (meinl): created
 */
package org.knime.base.node.meta.looper;

import java.io.File;
import java.io.IOException;

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
import org.knime.core.node.workflow.LoopStartNodeTerminator;

/**
 * This is the model for the interval loop start node. It lets the user defined
 * an interval in which a variable is increased by a certain amount in each
 * iteration.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class LoopStartIntervalNodeModel extends NodeModel implements
        LoopStartNodeTerminator {

    private double m_value;

    private final LoopStartIntervalSettings m_settings =
            new LoopStartIntervalSettings();

    /**
     * Creates a new model with one input and one output port.
     */
    public LoopStartIntervalNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        if ((m_settings.from() > m_settings.to()) ^ (m_settings.step() < 0)) {
            throw new InvalidSettingsException("From must be smaller than to");
        }

        m_value = m_settings.from();
        if (m_settings.integerLoop()) {
            pushFlowVariableInt("loop_from", (int)Math
                    .round(m_settings.from()));
            pushFlowVariableInt("loop_to", (int)Math.round(m_settings.to()));
            pushFlowVariableInt("loop_step", (int)m_settings.step());
            pushFlowVariableInt("loop_value", (int)Math.round(m_value));
        } else {
            pushFlowVariableDouble("loop_from", m_settings.from());
            pushFlowVariableDouble("loop_to", m_settings.to());
            pushFlowVariableDouble("loop_step", m_settings.step());
            pushFlowVariableDouble("loop_value", m_value);
        }
        return inSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        // let's see if we have access to the tail: if we do, it's not the
        // first time we are doing this...
        if (getLoopEndNode() == null) {
            // if it's null we know that this is the first time the
            // loop is being executed.
            assert m_value == m_settings.from();
        } else {
            assert m_value != m_settings.from();
            // otherwise we do this again, and we increment our counter
            // and we can do a quick sanity check
            // FIXME: this test is to specific, do we need it after all?
            // if (!(getLoopEndNode() instanceof LoopEndNodeModel)) {
            // throw new IllegalArgumentException("Loop tail has wrong type!");
            // }
        }
        // let's also put the counts on the stack for someone else:

        if (m_settings.integerLoop()) {
            pushFlowVariableInt("loop_from", (int)Math.round(m_settings.from()));
            pushFlowVariableInt("loop_to", (int)Math.round(m_settings.to()));
            pushFlowVariableInt("loop_step", (int)m_settings.step());
            pushFlowVariableInt("loop_value", (int)Math.round(m_value));
        } else {
            pushFlowVariableDouble("loop_from", m_settings.from());
            pushFlowVariableDouble("loop_to", m_settings.to());
            pushFlowVariableDouble("loop_step", m_settings.step());
            pushFlowVariableDouble("loop_value", m_value);
        }

        // increment counter for next iteration
        m_value += m_settings.step();
        return inData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean terminateLoop() {
        return m_value > m_settings.to();
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
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_value = m_settings.from();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        LoopStartIntervalSettings s = new LoopStartIntervalSettings();
        s.loadSettings(settings);

        if ((s.from() > s.to()) ^ (s.step() < 0)) {
            throw new InvalidSettingsException("From must be smaller than to");
        }
    }
}
