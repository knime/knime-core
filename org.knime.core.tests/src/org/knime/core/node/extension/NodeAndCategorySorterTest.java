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
 *   Mar 26, 2021 (hornm): created
 */
package org.knime.core.node.extension;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;
import org.knime.core.node.extension.NodeAndCategorySorter.NodeOrCategory;

/**
 * Tests {@link NodeAndCategorySorter}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings({"javadoc", "java:S1192"})
public class NodeAndCategorySorterTest {

    @Test
    public void testCategoriesBeforeNodes() {
        Cat c1 = new Cat("c1", "", "");
        Cat c2 = new Cat("c2", "", "");
        Node n1 = new Node("n1", "", "");
        Node n2 = new Node("n2", "", "");

        List<AbstractNodeOrCategory> sorted = NodeAndCategorySorter.sortNodesAndCategories(asList(n2, n1, c2, c1));
        assertThat("unexpected order", sorted, is(asList(c1, c2, n1, n2)));
    }

    @Test
    public void testAfterIds() {
        Cat c1 = new Cat("c1", "c2", "");
        Cat c2 = new Cat("c2", "", "");
        Cat c3 = new Cat("c3", "c2", "");
        Cat c4 = new Cat("c4", "c1", "");
        Node n1 = new Node("n1", "n2", "");
        Node n2 = new Node("n2", "n3", "");
        Node n3 = new Node("n3", "", "");

        List<AbstractNodeOrCategory> sorted =
            NodeAndCategorySorter.sortNodesAndCategories(asList(c1, c2, c3, c4, n1, n2, n3));
        assertThat("unexpected order", sorted, is(asList(c2, c1, c4, c3, n3, n2, n1)));

    }

    @Test
    public void testKNIMEBefore3rdParty() {
        Cat c1 = new Cat("c1", "", "other1");
        Cat c2 = new Cat("c2", "", "other2");
        Cat c3 = new Cat("c3", "", "jp.co.infocom.cheminfo.marvin");
        Cat c4 = new Cat("c4", "", "org.knime.foo");
        Cat c5 = new Cat("c5", "", "com.knime.bar");
        Node n1 = new Node("n1", "", "other1");
        Node n2 = new Node("n2", "", "other2");
        Node n3 = new Node("n3", "", "jp.co.infocom.cheminfo.marvin");
        Node n4 = new Node("n4", "", "org.knime.foo");
        Node n5 = new Node("n5", "", "com.knime.bar");

        List<AbstractNodeOrCategory> sorted =
            NodeAndCategorySorter.sortNodesAndCategories(asList(c1, c2, c3, c4, c5, n1, n2, n3, n4, n5));
        assertThat("unexpected order", sorted, is(asList(c3, c4, c5, c1, c2, n3, n4, n5, n1, n2)));

    }

    @Test
    public void testAfterIdCycle() {
        Node n1 = new Node("n1", "n2", "");
        Node n2 = new Node("n2", "n3", "");
        Node n3 = new Node("n3", "n1", "");

        String logMsg =
            trackLastErrorLogMessage(() -> NodeAndCategorySorter.sortNodesAndCategories(asList(n1, n2, n3)));
        assertThat("unexpected log message", logMsg, containsString("A cycle in after-relationships"));
    }

    @Test
    public void testLastId() {
        Cat c1 = new Cat("c1", "_last_", "");
        Cat c2 = new Cat("c2", "", "");
        Cat c3 = new Cat("c3", "", "");
        Node n1 = new Node("n1", "_last_", "");
        Node n2 = new Node("n2", "", "");
        Node n3 = new Node("n3", "", "");

        List<AbstractNodeOrCategory> sorted =
            NodeAndCategorySorter.sortNodesAndCategories(asList(c1, c2, c3, n1, n2, n3));
        assertThat("unexpected order", sorted, is(asList(c2, c3, c1, n2, n3, n1)));

        c1 = new Cat("c1", null, "");
        c2 = new Cat("c2", "", "");
        c3 = new Cat("c3", "", "");
        n1 = new Node("n1", null, "");
        n2 = new Node("n2", "", "");
        n3 = new Node("n3", "", "");

        sorted = NodeAndCategorySorter.sortNodesAndCategories(asList(c1, c2, c3, n1, n2, n3));
        assertThat("unexpected order", sorted, is(asList(c2, c3, c1, n2, n3, n1)));

    }

    @Test
    public void testDuplicateIds() {
        Cat c1 = new Cat("c1", "", "");
        Cat c2 = new Cat("c1", "", "");

        String logMsg = trackLastErrorLogMessage(() -> NodeAndCategorySorter.sortNodesAndCategories(asList(c1, c2)));
        assertThat("unexpected log message", logMsg, containsString("Duplicate repository entry IDs detected"));
    }

    private static class Node extends AbstractNodeOrCategory {

        public Node(final String idAndName, final String afterId, final String contributingPlugin) {
            super(idAndName, afterId, contributingPlugin);
        }

        @Override
        public boolean isNode() {
            return true;
        }

    }

    private static class Cat extends AbstractNodeOrCategory {

        public Cat(final String idAndName, final String afterId, final String contributingPlugin) {
            super(idAndName, afterId, contributingPlugin);
        }

        @Override
        public boolean isNode() {
            return false;
        }

    }

    private abstract static class AbstractNodeOrCategory implements NodeOrCategory<AbstractNodeOrCategory> {

        private String m_idAndName;

        private String m_afterId;

        private String m_contributingPlugin;

        public AbstractNodeOrCategory(final String idAndName, final String afterId, final String contributingPlugin) {
            m_idAndName = idAndName;
            m_afterId = afterId;
            m_contributingPlugin = contributingPlugin;
        }

        @Override
        public int compareTo(final AbstractNodeOrCategory o) {
            int nameOrder = getName().compareTo(o.getName());
            return (nameOrder == 0 ? getID().compareTo(o.getID()) : nameOrder);
        }

        @Override
        public String getID() {
            return m_idAndName;
        }

        @Override
        public String getName() { // NOSONAR
            return m_idAndName;
        }

        @Override
        public String getContributingPlugin() {
            return m_contributingPlugin;
        }

        @Override
        public String getAfterID() {
            return m_afterId;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
        }

    }

    private static String trackLastErrorLogMessage(final Runnable run) {
        AtomicReference<String> logMsg = new AtomicReference<>();
        AtomicReference<Level> level = new AtomicReference<>();
        AppenderSkeleton logAppender = new AppenderSkeleton() {

            @Override
            public boolean requiresLayout() {
                return false;
            }

            @Override
            public void close() {
                //
            }

            @Override
            protected void append(final LoggingEvent e) {
                logMsg.set(e.getMessage().toString());
                level.set(e.getLevel());
            }
        };

        Logger.getRootLogger().addAppender(logAppender);
        try {
            run.run();
        } finally {
            Logger.getRootLogger().removeAppender(logAppender);
        }
        assertThat("unexpected log level", level.get(), is(Level.ERROR));
        return logMsg.get();
    }

}
