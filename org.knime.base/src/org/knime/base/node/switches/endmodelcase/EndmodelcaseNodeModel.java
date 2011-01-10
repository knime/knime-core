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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Sept 17 2008 (mb): created (from wiswedel's TableToVariableNode)
 */
package org.knime.base.node.switches.endmodelcase;

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
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.inactive.InactiveBranchConsumer;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;

/**
 * End of a CASE Statement. Takes the object from one active branch and
 * outputs it. If zero or more than one branch are active: kabumm.
 *
 * @author M. Berthold, University of Konstanz
 */
public class EndmodelcaseNodeModel extends NodeModel
        implements InactiveBranchConsumer {

    /**
     * One + 3 optional inputs, one output.
     */
    protected EndmodelcaseNodeModel() {
        super(getInPortTypes(3),
                new PortType[] {new PortType(PortObject.class)});
    }

    private static final PortType[] getInPortTypes(final int nrIns) {
        if (nrIns < 1) {
            throw new IllegalArgumentException("invalid input count: " + nrIns);
        }
        PortType[] result = new PortType[nrIns];
        Arrays.fill(result, new PortType(PortObject.class, true));
        result[0] = new PortType(PortObject.class);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        int i = 0;
        int activeI = -1;
        while (i < inSpecs.length) {
            if (inSpecs[i] != null) {
                // port connected
                if (!(inSpecs[i] instanceof InactiveBranchPortObjectSpec)) {
                    // and active branch!
                    if (activeI >= 0) {
                        // but we already found one before!
                        throw new InvalidSettingsException("More than one"
                        		+ " active branch not supported!.");
                    }
                    activeI = i;
                }
            }
            i++;
        }
        if (activeI < 0) {
            throw new InvalidSettingsException("No active branch found!");
        }
        return new PortObjectSpec[]{ inSpecs[activeI] };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        int i = 0;
        int activeI = -1;
        while (i < inData.length) {
            if (inData[i] != null) {
                // port connected
                if (!(inData[i] instanceof InactiveBranchPortObject)) {
                    // and active branch!
                    if (activeI >= 0) {
                        // but we already found one before!
                        throw new InvalidSettingsException("More than one"
                                + " active branch not supported!.");
                    }
                    activeI = i;
                }
            }
            i++;
        }
        if (activeI < 0) {
            throw new InvalidSettingsException("No active branch found!");
        }
        return new PortObject[]{ inData[activeI] };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
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

}
