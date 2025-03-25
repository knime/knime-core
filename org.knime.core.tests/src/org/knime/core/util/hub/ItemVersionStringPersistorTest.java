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
 *   20 Mar 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.util.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import javax.swing.event.ChangeEvent;

import org.junit.jupiter.api.Test;
import org.knime.core.node.EmptyNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.SimpleConfig;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.mockito.Mockito;

/**
 * Tests for the {@link ItemVersionStringPersistor}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class ItemVersionStringPersistorTest {

    @SuppressWarnings("static-method")
    @Test
    void testSaveLoad() throws InvalidSettingsException {
        final var versions = List.of(ItemVersion.of(5), ItemVersion.currentState(), ItemVersion.mostRecent());
        final var settings =
            List.of(new SimpleConfig("specific"), new SimpleConfig("current state"), new SimpleConfig("most recent"));
        for (var i = 0; i < versions.size(); i++) {
            final var config = settings.get(i);
            ItemVersionStringPersistor.save(versions.get(i), config);
        }
        for (var i = 0; i < versions.size(); i++) {
            final var config = settings.get(i);
            final var loaded = ItemVersionStringPersistor.load(config).get();
            assertEquals(versions.get(i), loaded, "Loaded version should be equal to the test version");
        }
    }

    @SuppressWarnings("static-method")
    @Test
    void loadSettingsWithoutVersion() throws InvalidSettingsException {
        final var config = new SimpleConfig("no version");
        final var loadedVersion = ItemVersionStringPersistor.load(config);
        assertEquals(Optional.empty(), loadedVersion);
    }

    @SuppressWarnings("static-method")
    @Test
    void loadInvalidVersion() {
        final var config = new SimpleConfig("invalid version");
        config.addString(ItemVersionStringPersistor.CONFIG_KEY, "BOGUS_VERSION");
        final var exception =
            assertThrows(InvalidSettingsException.class, () -> ItemVersionStringPersistor.load(config));
        assertEquals(
            "Invalid Hub item version \"BOGUS_VERSION\". "
                + "Valid values are \"LATEST_STATE\", \"LATEST_VERSION\", or a non-negative integer value.",
            exception.getMessage());
    }

    /**
     * Tests that the "version" flow variable is of type string, because in addition to numeric identifiers we have
     * "current-state" and "most-recent" (ensures backwards-compatibility).
     */
    @SuppressWarnings("static-method")
    @Test
    void testFlowVariableModelHandling() {
        final var pane = new EmptyNodeDialogPane();
        final var model = ItemVersionStringPersistor.createFlowVariableModel(pane);
        assertEquals(VariableType.StringType.INSTANCE, model.getVariableType(),
            "Expected version variable to be string");
        assertEquals(ItemVersionStringPersistor.CONFIG_KEY, model.getKeys()[0],
            "Expected version variable to be under the correct config key");
    }

    @SuppressWarnings("static-method")
    @Test
    void testFlowVariableChangeEventHandling() throws InvalidSettingsException {
        // given a flow variable model that returns a specific version
        final var pane = new EmptyNodeDialogPane();
        final var model = ItemVersionStringPersistor.createFlowVariableModel(pane);
        final var spy = Mockito.spy(model);
        when(spy.getVariableValue()).thenReturn(Optional.of(new FlowVariable("itemVersion", "42")));
        when(spy.isVariableReplacementEnabled()).thenReturn(true);

        // we obtain the version from the change event
        final var expected = ItemVersion.of(42);
        final var evt = new ChangeEvent(spy);
        final var itemVersion = ItemVersionStringPersistor.fromFlowVariableChangeEvent(evt);
        assertTrue(itemVersion.isPresent(), "Expected an item version");
        assertEquals(expected, itemVersion.get(), "Expected the item version to be the same as the test version");
    }
}
