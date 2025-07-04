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
 *   Jul 2, 2025 (Paul Bärnreuther): created
 */
package org.knime.node.description;

import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.node.testing.DefaultNodeTestUtil.complete;
import static org.knime.node.testing.DefaultNodeTestUtil.createStage;

import org.junit.jupiter.api.Test;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.node.DefaultNode;
import org.knime.node.DefaultNode.RequireFullDescription;
import org.knime.node.DefaultNode.RequireIcon;
import org.knime.node.DefaultNode.RequireModel;
import org.knime.node.DefaultNode.RequireName;
import org.knime.node.DefaultNode.RequireShortDescription;
import org.knime.node.testing.TestWithWorkflowManager;

class DefaultNodeDescriptionTest extends TestWithWorkflowManager {

    @Test
    void testName() {
        final var myName = "My name";
        final var nc = addNode(complete(createStage(RequireName.class)//
            .name(myName)//
        ));

        final var description = nc.getNode().invokeGetNodeDescription();

        assertThat(description.getNodeName()).isEqualTo(myName);
    }

    @Test
    void testIcon() {
        final var myIcon = "./myIcon.png";
        final var nc = addNode(complete(createStage(RequireIcon.class)//
            .icon(myIcon)//
        ));

        final var description = nc.getNode().invokeGetNodeDescription();

        assertThat(description.getIconPath()).isEqualTo(myIcon);
    }

    @Test
    void testShortDescription() {
        final var myShortDescription = "my short description";
        final var nc = addNode(complete(createStage(RequireShortDescription.class)//
            .shortDescription(myShortDescription)//
        ));

        final var description = nc.getNode().invokeGetNodeDescription();

        assertThat(description.getShortDescription().orElse(null)).isEqualTo(myShortDescription);
    }

    @Test
    void testFullDescription() {
        final var myFullDescription = "my full description";
        final var nc = addNode(complete(createStage(RequireFullDescription.class)//
            .fullDescription(myFullDescription)//
        ));

        final var description = nc.getNode().invokeGetNodeDescription();

        assertThat(description.getIntro()).contains(myFullDescription);
    }

    @Test
    void testSinceVersion() {
        final var nc = addNode(complete(createStage(DefaultNode.class)//
            .sinceVersion(1, 2, 3) //
        ));

        final var description = nc.getNode().invokeGetNodeDescription();

        final var version = description.getSinceVersion().orElse(null);
        assertThat(version.getMajor()).isEqualTo(1);
        assertThat(version.getMinor()).isEqualTo(2);
        assertThat(version.getRevision()).isEqualTo(3);
    }

    @Test
    void testExternalResource() {
        final var nc = addNode(complete(createStage(DefaultNode.class)//
            .addExternalResource("www.google.de", "Google") //
            .addExternalResource("www.knime.com", "KNIME") //
        ));

        final var description = nc.getNode().invokeGetNodeDescription();

        assertThat(description.getLinks().get(1).getText()).isEqualTo("KNIME");
    }

    @Test
    void testKeywords() {
        final var nc = addNode(complete(createStage(DefaultNode.class)//
            .keywords("my", "key", "words") //
        ));

        final var description = nc.getNode().invokeGetNodeDescription();

        assertThat(description.getKeywords()).contains("my", "key", "words");
    }

    @Test
    void testViewDescription() {
        final var myViewDescription = "My view";
        final var nc = addNode(complete(createStage(DefaultNode.class)//
            .addView(v -> v//
                .withoutSettings() //
                .description(myViewDescription) //
                .page(p -> p.fromString(() -> "foo").relativePath("bar.html")) //
            )));

        final var description = nc.getNode().invokeGetNodeDescription();

        assertThat(description.getViewDescription(0)).isEqualTo(myViewDescription);
    }

    @SuppressWarnings("restriction")
    static final class MySettings implements DefaultNodeSettings {

        static final String MY_TITLE = "My title";

        static final String MY_DESCRIPTION = "My description";

