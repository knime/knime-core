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
 *   Dec 14, 2022 (wiswedel): created
 */
package org.knime.core.node.message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;

/**
 * Builder object for {@link Message}. Typically it would be instantiated via
 * {@link org.knime.core.node.NodeModel#createMessageBuilder()} and then filled while the node is executing. The actual
 * fields are described in {@link Message}, whereby the {@link #withSummary(String) summary} must not be
 * <code>null</code> in order to have a 'reasonable' message (otherwise an empty message is {@link #build() built}.
 *
 * <p>
 * A <i>warning {@link Message message}</i> is usually composed as follows, here an example processing a data table:
 *
 * <pre>
 * BufferedDataTable table = (BufferedDataTable)input[0];
 * MessageBuilder messageBuilder = createMessageBuilder();
 * RowIssueCollector rowIssueCollector = messageBuilder.newRowIssueCollector(table.getSpec(), 0);
 * long row = 0L;
 * try (RowCursor cursor = table.cursor()) {
 *     while (cursor.canForward()) {
 *         RowRead read = cursor.forward();
 *         if (hasProblem(read.getValue(someColumn))) {
 *             rowIssueCollector.collect(someColumn, row, "Some problem here");
 *         }
 *         row += 1;
 *     }
 * }
 * if (messageBuilder.getIssueCount() > 0) {
 *     messageBuilder.withSummary("some summary, possibly quoting the first issue or providing an issue count");
 * }
 * messageBuilder.build().ifPresent(m -> setWarning(m));
 * </pre>
 *
 * <p>
 * Similarly, an <i>error {@link Message message}</i> can be constructed and thrown via
 * {@link org.knime.core.node.KNIMEException}. See also {@link Message#fromRowIssue(String, int, long, int, String)}.
 *
 * @author Bernd Wiswedel, KNIME, Konstanz, Germany
 * @since 5.0
 */
@SuppressWarnings("javadoc")
public final class MessageBuilder {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(MessageBuilder.class);

    private String m_summary;
    private Issue m_relevantIssue;
    private long m_issueCount;
    private List<String> m_resolutions;

    MessageBuilder() {
        m_resolutions = new ArrayList<>();
    }

    String getSummary() {
        return m_summary;
    }

    Optional<Issue> getIssue() {
        return Optional.ofNullable(m_relevantIssue);
    }

    List<String> getResolutions() {
        return m_resolutions;
    }

    /**
     * Sets a summary and returns <code>this</code>. Setting a <code>null</code> summary (which is the default at
     * constrution time) will cause an empty default message to be built ({@link #build()}, ignoring any potential
     * issues or resolutions been set.
     *
     * @param summary That summary.
     * @return this
     */
    public MessageBuilder withSummary(final String summary) {
        m_summary = summary;
        return this;
    }

    /**
     * A new collector for row issues, as per example above.
     *
     * @param portIndex The index of the port where the the table came in (needed for post-processing the data and
     *            filling out context infos, e.g. previous 3 rows of an error row).
     *
     * @return A new issue collector. Adding issues to it will cause this message builder to be updated.
     */
    public RowIssueCollector newRowIssueCollector(final int portIndex) {
        return new RowIssueCollector(this, portIndex);
    }

    /**
     * Add/set a simple text based issue.
     *
     * @param preformatted The text of the issue.
     * @return this
     */
    public MessageBuilder addIssue(final String preformatted) {
        return addIssue(new DefaultIssue(preformatted));
    }

    MessageBuilder addIssue(final Issue issue) {
        if (m_relevantIssue == null) {
            m_relevantIssue = issue;
        }
        m_issueCount += 1;
        return this;
    }

    /**
     * A resolution hints (shown as enumerated list).
     *
     * @param res The resolution hints.
     * @return this.
     */
    public MessageBuilder addResolutions(final String... res) {
        CheckUtils.checkArgumentNotNull(res);
        CheckUtils.checkArgument(!ArrayUtils.contains(res, null), "Argument must not contain null elements");
        m_resolutions.addAll(Arrays.asList(res));
        return this;
    }

    /**
     * Get the first issue that was added to the message collector, useful in case the message count is 1 and the
     * first (and only) issue should be used to define the main summary.
     *
     * @return first issue, if any, or empty.
     */
    public Optional<String> getFirstIssue() {
        return getIssue().map(Issue::toPreformatted);
    }

    /**
     * @return the number of issues added to the message builder. Useful to have a resonable summary ("13 rows with
     *         problems"
     */
    public long getIssueCount() {
        return m_issueCount;
    }

    /**
     * Builds the final message or an empty {@link Optional} if no summary was set.
     *
     * @return The message.
     */
    public Optional<Message> build() {
        if (m_summary == null) {
            if (m_issueCount > 0) {
                LOGGER.coding(
                    String.format("Attempting to create a Message with %d issues but a 'null' summary", m_issueCount));
            }
            return Optional.empty();
        }
        return Optional.of(new Message(this));
    }
}
