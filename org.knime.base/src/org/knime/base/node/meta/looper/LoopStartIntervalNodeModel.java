/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
            pushScopeVariableInt("loop_from", (int)Math
                    .round(m_settings.from()));
            pushScopeVariableInt("loop_to", (int)Math.round(m_settings.to()));
            pushScopeVariableInt("loop_step", (int)m_settings.step());
            pushScopeVariableInt("loop_value", (int)Math.round(m_value));
        } else {
            pushScopeVariableDouble("loop_from", m_settings.from());
            pushScopeVariableDouble("loop_to", m_settings.to());
            pushScopeVariableDouble("loop_step", m_settings.step());
            pushScopeVariableDouble("loop_value", m_value);
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
            pushScopeVariableInt("loop_from", (int)Math.round(m_settings.from()));
            pushScopeVariableInt("loop_to", (int)Math.round(m_settings.to()));
            pushScopeVariableInt("loop_step", (int)m_settings.step());
            pushScopeVariableInt("loop_value", (int)Math.round(m_value));
        } else {
            pushScopeVariableDouble("loop_from", m_settings.from());
            pushScopeVariableDouble("loop_to", m_settings.to());
            pushScopeVariableDouble("loop_step", m_settings.step());
            pushScopeVariableDouble("loop_value", m_value);
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
