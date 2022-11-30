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
 *   Oct 28, 2022 (hornm): created
 */
package org.knime.core.ui.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.exec.dataexchange.in.BDTInNodeFactory;
import org.knime.core.node.missing.MissingNodeFactory;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.virtual.DefaultVirtualPortObjectInNodeFactory;
import org.mockito.Mockito;

/**
 * Tests {@link NodeTemplateId}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeTemplateIdTest {

    /**
     * Tests {@link NodeTemplateId#callWithNodeTemplateIdVariants(String, String, java.util.function.Function)}.
     */
    @Test
    public void testCallWithNodeTemplateIdVariants() {
        var templateId = "org.knime.core.node.workflow.virtual.DefaultVirtualPortObjectInNodeFactory";
        var dynamicTemplateId =
            "org.knime.core.node.workflow.virtual.DefaultVirtualPortObjectInNodeFactory#Virtual Start";
        UnaryOperator<String> fctMock = Mockito.mock(UnaryOperator.class);
        when(fctMock.apply(templateId)).thenReturn(null);
        when(fctMock.apply(dynamicTemplateId)).thenReturn("res");
        NodeTemplateId.callWithNodeTemplateIdVariants(
            "org.knime.core.node.workflow.virtual.DefaultVirtualPortObjectInNodeFactory", "Virtual Start", fctMock);
        verify(fctMock).apply(templateId);
        verify(fctMock).apply(dynamicTemplateId);
    }

    /**
     * Tests {@link NodeTemplateId#of(org.knime.core.node.NodeFactory)}
     */
    @Test
    @DisabledOnOs({OS.MAC}) // see UIEXT-647
    public void testOf() {
        var dynamicNodeFactory = new DefaultVirtualPortObjectInNodeFactory();
        dynamicNodeFactory.init();
        assertThat(NodeTemplateId.of(dynamicNodeFactory))
            .isEqualTo("org.knime.core.node.workflow.virtual.DefaultVirtualPortObjectInNodeFactory#Virtual Start");
        assertThat(NodeTemplateId.of(new BDTInNodeFactory()))
            .isEqualTo("org.knime.core.node.exec.dataexchange.in.BDTInNodeFactory");
        var missingNodeFactory = new MissingNodeFactory(new NodeAndBundleInformationPersistor("TEST"), null,
            new PortType[0], new PortType[0]);
        missingNodeFactory.init();
        assertThat(NodeTemplateId.of(missingNodeFactory)).isEqualTo("TEST#MISSING TEST");
    }

    /**
     * Tests {@link NodeTemplateId#ofDynamicNodeFactory(String, String)}.
     */
    @Test
    public void testOfDynamicNodeFactory() {
        assertThat(NodeTemplateId.ofDynamicNodeFactory(
            "org.knime.core.node.workflow.virtual.DefaultVirtualPortObjectInNodeFactory", "Virtual Start"))
                .isEqualTo("org.knime.core.node.workflow.virtual.DefaultVirtualPortObjectInNodeFactory#Virtual Start");
    }

}
