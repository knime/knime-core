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
 *   11 Mar 2022 (Dionysios Stolis): created
 */
package org.knime.core.node.workflow.loader;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.NodeLoaderTestUtils;
import org.knime.core.util.LoadVersion;
import org.knime.core.workflow.def.AuthorInformationDef;
import org.knime.core.workflow.def.WorkflowUISettingsDef;
import org.knime.core.workflow.def.impl.FallibleWorkflowDef;

/**
 *
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 */
class WorkflowLoaderTest {

    @Test
    void simpleMetaNodetLoaderTest() throws InvalidSettingsException, IOException {
        // given
        var file = NodeLoaderTestUtils.readResourceFolder("Workflow_Test");
        // when
        var workflowDef = (FallibleWorkflowDef)WorkflowLoader.load(file, LoadVersion.FUTURE);

        // then
        assertThat(workflowDef.getAnnotations()).hasSize(3).extracting(a -> !a.getText().isEmpty()).containsOnly(true);
        assertThat(workflowDef.getAuthorInformation()).isInstanceOf(AuthorInformationDef.class);
        //        assertThat(workflowDef.getCipher()).isNull();
        assertThat(workflowDef.getConnections()).hasSize(6).extracting(c -> c.getDestID() != null
            && c.getDestPort() != null && c.getSourceID() != null && c.getSourcePort() != null).containsOnly(true);
        assertThat(workflowDef.getName()).isNull();
        assertThat(workflowDef.getNodes()).hasSize(7).containsKeys("node_14", "node_13", "node_12", "node_7", "node_11",
            "node_8", "node_10");
        assertThat(workflowDef.getWorkflowEditorSettings()).isInstanceOf(WorkflowUISettingsDef.class);

        assertThat(workflowDef.hasExceptions()).isFalse();
    }
}
