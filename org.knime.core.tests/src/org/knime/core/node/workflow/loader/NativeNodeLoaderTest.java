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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.util.NodeLoaderTestUtils;
import org.knime.core.util.LoadVersion;
import org.knime.core.workflow.def.impl.DefaultFlowContextDef;

/**
 *
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 */
class NativeNodeLoaderTest {

    private ConfigBaseRO m_configBaseRO;

    private NativeNodeLoader m_nativeNodeLoader;


    @BeforeEach
    void setUp() {
        m_configBaseRO = mock(ConfigBaseRO.class);
        m_nativeNodeLoader = new NativeNodeLoader();
    }


    @Test
    void testGenericLoopStart() throws InvalidSettingsException, IOException {
        // given
        var file = NodeLoaderTestUtils.readResourceFolder("Generic_Loop_Start");

        when(m_configBaseRO.getInt("id")).thenReturn(1);
        when(m_configBaseRO.containsKey("customDescription")).thenReturn(true);
        when(m_configBaseRO.getString("node_type")).thenReturn("NativeNode");

        // when
        m_nativeNodeLoader.load(m_configBaseRO, file, LoadVersion.FUTURE);
        var nativeNodeDef = m_nativeNodeLoader.getNodeDef();

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
        assertThat(nativeNodeDef.getFlowStack()).hasSize(2) //
        .hasAtLeastOneElementOfType(DefaultFlowContextDef.class);
        //TODO assert the ConfigMap value
        assertThat(nativeNodeDef.getInternalNodeSubSettings().getChildren()).containsKey("memory_policy");
//        assertThat(nativeNodeDef.getModelSettings().getChildren());
        assertThat(nativeNodeDef.getVariableSettings()).isNull();

        // Assert NodeLoader
        assertThat(nativeNodeDef.getId()).isEqualTo(1);
        assertThat(nativeNodeDef.getAnnotation().getData()).isNull();
        assertThat(nativeNodeDef.getCustomDescription()).isEqualTo("test");
        assertThat(nativeNodeDef.getJobManager()).isNull();
        assertThat(nativeNodeDef.getLocks()) //
            .extracting("m_hasDeleteLock", "m_hasResetLock", "m_hasConfigureLock") //
            .containsExactly(false, false, false);
        assertThat(nativeNodeDef.getNodeType()).isEqualTo("NativeNode");
        assertThat(nativeNodeDef.getUiInfo()).extracting(n -> n.hasAbsoluteCoordinates(), n -> n.isSymbolRelative(),
            n -> n.getBounds().getHeight(), n -> n.getBounds().getLocation(), n -> n.getBounds().getWidth())
            .containsOnlyNulls();
    }

}
