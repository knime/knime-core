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
 *   May 25, 2023 (wiswedel): created
 */
package org.knime.core.node.port.report;

import java.util.Optional;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.report.ReportConfiguration.Orientation;
import org.knime.core.node.port.report.ReportConfiguration.PageMargins;
import org.knime.core.node.port.report.ReportConfiguration.PageSize;
import org.knime.core.node.util.CheckUtils;
import org.knime.shared.workflow.def.PageMarginsDef;
import org.knime.shared.workflow.def.ReportConfigurationDef;
import org.knime.shared.workflow.def.impl.PageMarginsDefBuilder;
import org.knime.shared.workflow.def.impl.ReportConfigurationDefBuilder;

/**
 * Describes report configuration, such as page format etc.
 *
 * @noreference This class is not intended to be referenced by clients.
 * @author Bernd Wiswedel, KNIME
 * @param pageSize non-null page size
 * @param orientation non-null orientation
 * @param margins non-null page margins
 *
 * @since 5.1
 */
public record ReportConfiguration(PageSize pageSize, Orientation orientation, PageMargins margins) {

    /**
     * Report configuration constructor using default page margins and resolution
     * @param pageSize non-null page size
     * @param orientation non-null orientation
     */
    public ReportConfiguration(final PageSize pageSize, final Orientation orientation) {
        this(pageSize, orientation, DEFAULT_PAGE_MARGINS);
    }

    /** page size, e.g. A4. */
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

    /** Page orientation (landscape/portrait). */
    @SuppressWarnings({"java:S115", "javadoc"}) // naming of constants still OK to be persisted in workflow file
    public enum Orientation {

        Portrait("portrait"),
        Landscape("landscape");

        private final String m_format;

