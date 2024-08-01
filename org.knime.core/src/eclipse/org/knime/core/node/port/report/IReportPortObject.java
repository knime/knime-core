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
 *   May 12, 2023 (wiswedel): created
 */
package org.knime.core.node.port.report;

import java.util.List;
import java.util.Objects;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.port.report.ReportUtil.ImageFormat;
import org.knime.core.node.util.CheckUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Generic interface for report ports associated with component outputs. Implementation lives in different (KNIME) repo.
 *
 * @author Bernd Wiswedel, KNIME, Konstanz, Germany
 * @noextend This interface is not intended to be extended by clients (outside of KNIME).
 * @noimplement This interface is not intended to be implemented by clients (outside of KNIME).
 * @since 5.1
 */
public interface IReportPortObject extends PortObject {

    /**
     * Shortcut to retrieve type from port type registry.
     */
    PortType TYPE = PortTypeRegistry.getInstance().getPortType(IReportPortObject.class);

    /**
     * Non-functional ("no-op") serializer defined to please the extension point definition. Not meant to be called.
     */
    final class NoOpSerializer extends FailOnInvocationPortObjectSerializer<IReportPortObject> {}

    @Override
    IReportPortObjectSpec getSpec();

    /**
     * @return The non-modifiable, non-null list of report fragments.
     * @since 5.2
     */
    List<ReportFragment> getReportFragments();

    /**
     * A single fragment in a report. This is usually a page (or a set of pages) contributed by a single component
     * (subnode). Combines the fragment content + the page configuration + the image format of views.
     *
     * @param content The content (html-like)
     * @param config the config.
     * @param imageFormat The image format used for views.
     * @since 5.4
     */
    public record ReportFragment(String content, ReportPageConfiguration config, ImageFormat imageFormat) {

        /**
         * @since 5.4
         */
        @SuppressWarnings("javadoc")
        public ReportFragment {
            CheckUtils.checkArgumentNotNull(content, "Content must not be null");
            CheckUtils.checkArgumentNotNull(config, "Config must not be null");
            CheckUtils.checkArgumentNotNull(imageFormat, "ImageFormat must not be null");
        }

        /**
         * @since 5.4
         */
        @SuppressWarnings("javadoc")
        @JsonCreator
        public static ReportFragment createReportFragment(@JsonProperty("content") final String content,
            @JsonProperty("config") final ReportPageConfiguration config,
            @JsonProperty("imageFormat") final ImageFormat imageFormat) {
            return new ReportFragment(content, config, Objects.requireNonNullElse(imageFormat, ImageFormat.PNG));
        }

    }

}