        @Widget(title = MY_TITLE, description = MY_DESCRIPTION)
        String m_setting;

    }

    @Test
    void testViewSettings() {
        final var nc = addNode(complete(createStage(DefaultNode.class)//
            .addView(v -> v//
                .settingsClass(MySettings.class) //
                .description("A view") //
                .page(p -> p.fromString(() -> "foo").relativePath("bar.html")) //
            )));

        final var description = nc.getNode().invokeGetNodeDescription();

        assertThat(description.getDialogOptionGroups().get(0).getOptions()).hasSize(1);
    }

    @Test
    void testModelSettings() {
        final var nc = addNode(complete(createStage(RequireModel.class)//
            .model(m -> m//
                .settingsClass(MySettings.class) //
                .configure((i, o) -> {
                    // No configure
                }) //
                .execute((i, o) -> {
                    // No execute
                }) //
            )));

        final var description = nc.getNode().invokeGetNodeDescription();

        assertThat(description.getDialogOptionGroups().get(0).getOptions()).hasSize(1);
    }

    @Test
    void testModelAndViewSettings() {
        final var nc = addNode(complete(createStage(RequireModel.class)//
            .model(m -> m//
                .settingsClass(MySettings.class) //
                .configure((i, o) -> {
                    // No configure
                }) //
                .execute((i, o) -> {
                    // No execute
                }) //
            ).addView(v -> v//
                .settingsClass(MySettings.class) //
                .description("A view") //
                .page(p -> p.fromString(() -> "foo").relativePath("bar.html")) //
            )));

        final var description = nc.getNode().invokeGetNodeDescription();

        assertThat(description.getDialogOptionGroups().get(0).getOptions()).hasSize(2);
    }

    @Test
    void testNoSettings() {
        final var nc = addNode(complete(createStage(DefaultNode.class)));

        final var description = nc.getNode().invokeGetNodeDescription();

        assertThat(description.getDialogOptionGroups().get(0).getOptions()).isEmpty();
    }

    @Test
    void testInputPortDescriptions() {
        final var myPortName = "My Input Port";
        final var myPortDescription = "This is my input port";

        final var nc =
            addNodeWithPorts(p -> p.addInputPort(myPortName, myPortDescription, FlowVariablePortObject.TYPE));

        final var description = nc.getNode().invokeGetNodeDescription();

        assertThat(description.getInportName(0)).isEqualTo(myPortName);
        assertThat(description.getInportDescription(0)).isEqualTo(myPortDescription);
    }

    @Test
    void testConfigurableInputPortDescriptions() {
        final var myPortName = "My Configurable Input";
        final var myPortDescription = "My configurable input!";
        final var myPortIdentifier = "myConfigurableInput";
        final var nc = addNodeWithDynamicPorts(d -> d.addInputPortGroup(myPortIdentifier, g -> g.name(myPortName)
            .description(myPortDescription).extendable().supportedTypes(FlowVariablePortObject.TYPE)));

        final var description = nc.getNode().invokeGetNodeDescription();

        assertThat(description.getDynamicInPortGroups()).hasSize(1);
        assertThat(description.getDynamicInPortGroups().get(0).getGroupIdentifier()).isEqualTo(myPortIdentifier);
        assertThat(description.getDynamicInPortGroups().get(0).getGroupName()).isEqualTo(myPortName);
        assertThat(description.getDynamicInPortGroups().get(0).getGroupDescription()).isEqualTo(myPortDescription);
    }

    @Test
    void testOutputPortDescriptions() {
        final var myPortName = "My Output Port";
        final var myPortDescription = "This is my output port";

        final var nc =
            addNodeWithPorts(p -> p.addOutputPort(myPortName, myPortDescription, FlowVariablePortObject.TYPE));

        final var description = nc.getNode().invokeGetNodeDescription();

        assertThat(description.getOutportName(0)).isEqualTo(myPortName);
        assertThat(description.getOutportDescription(0)).isEqualTo(myPortDescription);

    }

