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
package org.knime.core.node.wizard;

import org.knime.core.node.dialog.ValueControlledNode;
import org.knime.core.node.interactive.InteractiveNode;
import org.knime.core.node.web.WebViewContent;

/**
 *
 * @author Christian Albrecht, KNIME AG, Zurich, Switzerland
 * @param <REP> The concrete class of the {@link WebViewContent} acting as representation of the view.
 * @param <VAL> The concrete class of the {@link WebViewContent} acting as value of the view.
 * @since 2.9
 */
public interface WizardNode<REP extends WebViewContent, VAL extends WebViewContent>
        extends InteractiveNode<REP, VAL>, ValueControlledNode, ViewHideable {

    /**
     * Create content which can be used by the web view implementation.
     * @return Content required for the web view.
     * @since 2.10
     */
    @Override
    public REP getViewRepresentation();

    /**
     * {@inheritDoc}
     * @since 2.10
     */
    @Override
    public VAL getViewValue();

    /**
     * @return an empty instance of the concrete {@link WebViewContent} implementation
     * @since 2.10
     */
    public REP createEmptyViewRepresentation();

    /**
     * @return an empty instance of the concrete {@link WebViewContent} implementation
     * @since 2.10
     */
    public VAL createEmptyViewValue();

    /**
     * @return The object id used in the javascript implementation of the view.
     */
    public String getJavascriptObjectID();

    /**
     * @return The path to the generated HTML containing the view or null if not applicable.
     * @since 2.11
     */
    public String getViewHTMLPath();

    /**
     * @return A view creator object, used to construct the web view.
     * @since 2.11
     */
    public WizardViewCreator<REP, VAL> getViewCreator();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHideInWizard();

    /**
     * {@inheritDoc}
     * @since 3.5
     */
    @Override
    public void setHideInWizard(final boolean hide);
}
