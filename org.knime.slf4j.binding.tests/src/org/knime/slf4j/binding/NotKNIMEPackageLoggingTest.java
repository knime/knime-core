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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.knime.core.node.NodeLogger.LEVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests logging for a third party package (not expected to be logged in NodeLogger unless explicitly added).
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
public final class NotKNIMEPackageLoggingTest {

    /** A rule checking if message are received and correct. */
    @RegisterExtension
    ExpectedLogMessage m_expectedLogMessage = ExpectedLogMessage.newInstance();

    /** Test if errors from 3rd party packages are swallowed. */
    @Test
    void testMessageNotReceived() {
        String message = "Some simple message on ERROR that is not expected on log output";
        m_expectedLogMessage.expectNone();
        Logger logger = LoggerFactory.getLogger("some.third.party.package.Object");
        logger.error(message);
    }

    /** Test if errors from 3rd party packages are logged IF their logger was previously added
     * (usually through log4j.xml). */
    @Test
    void testMessageReceived() {
        String message = "Some simple message on ERROR that is expected on log output";
        m_expectedLogMessage.expect(LEVEL.ERROR, message);
        LogManager.getLogger("some.fourth"); // this is done by the user by a line in log4j.xml
        Logger logger = LoggerFactory.getLogger("some.fourth.party.package.Object");
        logger.error(message);
    }

}