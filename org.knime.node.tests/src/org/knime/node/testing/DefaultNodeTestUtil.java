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
 *   Jul 3, 2025 (Paul Bärnreuther): created
 */
package org.knime.node.testing;

import java.util.function.Consumer;
import java.util.function.Function;

import org.knime.core.node.util.CheckUtils;
import org.knime.node.DefaultModel;
import org.knime.node.DefaultModel.RequireModelSettings;
import org.knime.node.DefaultNode;
import org.knime.node.DefaultNode.RequireFullDescription;
import org.knime.node.DefaultNode.RequireIcon;
import org.knime.node.DefaultNode.RequireModel;
import org.knime.node.DefaultNode.RequireName;
import org.knime.node.DefaultNode.RequireShortDescription;
import org.knime.node.DefaultNodeFactory;
import org.knime.node.RequirePorts;
import org.knime.node.RequirePorts.PortsAdder;

/**
 * Testing utility to create {@link DefaultNodeFactory}s for unit tests.
 *
 * @author Paul Bärnreuther
 */
public class DefaultNodeTestUtil {

    private DefaultNodeTestUtil() {
        // Utility
    }

    static final class TestNodeFactory extends DefaultNodeFactory {

        TestNodeFactory(final DefaultNode node) {
            super(node);
        }

    }

    private static final String DEFAULT_NAME = "Test node";

    private static final String DEFAULT_ICON = "./testNodeIcon.svg";

    private static final String DEFAULT_SHORT_DESCRIPTION = "This node is used for testing purposes.";

    private static final String DEFAULT_FULL_DESCRIPTION =
        "This node is used for testing purposes. Use the methods within TestNode to construct such a node.";

    private static final Consumer<PortsAdder> EMPTY_PORTS = p -> {
    };

    private static final Function<RequireModelSettings, DefaultModel> EMPTY_MODEL = m -> m//
        .withoutSettings()//
        .configure((i, o) -> {
        })//
        .execute((i, o) -> {
        });

    /**
     * Use this method to shortcut to a stage in the fluent node api. I.e. fill in the stages before a certain one. Use
     * {@link #complete(Object)} to fill in the missing stages after a certain one.
     *
     * @param stageClass the descired stage to start from
     * @param nodeCreator a function that creates a node from the current stage
     * @param <T> the type of the current stage
     * @return a {@link DefaultNodeFactory} that can be used to create a node
     */
    public static <T> DefaultNodeFactory createNodeFactoryFromStage(final Class<T> stageClass,
        final Function<T, DefaultNode> nodeCreator) {
        return new TestNodeFactory(nodeCreator.apply(createStage(stageClass)));
    }

    @SuppressWarnings({"unchecked", "javadoc"})
    public static <T> T createStage(final Class<T> stageClass) {
        if (RequireName.class.equals(stageClass)) {
            return (T)DefaultNode.create();
        }
        if (RequireIcon.class.equals(stageClass)) {
            return (T)createStage(RequireName.class).name(DEFAULT_NAME);
        }
        if (RequireShortDescription.class.equals(stageClass)) {
            return (T)createStage(RequireIcon.class).icon(DEFAULT_ICON);
        }
        if (RequireFullDescription.class.equals(stageClass)) {
            return (T)createStage(RequireShortDescription.class).shortDescription(DEFAULT_SHORT_DESCRIPTION);
        }
        if (RequirePorts.class.equals(stageClass)) {
            return (T)createStage(RequireFullDescription.class).fullDescription(DEFAULT_FULL_DESCRIPTION);
        }
        if (RequireModel.class.equals(stageClass)) {
            return (T)createStage(RequirePorts.class).ports(EMPTY_PORTS);
        }
        CheckUtils.checkArgument(DefaultNode.class.equals(stageClass), "Unexpected stage: " + stageClass);
        return (T)createStage(RequireModel.class).model(EMPTY_MODEL);
    }

    /**
     * Use this method to fill in the missing stages of a node after the current one.
     *
     * @param <T> the type of the current stage
     * @param stage the current stage of the node
     * @return the completed node
     */
    public static <T> DefaultNodeFactory complete(final T stage) {
        return new TestNodeFactory(completeStage(stage));
    }

    static <T> DefaultNode completeStage(final T stage) {
        if (stage instanceof DefaultNode s) {
            return s;
        }
        if (stage instanceof RequireModel s) {
            return completeStage(s.model(EMPTY_MODEL));
        }
        if (stage instanceof RequirePorts s) {
            return completeStage(s.ports(EMPTY_PORTS));
        }
        if (stage instanceof RequireFullDescription s) {
            return completeStage(s.fullDescription(DEFAULT_FULL_DESCRIPTION));
        }
        if (stage instanceof RequireShortDescription s) {
            return completeStage(s.shortDescription(DEFAULT_SHORT_DESCRIPTION));
        }
        if (stage instanceof RequireIcon s) {
            return completeStage(s.icon(DEFAULT_ICON));
        }
        if (stage instanceof RequireName s) {
            return completeStage(s.name(DEFAULT_NAME));
        }
        throw new IllegalArgumentException("Unexpected stage: " + stage.getClass());
    }

}
