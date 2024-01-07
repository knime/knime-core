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
import java.util.function.Function;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.config.base.ConfigBaseWO;
import org.knime.core.node.message.Issue.Type;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeMessage;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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
@JsonSerialize(using = JsonMessageSerializer.class)
@JsonDeserialize(using = JsonMessageDeserializer.class)
public final class Message {

    private static final String CFG_SUMMARY = "summary";
    private static final String CFG_ISSUE = "issue";
    private static final String CFG_RESOLUTIONS = "resolutions";

    private final String m_summary;
    private final Issue m_issue;
    private final List<String> m_resolutions;

    Message(final MessageBuilder builder) {
        m_summary = builder.getSummary().orElseThrow(() -> new IllegalArgumentException("Summary must not be null"));
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
     * API method used by the framework to post-process messages and amend their issues by some data. It will render the
     * context (i.e. the previous rows) in which the problem occured. This method then decides whether it's worth the
     * effort to resolve the details, e.g. in streaming mode or for extremely large data it will not do the expensive
     * table scan.
     *
     * @param inputs The original data that is passed to the node that is produced this message
     * @return A new message with better details.
     * @noreference This method is not intended to be referenced by clients.
     */
    public Message renderIssueDetails(final PortObject[] inputs) {
        if (m_issue instanceof RowIssue ri) {
            return builder() //
                .withSummary(m_summary) //
                .addIssue(ri.toDefaultIssue(inputs)) //
                .addResolutions(m_resolutions.toArray(String[]::new)) //
                .build().orElseThrow();
        }
        return this;
    }

    /**
     * This <code>Message</code> as a string that can be printed to the logger ({@link org.knime.core.node.NodeLogger}).
     * It will include the summary and the (first) issue, if present.
     * @since 5.1
     * @return that (non-null) string.
     */
    public String toLogPrintable() {
        var strBuilder = new StringBuilder(getSummary());
        getIssue() //
            .map(Issue::toPreformatted) //
            .map(String::lines) //
            .stream() //
            .flatMap(Function.identity()) //
            .map("\n"::concat) //
            .forEach(strBuilder::append);
        return strBuilder.toString();
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
     * Adapts this message as exception that can be thrown in a node's <code>configure</code> method.
     *
     * @return A new exception representing this message.
     * @since 5.2
     */
    public InvalidSettingsException toInvalidSettingsException() {
        return new MessageAwareInvalidSettingsException(this, null);
    }

    /**
     * Adapts this message as exception that can be thrown in a node's <code>configure</code> method.
     *
     * @param cause An optional cause
     * @return A new exception representing this message.
     * @since 5.2
     */
    public InvalidSettingsException toInvalidSettingsException(final Throwable cause) {
        return new MessageAwareInvalidSettingsException(this, cause);
    }

    /**
     * Adapts the message as exception as generic {@link KNIMEException}, e.g. thrown by a node's <code>execute</code>
     * method.
     *
     * @return A new exception representing this message.
     * @since 5.2
     */
    public KNIMEException toKNIMEException() {
        return KNIMEException.of(this);
    }

    /**
     * Adapts the message as exception as generic {@link KNIMEException}, e.g. thrown by a node's <code>execute</code>
     * method.
     *
     * @param cause An optional cause.
     * @return A new exception representing this message.
     * @since 5.2
     */
    public KNIMEException toKNIMEException(final Throwable cause) {
        return KNIMEException.of(this, cause);
    }

    /**
     * Converts a {@link NodeMessage} into a {@link Message}. The argument must not be a RESET message; it's type is
     * ignored.
     *
     * @param nodeMessage to read from
     * @return A non-null message object
     * @throws IllegalArgumentException null argument or of type
     * {@link org.knime.core.node.workflow.NodeMessage.Type#RESET}
     */
    public static Message fromNodeMessage(final NodeMessage nodeMessage) {
        CheckUtils.checkArgumentNotNull(nodeMessage);
        CheckUtils.checkArgument(nodeMessage.getMessageType() != NodeMessage.Type.RESET, "Invalid type: %s",
            NodeMessage.Type.RESET);
        CheckUtils.checkArgumentNotNull(nodeMessage.getMessage(), "message text must not be null");
        var builder = builder().withSummary(nodeMessage.getMessage());
        nodeMessage.getIssue().ifPresent(builder::addTextIssue);
        builder.addResolutions(nodeMessage.getResolutions().toArray(String[]::new));
        return builder.build().orElseThrow(() -> new IllegalArgumentException("NodeMessage has null 'message' field"));
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
        config.addString(CFG_SUMMARY, m_summary);
        if (m_issue != null) {
            var issueConfig = config.addConfigBase(CFG_ISSUE);
            m_issue.getType().saveType(issueConfig);
            m_issue.saveTo(issueConfig);
        }
        config.addStringArray(CFG_RESOLUTIONS, m_resolutions.toArray(String[]::new));
    }

    /**
     * Load a message previously written using {@link #saveTo(ConfigBaseWO)}.
     *
     * @param config To load from.
     * @return a message or an empty message if none was stored (summary is null)
     * @throws InvalidSettingsException ....
     */
    public static Optional<Message> load(final ConfigBaseRO config) throws InvalidSettingsException {
        if (!config.containsKey(CFG_SUMMARY)) {
            return Optional.empty();
        }
        MessageBuilder builder = builder().withSummary(config.getString(CFG_SUMMARY));
        if (config.containsKey(CFG_ISSUE)) {
            final var issueConfig = config.getConfigBase(CFG_ISSUE);
            final var type = Type.loadType(issueConfig);
            final var issue = type.loadIssue(issueConfig);
            builder.addIssue(issue);
        }
        return builder.addResolutions(config.getStringArray(CFG_RESOLUTIONS)).build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this) //
            .append(CFG_SUMMARY, m_summary) //
            .append(CFG_ISSUE, m_issue) //
            .append(CFG_RESOLUTIONS, m_resolutions) //
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
     * A new message builder based off of this message. Use to, e.g. change the summary or add different details.
     *
     * @return A new builder using fields from this instance.
     * @since 5.1
     */
    public MessageBuilder modify() {
        return new MessageBuilder(this);
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
     * Factory method to create a message from just a plain summary string, incl. possible resolutions. Equivalent to
     * <code>builder().withSummary(String).addResolutions(String...).build()</code>
     *
     * @param summary The summary (null causes an exception to be thrown)
     * @param res The resolution hint(s), must not contain null (but may be empty)
     * @return Such a message.
     * @since 5.3
     */
    public static Message fromSummaryWithResolution(final String summary, final String... res) {
        return builder().withSummary(summary).addResolutions(res).build()
                .orElseThrow(() -> new IllegalArgumentException("argument must not be null"));
    }

    /**
     * Factory method to create a message from an individual "row issue", used to create a
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
     * @param description The description of the problem. <code>null</code> is OK but discouraged.
     * @return Such a message.
     */
    public static Message fromRowIssue(final String summary, final int portIndex, final long row, final int column,
        final String description) {
        return builder() //
            .withSummary(summary) //
            .addRowIssue(portIndex, column, row, description) //
            .build() //
            .orElseThrow(() -> new IllegalArgumentException("argument must not be null"));
    }

}