    @Test
    void testConfigurableOutputPortDescriptions() {
        final var myPortName = "My Configurable Output";
        final var myPortDescription = "My configurable output!";
        final var myPortIdentifier = "myConfigurableOutput";
        final var nc = addNodeWithDynamicPorts(d -> d.addOutputPortGroup(myPortIdentifier, g -> g.name(myPortName)
            .description(myPortDescription).extendable().supportedTypes(FlowVariablePortObject.TYPE)));

        final var description = nc.getNode().invokeGetNodeDescription();

        assertThat(description.getDynamicOutPortGroups()).hasSize(1);
        assertThat(description.getDynamicOutPortGroups().get(0).getGroupIdentifier()).isEqualTo(myPortIdentifier);
        assertThat(description.getDynamicOutPortGroups().get(0).getGroupName()).isEqualTo(myPortName);
        assertThat(description.getDynamicOutPortGroups().get(0).getGroupDescription()).isEqualTo(myPortDescription);
    }

    @Test
    void testFixedInputAndOutputPortDescriptions() {
        final var myInputPortName = "My Input Port";
        final var myInputPortDescription = "This is my input port";
        final var myOutputPortName = "My Output Port";
        final var myOutputPortDescription = "This is my output port";
        final var myGroupIdentifier = "myPortGroupIdentifier";

        final var nc = addNodeWithDynamicPorts(d -> d.addInputAndOutputPortGroup(myGroupIdentifier, g -> g //
            .inputName(myInputPortName) //
            .inputDescription(myInputPortDescription) //
            .outputName(myOutputPortName) //
            .outputDescription(myOutputPortDescription) //
            .fixed(BufferedDataTable.TYPE)));

        final var description = nc.getNode().invokeGetNodeDescription();

        assertThat(description.getInportName(0)).isEqualTo(myInputPortName);
        assertThat(description.getInportDescription(0)).isEqualTo(myInputPortDescription);
        assertThat(description.getOutportName(0)).isEqualTo(myOutputPortName);
        assertThat(description.getOutportDescription(0)).isEqualTo(myOutputPortDescription);
    }

    @Test
    void testConfigurableInputAndOutputPortDescriptions() {
        final var myInputPortName = "My Input Port";
        final var myInputPortDescription = "This is my input port";
        final var myOutputPortName = "My Output Port";
        final var myOutputPortDescription = "This is my output port";
        final var myGroupIdentifier = "myPortGroupIdentifier";

        final var nc = addNodeWithDynamicPorts(d -> d.addInputAndOutputPortGroup(myGroupIdentifier, g -> g //
            .inputName(myInputPortName) //
            .inputDescription(myInputPortDescription) //
            .outputName(myOutputPortName) //
            .outputDescription(myOutputPortDescription) //
            .optional()//
            .supportedTypes(BufferedDataTable.TYPE)//
            .defaultPortType(BufferedDataTable.TYPE)));

        final var description = nc.getNode().invokeGetNodeDescription();

        assertThat(description.getDynamicInPortGroups()).hasSize(1);
        assertThat(description.getDynamicInPortGroups().get(0).getGroupIdentifier()).isEqualTo(myGroupIdentifier);
        assertThat(description.getDynamicInPortGroups().get(0).getGroupName()).isEqualTo(myInputPortName);
        assertThat(description.getDynamicInPortGroups().get(0).getGroupDescription()).isEqualTo(myInputPortDescription);

        assertThat(description.getDynamicOutPortGroups()).hasSize(1);
        assertThat(description.getDynamicOutPortGroups().get(0).getGroupIdentifier()).isEqualTo(myGroupIdentifier);
        assertThat(description.getDynamicOutPortGroups().get(0).getGroupName()).isEqualTo(myOutputPortName);
        assertThat(description.getDynamicOutPortGroups().get(0).getGroupDescription())
            .isEqualTo(myOutputPortDescription);
    }
}
