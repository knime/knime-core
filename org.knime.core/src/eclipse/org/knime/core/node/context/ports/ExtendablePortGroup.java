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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Oct 8, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.node.context.ports;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.node.port.PortType;

/**
 * Interface defining any port group where ports can be added and removed.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @noreference This interface is not intended to be referenced by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ExtendablePortGroup extends ConfigurablePortGroup {

    @Override
    default PortType[] getInputPorts() {
        if (definesInputPorts()) {
            return ArrayUtils.addAll(getFixedPorts(), getConfiguredPorts());
        }
        throw UNSUPPORTED_INPUT_OPERATION;
    }

    @Override
    default PortType[] getOutputPorts() {
        if (definesOutputPorts()) {
            return ArrayUtils.addAll(getFixedPorts(), getConfiguredPorts());
        }
        throw UNSUPPORTED_OUTPUT_OPERATION;
    }

    /**
     * Returns the fixed ports.
     *
     * @return the fixed ports
     */
    PortType[] getFixedPorts();

    /**
     * Returns the configured ports.
     *
     * @return the configured ports
     */
    PortType[] getConfiguredPorts();

    /**
     * Defines wether or not additional ports can be added.
     *
     * @return {@code true} if more ports can be added, {@code false} otherwise
     */
    boolean canAddPort();

    /**
     * Returns flag indicating whether or not additional ports have been configured.
     *
     * @return {@code true} if additional ports have been added, {@code false} otherwise
     */
    boolean hasConfiguredPorts();

    /**
     * Adds an port to the configured ports.
     *
     * @param pType the port to be added
     */
    void addPort(final PortType pType);

    /**
     * Removes the last port from the configured ports list.
     *
     * @return the last element from this list
     */
    PortType removeLastPort();

    /**
     * Removes the port at a specified index.
     * @param portIndex The index <i>within the group</i> to remove.
     *
     * @return the port type that was removed
     */
    PortType removePort(int portIndex);

}
