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
 *   Nov 7, 2023 (wiswedel): created
 */
package org.knime.core.node.port.report;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.report.ReportPageConfiguration.PageMargins;
import org.knime.core.node.port.report.ReportPageConfiguration.PageOrientation;
import org.knime.core.node.port.report.ReportPageConfiguration.PageSize;
import org.knime.core.node.port.report.ReportPageConfiguration.PixelDimension;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author wiswedel
 */
class ReportPageConfigurationTest {

    /**
     * Tests factory method with null args.
     * @see ReportPageConfiguration#of(PageSize, PageOrientation, PageMargins)
     */
    @SuppressWarnings("static-method")
    @Test
    final void testOfIllegalArgument() {
        assertThrowsExactly(IllegalArgumentException.class,
            () -> ReportPageConfiguration.of(PageSize.B5, null, new PageMargins(0, 0, 0, 0)),
            "null orientation must cause exception");
        assertThrowsExactly(IllegalArgumentException.class,
            () -> ReportPageConfiguration.of(null, PageOrientation.Portrait, new PageMargins(0, 0, 0, 0)),
            "null page size must cause exception");
        assertDoesNotThrow(() -> ReportPageConfiguration.of(PageSize.A5, PageOrientation.Portrait, null));
    }

    /**
     * Tests factory method.
     * @see ReportPageConfiguration#of(PageSize, PageOrientation, PageMargins)
     */
    @SuppressWarnings("static-method")
    @Test
    final void testOf() {
        final var config = ReportPageConfiguration.of(PageSize.JIS_B4, PageOrientation.Landscape, null);
        assertThat("page size", config.getPageSize(), is(PageSize.JIS_B4));
        assertThat("page orientation", config.getPageOrientation(), is(PageOrientation.Landscape));
        assertNotNull(config.getPageMargins(), "page margin not null");
        // default value is, as of today (7 Nov '23)
        assertThat("page margin has default value", config.getPageMargins().bottomMargin(), is(0.395));
    }

    /**
     * Tests jackson serialization
     */
    @SuppressWarnings("static-method")
    @Test
    final void testSerialization() throws JsonProcessingException {
        final var config =
            ReportPageConfiguration.of(PageSize.Letter, PageOrientation.Landscape, new PageMargins(0.1, 0.2, 0.3, 0.4));
        final var objectMapper = new ObjectMapper();
        final var jsonSerializedString = objectMapper.writeValueAsString(config);
        JSONAssert.assertEquals("""
                {
                  "size" : "Letter",
                  "orientation" : "Landscape",
                  "margins" : {
                    "top" : 0.1,
                    "right" : 0.2,
                    "bottom" : 0.3,
                    "left" : 0.4
                  }
                }
                """, jsonSerializedString, false);
        final var configClone = objectMapper.readValue(jsonSerializedString, ReportPageConfiguration.class);
        assertNotSame(config, configClone, "original and deserialized clone");
        assertEquals(config.hashCode(), configClone.hashCode(), "original and deserialized clone same hash");
        assertEquals(config, configClone, "original and deserialized clone");
    }

    /**
     * test {@link PageMargins#asCSSString()} etc.
     */
    @SuppressWarnings("static-method")
    @Test
    final void testMarginCalculation() throws JsonProcessingException {
        final var configLandscape = ReportPageConfiguration.of( //
            PageSize.Letter, PageOrientation.Landscape, new PageMargins(0.1, 0.2, 0.3, 0.4));
        assertEquals("letter", configLandscape.getPageSize().getHumanReadableFormat(), "human readable page size");
        assertEquals(new PixelDimension(1056, 816), configLandscape.getPixelDimension(false), "pixel dimensions");
        assertEquals(new PixelDimension(999, 777), configLandscape.getPixelDimension(true), "pixel dimensions");
        assertEquals("0.1in 0.2in 0.3in 0.4in", configLandscape.getPageMargins().asCSSString(), "margins css");

        final var configPortrait = ReportPageConfiguration.of( //
            PageSize.Letter, PageOrientation.Portrait, new PageMargins(0.1, 0.2, 0.3, 0.4));
        assertEquals(new PixelDimension(816, 1056), configPortrait.getPixelDimension(false), "pixel dimensions");
        assertEquals(new PixelDimension(759, 1017), configPortrait.getPixelDimension(true), "pixel dimensions");
        assertEquals("0.1in 0.2in 0.3in 0.4in", configPortrait.getPageMargins().asCSSString(), "margins css");
    }

    /**
     * test {@link PageOrientation}.
     * @throws InvalidSettingsException
     */
    @SuppressWarnings("static-method")
    @Test
    final void testPageOrientation() throws InvalidSettingsException {
        assertEquals(PageOrientation.Landscape, PageOrientation.fromName("Landscape"), "parsing by name");
        assertThrows(InvalidSettingsException.class, () -> PageOrientation.fromName("unknown"));
        assertThrows(InvalidSettingsException.class, () -> PageOrientation.fromName(null));
        assertEquals("portrait", PageOrientation.Portrait.getOrientation());
    }

    /**
     * test {@link PageOrientation}.
     * @throws InvalidSettingsException
     */
    @SuppressWarnings("static-method")
    @Test
    final void testPageSize() throws InvalidSettingsException {
        assertEquals(PageSize.JIS_B4, PageSize.fromName("JIS_B4"), "parsing by name");
        assertEquals("JIS-B4", PageSize.JIS_B4.getHumanReadableFormat());
        assertThrows(InvalidSettingsException.class, () -> PageSize.fromName("unknown"));
        assertThrows(InvalidSettingsException.class, () -> PageSize.fromName(null));
        assertEquals(11.0, PageSize.Letter.getHeight());
        assertEquals(8.5, PageSize.Letter.getWidth());
    }

}
