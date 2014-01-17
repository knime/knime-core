/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   29.08.2013 (thor): created
 */
package org.knime.testing.internal.diffcheckers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.NodeLogger;
import org.knime.testing.core.AbstractDifferenceChecker;
import org.knime.testing.core.DifferenceChecker;
import org.knime.testing.core.DifferenceCheckerFactory;

/**
 * Difference checker for contents of URIs.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
public class URIContentDifferenceChecker extends AbstractDifferenceChecker<URIDataValue> {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(URIContentDifferenceChecker.class);

    /**
     * Factory for the {@link URIContentDifferenceChecker}.
     */
    public static class Factory implements DifferenceCheckerFactory<URIDataValue> {
        /**
         * {@inheritDoc}
         */
        @Override
        public Class<URIDataValue> getType() {
            return URIDataValue.class;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDescription() {
            return DESCRIPTION;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DifferenceChecker<URIDataValue> newChecker() {
            return new URIContentDifferenceChecker();
        }
    }

    static final String DESCRIPTION = "URI contents";

    /**
     * {@inheritDoc}
     */
    @Override
    public Result check(final URIDataValue expected, final URIDataValue got) {
        try {
            return compare(expected, got);
        } catch (MalformedURLException ex) {
            LOGGER.error("Exception while comparing " + expected.getURIContent().getURI() + " and "
                    + got.getURIContent().getURI() + ": " + ex.getMessage(), ex);
            return new Result("Exception while comparing " + expected.getURIContent().getURI() + " and "
                    + got.getURIContent().getURI() + ": " + ex.getMessage());
        } catch (IOException ex) {
            LOGGER.error("Exception while comparing " + expected.getURIContent().getURI() + " and "
                    + got.getURIContent().getURI() + ": " + ex.getMessage(), ex);
            return new Result("Exception while comparing " + expected.getURIContent().getURI() + " and "
                    + got.getURIContent().getURI() + ": " + ex.getMessage());
        }
    }

    private Result compare(final URIDataValue expected, final URIDataValue got) throws MalformedURLException,
            IOException {
        final BufferedInputStream buffInA =
                new BufferedInputStream(expected.getURIContent().getURI().toURL().openStream(), 8192);
        final BufferedInputStream buffInB =
                new BufferedInputStream(got.getURIContent().getURI().toURL().openStream(), 8192);

        long readBytes = 0;
        while (true) {
            int read1 = buffInA.read();
            int read2 = buffInB.read();
            if (read1 == read2) {
                if (read1 == -1) {
                    break;
                }
            } else {
                if (read1 == -1) {
                    return new Result("Contents of '" + got.getURIContent().getURI() + "' are longer than expected");
                } else if (read2 == -1) {
                    return new Result("Contents of '" + got.getURIContent().getURI() + "' are shorter than expected");
                } else {
                    return new Result("Expected " + read1 + ", got " + read2 + " at byte position " + readBytes);
                }
            }
            readBytes++;
        }
        buffInA.close();
        buffInB.close();

        return OK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
