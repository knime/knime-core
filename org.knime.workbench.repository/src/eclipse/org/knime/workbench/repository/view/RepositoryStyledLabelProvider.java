/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   21.08.2014 (Marcel Hanser): created
 */
package org.knime.workbench.repository.view;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.knime.core.eclipseUtil.OSGIHelper;
import org.knime.core.node.NodeFactory;
import org.knime.workbench.repository.model.AbstractRepositoryObject;
import org.knime.workbench.repository.model.IContainerObject;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.NodeTemplate;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * Adds additional information to a KNIME repository object. I.e. its adds the Category path as a grey string to the
 * node name. Also it adds a tool-tip containing the vendor and the bundle name to a node item. .
 *
 * @author Marcel Hanser
 */
final class RepositoryStyledLabelProvider extends StyledCellLabelProvider implements ILabelProvider {

    private RepositoryLabelProvider m_provider;

    private boolean m_appendCategory;

    private String[] m_appendAdditionalInfoKeys;

    /**
     *
     * @param provider the usual {@link RepositoryLabelProvider} which provides the node name and the icon
     * @param appendCategory if the category should be appended additionally (in a lighter color)
     * @param the keys of the additional information to be appended. If the additional information is not available (see
     *            {@link AbstractRepositoryObject#getAdditionalInfo(String)}), it won't be appended.
     */
    RepositoryStyledLabelProvider(final RepositoryLabelProvider provider, final boolean appendCategory,
        final String... appendAdditionalInfoKeys) {
        super();
        this.m_provider = provider;
        m_appendCategory = appendCategory;
        m_appendAdditionalInfoKeys = appendAdditionalInfoKeys;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText(final Object element) {
        String vendorAndBundle = getVendorAndBundle(element);
        if (!vendorAndBundle.isEmpty()) {
            return vendorAndBundle;
        }
        return super.getToolTipText(element);
    }

    @Override
    public void update(final ViewerCell cell) {
        Object obj = cell.getElement();
        if (obj instanceof IRepositoryObject) {

            StyledString styledString = new StyledString(m_provider.getText(obj));

            if (m_appendCategory) {
                //receive the category string.
                String categoryString = getCategoryString((IRepositoryObject)obj);
                if (!categoryString.isEmpty()) {
                    styledString.append(String.format("\t - %s", categoryString), StyledString.DECORATIONS_STYLER);
                }
            }
            if (m_appendAdditionalInfoKeys.length > 0) {
                if (obj instanceof AbstractRepositoryObject) {
                    AbstractRepositoryObject aro = (AbstractRepositoryObject)obj;
                    for (String key : m_appendAdditionalInfoKeys) {
                        if (aro.getAdditionalInfo(key) != null) {
                            styledString.append(
                                String.format("\t [%s]", ((AbstractRepositoryObject)obj).getAdditionalInfo(key)),
                                StyledString.QUALIFIER_STYLER);
                        }
                    }
                }
            }
            cell.setText(styledString.toString());
            cell.setStyleRanges(styledString.getStyleRanges());
        } else {
            cell.setText(m_provider.getText(obj));
        }

        cell.setImage(m_provider.getImage(obj));
    }

    /**
     * @param robj
     * @return
     */
    private static String getVendorAndBundle(final Object robj) {
        StringBuilder toReturn = new StringBuilder();
        if (robj instanceof NodeTemplate) {
            @SuppressWarnings("rawtypes")
            final Class<? extends NodeFactory> facClass = ((NodeTemplate)robj).getFactory();
            Bundle bundle = OSGIHelper.getBundle(facClass);
            if (bundle != null) {
                toReturn.append("Vendor: ");
                toReturn.append(bundle.getHeaders().get(Constants.BUNDLE_VENDOR));
                toReturn.append(" - ");
            }
            ((NodeTemplate)robj).getContributingPlugin();
        }
        if (robj instanceof IRepositoryObject) {
            toReturn.append("Plugin: ");
            toReturn.append(((IRepositoryObject)robj).getContributingPlugin());
        }
        return toReturn.toString();
    }

    private static String getCategoryString(final IRepositoryObject obj) {
        StringBuilder builder = new StringBuilder();

        IContainerObject curr = obj.getParent();
        while (curr != null && curr.getParent() != null) {
            builder.insert(0, "/" + curr.getName());
            curr = curr.getParent();
        }

        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Image getImage(final Object element) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText(final Object element) {
        if (element instanceof IRepositoryObject) {
            return ((IRepositoryObject)element).getName();
        }
        return element.toString();
    }
}
