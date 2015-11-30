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
 *   Oct 7, 2015 (albrecht): created
 */
package org.knime.workbench.repository.view;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.repository.KNIMERepositoryPlugin;

/**
 * Toggle button the determines whether the fuzzy search modus (with node list) or default text search (with node tree)
 * should be used.
 *
 * @author Martin Horn, University of Konstanz
 */
class FuzzySearchAction extends Action {


    private Runnable m_callback;

    private static final IPreferenceStore PREF_STORE = KNIMERepositoryPlugin.getDefault().getPreferenceStore();

    private static final String P_FUZZY_SEARCH = "fuzzy_search";

    /**
     * @param callback call back if the button has been clicked
     *
     */
    public FuzzySearchAction(final Runnable callback) {
        setImageDescriptor(ImageRepository.getIconDescriptor(SharedImages.FuzzySearch));
        //this somehow magically turns it into a toggle button
        setChecked(false);

        //load state from preference store
        setChecked(PREF_STORE.getBoolean(P_FUZZY_SEARCH));
        m_callback = callback;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        m_callback.run();

        //store state in the preference store
        PREF_STORE.setValue(P_FUZZY_SEARCH, isChecked());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Fuzzy Search";
    }

}
