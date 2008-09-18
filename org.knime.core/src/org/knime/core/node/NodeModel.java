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
 * --------------------------------------------------------------------- *
 *
 * History
 *   17.01.2006(sieb, ohl): reviewed
 */
package org.knime.core.node;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * Class implements the general model of a node which gives access to the
 * <code>DataTable</code>,<code>HiLiteHandler</code>, and
 * <code>DataTableSpec</code> of all outputs.
 * <p>
 * The <code>NodeModel</code> should contain the node's "model", i.e., what
 * ever is stored, contained, done in this node - it's the "meat" of this node.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class NodeModel extends GenericNodeModel {

    private static PortType[] createDataPorts(final int nrDataPorts) {
        PortType[] pTypes = new PortType[nrDataPorts];
        for (int i = 0; i < nrDataPorts; i++) {
            pTypes[i] = BufferedDataTable.TYPE;
        }
        return pTypes;
    }

    /**
     * Creates a NodeModel with the given number of data in- and out-ports.
     *
     * @param nrDataIns the number of <code>DataTable</code> elements expected
     *            as inputs
     * @param nrDataOuts the number of <code>DataTable</code> objects expected
     *            at the output
     */
    protected NodeModel(final int nrDataIns, final int nrDataOuts) {
        super(createDataPorts(nrDataIns), createDataPorts(nrDataOuts));
    }

    /**
     * @see #configure(PortObjectSpec[])
     */
    protected abstract DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException;

    /**
     * {@inheritDoc}
     */
    @Override
    protected final PortObjectSpec[] configure(final PortObjectSpec[] inPorts)
            throws InvalidSettingsException {
        DataTableSpec[] inSpecs = new DataTableSpec[inPorts.length];
        for (int i = 0; i < inSpecs.length; i++) {
            inSpecs[i] = (DataTableSpec) inPorts[i];
        }
        return this.configure(inSpecs);
    }

    /**
     * @see #execute(PortObject[], ExecutionContext)
     */
    protected abstract BufferedDataTable[] execute(
            final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception;

    @Override
    protected final PortObject[] execute(
            final PortObject[] inPorts, final ExecutionContext exec)
            throws Exception {
        BufferedDataTable[] inData = new BufferedDataTable[inPorts.length];
        for (int i = 0; i < inData.length; i++) {
            inData[i] = (BufferedDataTable) inPorts[i];
        }
        return this.execute(inData, exec);
    }

}

