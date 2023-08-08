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
 *   Aug 8, 2023 (wiswedel): created
 */
package org.knime.core.node.port;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.image.ImagePortObject;

/**
 * Tests {@link PortTypeRegistry} extension point and discovery of unregistered types.
 *
 * @author Bernd Wiswedel
 */
class PortTypeRegistryTest {

    /** A port object not registered via extension point. Expected to find it under its parent. */
    private static final class TestImagePortObject extends ImagePortObject {

    }

    @SuppressWarnings("static-method")
    @Test
    final void testPortTypeInCore() {
        final var instance = PortTypeRegistry.getInstance();
        Assertions.assertThat(instance.getAvailablePortTypeMap()) //
            .as("Generic Port Object is registered").containsKey(PortObject.class) //
            .as("BDT is registered").containsKey(BufferedDataTable.class) //
            .as("Image Port properly registered via extension point (present)").containsKey(ImagePortObject.class) //
            .as("Unknown runtime port object not registered").doesNotContainKey(TestImagePortObject.class);

        PortType imagePOType = instance.getPortType(ImagePortObject.class);
        Assertions.assertThat(imagePOType) //
            .as("Image Type is not null").isNotNull() //
            .as("object class of image").extracting(type -> type.getPortObjectClass()).isEqualTo(ImagePortObject.class);

        PortType testImagePOType = instance.getPortType(TestImagePortObject.class);
        Assertions.assertThat(testImagePOType).as("runtime port object class to have same type as registered type")
            .isSameAs(imagePOType);
        Assertions.assertThat(instance.getAvailablePortTypeMap()) //
            .as("Unknown runtime port object now registered").containsKey(TestImagePortObject.class);

    }

}
