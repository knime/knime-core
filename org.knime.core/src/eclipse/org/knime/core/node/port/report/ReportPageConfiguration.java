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
 *   Nov 3, 2023 (wiswedel): created
 */
package org.knime.core.node.port.report;

import java.util.Objects;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines a report page, that is page size, orientation, margins, etc. It's also a property
 * of {@link IReportPortObject} to define page properties of any future pages appended to a
 * report.
 *
 * @author Bernd Wiswedel
 * @since 5.2
 */
@JsonAutoDetect(getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
public final class ReportPageConfiguration {

    /** page size, e.g. A4.
     * @since 5.2 */
    @SuppressWarnings({"java:S115", "javadoc"}) // naming of constants still OK to be persisted in workflow file
    public enum PageSize {
        A5     ("A5",     5.83,     8.27     ),
        A4     ("A4",     8.27,     11.69    ),
        A3     ("A3",     11.69,    16.54    ),
        B5     ("B5",     6.93,     9.84     ),
        B4     ("B4",     9.84,     13.9     ),
        JIS_B5 ("JIS-B5", 7.166667, 10.125   ),
        JIS_B4 ("JIS-B4", 10.125,   14.333333),
        Letter ("letter", 8.5,      11       ),
        Legal  ("legal",  8.5,      14       ),
        Ledger ("ledger", 11,       17       );

        private final String m_humanReadableFormat;
        private final double m_width;
        private final double m_height;

        PageSize(final String humanReadableFormat, final double width, final double height) {
            m_humanReadableFormat = humanReadableFormat;
            m_width = width;
            m_height = height;
        }

        /**
         * @return the format as it can be represented to the user, e.g. "letter".
         */
        public String getHumanReadableFormat() {
            return m_humanReadableFormat;
        }

        /**
         * @return the width for the page in portrait orientation in inches
         */
        public double getWidth() {
            return m_width;
        }

        /**
         * @return the height for the page in portrait orientation in inches
         */
        public double getHeight() {
            return m_height;
        }

        /**
         * Load from disk ({@link #name()}) ... with proper exception handling.
         * @param name the name as persisted on disk, possibly <code>null</code>.
         * @return The page size object
         * @throws InvalidSettingsException If unknown/wrong/null
         */
        public static PageSize fromName(final String name) throws InvalidSettingsException {
            return Stream.of(values()).filter(ps -> ps.name().equals(name)) //
                    .findFirst() //
                    .orElseThrow(() -> new InvalidSettingsException("Invalid Page Size: " + name));
        }
    }

    /** Page orientation (landscape/portrait).
     * @since 5.2 */
    @SuppressWarnings({"java:S115", "javadoc"}) // naming of constants still OK to be persisted in workflow file
    public enum PageOrientation {

        Portrait("portrait"),
        Landscape("landscape");

        private final String m_format;

        PageOrientation(final String format) {
            m_format = format;
        }

        /**
         * @return the format, e.g. "portrait".
         */
        public String getOrientation() {
            return m_format;
        }

        /**
         * Load from disk ({@link #name()}) ... with proper exception handling.
         * @param name the name as persisted on disk, possibly <code>null</code>.
         * @return The page size object
         * @throws InvalidSettingsException If unknown/wrong/null
         */
        public static PageOrientation fromName(final String name) throws InvalidSettingsException {
            return Stream.of(values()).filter(ps -> ps.name().equals(name)) //
                    .findFirst() //
                    .orElseThrow(() -> new InvalidSettingsException("Invalid Orientation: " + name));
        }
    }

    /**
     * Page margins which can be supplied as 1-4 values, similar to CSS margin definition shorthands.
     * All values are assumed to be in inches.
     * @param topMargin the value in inches to assign to the top page margin
     * @param rightMargin the value in inches to assign to the right page margin
     * @param bottomMargin the value in inches to assign to the bottom page margin
     * @param leftMargin the value in inches to assign to the left page margin
     *
     * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
     * @since 5.2
     */
    @SuppressWarnings("java:S125") // commented out code
    public record PageMargins( //
        @JsonProperty("top") double topMargin, //
        @JsonProperty("right") double rightMargin, //
        @JsonProperty("bottom") double bottomMargin, //
        @JsonProperty("left") double leftMargin) {

        /**
         * Shorthand constructor to supply all 4 margins as the same value.
         * @param allMargins the value in inches to assign to top, right, bottom and left page margins
         */
        // must not define other constructor for inner records - breaks jackson serialization in 2.13.2
        // (fixed in 2.15.0 - probably related: https://github.com/FasterXML/jackson-databind/issues/3102)
        // public PageMargins(final double allMargins) {
        //     this(allMargins, allMargins, allMargins, allMargins);
        // }

        /**
         * Returns the set margin values as CSS value string
         * @return the concatenated CSS value string
         */
        public String asCSSString() {
            final String unit = "in ";
            final StringBuilder css = new StringBuilder();
            css.append(topMargin);
            css.append(unit);
            css.append(rightMargin);
            css.append(unit);
            css.append(bottomMargin);
            css.append(unit);
            css.append(leftMargin);
            css.append(unit);
            return css.substring(0, css.length() - 1);
        }

    }

    // @SuppressWarnings({"java:S115", "javadoc"})
    /** Return type of {@link #getPixelDimension(boolean)}. */
    public static record PixelDimension(int width, int height) {}

    /**
     * Rendering resolution in ppi, constant for now (96ppi)
     */
    private static final int RESOLUTION_PPI = 96;

    private static final double DEFAULT_PAGE_MARGINS_VALUE = 0.395;
    /**
     * Default page margins, 1cm (0.395in) for each side
     */
    private static final PageMargins DEFAULT_PAGE_MARGINS = new PageMargins(DEFAULT_PAGE_MARGINS_VALUE,
        DEFAULT_PAGE_MARGINS_VALUE, DEFAULT_PAGE_MARGINS_VALUE, DEFAULT_PAGE_MARGINS_VALUE);

    @JsonProperty("size")
    private final PageSize m_pageSize;
    @JsonProperty("orientation")
    private final PageOrientation m_pageOrientation;
    @JsonProperty("margins")
    private final PageMargins m_pageMargins;

    @JsonIgnore
    private ReportPageConfiguration(final PageSize pageSize, final PageOrientation pageOrientation,
        final PageMargins pageMargins) {
        m_pageSize = CheckUtils.checkArgumentNotNull(pageSize);
        m_pageOrientation = CheckUtils.checkArgumentNotNull(pageOrientation);
        m_pageMargins = CheckUtils.checkArgumentNotNull(pageMargins);
    }

    /**
     * Determines the dimensions in pixels of a page size.
     *
     * @param subtractMargins true, if the defined margins should be subtracted from the page dimensions
     * @return a {@link PixelDimension} containing width and height of
     */
    public PixelDimension getPixelDimension(final boolean subtractMargins) { // NOSONAR
        int res = RESOLUTION_PPI;
        int pageWidth = getDevicePixelForPhysicalDimension(m_pageSize.getWidth(), res);
        int pageHeight = getDevicePixelForPhysicalDimension(m_pageSize.getHeight(), res);

        int width = m_pageOrientation == PageOrientation.Portrait ? pageWidth : pageHeight;
        int height = m_pageOrientation == PageOrientation.Portrait ? pageHeight : pageWidth;

        if (subtractMargins) {
            int marginTop = getDevicePixelForPhysicalDimension(m_pageMargins.topMargin, res);
            int marginRight = getDevicePixelForPhysicalDimension(m_pageMargins.rightMargin, res);
            int marginBottom = getDevicePixelForPhysicalDimension(m_pageMargins.bottomMargin, res);
            int marginLeft = getDevicePixelForPhysicalDimension(m_pageMargins.leftMargin, res);

            int widthMargin = Math.max(0, width - marginLeft - marginRight);
            int heightMargin = Math.max(0, height - marginTop - marginBottom);

            return new PixelDimension(widthMargin, heightMargin);
        }
        return new PixelDimension(width, height);
    }



    /**
     * @return the pageSize
     */
    public PageSize getPageSize() {
        return m_pageSize;
    }

    /**
     * @return the pageOrientation
     */
    public PageOrientation getPageOrientation() {
        return m_pageOrientation;
    }

    /**
     * @return the pageMargins
     */
    public PageMargins getPageMargins() {
        return m_pageMargins;
    }

    /**
     * Returns a rounded pixel value for a given physical value in inches
     *
     * @param physicalValue a value in inches to be converted to pixel
     * @param resolution the resolution at which to convert the physical value
     *
     * @return the value in device pixels
     */
    private static int getDevicePixelForPhysicalDimension(final double physicalValue, final int resolution) {
        return (int)Math.round(physicalValue * resolution);
    }

    @Override
    public String toString() {
        return "%s - %s".formatted(m_pageSize.getHumanReadableFormat(), m_pageOrientation.getOrientation());
    }

    @Override
    public int hashCode() {
        // eclipse auto generated
        final int prime = 31;
        int result = 1;
        result = prime * result + m_pageMargins.hashCode();
        result = prime * result + m_pageOrientation.hashCode();
        result = prime * result + m_pageSize.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ReportPageConfiguration rpc) {
            return m_pageSize == rpc.m_pageSize //
                && m_pageOrientation == rpc.m_pageOrientation //
                && m_pageMargins.equals(rpc.m_pageMargins);
        }
        return false;
    }

    /**
     * Creates new {@link ReportPageConfiguration}. Page size and orientation must not be null.
     * @param pageSize The page size, not null.
     * @param pageOrientation The orientation, not null.
     * @param pageMargins The margin - maybe null in which case a default margin is used.
     * @return A new config.
     */
    @JsonCreator
    public static ReportPageConfiguration of( //
        @JsonProperty("size") final PageSize pageSize, //
        @JsonProperty("orientation") final PageOrientation pageOrientation, //
        @JsonProperty("margins") final PageMargins pageMargins) {
        return new ReportPageConfiguration(pageSize, pageOrientation,
            Objects.requireNonNullElse(pageMargins, DEFAULT_PAGE_MARGINS));
    }

}
