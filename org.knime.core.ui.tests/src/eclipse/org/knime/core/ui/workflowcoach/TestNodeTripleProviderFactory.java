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
 *   Oct 26, 2022 (hornm): created
 */
package org.knime.core.ui.workflowcoach;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.knime.core.node.NodeInfo;
import org.knime.core.node.NodeTriple;
import org.knime.core.ui.workflowcoach.data.NodeTripleProvider;
import org.knime.core.ui.workflowcoach.data.NodeTripleProviderFactory;
import org.knime.core.ui.workflowcoach.data.UpdatableNodeTripleProvider;

/**
 * A test node triple provider used in {@link NodeRecommendationManagerTest}. Please note: This must be registered as an
 * extension point {@code org.knime.core.ui.nodetriples} of this fragment using the {@code fragment.xml}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Kai Franze, KNIME GmbH
 */
public class TestNodeTripleProviderFactory implements NodeTripleProviderFactory {

    @Override
    public List<NodeTripleProvider> createProviders() {
        return List.of(//
            new TestNodeTripleProvider(), //
            new TestNodeTripleProvider2(), //
            new TestUpdatableNodeTripleProvider(), //
            new TestUpdatableNodeTripleProvider2());
    }

    @Override
    public String getPreferencePageID() {
        return "";
    }

    private static final class TestNodeTripleProvider implements NodeTripleProvider {

        @Override
        public String getName() {
            return "Test node triple provider";
        }

        @Override
        public String getDescription() {
            return "Test node triple provider description";
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public Stream<NodeTriple> getNodeTriples() throws IOException {
            var portObjectIn = new NodeInfo(//
                "org.knime.core.node.exec.dataexchange.in.PortObjectInNodeFactory", //
                "PortObject Reference Reader");
            var rowFilter = new NodeInfo(//
                "test_org.knime.base.node.preproc.filter.row.RowFilterNodeFactory", // Added "test_" to avoid side effects
                "Test Row Filter");
            var rowSplitter = new NodeInfo(//
                "test_org.knime.base.node.preproc.filter.row2.RowSplitterNodeFactory", // Added "test_" to avoid side effects
                "Test Row Splitter");
            var nonExisting = new NodeInfo(//
                "non.existing.factory", //
                "Non-Existing Node");
            return Stream.of(//
                new NodeTriple(null, portObjectIn, rowFilter), //
                new NodeTriple(null, portObjectIn, rowSplitter), //
                new NodeTriple(rowSplitter, rowFilter, rowSplitter), //
                new NodeTriple(null, null, rowFilter), //
                new NodeTriple(null, null, portObjectIn), //
                new NodeTriple(rowSplitter, null, rowFilter), //
                new NodeTriple(rowSplitter, null, portObjectIn), //
                new NodeTriple(null, rowFilter, rowSplitter), //
                new NodeTriple(rowFilter, nonExisting, rowFilter)//
            );
        }

        @Override
        public Optional<LocalDateTime> getLastUpdate() {
            return Optional.empty();
        }
    }

    private static final class TestNodeTripleProvider2 implements NodeTripleProvider {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return "Test node triple provider 2";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDescription() {
            return "Test node triple provider 2 description";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public Stream<NodeTriple> getNodeTriples() throws IOException {
            var portObjectIn = new NodeInfo(//
                "org.knime.core.node.exec.dataexchange.in.PortObjectInNodeFactory", //
                "PortObject Reference Reader");
            var rowFilter = new NodeInfo(//
                "test_org.knime.base.node.preproc.filter.row.RowFilterNodeFactory", // Added "test_" to avoid side effects
                "Test Row Filter");
            return Stream.of(//
                new NodeTriple(null, portObjectIn, rowFilter), //
                new NodeTriple(null, null, rowFilter), //
                new NodeTriple(null, null, portObjectIn));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<LocalDateTime> getLastUpdate() {
            return Optional.empty();
        }

    }

    private static final class TestUpdatableNodeTripleProvider implements UpdatableNodeTripleProvider {

        private static boolean updateRequired = true;

        @Override
        public String getName() {
            return "Test updateble node triple provider";
        }

        @Override
        public String getDescription() {
            return "Test updateble node triple provider description";
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public Stream<NodeTriple> getNodeTriples() throws IOException {
            return Stream.empty();
        }

        @Override
        public Optional<LocalDateTime> getLastUpdate() {
            return Optional.empty();
        }

        @Override
        public void update() throws Exception {
            updateRequired = false; // Property `updateRequired` must be static to persist
        }

        @Override
        public boolean updateRequired() {
            return updateRequired;
        }

    }

    private static final class TestUpdatableNodeTripleProvider2 implements UpdatableNodeTripleProvider {

        @Override
        public String getName() {
            return "Test updateble node triple provider 2";
        }

        @Override
        public String getDescription() {
            return "Test updateble node triple provider description 2";
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public Stream<NodeTriple> getNodeTriples() throws IOException {
            return Stream.empty();
        }

        @Override
        public Optional<LocalDateTime> getLastUpdate() {
            return Optional.empty();
        }

        @Override
        public void update() throws Exception {
            // Do nothing
        }

        @Override
        public boolean updateRequired() {
            return false;
        }

    }

}
