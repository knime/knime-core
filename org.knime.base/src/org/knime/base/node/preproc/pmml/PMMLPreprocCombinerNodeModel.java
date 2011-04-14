/*
 *
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
 * -------------------------------------------------------------------
 *
 * History
 *   Mar 17, 2010 (morent): created
 */
package org.knime.base.node.preproc.pmml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.knime.core.node.port.pmml.preproc.PMMLPreprocOperation;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocPortObject;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocPortObjectSpec;

/**
 * Combines two PMML preprocessing port objects into one.
 *
 * @author Dominik Morent
 */
public class PMMLPreprocCombinerNodeModel extends NodeModel {

    /**
     * Constructor for the node model.
     */
    protected PMMLPreprocCombinerNodeModel() {
        super(new PortType[] {
                PMMLPreprocPortObject.TYPE, PMMLPreprocPortObject.TYPE},
                new PortType[] {PMMLPreprocPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {
        PMMLPreprocPortObject firstPort = (PMMLPreprocPortObject) inObjects[0];
        PMMLPreprocPortObject secondPort = (PMMLPreprocPortObject) inObjects[1];

        /* Make sure that each preprocessing node is at most merged once into
         * the PMML preprocessing fragment. */
        Set<String> opClassNames = new HashSet<String>();
        for (PMMLPreprocOperation op : firstPort.getOperations()) {
            opClassNames.add(op.getClass().getName());
        }
        for (PMMLPreprocOperation op : secondPort.getOperations()) {
            String className = op.getClass().getName();
            if (opClassNames.contains(className)) {
                throw new Exception(className + " is already contained in "
                        + "PMML Fragment. Each preprocessing node is allowed at"
                        + " most once.");
            }
        }

        PMMLPreprocPortObject outport = new PMMLPreprocPortObject(
                firstPort, secondPort);
        return new PortObject[] {outport};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to be reset
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         // node has no settings
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // node has no settings
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // node has no settings
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // node has no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // node has no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        List<String> colNames = new ArrayList<String>();
        for (PortObjectSpec spec : inSpecs) {
            colNames.addAll(((PMMLPreprocPortObjectSpec)spec).getColumnNames());
        }
        return new PortObjectSpec[] {new PMMLPreprocPortObjectSpec(colNames)};
    }



}

