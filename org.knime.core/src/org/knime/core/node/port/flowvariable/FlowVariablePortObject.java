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
 *   Sep 17, 2008 (wiswedel): created
 */
package org.knime.core.node.port.flowvariable;

import java.io.IOException;

import javax.swing.JComponent;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;

/**
 * A singleton port object representing a variable input. Flow Variables ports
 * and their connections carry no data, nor variables but just represent a
 * connection. Variables are propagated through the workflow using framework
 * methods, each node can read and write variables using methods in the
 * NodeModel (as opposed to have these methods defined on the port object).
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class FlowVariablePortObject implements PortObject {

    /** Type representing this port object. */
    public static final PortType TYPE =
        new PortType(FlowVariablePortObject.class);

    /** Type representing this port object as optional. */
    public static final PortType TYPE_OPTIONAL =
        new PortType(FlowVariablePortObject.class, true);

    /** Singleton instance to be used. */
    public static final FlowVariablePortObject INSTANCE =
        new FlowVariablePortObject();

    /** Constructor, not to be used.
     * @deprecated There is only one
     * {@link FlowVariablePortObject#INSTANCE instance} to this class. Future
     * versions of KNIME will reduce the scope of this constructor and declare
     * this class final.
     */
    @Deprecated
    public FlowVariablePortObject() {
        // declared deprecated in v2.3
    }

    /** Serializer for this port object. It will return the singleton upon
     * read.
     * @return The serializer as required by the PortObject class.
     */
    public static PortObjectSerializer<FlowVariablePortObject>
    getPortObjectSerializer() {
        return new PortObjectSerializer<FlowVariablePortObject>() {

            @Override
            public FlowVariablePortObject loadPortObject(
                    final PortObjectZipInputStream in,
                    final PortObjectSpec spec, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
                return INSTANCE;
            }

            @Override
            public void savePortObject(final FlowVariablePortObject portObject,
                    final PortObjectZipOutputStream out,
                    final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
                // no op
            }

        };
    }

    /** {@inheritDoc} */
    @Override
    public PortObjectSpec getSpec() {
        return FlowVariablePortObjectSpec.INSTANCE;
    }

    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        return "Variables connection";
    }

    /** {@inheritDoc} */
    @Override
    public JComponent[] getViews() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        return getClass().equals(obj.getClass());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
