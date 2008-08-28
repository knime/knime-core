/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   13.02.2008 (thor): created
 */
package org.knime.base.node.meta.looper;

import java.io.File;
import java.io.IOException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.LoopStartNode;

/**
 * This model is the head node of a for loop.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ForLoopHeadNodeModel extends GenericNodeModel implements LoopStartNode {

    private int m_iteration;

    private final ForLoopHeadSettings m_settings = new ForLoopHeadSettings();

    /**
     * Creates a new model with one input and one output port.
     */
    public ForLoopHeadNodeModel() {
        super(new PortType[] {new PortType(PortObject.class)},
                new PortType[] {new PortType(PortObject.class)});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_settings.loops() < 1) {
            throw new InvalidSettingsException("Cannot loop fewer than once");
        }
        assert m_iteration == 0;
        pushScopeVariableInt("currentIteration", m_iteration);
        pushScopeVariableInt("maxIterations", m_settings.loops());
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
            assert m_iteration == 0;
        } else {
            assert m_iteration > 0;
            // otherwise we do this again, and we increment our counter
            // and we can do a quick sanity check
            if (!(getLoopEndNode() instanceof ForLoopTailNodeModel)) {
                throw new IllegalArgumentException("Loop tail has wrong type!");
            }
        }
        // we need to put the counts on the stack for the loop's tail to see:
        pushScopeVariableInt("currentIteration", m_iteration);
        pushScopeVariableInt("maxIterations", m_settings.loops());
        // increment counter for next iteration
        m_iteration++;
        return inData;
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
        m_settings.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_iteration = 0;
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
        new ForLoopHeadSettings().loadSettingsFrom(settings);
    }
}
