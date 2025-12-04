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
 *   May 14, 2023 (wiswedel): created
 */
package org.knime.core.node.port.report;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.knime.core.internal.CorePlugin;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.util.ClassUtils;
import org.knime.core.node.workflow.SubNodeContainer;

/**
 * Utility methods to locate {@link IReportService} etc.
 *
 * @noreference This class is not intended to be referenced by clients.
 * @author Bernd Wiswedel, KNIME
 * @since 5.1
 */
public final class ReportUtil {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ReportUtil.class);

    private ReportUtil() {
    }

    /**
     * @return true if the reporting extension is installed (enables controls in layout editor).
     */
    public static boolean isReportingExtensionInstalled() {
        return CorePlugin.getInstance().getReportService().isPresent();
    }

    /**
     * @return the {@link URI} to our documentation for the reporting guide
     * @since 5.10
     */
    public static URI getReportingGuideURI() {
        return URI.create("https://docs.knime.com/latest/analytics_platform_reporting_guide");
    }

    /**
     * Called after {@link SubNodeContainer} execution to fill report output. Produces inactive port when
     * extension is missing.
     *
     * @param container The subnode whose output is to be generated.
     * @param context For progress/cancelation
     * @return The report port object (or an inactive object).
     * @deprecated use {@link #computeReportObjectWithExceptions(SubNodeContainer, ExecutionContext)}
     */
    @Deprecated(since = "5.10", forRemoval = true)
    public static PortObject computeReportObject(final SubNodeContainer container, final ExecutionContext context) {
        Optional<IReportService> serviceOptional = CorePlugin.getInstance().getReportService();
        if (serviceOptional.isPresent()) {
            return serviceOptional.map(service -> service.createOutput(container, context))
                .map(PortObject.class::cast).orElse(InactiveBranchPortObject.INSTANCE);
        } else {
            LOGGER.warn("No report service registered (extension installed?) - producing inactive output");
            return InactiveBranchPortObject.INSTANCE;
        }
    }

    /**
     * Called after {@link SubNodeContainer} execution to fill report output. Will consider
     * {@link ReportConfiguration#isFailOnReportError()} to determine whether the port will be set to
     * inactive or whether exceptions are thrown (i.e. fails node execution).
     *
     * @param container The subnode whose output is to be generated.
     * @param context For progress/cancelation
     * @return The report port object (throws exceptions if report generation fails).
     * @throws CanceledExecutionException if the node execution was cancelled
     * @throws TimeoutException if the reporting page(s) are not initialized or generated in time
     * @throws ExecutionException if something goes wrong during the task of report generation
     * @throws InterruptedException if the thread is interrupted while report generation is in progress
     * @since 5.10
     */
    public static PortObject computeReportObjectWithExceptions(final SubNodeContainer container,
        final ExecutionContext context)
        throws CanceledExecutionException, TimeoutException, ExecutionException, InterruptedException {
        Optional<IReportService> serviceOptional = CorePlugin.getInstance().getReportService();
        if (serviceOptional.isPresent()) {
            final var service = serviceOptional.get();
            final var report = service.createOutputWithExceptions(container, context);
            return Optional.ofNullable(report) //
                .flatMap(po -> ClassUtils.castOptional(PortObject.class, po)) //
                .orElse(InactiveBranchPortObject.INSTANCE);
        } else {
            LOGGER.warn("No report service registered (extension installed?) - producing inactive output");
            return InactiveBranchPortObject.INSTANCE;
        }
    }

    /**
     * Determines a subnode's report output object spec. Possible return values:
     *
     * <ul>
     * <li>The report spec - normal case when a report can be generated and the input info is available
     * <li>An inactive port - report extension not found or loaded
     * <li>An empty optional - service is present but input of a component is not populated
     * </ul>
     *
     * @param container The subnode whose output is to be generated.
     * @return see above
     * @since 5.2
     */
    public static Optional<? extends PortObjectSpec> computeReportObjectSpec(final SubNodeContainer container) {
        Optional<IReportService> serviceOptional = CorePlugin.getInstance().getReportService();
        if (serviceOptional.isPresent()) {
            return serviceOptional.get().createOutputSpec(container);
        } else {
            return Optional.of(InactiveBranchPortObjectSpec.INSTANCE);
        }
    }

    /**
     * The possible image formats
     *
     * @since 5.4
     */
    public enum ImageFormat {
            SVG("SVG"), PNG("PNG");

        private final String m_imageFormat;

        ImageFormat(final String imageFormat) {
            m_imageFormat = imageFormat;
        }

        /**
         * @return the image format
         */
        public String getImageFormat() {
            return m_imageFormat;
        }
    }
}
