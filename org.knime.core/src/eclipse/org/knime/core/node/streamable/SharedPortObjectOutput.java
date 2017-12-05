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
 *   Dec 4, 2017 (clemens): created
 */
package org.knime.core.node.streamable;

import java.io.Serializable;

/**
 *
 * @author Clemens von Schwerin, University of Ulm
 * @since 3.5
 */
public class SharedPortObjectOutput<T extends Serializable> extends PortOutput {

    private SharedContainerPortObject<T> m_portObject;

    /**
     * Constructor.
     * Create an internal {@link SharedContainerPortObject} with a null encapsualted object.
     */
    public SharedPortObjectOutput() {
        m_portObject = new SharedContainerPortObject<T>();
    }

    /** @param objectToShare the object to share with another node, non-null. */
    public void setContainedPortObject(final T objectToShare) {
        assert objectToShare != null;
        m_portObject.set(objectToShare);
    }

    /**
     * @return the port object
     */
    public SharedContainerPortObject<T> getPortObject() {
        return m_portObject;
    }

    /**
     * Wait for the latest version of the object encapsulated in the port object. Lock and retrieve it.
     * Run the given updater on it.
     *
     * @param updater a updater using the encapsulated object
     */
    public void updateAndPush(final Updater<T> updater) {
        try {
            updater.update(m_portObject.getAndLock());
        } finally {
            m_portObject.unlock();
        }
    }

    /**
     * A function using the object encapsulated in the port object (command pattern).
     * @param <T> the type of the object to share inside the port object
     */
    public interface Updater<T> {

        /**
         * Update the given object (e.g. do a training step on a model).
         * NOTE: Do not persist the passed object.
         *
         * @param object the object that should be used for updating
         */
        void update(T object);
    }

}
