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
 *   31 Oct 2022 (marcbux): created
 */
package org.knime.testing.node.view;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.knime.core.webui.data.DataServiceContext;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public class WarningMessageAsserterUtil {

    private WarningMessageAsserterUtil() {
    }

    /**
     * This class receives messages for testing the received warning messages during testing the data service of a node.
     */
    public static final class WarningMessageAsserter {
        private final Set<String> m_registeredMessages;

        private final Set<String> m_receivedMessages;

        /**
         * @param messages the messages, the consumer should receive during the test.
         */
        public WarningMessageAsserter(final String... messages) {
            m_receivedMessages = new HashSet<>();
            m_registeredMessages = new HashSet<>();
            Arrays.stream(messages).forEach(msg -> this.registerMessage(msg));
        }

        /**
         * Adds a new message to the registered ones.
         *
         * @param message
         */
        private void registerMessage(final String message) {
            m_registeredMessages.add(message);
        }

        /**
         * Adds a new message to the received ones.
         *
         * @param message
         */
        public void receiveMessage(final String message) {
            m_receivedMessages.add(message);
        }

        /**
         * @return true if all warning messages registered in the constructor were received. Hereby, the order of the
         *         messages does not matter.
         */
        public boolean allRegisteredMessagesCalled() {
            return m_registeredMessages.stream().allMatch(message -> m_receivedMessages.contains(message));
        }

        private boolean noUnregisteredMessagesCalled() {
            return m_receivedMessages.stream().allMatch(message -> m_registeredMessages.contains(message));
        }

        /**
         * @return true if the set of received messages equals the expected messages.
         */
        public boolean receivedExactlyRegisteredMessages() {
            return this.allRegisteredMessagesCalled() && this.noUnregisteredMessagesCalled();
        }
    }

    /**
     * This class receives messages from the current {@link DataServiceContext} for testing the received warning
     * messages during testing the data service of a node
     *
     * @author Paul Bärnreuther
     */
    public static final class DataServiceContextWarningMessagesAsserter {

        private WarningMessageAsserter m_warningMessageAsserter;

        /**
         * @param messages the messages, the dataServiceContext should receive during the test.
         */
        public DataServiceContextWarningMessagesAsserter(final String... messages) {
            m_warningMessageAsserter = new WarningMessageAsserter(messages);
        }

        /**
         * @return true if all warning messages registered in the constructor were received. Hereby, the order of the
         *         messages does not matter.
         */
        public boolean allRegisteredMessagesCalled() {
            setReceivedMessages();
            return m_warningMessageAsserter.allRegisteredMessagesCalled();
        }

        /**
         * @return true if the set of received messages equals the expected messages.
         */
        public boolean receivedExactlyRegisteredMessages() {
            setReceivedMessages();
            return m_warningMessageAsserter.receivedExactlyRegisteredMessages();
        }

        private void setReceivedMessages() {
            final var dataServiceContext = DataServiceContext.getContext();
            Arrays.asList(dataServiceContext.getWarningMessages()).stream()
                .forEach(m_warningMessageAsserter::receiveMessage);
        }
    }

}
