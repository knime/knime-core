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
 *   Oct 13, 2016 (hornm): created
 */
package org.knime.core.ui.wrapper;

import org.knime.core.node.AbstractNodeView.ViewableModel;
import org.knime.core.node.web.WebViewContent;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.workflow.action.InteractiveWebViewsResult;
import org.knime.core.ui.node.workflow.InteractiveWebViewsResultUI;

/**
 * UI-interface implementation that wraps a {@link InteractiveWebViewsResult}.
 *
 * @author Martin Horn, University of Konstanz
 * @param <T>
 * @param <REP>
 * @param <VAL>
 * @since 3.6
 */
public class InteractiveWebViewsResultWrapper<T extends ViewableModel & WizardNode<REP, VAL>, REP extends WebViewContent, VAL extends WebViewContent>
    extends AbstractWrapper<InteractiveWebViewsResult> implements InteractiveWebViewsResultUI<T, REP, VAL> {

    /**
     * @param delegate the implementation to delegate to
     */
    protected InteractiveWebViewsResultWrapper(final InteractiveWebViewsResult delegate) {
        super(delegate);
    }

    /**
     * Wraps the object via {@link Wrapper#wrapOrGet(Object, java.util.function.Function)}.
     *
     * @param delegate the object to be wrapped
     * @return a new wrapper or a already existing one
     */
    public static final <T extends ViewableModel & WizardNode<REP, VAL>, REP extends WebViewContent, VAL extends WebViewContent>
        InteractiveWebViewsResultWrapper<T, REP, VAL> wrap(final InteractiveWebViewsResult delegate) {
        return (InteractiveWebViewsResultWrapper<T, REP, VAL>)Wrapper.wrapOrGet(delegate,
            o -> new InteractiveWebViewsResultWrapper(o));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return unwrap().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SingleInteractiveWebViewResultUI<T, REP, VAL> get(final int index) {
        return SingleInteractiveWebViewResultWrapper.wrap(unwrap().get(index));
    }

}
