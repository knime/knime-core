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
 *   Jan 6, 2023 (wiswedel): created
 */
package org.knime.core.node.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;
import org.knime.core.node.ModelContent;

/**
 * Tests basic functionality to class {@link Message}.
 * @author Bernd Wiswedel, KNIME GmbH
 */
class MessageTest {

    /**
     * Test method for {@link org.knime.core.node.message.Message#Message(org.knime.core.node.message.MessageBuilder)}.
     */
    @Test
    void testMessage() {
        var messageBuilder = Message.builder();
        final var issueDesc = "Some Message in Row with Index 10";
        messageBuilder.addRowIssue(0, 0, 10, issueDesc);
        messageBuilder.addRowIssue(0, 0, 550, "Some Message in Row with Index 550");
        ThrowingConsumer<RowIssue> rowIssueCondition = issue -> {
            assertThat(issue.toPreformatted()).contains(issueDesc);
        };
        assertThat(messageBuilder.getIssueCount()).isEqualTo(2);
        assertThat(messageBuilder.getFirstIssue()).get().asString().contains(issueDesc);
        final var summary = "Some Summary";
        messageBuilder.withSummary(summary);
        var messageOptional = messageBuilder.build();
        final var message = messageOptional.get();
        assertThat(message).extracting(m -> m.getSummary()).isEqualTo(summary);
        assertThat(message).extracting(m -> m.getResolutions()).asList().isEmpty();
        assertThat(message).extracting(m -> m.getIssue().orElseThrow()).asInstanceOf(type(RowIssue.class))
            .satisfies(rowIssueCondition);
    }

    @Test
    void testEmpty() throws Exception {
        var messageBuilder = Message.builder();
        assertThat(messageBuilder.build()).as("empty message since no details provided").isEmpty();
        messageBuilder.addResolutions("Resolution 1", "Resolution 2");
        messageBuilder.addIssue(new DefaultIssue("Here is problem 1"));
        messageBuilder.addIssue(new DefaultIssue("Here is problem 2"));
        var message = messageBuilder.build();
        assertThat(message).as("Message with no summary should be empty (even though details were specified").isEmpty();
    }

    @Test
    void testSaveAndLoad_RowIssues() throws Exception {
        var messageBuilder = Message.builder();
        messageBuilder.addResolutions("Resolution 1", "Resolution 2");
        messageBuilder.addRowIssue(0, 0, 1, "Error in Row 1, Column 0");
        messageBuilder.withSummary("Some Summary Message");
        var message = messageBuilder.build().orElseThrow();

        // for coverage
        assertThat(message).isNotEqualTo(new Object()) //
            .isEqualTo(message) //
            .extracting(m -> m.toString()).isNotNull();

        var config = new ModelContent("temp");
        message.saveTo(config);
        var clone = Message.load(config).orElseThrow();
        assertThat(message).isEqualTo(clone) //
            .hasSameHashCodeAs(clone) //
            .isNotSameAs(clone);

        var messageFirstIssue = message.getIssue().orElseThrow();
        var cloneFirstIssue = clone.getIssue().orElseThrow();

        // for coverage
        assertThat(messageFirstIssue).isNotEqualTo(new Object()) //
            .isEqualTo(messageFirstIssue) //
            .extracting(m -> m.toString()).isNotNull();

        assertThat(messageFirstIssue).isEqualTo(cloneFirstIssue) //
            .hasSameHashCodeAs(cloneFirstIssue) //
            .isNotSameAs(cloneFirstIssue);
    }

    @Test
    void testSaveAndLoad_DefaultIssues() throws Exception {
        var messageBuilder = Message.builder();
        messageBuilder.addResolutions("Resolution 1", "Resolution 2");
        var issueText = "Issue Details XYZ";
        messageBuilder.addTextIssue(issueText);
        messageBuilder.withSummary("Some Summary Message");
        var message = messageBuilder.build().orElseThrow();

        // for coverage
        assertThat(message).isNotEqualTo(new Object()) //
            .isEqualTo(message) //
            .extracting(m -> m.toString()).isNotNull();

        var config = new ModelContent("temp");
        message.saveTo(config);
        var clone = Message.load(config).orElseThrow();
        assertThat(message).isEqualTo(clone) //
            .hasSameHashCodeAs(clone) //
            .isNotSameAs(clone);

        var messageFirstIssue = message.getIssue().orElseThrow();
        var cloneFirstIssue = clone.getIssue().orElseThrow();

        // for coverage
        assertThat(messageFirstIssue).isNotEqualTo(new Object()) //
            .isEqualTo(messageFirstIssue) //
            .extracting(m -> m.toPreformatted()).isEqualTo(issueText) //
            .extracting(m -> m.toString()).isNotNull();

        assertThat(messageFirstIssue).isEqualTo(cloneFirstIssue) //
            .hasSameHashCodeAs(cloneFirstIssue) //
            .isNotSameAs(cloneFirstIssue);
    }
}
