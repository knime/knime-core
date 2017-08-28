/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Sep 22, 2016 (hornm): created
 */
package org.knime.core.util;

import org.knime.core.def.node.port.PortTypeUID;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;

/**
 * Utility class for {@link PortType}-functions (e.g. conversion).
 *
 * @author Martin Horn, KNIME.com
 */
public class PortTypeUtil {

    public static final PortTypeUID BufferedDataTablePortTypeUID = getPortTypeUID(BufferedDataTable.TYPE);
    public static final PortTypeUID BuffferedDataTablePortTypeOptionalUID = getPortTypeUID(BufferedDataTable.TYPE_OPTIONAL);
    public static final PortTypeUID FlowVariablePortTypeUID = getPortTypeUID(FlowVariablePortObject.TYPE);
    public static final PortTypeUID FlowVariablePortTypeOptionalUID = getPortTypeUID(FlowVariablePortObject.TYPE_OPTIONAL);

    private PortTypeUtil() {
        // utility class
    }

    /**
     * Determines the unique port type identifier {@PortTypeUID} from the given {@link PortType}.
     *
     * @param portType
     * @return the new port type uid
     */
    public static PortTypeUID getPortTypeUID(final PortType portType) {
        return PortTypeUID.builder(portType.getPortObjectClass().getName()).setName(portType.getName())
            .setColor(portType.getColor()).setIsHidden(portType.isHidden()).setIsOptional(portType.isOptional())
            .build();
    }

    /**
     * Determines the {@link PortType} from the given {@link PortTypeUID}.
     *
     * @param portTypeUID
     * @return the determined port type
     */
    public static PortType getPortType(final PortTypeUID portTypeUID) {
        //TODO: how to deal with the isHidden, color etc. attributes???
        PortTypeRegistry ptr = PortTypeRegistry.getInstance();
        return ptr.getPortType(ptr.getObjectClass(portTypeUID.getPortObjectClassName()).get(),
            portTypeUID.isOptional());
    }
}
