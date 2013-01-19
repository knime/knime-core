/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ------------------------------------------------------------------------
 * History
 *   Apr 14, 2011 (morent): created
 */

package org.knime.core.pmml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlOptions;
import org.dmg.pmml.PMMLDocument;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public final class PMMLValidator {
    private static final Log LOGGER = LogFactory.getLog(PMMLValidator.class);

    private static final String PMML_NAMESPACE_URI = "@"
        + PMMLDocument.type.getDocumentElementName().getNamespaceURI();

    private PMMLValidator() {
        // utility class with static methods only
    }

    /**
     * Validates a document against the schema and returns the number of
     * errors found.
     *
     * @param pmmlDocument the PMMML document
     * @return the error message or an em
     */
    public static Map<String, String> validatePMML(
            final PMMLDocument pmmlDocument) {
        XmlOptions validateOptions = new XmlOptions();
        List<XmlError> errorList = new ArrayList<XmlError>();
        validateOptions.setErrorListener(errorList);
        pmmlDocument.validate(validateOptions);
        Map<String, String> errorMessages = new TreeMap<String, String>();
        for (XmlError error : errorList) {
            String location = error.getCursorLocation().xmlText();
            if (location.length() > 50) {
                location = location.substring(0, 50) + "[...]";
            }
            String errorMessage = error.getMessage().replace(PMML_NAMESPACE_URI, "");
            LOGGER.error(location + ": " + errorMessage);
            errorMessages.put(location, errorMessage);
        }
        return errorMessages;
    }

}

