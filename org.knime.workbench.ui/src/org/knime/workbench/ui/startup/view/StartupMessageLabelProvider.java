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
 *   21.07.2014 (thor): created
 */
package org.knime.workbench.ui.startup.view;

import java.util.Dictionary;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.ui.startup.StartupMessage;

/**
 * Label provider for the startup messages view.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class StartupMessageLabelProvider implements ITableLabelProvider {
    /**
     * {@inheritDoc}
     */
    @Override
    public void addListener(final ILabelProviderListener listener) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLabelProperty(final Object element, final String property) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListener(final ILabelProviderListener listener) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Image getColumnImage(final Object element, final int columnIndex) {
        if ((element instanceof StartupMessage) && (columnIndex == 0)) {
            switch (((StartupMessage) element).getType()) {
                case StartupMessage.ERROR:
                    return ImageRepository.getImage(SharedImages.Error);
                case StartupMessage.WARNING:
                    return ImageRepository.getImage(SharedImages.Warning);
                case StartupMessage.INFO:
                    return ImageRepository.getImage(SharedImages.Info);
                default:
                    return null;
            }
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnText(final Object element, final int columnIndex) {
        if (element instanceof StartupMessage) {
            switch (columnIndex) {
                case 0:
                    return ((StartupMessage) element).getMessage();
                case 1:
                    Dictionary<String, String> headers = ((StartupMessage) element).getBundle().getHeaders();
                    return headers.get("Bundle-Name");
                default:
                    return "";
            }
        } else {
            return "";
        }
    }
}
