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
 *   28 Feb 2022 (Dionysios Stolis): created
 */
package org.knime.core.node.workflow.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBase;
import org.knime.core.node.util.NodeLoaderTestUtils;
import org.knime.core.util.LoadVersion;
import org.knime.core.workflow.def.ConfigMapDef;
import org.knime.core.workflow.def.JobManagerDef;
import org.knime.core.workflow.def.NodeAnnotationDef;
import org.knime.core.workflow.def.WorkflowDef;
import org.knime.core.workflow.def.impl.FallibleBaseNodeDef;
import org.knime.core.workflow.def.impl.FallibleConfigurableNodeDef;

/**
 *
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 */
class ComponentLoaderTest {

    @Test
    void simpleComponentLoaderTest() throws InvalidSettingsException, IOException {
        // given
        var file = NodeLoaderTestUtils.readResourceFolder("Simple_Component");

        ConfigBase m_configBaseRO = new SimpleConfig("mock");
        m_configBaseRO.addInt("id", 1);
        m_configBaseRO.addString("node_type", "SubNode");
        m_configBaseRO.addString("customDescription", "");

        // when
        var componentDef = ComponentLoader.load(m_configBaseRO, file, LoadVersion.FUTURE);
        var singleNodeDef = (FallibleConfigurableNodeDef) componentDef.getConfigurableNode();
        var nodeDef = (FallibleBaseNodeDef) singleNodeDef.getBaseNode();

        // then

        // Assert ComponentLoader
        assertThat(componentDef.getDialogSettings()).isNull();
        assertThat(componentDef.getInPorts()).hasSize(1).extracting(p -> p.getIndex(), p -> p.getName()) //
            .containsExactlyInAnyOrder( //
                tuple(0, "inport_0"));
        assertThat(componentDef.getOutPorts()).isEmpty();
        assertThat(componentDef.getVirtualInNodeId()).isEqualTo(3);
        assertThat(componentDef.getVirtualOutNodeId()).isEqualTo(4);
        assertThat(componentDef.getLink()).isNotNull();

        // Assert SingleNodeLoader
        assertThat(singleNodeDef.getFlowStack()).isEmpty(); //
        //TODO assert the ConfigMap value
        assertThat(singleNodeDef.getInternalNodeSubSettings().getChildren()).containsKey("memory_policy");
        //        assertThat(nativeNodeDef.getModelSettings().getChildren());
        assertThat(singleNodeDef.getVariableSettings().getChildren()).isEmpty();

        // Assert NodeLoader
        assertThat(nodeDef.getId()).isEqualTo(1);
        assertThat(nodeDef.getAnnotation().getData()).isNull();
        assertThat(nodeDef.getCustomDescription()).isNull();
        assertThat(nodeDef.getJobManager()).isNotNull();
        assertThat(nodeDef.getLocks()) //
            .extracting("m_hasDeleteLock", "m_hasResetLock", "m_hasConfigureLock") //
            .containsExactly(false, false, false);
        assertThat(nodeDef.getUiInfo()).extracting(n -> n.hasAbsoluteCoordinates(), n -> n.isSymbolRelative(),
            n -> n.getBounds().getHeight(), n -> n.getBounds().getLocation(), n -> n.getBounds().getWidth())
            .containsNull();

        assertThat(componentDef.getSuppliers().size()).isOne();
        assertThat(singleNodeDef.hasExceptions()).isFalse();
        assertThat(nodeDef.hasExceptions()).isFalse();
    }

    @Test
    void multiPortComponentTest() throws InvalidSettingsException, IOException {
        // given
        var file = NodeLoaderTestUtils.readResourceFolder("MultiPort_Component");

        ConfigBase workflowConfig = new SimpleConfig("mock");
        workflowConfig.addInt("id", 431);
        workflowConfig.addIntArray("extrainfo.node.bounds", new int[]{2541, 1117, 122, 65});

        // when
        var componentDef = ComponentLoader.load(workflowConfig, file, LoadVersion.FUTURE);
        var singleNodeDef = componentDef.getConfigurableNode();
        var nodeDef = singleNodeDef.getBaseNode();

        // then

        // Assert MetaNodeLoader
        assertThat(componentDef.getInPorts())
            .extracting(p -> p.getIndex(), p -> p.getName(),
                p -> p.getPortType().getPortObjectClass().endsWith("BufferedDataTable"))
            .contains(tuple(0, "inport_0", true));
        assertThat(componentDef.getOutPorts())
            .extracting(p -> p.getIndex(), p -> p.getName(),
                p -> p.getPortType().getPortObjectClass().endsWith("BufferedDataTable"))
            .contains(tuple(0, "outport_0", true), tuple(1, "outport_1", true));
        assertThat(componentDef.getVirtualInNodeId()).isEqualTo(10);
        assertThat(componentDef.getVirtualOutNodeId()).isEqualTo(11);
        assertThat(componentDef.getLink()).isNotNull();
        assertThat(componentDef.getWorkflow()).isInstanceOf(WorkflowDef.class);

        // Assert SingleNodeLoader
        assertThat(singleNodeDef.getFlowStack()).isEmpty(); //
        //TODO assert the ConfigMap value
        assertThat(singleNodeDef.getInternalNodeSubSettings().getChildren()).containsKey("memory_policy");
        //        assertThat(nativeNodeDef.getModelSettings().getChildren());
        assertThat(singleNodeDef.getVariableSettings()).isInstanceOf(ConfigMapDef.class);

        // Assert NodeLoader
        assertThat(nodeDef.getId()).isEqualTo(431);
        assertThat(nodeDef.getAnnotation()).isInstanceOf(NodeAnnotationDef.class);
        assertThat(nodeDef.getCustomDescription()).isNull();
        assertThat(nodeDef.getJobManager()).isInstanceOf(JobManagerDef.class);
        assertThat(nodeDef.getLocks()) //
            .extracting("m_hasDeleteLock", "m_hasResetLock", "m_hasConfigureLock") //
            .containsExactly(false, false, false);
        assertThat(nodeDef.getUiInfo()).extracting(n -> n.getBounds().getLocation().getX(),
            n -> n.getBounds().getLocation().getY(), n -> n.getBounds().getHeight(), n -> n.getBounds().getWidth())
            .containsExactly(2541, 1117, 122, 65);

        assertThat(componentDef.hasExceptions()).isFalse();
    }

}
