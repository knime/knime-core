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

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.config.base.ConfigBaseWO;
import org.knime.core.node.message.Issue.Type;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeMessage;

/**
 * Read-only objects that capture the details of exceptions and warnings issued by node implementations. It comprises a
 * <i>summary</i>, an optional <i>issue</i> (aka detail) and a list of possible <i>resolutions</i>.
 *
 * <p>
 * Here is an example:
 * <p>
 * <i>Summary</i>:<br />
 * String “1/25/2022” in row 17 does not match date pattern. “25” cannot be converted to month.
 *
 * <p>
 * <i>Issue</i> (shown in a fixed font):
 *
 * <pre>
 * Row #  date_string
 *  15  "1/3/2022"
 *  16  "1/6/2022"
 *  17  "1/25/2022"
 *         ^^
 * Pattern: "dd/mm/yyyy"
 * </pre>
 *
 * <i>Resolutions:</i>
 * <ul>
 * <li>Change the date pattern to match all provided strings. The similar and common pattern “mm/dd/yyyy” matches this
 * string.</li>
 * <li>Set Output Missing or Fail to output Missing values for non-matching strings.</li>
 *
 * @author Bernd Wiswedel, KNIME, Konstanz, Germany
 * @since 5.0
 */
public final class Message {

    private final String m_summary;
    private final Issue m_issue;
    private final List<String> m_resolutions;

    Message(final MessageBuilder builder) {
        m_summary = CheckUtils.checkArgumentNotNull(builder.getSummary());
        m_issue = builder.getIssue().orElse(null);
        m_resolutions = builder.getResolutions();
    }

    /**
     * @return the non-null summary.
     */
    public String getSummary() {
        return m_summary;
    }

    /**
     * @return the first issue that was set, if any.
     */
    Optional<Issue> getIssue() {
        return Optional.ofNullable(m_issue);
    }

    /**
     * @return possible resolutions, possibly empty (never null).
     */
    public List<String> getResolutions() {
        return m_resolutions;
    }

    /**
     * API method used by the framework to post-process messages and amend their issues by some data. That is show the
     * context (i.e. the previous rows) as to where the problem occured. This method then decides whether it's worth the
     * effort to resolve the details, e.g. in streaming mode or for extremely large data it will not do the expensive
     * table scan.
     *
     * @param inputs The original data that is passed to the node that is produced this message
     * @return A new message with better details.
     */
    public Message fillIssues(final PortObject[] inputs) {
        MessageBuilder builder = builder() //
                .withSummary(m_summary) //
                .addResolutions(m_resolutions.toArray(String[]::new));
        if (m_issue instanceof RowIssue) {
            builder.addIssue(((RowIssue)m_issue).toDefaultIssue(inputs));
        }
        return builder.build().orElseThrow();
    }

    /**
     * Framework method to convert user message to a framwork message. Not to be used.
     *
     * @param type ...
     * @return ...
     *
     * @noreference This method is not intended to be referenced by clients.
     */
    public NodeMessage toNodeMessage(final NodeMessage.Type type) {
        var issue = m_issue != null ? m_issue.toPreformatted() : null;
        return new NodeMessage(type, m_summary, issue, m_resolutions);
    }


    /**
     * Convenience method to extract the summary, taking <code>null</code> into account.
     *
     * @param message The message to read out (can be null).
     * @return The summary (or null)
     */
    public static String getSummaryFrom(final Message message) {
        return message == null ? null : message.getSummary();
    }

    /**
     * Saves this object to the argument.
     *
     * @param config to save to.
     */
    public void saveTo(final ConfigBaseWO config) {
        config.addString("summary", m_summary);
        if (m_issue != null) {
            var issueConfig = config.addConfigBase("issue");
            m_issue.getType().saveType(issueConfig);
            m_issue.saveTo(issueConfig);
        }
        config.addStringArray("resolutions", m_resolutions.toArray(String[]::new));
    }

    /**
     * Load a message previously writting using {@link #saveTo(ConfigBaseWO)}.
     *
     * @param config To load from.
     * @return a message or an empty message if none was stored (summary is null)
     * @throws InvalidSettingsException ....
     */
    public static Optional<Message> load(final ConfigBaseRO config) throws InvalidSettingsException {
        if (!config.containsKey("summary")) {
            return Optional.empty();
        }
        MessageBuilder builder = builder().withSummary(config.getString("summary"));
        if (config.containsKey("issue")) {
            final var issueConfig = config.getConfigBase("issue");
            final var type = Type.loadType(issueConfig);
            final Issue issue = type.loadIssue(issueConfig);
            builder.addIssue(issue);
        }
        return builder.addResolutions(config.getStringArray("resolutions")).build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this) //
            .append("summary", m_summary) //
            .append("issues", m_issue) //
            .append("resolution", m_resolutions) //
            .build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Message)) {
            return false;
        }
        var m = (Message)obj;
        return new EqualsBuilder() //
            .append(m_summary, m.m_summary) //
            .append(m_issue, m.m_issue) //
            .append(m_resolutions, m.m_resolutions) //
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder() //
                .append(m_summary) //
                .append(m_issue) //
                .append(m_resolutions) //
                .toHashCode();
    }

    /**
     * A static build for a {@link Message}. Client should rather use
     * {@link org.knime.core.node.NodeModel#createMessageBuilder()}.
     *
     * @return A new builder.
     * @noreference This method is not intended to be referenced by clients.
     */
    @SuppressWarnings("javadoc")
    public static MessageBuilder builder() {
        return new MessageBuilder();
    }

    /**
     * Factory method to create a message from just a plain summary string. Equivalent to
     * <code>builder().withSummary(String).build()</code>
     *
     * @param summary The summary (null causes an exception to be thrown)
     * @return Such a message.
     */
    public static Message fromSummary(final String summary) {
        return builder().withSummary(summary).build()
            .orElseThrow(() -> new IllegalArgumentException("argument must not be null"));
    }

    /**
     * Factory method to create a message from an indivual "row issue", used to create a
     * {@link org.knime.core.node.KNIMEException}, e.g.:
     *
     * <pre>
     * if (problemOccured) {
     *     throw KNIMEException.of(Message.fromRowIssue("summary of the problem", 0, 5, 2, "unable to parse xyz"));
     * }
     * </pre>
     *
     * @param summary The summary (null causes an exception to be thrown)
     * @param portIndex Port index where the table with issues is connected to.
     * @param row Index of row causing the issue.
     * @param column Index of column causing the issue.
     * @param message The message associated with the issue (not null).
     * @return Such a message.
     */
    public static Message fromRowIssue(final String summary, final int portIndex, final long row, final int column,
        final String message) {
        var builder = builder().withSummary(summary);
        builder.newRowIssueCollector(portIndex).collect(column, row, message);
        return builder.build().orElseThrow(() -> new IllegalArgumentException("argument must not be null"));
    }

}
