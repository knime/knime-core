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
 *   18 Mar 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.util.urlresolve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.net.WWWFormCodec;
import org.junit.jupiter.api.Test;
import org.knime.core.node.workflow.TemplateUpdateUtil.LinkType;
import org.knime.core.util.hub.ItemVersion;
import org.knime.core.util.hub.SpecificVersion;

/**
 * Tests for {@link URLResolverUtil}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class URLResolverUtilTest {

    // mostly copied from HubItemVersionTest

    /**
     * Example input: knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=3&param=4 <br/>
     * Example output: HubItemVersion(LinkType.FIXED_VERSION, 3)
     *
     * @throws MalformedURLException
     */
    @SuppressWarnings("static-method")
    @Test
    void testExtractVersionNumberWhenNotPresent() throws MalformedURLException, URISyntaxException {
        // given a URI/URL without a version number
        final URI knimeURI = new URI("knime://SomeMountPoint/some/path?someParameter=12");
        final URI httpsURI = new URI("https://www.knime.com");

        // when extracting the version number
        final var knimeURIVersion = URLResolverUtil.parseVersion(knimeURI.getQuery());
        final var httpsURIVersion = URLResolverUtil.parseVersion(httpsURI.getQuery());

        // then the version is empty
        assertEquals(Optional.empty(), knimeURIVersion, "version should be empty in URI: " + knimeURI);
        assertEquals(Optional.empty(), httpsURIVersion, "version should be empty in URI: " + httpsURI);
    }

    /**
     * Example input: knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=3&param=4 <br/>
     * Example output: HubItemVersion(LinkType.FIXED_VERSION, 3)
     *
     * @throws MalformedURLException
     */
    @SuppressWarnings("static-method")
    @Test
    void testExtractVersionNumberFixed() throws MalformedURLException, URISyntaxException {
     // given a URI/URL with a version number
        final URI uri = URI.create("knime://My-KNIME-Hub/*02j3f023j?multiValueParam=4,4&version=3&otherParam=book");
        final URL url =
            new URL("https://www.hub.knime.com/repo/some/item?multiValueParam=4,4&version=3&otherParam=book");

        // when extracting the version number
        var uriVersion = URLResolverUtil.parseVersion(uri.getQuery()).get();
        var urlVersion = URLResolverUtil.parseVersion(url.toURI().getQuery()).get();

        // then the link type is fixed version and the version number is 3
        final var version3 = new SpecificVersion(3);
        assertEquals(version3, uriVersion, "version 3 should be returned for uri: " + uri);
        assertEquals(version3, urlVersion, "version 3 should be returned for url: " + url);
    }


    /**
     * Example input: knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=latest-state&param=4 <br/>
     * Example output: HubItemVersion(LinkType.LATEST_STATE, null)
     *
     * @throws MalformedURLException
     * @throws URISyntaxException
     */
    @SuppressWarnings("static-method")
    @Test
    void testExtractVersionNumberLatestState() throws MalformedURLException, URISyntaxException {
        // given a URI/URL with latest version
        final URI uri = URI.create("knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=most-recent&param=4");
        final URL url =
            new URL("https://www.hub.knime.com/repo/some/item?someParameter=12&version=most-recent&param=4");

        // when extracting the version number
        var uriVersion = URLResolverUtil.parseVersion(uri.getQuery()).get();
        var urlVersion = URLResolverUtil.parseVersion(url.getQuery()).get();

        // then the link type is latest version and the version number is null
        final var latest = ItemVersion.mostRecent();
        assertEquals(latest, uriVersion, "latest version should be returned for uri: " + uri);
        assertEquals(latest, urlVersion, "latest version should be returned for url: " + url);
    }

    /**
     * Example input: knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=3&param=4 <br/>
     * Example output: knime://My-KNIME-Hub/*02j3f023j?someParameter=12&param=4
     * @throws URISyntaxException
     */
    @SuppressWarnings("static-method")
    @Test
    void testApplyCurrentStateRemovesVersion() throws URISyntaxException {
        // given a URI/URL with version set
        final URI uri = URI.create("knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=3&param=4");

        // when setting to current state
        final var builder = new URIBuilder(uri);
        final var appliedParams = URLResolverUtil.applyTo(ItemVersion.currentState(), builder.getQueryParams());
        final var appliedUri = builder.setParameters(appliedParams).build();

        // then the version is removed
        assertEquals(URI.create("knime://My-KNIME-Hub/*02j3f023j?someParameter=12&param=4"), appliedUri,
            "version should be removed from URI");
    }

    /**
     * Example input: knime://SomeMountPoint/some/path?someParameter=12 <br/>
     * Example output: unchanged
     * @throws URISyntaxException
     */
    @SuppressWarnings("static-method")
    @Test
    void testApplyCurrentStateToCurrentStateLeavesUnchanged() throws URISyntaxException {
        // given a URI/URL without version set
        final URI uri = URI.create("knime://SomeMountPoint/some/path?someParameter=12");

        // when setting to current state
        final var builder = new URIBuilder(uri);
        final var appliedParams = URLResolverUtil.applyTo(ItemVersion.currentState(), builder.getQueryParams());
        final var appliedUri = builder.setParameters(appliedParams).build();

        // then the result is unchanged
        assertEquals(uri, appliedUri, "URI should be unchanged.");
    }

    /**
     * Example input: knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=3&param=4 <br/>
     * Example output: knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=4&param=4
     * @throws URISyntaxException
     */
    @SuppressWarnings("static-method")
    @Test
    void testApplyFixedVersionUpdatesVersion() throws URISyntaxException {
        // given a URI with version set
        URI withVersion3 = URI.create("knime://My-KNIME-Hub/*02j3f023j?param=12&version=3&param=4");
        // when setting a different version
        final var builder = new URIBuilder(withVersion3);
        final var appliedParams = URLResolverUtil.applyTo(ItemVersion.of(4), builder.getQueryParams());
        final var withVersion4 = builder.setParameters(appliedParams).build();

        // then the version is removed
        assertEquals(URI.create("knime://My-KNIME-Hub/*02j3f023j?param=12&version=4&param=4"), withVersion4,
            "version should be updated in URI");
    }

    /**
     * Example input: knime://SomeMountPoint/some/path <br/>
     * Example output: knime://SomeMountPoint/some/path?version=4
     * @throws URISyntaxException
     */
    @SuppressWarnings("static-method")
    @Test
    void testApplyFixedVersionAddsVersion() throws URISyntaxException {
        // given a URI without version set
        URI withoutVersion = URI.create("knime://SomeMountPoint/some/path");
        // when setting a version
        final var builder = new URIBuilder(withoutVersion);
        final var appliedParams = URLResolverUtil.applyTo(ItemVersion.of(4), builder.getQueryParams());
        final var withVersion4 = builder.setParameters(appliedParams).build();
        // then the version is added
        assertEquals(URI.create("knime://SomeMountPoint/some/path?version=4"), withVersion4,
            "version should be added to URI");
    }

    /**
     * Example input: knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=3&param=4 <br/>
     * Example output: knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=most-recent&param=4
     * @throws URISyntaxException
     */
    @SuppressWarnings("static-method")
    @Test
    void testApplyLatestVersionReplacesFixedVersion() throws URISyntaxException {
        // given a URI with version set
        URI withVersion3 = URI.create("knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=3&param=4");
        // when setting to latest version
        final var builder = new URIBuilder(withVersion3);
        final var appliedParams = URLResolverUtil.applyTo(ItemVersion.mostRecent(), builder.getQueryParams());
        final var withVersionLatest = builder.setParameters(appliedParams).build();

        // then the version is set to most-recent
        assertEquals(URI.create("knime://My-KNIME-Hub/*02j3f023j?someParameter=12&version=most-recent&param=4"),
            withVersionLatest, "version should be set to most-recent in URI");
    }

    // no migration from Space Versioning anymore

    /**
     * Example input: knime://My-KNIME-Hub/*02j3f023j <br/>
     * Example output: knime://My-KNIME-Hub/*02j3f023j?version=3&spaceVersion=3
     *
     * @throws URISyntaxException
     */
    @SuppressWarnings("static-method")
    @Test
    void testAddToUriAddsNewParameterOnly() throws URISyntaxException {
        // given a fixed version
        final var version = ItemVersion.of(3);
        final var builder = new URIBuilder(URI.create("knime://My-KNIME-Hub/*02j3f023j?someParameter=12&param=4"));

        // when adding to a URI
        URLResolverUtil.addVersionQueryParameter(version, builder::addParameter);
        final var result = builder.build();

        // then the resulting URI has only the item version parameter but not the legacy space version
        final List<NameValuePair> params = WWWFormCodec.parse(result.getQuery(), StandardCharsets.UTF_8);
        assertTrue(params.contains(new BasicNameValuePair(LinkType.VERSION_QUERY_PARAM, "3")),
            "URI should have query parameter version=3");
        assertFalse(params.contains(new BasicNameValuePair("spaceVersion", "3")),
            "URI should not have query parameter spaceVersion=3");
    }

    @SuppressWarnings("static-method")
    @Test
    void testConflictingVersionsException() {
        final var params = "version=3&version=most-recent";
        final var thrown = assertThrows(IllegalArgumentException.class, () -> URLResolverUtil.parseVersion(params));
        assertTrue(thrown.getMessage().contains("Conflicting version parameters"),
            "Expected exception message to talk about conflicting versions, is: " + thrown.getMessage());
    }

    @SuppressWarnings("static-method")
    @Test
    void testNonNumericVersionException() {
        final var param = "version=bogus-version";
        final var thrown = assertThrows(IllegalArgumentException.class, () -> URLResolverUtil.parseVersion(param));
        assertTrue(thrown.getMessage().contains("Cannot parse specific version from value: "),
            "Expected exception message talking about version parse error, is: " + thrown.getMessage());
    }

}
