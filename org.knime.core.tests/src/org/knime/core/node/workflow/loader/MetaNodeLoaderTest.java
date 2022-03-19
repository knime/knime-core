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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.util.NodeLoaderTestUtils;
import org.knime.core.util.LoadVersion;
import org.knime.core.workflow.def.JobManagerDef;
import org.knime.core.workflow.def.NodeAnnotationDef;
import org.knime.core.workflow.def.NodeUIInfoDef;
import org.knime.core.workflow.def.WorkflowDef;
import org.knime.core.workflow.def.impl.FallibleBaseNodeDef;

/**
 *
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 */
class MetaNodeLoaderTest {

    private ConfigBaseRO m_configBaseRO;

    @BeforeEach
    void setUp() {
        m_configBaseRO = mock(ConfigBaseRO.class);
    }

    @Test
    void simpleMetaNodetLoaderTest() throws InvalidSettingsException, IOException {
        // given
        var file = NodeLoaderTestUtils.readResourceFolder("Simple_Metanode");

        when(m_configBaseRO.getInt("id")).thenReturn(1);
        when(m_configBaseRO.containsKey("customDescription")).thenReturn(true);

        // when
        var metanodeDef = MetaNodeLoader.load(m_configBaseRO, file, LoadVersion.FUTURE);
        var nodeDef = (FallibleBaseNodeDef)metanodeDef.getBaseNode();

        // then

        // Assert MetaNodeLoader
        assertThat(metanodeDef.getInPorts()).isEmpty();
        assertThat(metanodeDef.getOutPorts()).isEmpty();
        assertThat(metanodeDef.getInPortsBarUIInfo()).isNotNull();
        assertThat(metanodeDef.getOutPortsBarUIInfo()).isNotNull();
        assertThat(metanodeDef.getLink()).isNull();
        assertThat(metanodeDef.getWorkflow()).isNotNull();

        //TODO Shall we pass it to the workflow test or something similar?
        var workflow = metanodeDef.getWorkflow();

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

        assertThat(metanodeDef.getSupplierExceptions()).isEmpty();
        assertThat(nodeDef.getSupplierExceptions()).isEmpty();
    }

    @Test
    void multiportMetaNodetLoaderTest() throws IOException, InvalidSettingsException {
        // given
        var file = NodeLoaderTestUtils.readResourceFolder("MultiPort_Metanode");

        when(m_configBaseRO.getInt("id")).thenReturn(431);
        when(m_configBaseRO.containsKey("customDescription")).thenReturn(false);
        when(m_configBaseRO.containsKey("annotations")).thenReturn(false);
        when(m_configBaseRO.getIntArray("extrainfo.node.bounds")) //
            .thenReturn(new int[]{2541, 1117, 122, 65});

        // when
        var metanodeDef = MetaNodeLoader.load(m_configBaseRO, file, LoadVersion.FUTURE);
        var nodeDef = (FallibleBaseNodeDef)metanodeDef.getBaseNode();

        // then

        // Assert MetaNodeLoader
        assertThat(metanodeDef.getInPorts())
            .extracting(p -> p.getIndex(), p -> p.getName(),
                p -> p.getPortType().getPortObjectClass().endsWith("BufferedDataTable"))
            .contains(tuple(0, "Inport 0", true));
        assertThat(metanodeDef.getOutPorts())
            .extracting(p -> p.getIndex(), p -> p.getName(),
                p -> p.getPortType().getPortObjectClass().endsWith("BufferedDataTable"))
            .contains(tuple(0, "Connected to: Concatenated table", true),
                tuple(1, "Connected to: Concatenated table", true));
        assertThat(metanodeDef.getInPortsBarUIInfo()).isInstanceOf(NodeUIInfoDef.class);
        assertThat(metanodeDef.getOutPortsBarUIInfo()).isInstanceOf(NodeUIInfoDef.class);
        assertThat(metanodeDef.getLink()).isNull();
        assertThat(metanodeDef.getWorkflow()).isInstanceOf(WorkflowDef.class);

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
        try {
            //TODO Cant cast to WithExceptionsDefaultNodeDef
            var exceptions = nodeDef.getSupplierExceptions();
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertThat(metanodeDef.getSupplierExceptions()).isEmpty();
    }
}
