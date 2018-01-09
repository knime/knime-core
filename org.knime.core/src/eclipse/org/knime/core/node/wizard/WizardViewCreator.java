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
 *   21.11.2014 (Christian Albrecht, KNIME AG, Zurich, Switzerland): created
 */
package org.knime.core.node.wizard;

import java.io.IOException;
import java.nio.file.Path;

import org.knime.core.node.web.WebTemplate;
import org.knime.core.node.web.WebViewContent;

/**
 *
 * @author Christian Albrecht, KNIME AG, Zurich, Switzerland
 * @param <REP> the {@link WebViewContent} implementation used as view representation
 * @param <VAL> the {@link WebViewContent} implementation used as view value
 * @since 2.11
 */
public interface WizardViewCreator<REP extends WebViewContent, VAL extends WebViewContent> {

    /**
     * @return The {@link WebTemplate} object created by the view creator.
     */
    public WebTemplate getWebTemplate();

    /**
     * Creates all web resources, returns path to the created HTML file which contains the JS view.
     *
     * @param viewRepresentation the view representation
     * @param viewValue the view value
     * @param viewTitle the view title
     * @return the path to the view HTML
     * @throws IOException on IO error
     */
    public String createWebResources(final String viewTitle, final REP viewRepresentation, final VAL viewValue)
            throws IOException;

    /**
     * Creates the JavaScript code to initialize the view implementation with the respective view representation and
     * value objects.
     *
     * @param parseArguments true, if a script to parse the given representation and value is supposed to be included.
     *            If false, representation and value can be given as null and need to be parsed before the resulting
     *            init call with the variables 'parsedRepresentation' and 'parsedValue' present, if applicable.
     * @param viewRepresentation The view representation.
     * @param viewValue The view value.
     * @return The JavaScript code to initialize the view.
     * @since 3.4
     */
    public String createInitJSViewMethodCall(final boolean parseArguments, final REP viewRepresentation, final VAL viewValue);

    /**
     * Creates the JavaScript code to initialize the view implementation with the respective
     * view representation and value objects.
     *
     * @param viewRepresentation The view representation.
     * @param viewValue The view value.
     * @return The JavaScript code to initialize the view.
     */
    public default String createInitJSViewMethodCall(final REP viewRepresentation, final VAL viewValue) {
        return createInitJSViewMethodCall(true, viewRepresentation, viewValue);
    }

    /**
     * Serializes a given view representation into a JSON string
     * @param viewRepresentation the representation to serialize
     * @return the serialized JSON string
     * @throws IllegalArgumentException on serialization error
     * @since 3.4
     */
    public String getViewRepresentationJSONString(final REP viewRepresentation);

    /**
     * Serializes a given view value into a JSON string
     * @param viewValue the value to serialize
     * @return the serialized JSON string
     * @throws IllegalArgumentException on serialization error
     * @since 3.4
     */
    public String getViewValueJSONString(final VAL viewValue);

    /**
     * @return The namespace prefix for all method calls of the respective view implementation.
     */
    public String getNamespacePrefix();

    /**
     * Creates a minimal HTML string to display a message.
     * @param message The message to display.
     * @return The created HTML string
     */
    public String createMessageHTML(final String message);

    /**
     * Wraps a JavaScript code block in a try/catch block.
     * In the catch block an alert with the error message and stack trace is shown.
     * @param jsCode The code block to wrap.
     * @return The resulting JavaScript as string.
     */
    public String wrapInTryCatch(final String jsCode);

    /**
     * Returns the current location where web resources are held.
     * @return the location, may be null
     * @since 3.4
     */
    public Path getCurrentLocation();

}
