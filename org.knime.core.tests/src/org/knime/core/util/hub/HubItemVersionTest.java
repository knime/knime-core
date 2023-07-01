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
 *   26 Jun 2023 (carlwitt): created
 */
package org.knime.core.util.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

import org.junit.Test;
import org.knime.core.node.workflow.TemplateUpdateUtil.LinkType;

/**
 * Tests for {@link HubItemVersion}.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class HubItemVersionTest {

    /**
     * Test that the utility constructors create the expected objects.
     */
    @Test
    public void utilityConstructors() {
        assertEquals(new HubItemVersion(LinkType.LATEST_VERSION, null), HubItemVersion.latestVersion(),
            "Utility constructor for latest version is faulty.");
        assertEquals(new HubItemVersion(LinkType.LATEST_STATE, null), HubItemVersion.currentState(),
            "Utility constructor for latest state is faulty.");
        assertEquals(new HubItemVersion(LinkType.FIXED_VERSION, 3), HubItemVersion.of(3),
            "Utility constructor for fixed version is faulty.");
    }

    /**
     * Example input:  knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=3&param=4
     * Example output: HubItemVersion(LinkType.FIXED_VERSION, 3)
     */
    @Test
    public void testExtractVersionNumber() {
        // given a URI without a version number
        URI withoutSpaceVersion = URI.create("knime://SomeMountPoint/some/path?someParameter=12");
        // when extracting the version number
        var version = HubItemVersion.of(withoutSpaceVersion).orElse(HubItemVersion.currentState());
        // then the link type is latest state and the version number is null
        assertEquals(HubItemVersion.currentState(), version,
            "currentState should be returned for URIs without version number");

        // given a URI with a version number
        URI withSpaceVersion3 = URI.create("knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=3&param=4");
        // when extracting the version number
        version = HubItemVersion.of(withSpaceVersion3).orElse(HubItemVersion.currentState());
        // then the link type is fixed version and the version number is 3
        assertEquals(new HubItemVersion(LinkType.FIXED_VERSION, 3),
            HubItemVersion.of(withSpaceVersion3).orElseThrow(),
            "version 3 should be returned for input: " + withSpaceVersion3);

        // given a URI with latest version
        URI withVersionLatest =
            URI.create("knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=most-recent&param=4");
        // when extracting the version number
        version = HubItemVersion.of(withVersionLatest).orElse(HubItemVersion.currentState());
        // then the link type is latest version and the version number is null
        assertEquals(new HubItemVersion(LinkType.LATEST_VERSION, null),
            HubItemVersion.of(withVersionLatest).orElseThrow(),
            "latest version should be returned for input: " + withVersionLatest);
    }

    /**
     * Example input:  knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=3&param=4
     * Example output: knime://My-KNIME-Hub/*02j3f023j?someParameter=12&param=4
     */
    @Test
    public void testRemoveVersion() {
        // given a URI with version set
        URI withVersion3 = URI.create("knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=3&param=4");
        // when setting to current state by removing query parameter (set item version to null)
        var withoutVersion = HubItemVersion.currentState().applyTo(withVersion3);
        // then the version is removed
        assertEquals(URI.create("knime://My-KNIME-Hub/*02j3f023j?someParameter=12&param=4"), withoutVersion,
            "version should be removed from URI");

        // given a URI without version set
        URI withoutSpaceVersion = URI.create("knime://SomeMountPoint/some/path?someParameter=12");
        // when setting to latest by removing query parameter (set item version null)
        var unchanged = HubItemVersion.currentState().applyTo(withoutSpaceVersion);
        // then the url is unchanged
        assertEquals(withoutSpaceVersion, unchanged, "URI should be unchanged.");
    }

    /**
     * Example input: knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=3&param=4 Example output:
     * knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=4&param=4
     */
    @Test
    public void testUpdateVersion() {
        // given a URI with version set
        URI withVersion3 = URI.create("knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=3&param=4");
        // when setting a different version
        var withVersion4 = HubItemVersion.of(4).applyTo(withVersion3);
        // then the version is removed
        assertEquals(URI.create("knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=4&param=4"), withVersion4,
            "version should be updated in URI");
    }

    /**
     * Example input:  knime://SomeMountPoint/some/path
     * Example output: knime://SomeMountPoint/some/path?version=4
     */
    @Test
    public void testAddVersion() {
        // given a URI without version set
        URI withoutVersion = URI.create("knime://SomeMountPoint/some/path");
        // when setting a version
        var withVersion4 = HubItemVersion.of(4).applyTo(withoutVersion);
        // then the version is added
        assertEquals(URI.create("knime://SomeMountPoint/some/path?version=4"), withVersion4,
            "version should be added to URI");
    }

    /**
     * Example input:  knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=3&param=4
     * Example output: knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=most-recent&param=4
     */
    @Test
    public void testSetLatestVersion() {
        // given a URI with version set
        URI withVersion3 = URI.create("knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=3&param=4");
        // when setting to latest version
        var withVersionLatest = HubItemVersion.latestVersion().applyTo(withVersion3);
        // then the version is set to most-recent
        assertEquals(URI.create("knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=most-recent&param=4"),
            withVersionLatest, "version should be set to most-recent in URI");
    }

    /**
     * Example input:  knime://My-KNIME-Hub/*02j3f023j?someParameter=12&spaceVersion=3&param=4
     * Example output: knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=3&param=4
     */
    @Test
    public void testMigrateVersion() {
        // given a URI with a space version set and some other parameters
        URI withSpaceVersion3 = URI.create("knime://My-KNIME-Hub/*02j3f023j?someParameter=12&spaceVersion=3&param=4");
        // when calling migrate version
        var withVersion3 = HubItemVersion.migrateFromSpaceVersion(withSpaceVersion3);
        // then the version is set to version=3
        assertEquals(URI.create("knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=3&param=4"), withVersion3,
            "version should be migrated to version=3 in URI");
    }

    /**
     * Test that null inputs are handled correctly. They are quite common.
     */
    @Test
    public void testNullInputs() {
        // given null
        URI nonExistent = null;

        // when migrating
        var migrated = HubItemVersion.migrateFromSpaceVersion(nonExistent);
        // we get null
        assertNull(migrated, "Null URI should be untouched by migration.");

        // when applying, we expect an illegal argument exception
        final var version = HubItemVersion.of(1);
        assertThrows(IllegalArgumentException.class, () -> version.applyTo(nonExistent), "Null URI should not be applicable.");

        // when parsing, we expect an illegal argument exception
        assertThrows(IllegalArgumentException.class, () -> HubItemVersion.of(nonExistent), "Null URI should not be parsable.");
    }

}
