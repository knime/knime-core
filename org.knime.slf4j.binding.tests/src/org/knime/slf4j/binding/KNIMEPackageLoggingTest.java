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
 *   Oct 12, 2020 (wiswedel): created
 */
package org.knime.slf4j.binding;

import org.apache.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests logging for a "org.knime" based logger on all different log levels and all sorts of test methods
 * (slf4j has many).
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class KNIMEPackageLoggingTest {

    /**
     * Expect "org.knime.foo.Bar" loggers to log. Expect "some.other.Blah" not to log -- in KNIME AP
     * this is controlled via log4j.xml. See also <code>org.slf4j.impl.NodeLoggerLoggerFactory#getLogger(String)</code>.
     */
    @BeforeAll
    static void addOrgKnimeToLog4JManager() {
        LogManager.getLogger("org.knime");
    }

    /** A rule checking if message are received and correct. */
    @RegisterExtension
    ExpectedLogMessage m_expectedLogMessage = ExpectedLogMessage.newInstance();

    /** The logger to use. Can't be static since {@link #addOrgKnimeToLog4JManager()} must run first. */
    private Logger m_slf4jLogger;

    /** Inits the logger (package "org.knime.*"). */
    @BeforeEach
    void initLogger() {
        m_slf4jLogger = LoggerFactory.getLogger(KNIMEPackageLoggingTest.class);
    }

    /** Test, e.g. {@link Logger#debug(String)}. */
    @ParameterizedTest(name = "{0}")
    @EnumSource(TestLevel.class)
    void testMessageOnly(TestLevel testLevel) {
        String message = "Some simple message on " + testLevel.getNodeLoggerLevel().toString();
        m_expectedLogMessage.expect(testLevel.getNodeLoggerLevel(), message);
        testLevel.messageOnly().logMessage(m_slf4jLogger, message);
    }

    /** Test, e.g. {@link Logger#debug(String, Object)}. */
    @ParameterizedTest(name = "{0}")
    @EnumSource(TestLevel.class)
    void testMessageOneArg(TestLevel testLevel) {
        String level = testLevel.getNodeLoggerLevel().toString();
        String message = "Message with One Argument (Foo) on " + level;
        String messageRaw = "Message with One Argument ({}) on " + level;
        m_expectedLogMessage.expect(testLevel.getNodeLoggerLevel(), message);
        testLevel.messageOneArg().logMessage(m_slf4jLogger, messageRaw, "Foo");
    }

    /** Test, e.g. {@link Logger#debug(String, Object, Object)}. */
    @ParameterizedTest(name = "{0}")
    @EnumSource(TestLevel.class)
    void testMessageTwoArgs(TestLevel testLevel) {
        String level = testLevel.getNodeLoggerLevel().toString();
        String message = "Message with Two Arguments (Foo and Bar) on " + level;
        String messageRaw = "Message with Two Arguments ({} and {}) on " + level;
        m_expectedLogMessage.expect(testLevel.getNodeLoggerLevel(), message);
        testLevel.messageTwoArgs().logMessage(m_slf4jLogger, messageRaw, "Foo", "Bar");
    }

    /** Test, e.g. {@link Logger#debug(String, Object[])}. */
    @ParameterizedTest(name = "{0}")
    @EnumSource(TestLevel.class)
    void testMessageArrayArg(TestLevel testLevel) {
        String level = testLevel.getNodeLoggerLevel().toString();
        String message = "Message with Array Argument (Foo and Bar and FooBar) on " + level;
        String messageRaw = "Message with Array Argument ({} and {} and {}) on " + level;
        m_expectedLogMessage.expect(testLevel.getNodeLoggerLevel(), message);
        testLevel.messageArrayArg().logMessage(m_slf4jLogger, messageRaw, new String[] {"Foo", "Bar", "FooBar"});
    }

    /** Test, e.g. {@link Logger#debug(String, Throwable)}. */
    @ParameterizedTest(name = "{0}")
    @EnumSource(TestLevel.class)
    void testMessageAndException(TestLevel testLevel) {
        String message = "Message with Exception on " + testLevel.getNodeLoggerLevel().toString();
        Exception e = new RuntimeException("Throwable message: " + message);
        m_expectedLogMessage.expect(testLevel.getNodeLoggerLevel(), message, e);
        testLevel.messageAndException().logMessage(m_slf4jLogger, message, e);
    }

}