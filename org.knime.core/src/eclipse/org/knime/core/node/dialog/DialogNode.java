/*
 * ------------------------------------------------------------------------
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
 * Created on 08.10.2013 by Christian Albrecht, KNIME AG, Zurich, Switzerland
 */
package org.knime.core.node.dialog;

import java.util.regex.Pattern;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;

import org.knime.core.node.InvalidSettingsException;


/**
 * A node that contributes to a component configuration dialog.
 *
 * @author Christian Albrecht, KNIME AG, Zurich, Switzerland
 * @param <REP> The configuration content of the dialog node.
 * @param <VAL> The node value implementation of the dialog node.
 * @since 2.9
 */
public interface DialogNode<REP extends DialogNodeRepresentation<VAL>, VAL extends DialogNodeValue>
        extends MetaNodeDialogNode, ValueControlledNode {

    /** Pattern used for {@link #getParameterName() parameter name} validation. Must only consist of word characters or
     * dashes - no spaces no special characters. Name must start with a letter, then it may contain any word character
     * (including '-' and '_') and ends with a word character (no '-' or '_'), for instance:<br>
     * <pre>
     * "abc"       - OK
     * "abc-d0-hi" - OK
     * "foo-bar-0" - NOT OK
     * "013"       - NOT OK
     * "foo-_bar"  - NOT OK
     * "foo-bar-"  - NOT OK
     * </pre>
     * @since 2.12
     */
    // last segment is a "lookbehind" to disallow the string to end with a digit
    public static final Pattern PARAMETER_NAME_PATTERN = Pattern.compile("^[a-zA-Z](?:[-_]?[a-zA-Z0-9]+)*(?<![0-9])$");

    /** Similar to {@link #PARAMETER_NAME_PATTERN} but allowing a digit at the end. Left here to be able to load
     * old workflows (prior KNIME AP 3.5), which may use this legacy format.
     * @noreference This field is not intended to be referenced by clients.
     * @since 3.5 */
    public static final Pattern PARAMETER_NAME_PATTERN_LEGACY = Pattern.compile("^[a-zA-Z](?:[-_]?[a-zA-Z0-9]+)*$");

    /**
     * Input verifier for swing component, such as text field, that checks against {@link #PARAMETER_NAME_PATTERN}.
     *
     * @since 2.12
     */
    public static final InputVerifier PARAMETER_NAME_VERIFIER = new InputVerifier() {
        @Override
        public boolean verify(final JComponent input) {
           if (input instanceof JTextComponent) {
              JTextComponent textComponent = (JTextComponent)input;
              String text = textComponent.getText();
              return DialogNode.PARAMETER_NAME_PATTERN.matcher(text).matches();
           }
           return true;
        }

        @Override
        public boolean shouldYieldFocus(final JComponent input) {
           return verify(input);
        }
    };

    /**
     * @return The representation content of the dialog node.
     * @since 2.10
     */
    public REP getDialogRepresentation();

    /** Used by the framework to create an empty and uninitialized dialog value instance. This is then kept,
     * for instance in the subnode with updated user selected values. (Load &amp; Save will be called to fill content.)
     * @return A new empty instance of the dialog value class, not null. */
    public VAL createEmptyDialogValue();

    /** Loads a value edited outside the node into the node. 'Outside' here refers to the dialog of a sub- or metanode.
     * @param value A value whose content should be used in the next calls of configure and execute. May be null
     * to fall back to the defaults (as per configuration). */
    public void setDialogValue(VAL value);

    /** Get the default dialog value (use defaults as per node configuration).
     * @return The default value.
     *
     * @since 2.12
     */
    public VAL getDefaultValue();

    /** Get the currently set dialog value or null if non is set (use defaults as per node configuration).
     * @return The value currently set. */
    public VAL getDialogValue();

    /** Called prior setting a new dialog value. Implements can make sanity checks on the argument. Calling this
     * method does not change the state/value set in the node.
     * @param value The validate - not null.
     * @throws InvalidSettingsException If invalid.
     * @since 2.12
     */
    public void validateDialogValue(final VAL value) throws InvalidSettingsException;

    /** A simple name that is associated with this node for external parameterization. This is for instance used
     * in command line control or when parameters are set via a web service invocation (that is, the workflow itself
     * is the web service implementation). The returned value must not be null. An empty string is discouraged and
     * only used for backward compatibility reasons (workflows saved prior 2.12 do not have this property).
     * @return Parameter name used for external parameterization. Result must match {@link #PARAMETER_NAME_PATTERN}
     * @since 2.12
     */
    public String getParameterName();

    /** Property set in the configuration dialog of the node to hide this quickform/dialog node in the
     * meta or subnode dialog.
     * @return that property. */
    public boolean isHideInDialog();

    /** Property set in the configuration dialog of the node to hide this quickform/dialog node in the
     * meta or subnode dialog
     * @param hide true, if node is supposed to be ignored for dialog, false otherwise
     * @since 3.5
     */
    public void setHideInDialog(final boolean hide);

}
