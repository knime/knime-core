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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.util.NodeLoaderTestUtils;
import org.knime.core.util.LoadVersion;
import org.knime.core.workflow.def.impl.DefaultFlowContextDef;
import org.knime.core.workflow.def.impl.NativeNodeDefBuilder.WithExceptionsDefaultNativeNodeDef;
import org.knime.core.workflow.def.impl.NodeDefBuilder.WithExceptionsDefaultNodeDef;
import org.knime.core.workflow.def.impl.SingleNodeDefBuilder.WithExceptionsDefaultSingleNodeDef;

/**
 *
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 */
class NativeNodeLoaderTest {

    private ConfigBaseRO m_configBaseRO;

    @BeforeEach
    void setUp() {
        m_configBaseRO = mock(ConfigBaseRO.class);
    }


    @Test
    void testGenericLoopStart() throws InvalidSettingsException, IOException {
        // given
        var file = NodeLoaderTestUtils.readResourceFolder("Generic_Loop_Start");

        when(m_configBaseRO.getInt("id")).thenReturn(1);
        when(m_configBaseRO.containsKey("customDescription")).thenReturn(true);

        // when

        var nativeNodeDef = NativeNodeLoader.load(m_configBaseRO, file, LoadVersion.FUTURE);
        var singleNodeDef = nativeNodeDef.getNode();
        var nodeDef = singleNodeDef.getNode();

        // then

        // Assert NativeNodeLoader
        assertThat(nativeNodeDef.getNodeName()).isEqualTo("Generic Loop Start");
        assertThat(nativeNodeDef.getBundle()).extracting(bundle -> bundle.getName(),
            bundle -> bundle.getSymbolicName(), bundle -> bundle.getVendor(), bundle -> bundle.getVersion())
        .containsExactly("KNIME Base Nodes", "org.knime.base", "KNIME AG, Zurich, Switzerland", "4.6.0.v202201041551");
        assertThat(nativeNodeDef.getFeature()).extracting(feature -> feature.getName(),
            feature -> feature.getSymbolicName(), feature -> feature.getVendor(), feature -> feature.getVersion())
        .containsExactly(null, null, null, "0.0.0");
        //TODO assert the creation config value
        assertThat(nativeNodeDef.getNodeCreationConfig().getChildren()).containsKey("Pass through");

        // Assert SingleNodeLoader
        assertThat(singleNodeDef.getFlowStack()).hasSize(2) //
        .hasAtLeastOneElementOfType(DefaultFlowContextDef.class);
        //TODO assert the ConfigMap value
        assertThat(singleNodeDef.getInternalNodeSubSettings().getChildren()).containsKey("memory_policy");
//        assertThat(nativeNodeDef.getModelSettings().getChildren());
        assertThat(singleNodeDef.getVariableSettings()).isNotNull();

        // Assert NodeLoader
        assertThat(nodeDef.getId()).isEqualTo(1);
        assertThat(nodeDef.getAnnotation().getData()).isNull();
        assertThat(nodeDef.getCustomDescription()).isEqualTo("test");
        assertThat(nodeDef.getJobManager().getFactory()).isEqualTo("");
        assertThat(nodeDef.getLocks()) //
            .extracting("m_hasDeleteLock", "m_hasResetLock", "m_hasConfigureLock") //
            .containsExactly(false, false, false);
        assertThat(nodeDef.getUiInfo()).extracting(n -> n.hasAbsoluteCoordinates(), n -> n.isSymbolRelative(),
            n -> n.getBounds().getHeight(), n -> n.getBounds().getLocation(), n -> n.getBounds().getWidth())
            .containsNull();

        assertThat(((WithExceptionsDefaultNativeNodeDef) nativeNodeDef).getLoadExceptions()).isEmpty();
        assertThat(((WithExceptionsDefaultSingleNodeDef) singleNodeDef).getLoadExceptions()).isEmpty();
        assertThat(((WithExceptionsDefaultNodeDef) nodeDef).getLoadExceptions()).isEmpty();
    }

    @Test
    void testGenericLoopStart_withNodeDefException() throws InvalidSettingsException, IOException {
        // given
        var file = NodeLoaderTestUtils.readResourceFolder("Generic_Loop_Start");

        when(m_configBaseRO.getInt("id")).thenReturn(1);
        when(m_configBaseRO.containsKey("customDescription")).thenReturn(true);
        when(m_configBaseRO.getBoolean("absolute_coordinates")).thenThrow(InvalidSettingsException.class);

        // when
        var nativeNodeDef = NativeNodeLoader.load(m_configBaseRO, file, LoadVersion.FUTURE);
        var singleNodeDef = nativeNodeDef.getNode();
        var nodeDef = singleNodeDef.getNode();

        // then
        assertThat(((WithExceptionsDefaultNativeNodeDef) nativeNodeDef).getLoadExceptions()).isEmpty();
        assertThat(((WithExceptionsDefaultSingleNodeDef) singleNodeDef).getLoadExceptions()).isEmpty();
        assertThat(((WithExceptionsDefaultNodeDef) nodeDef).getLoadExceptions().size()).isOne();
    }

}
