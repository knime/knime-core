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
 *   20 Sept 2023 (carlwitt): created
 */
package org.knime.core.util.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.SimpleConfig;

/**
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
class HubItemVersionPersistorTest {

    @Test
    void testSaveLoad() throws InvalidSettingsException {
        // given test versions
        var versions = List.of(HubItemVersion.of(5), HubItemVersion.currentState(), HubItemVersion.latestVersion());

        // when writing to node settings
        var settings =
            List.of(new SimpleConfig("fixed"), new SimpleConfig("current state"), new SimpleConfig("latest version"));
        for (int i = 0; i < versions.size(); i++) {
            var config = settings.get(i);
            HubItemVersionPersistor.save(versions.get(i), config);
        }

        // then loaded settings should be equal to the test versions
        for (int i = 0; i < versions.size(); i++) {
            var config = settings.get(i);
            var loadedVersion = HubItemVersionPersistor.load(config).get();
            assertEquals(versions.get(i), loadedVersion, "loaded version should be equal to the test version");
        }
    }

    @Test
    void loadSettingsWithoutVersion() throws InvalidSettingsException {
        // given a node settings without a version
        var config = new SimpleConfig("no version");

        // when loading the version
        var loadedVersion = HubItemVersionPersistor.load(config);

        // then the version should be empty
        assertEquals(Optional.empty(), loadedVersion);
    }

    @Test
    void loadInvalidVersion() {
        // given an invalid version
        var config = new SimpleConfig("invalid version");
        config.addString(HubItemVersionPersistor.CONFIG_KEY, "invalid version");

        // when loading the version
        // then expect an invalid settings exception
        var exception = assertThrows(InvalidSettingsException.class, () -> HubItemVersionPersistor.load(config));
        assertEquals("Invalid hub item version invalid version. Must be LATEST_STATE or LATEST_VERSION or an integer "
            + "version number > 0.", exception.getMessage());
    }

}