        Orientation(final String format) {
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
        public static Orientation fromName(final String name) throws InvalidSettingsException {
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
     */
    public record PageMargins(double topMargin, double rightMargin, double bottomMargin, double leftMargin) {

        /**
         * Shorthand constructor to supply all 4 margins as the same value.
         * @param allMargins the value in inches to assign to top, right, bottom and left page margins
         */
        public PageMargins(final double allMargins) {
            this(allMargins, allMargins, allMargins, allMargins);
        }

        /**
         * Shorthand constructor to supply top/bottom and left/right margins as the same value.
         * @param topBottomMargin the value in inches to assign to top and bottom page margins
         * @param leftRightMargin the value in inches to assign to left and right page margins
         */
        public PageMargins(final double topBottomMargin, final double leftRightMargin) {
            this(topBottomMargin, leftRightMargin, topBottomMargin, leftRightMargin);
        }

        /**
         * Shorthand constructor to supply left/right margins as the same value.
         * @param topMargin the value in inches to assign to the top page margin
         * @param leftRightMargin the value in inches to assign to left and right page margins
         * @param bottomMargin the value in inches to assign to the bottom page margin
         */
        public PageMargins(final double topMargin, final double leftRightMargin, final double bottomMargin) {
            this(topMargin, leftRightMargin, bottomMargin, leftRightMargin);
        }

        /**
         * Returns the set margin values as CSS value string
         * @return the concatenated CSS value string
         */
        public String asCSSString() {
            String unit = "in ";
            StringBuilder css = new StringBuilder();
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

    /**
     * Rendering resolution in ppi, constant for now (96ppi)
     */
    private static final int RESOLUTION_PPI = 96;

    /**
     * Default page margins, 1cm (0.395in) for each side
     */
    private static final PageMargins DEFAULT_PAGE_MARGINS = new PageMargins(0.395);

    /**
     * Only checks null-ness.
     */
    public ReportConfiguration {
        CheckUtils.checkArgumentNotNull(pageSize);
        CheckUtils.checkArgumentNotNull(orientation);
        CheckUtils.checkArgumentNotNull(margins);
    }

    /**
     * Save config to disc/workflow.
     *
     * @param settings To save to.
     */
    public void save(final NodeSettingsWO settings) { //NOSONAR (more generic arg possible)
        settings.addString("pagesize", pageSize.name());
        settings.addString("orientation", orientation.name());
        NodeSettingsWO marginSettings = settings.addNodeSettings("pagemargins");
        marginSettings.addDouble("top", margins.topMargin);
        marginSettings.addDouble("right", margins.rightMargin);
        marginSettings.addDouble("bottom", margins.bottomMargin);
        marginSettings.addDouble("left", margins.leftMargin);
    }

    /** Counterpart to {@link #save(NodeSettingsWO)}.
     * @param settings To load from.
     * @return A new instance
     * @throws InvalidSettingsException in case of errors
     */
    public static ReportConfiguration load(final NodeSettingsRO settings) throws InvalidSettingsException { // NOSONAR
        final var pagesize = PageSize.fromName(settings.getString("pagesize"));
        final var orientation = Orientation.fromName(settings.getString("orientation"));
        var pageMargins = DEFAULT_PAGE_MARGINS;
        try {
            NodeSettingsRO marginSettings = settings.getNodeSettings("pagemargins");
            double topMargin = marginSettings.getDouble("top");
            double rightMargin = marginSettings.getDouble("right");
            double bottomMargin = marginSettings.getDouble("bottom");
            double leftMargin = marginSettings.getDouble("left");
            pageMargins = new PageMargins(topMargin, rightMargin, bottomMargin, leftMargin);
        } catch (InvalidSettingsException ex) { /* use default margins */ }

        return new ReportConfiguration(pagesize, orientation, pageMargins);
    }

    /**
     * Convert to Def object for new workflow serialization.
     *
     * @return A def object.
     */
    public ReportConfigurationDef toDef() {
        PageMarginsDef pageMargins = new PageMarginsDefBuilder()
                .setTop(margins.topMargin) //
                .setRight(margins.rightMargin) //
                .setBottom(margins.bottomMargin) //
                .setLeft(margins.leftMargin) //
                .build();
        return new ReportConfigurationDefBuilder() //
            .setOrientation(orientation.getOrientation()) //
            .setPageSize(pageSize.getHumanReadableFormat()) //
            .setPageMargins(pageMargins) //
            .build();
    }

    /**
     * Counterpart to {@link #toDef()}.
     *
     * @param def To read from.
     * @return A new optional report config, if def was non-null.
     * @throws InvalidSettingsException ...
     */
    public static Optional<ReportConfiguration> fromDef(final ReportConfigurationDef def)
        throws InvalidSettingsException {
        if (def == null) {
            return Optional.empty();
        }
        final var orDef = def.getOrientation();
        final var orientation = Stream.of(Orientation.values()).filter(o -> o.getOrientation().equals(orDef))
            .findFirst().orElseThrow(() -> new InvalidSettingsException("Invalid orientation identifier: " + orDef));
        final var psDef = def.getPageSize();
        final var pageSize = Stream.of(PageSize.values()).filter(o -> o.getHumanReadableFormat().equals(psDef))
            .findFirst().orElseThrow(() -> new InvalidSettingsException("Invalid page size identifier: " + psDef));
        var marginsDef = def.getPageMargins();
        var pageMargins = new PageMargins(marginsDef.getTop(), marginsDef.getRight(), marginsDef.getBottom(), marginsDef.getLeft());
        return Optional.of(new ReportConfiguration(pageSize, orientation, pageMargins));
    }

    @SuppressWarnings({"java:S115", "javadoc"})
    public static record PixelDimension(int width, int height) {}

    /**
     * Determines the dimensions in pixels of a page size, given a {@link ReportConfiguration}
     * @param config the configuration object containing page format, orientation and margins
     * @param subtractMargins true, if the defined margins should be subtracted from the page dimensions, false otherwise
     *
     * @return a {@link PixelDimension} containing width and height of
     */
    public static PixelDimension getPixelDimension(final ReportConfiguration config, final boolean subtractMargins) {
        int res = ReportConfiguration.RESOLUTION_PPI;
        int pageWidth = getDevicePixelForPhysicalDimension(config.pageSize.getWidth(), res);
        int pageHeight = getDevicePixelForPhysicalDimension(config.pageSize.getHeight(), res);

        int width = config.orientation() == Orientation.Portrait ? pageWidth : pageHeight;
        int height = config.orientation() == Orientation.Portrait ? pageHeight : pageWidth;

        if (subtractMargins) {
            int marginTop = getDevicePixelForPhysicalDimension(config.margins.topMargin, res);
            int marginRight = getDevicePixelForPhysicalDimension(config.margins.rightMargin, res);
            int marginBottom = getDevicePixelForPhysicalDimension(config.margins.bottomMargin, res);
            int marginLeft = getDevicePixelForPhysicalDimension(config.margins.leftMargin, res);

            int widthMargin = Math.max(0, width - marginLeft - marginRight);
            int heightMargin = Math.max(0, height - marginTop - marginBottom);

            return new PixelDimension(widthMargin, heightMargin);
        }
        return new PixelDimension(width, height);
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

}
