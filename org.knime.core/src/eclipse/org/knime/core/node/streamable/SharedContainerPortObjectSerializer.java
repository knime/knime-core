/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 *   Dec 6, 2017 (clemens): created
 */
package org.knime.core.node.streamable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObject.PortObjectSerializer;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;

/**
 *
 * @author clemens
 * @since 3.5
 */
public abstract class SharedContainerPortObjectSerializer extends PortObjectSerializer<SharedContainerPortObject<?>> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void savePortObject(final SharedContainerPortObject<?> portObject, final PortObjectZipOutputStream out,
        final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
        ObjectOutputStream objOut = new ObjectOutputStream(out);
        try{
            objOut.writeObject(portObject.getAndLock());
        } finally {
            portObject.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SharedContainerPortObject<?> loadPortObject(final PortObjectZipInputStream in, final PortObjectSpec spec,
        final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
        ObjectInputStream objIn = new ObjectInputStream(in);
        Class<? extends Serializable> objclass = getContainedClassForPortobject((SharedContainerPortObjectSpec)spec);
        try {
            return cast(objclass, objIn.readObject());
        } catch (ClassNotFoundException ex) {
            throw new IOException("Could not find class: " + ((SharedContainerPortObjectSpec)spec).getClass());
        }
    }

    private <T extends Serializable>SharedContainerPortObject<T> cast(final Class<T> objclass, final Object obj) {
        SharedContainerPortObject<T> containerObj = new SharedContainerPortObject<T>(objclass.cast(obj));
        return containerObj;
    }

    protected abstract Class<? extends Serializable> getContainedClassForPortobject(SharedContainerPortObjectSpec spec);

}
