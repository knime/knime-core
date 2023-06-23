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
import org.knime.core.node.util.CheckUtils;
import org.knime.shared.workflow.def.ReportConfigurationDef;
import org.knime.shared.workflow.def.impl.ReportConfigurationDefBuilder;

/**
 * Describes report configuration, such as page format etc.
 *
 * @noreference This class is not intended to be referenced by clients.
 * @author Bernd Wiswedel, KNIME
 * @param pageSize non-null page size
 * @param orientation non-null orientation
 * @since 5.1
 */
public record ReportConfiguration(PageSize pageSize, Orientation orientation) {

    /** page size, e.g. A4. */
    @SuppressWarnings({"java:S115", "javadoc"}) // naming of constants still OK to be persisted in workflow file
    public enum PageSize {
        A5("A5"),
        A4("A4"),
        A3("A3"),
        B5("B5"),
        B4("B4"),
        JIS_B5("JIS-B5"),
        JIS_B4("JIS-B4"),
        Letter("letter"),
        Legal("legal"),
        Ledger("ledger");

        private final String m_humanReadableFormat;

        PageSize(final String humanReadableFormat) {
            m_humanReadableFormat = humanReadableFormat;
        }

        /**
         * @return the format as it can be represented to the user, e.g. "letter".
         */
        public String getHumanReadableFormat() {
            return m_humanReadableFormat;
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
     * Only checks null-ness.
     */
    public ReportConfiguration {
        CheckUtils.checkArgumentNotNull(pageSize);
        CheckUtils.checkArgumentNotNull(orientation);
    }

    /**
     * Save config to disc/workflow.
     *
     * @param settings To save to.
     */
    public void save(final NodeSettingsWO settings) { //NOSONAR (more generic arg possible)
        settings.addString("pagesize", pageSize.name());
        settings.addString("orientation", orientation.name());
    }

    /** Counterpart to {@link #save(NodeSettingsWO)}.
     * @param settings To load from.
     * @return A new instance
     * @throws InvalidSettingsException in case of errors
     */
    public static ReportConfiguration load(final NodeSettingsRO settings) throws InvalidSettingsException { // NOSONAR
        final var pagesize = PageSize.fromName(settings.getString("pagesize"));
        final var orientation = Orientation.fromName(settings.getString("orientation"));
        return new ReportConfiguration(pagesize, orientation);
    }

    /**
     * Convert to Def object for new workflow serialization.
     *
     * @return A def object.
     */
    public ReportConfigurationDef toDef() {
        return new ReportConfigurationDefBuilder() //
            .setOrientation(orientation.getOrientation()) //
            .setPageSize(pageSize.getHumanReadableFormat()) //
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
        return Optional.of(new ReportConfiguration(pageSize, orientation));
    }

    public static record PixelDimension(int width, int height) {}

    public static PixelDimension getPixelDimension(final ReportConfiguration config) {
        var dim = switch (config.pageSize()) {
            // 150 PPI/DPI
            case A5 -> new PixelDimension( 874, 1240);
            case A4 -> new PixelDimension(1240, 1754);
            case A3 -> new PixelDimension(2480, 1754);
            default -> null;
        };

        if (dim == null) {
            throw new IllegalStateException("Page size " + config.pageSize() + " not supported");
        }

        if (config.orientation() == Orientation.Landscape) {
            dim = new PixelDimension(dim.height, dim.width);
        }

        return dim;
    }

}
