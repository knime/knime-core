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
 * ---------------------------------------------------------------------
 *
 * Created on 21.03.2013 by peter
 */
package org.knime.core.node.defaultnodesettings;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.DateInputDialog;

/**
 * StandardDialogComponent allowing the input of an user specified Date. Thereby the Date can be optional, as well as
 * the different fields of the date can be displayed as needed.
 *
 *
 * @author Sebastian Peter, University of Konstanz
 * @since 2.8
 */
public class DialogComponentDate extends DialogComponent {

    private DateInputDialog m_dialogcomp;

    private SettingsModelDate m_datemodel;

    /**
     * Instantiates a new DateDialogComponent, where the model stores the user input and the label is put as a
     * description on to the dialog. Using this constructor the date is optional.
     *
     * @param model to store the inputed date
     * @param label to place on the dialog
     */
    public DialogComponentDate(final SettingsModelDate model, final String label) {
        this(model, label, true);
    }

    /**
     * Instantiates a new DateDialogComponent, where the model stores the user input and the label is put as a
     * description on to the dialog. Using this constructor the date can be optional or mandatory.
     *
     *
     * @param model to store the inputed date
     * @param label to place on the dialog
     * @param optional specifies whether the date is optional (true) or mandatory (false)
     */
    public DialogComponentDate(final SettingsModelDate model, final String label, final boolean optional) {
        super(model);
        m_datemodel = model;
        m_dialogcomp = new DateInputDialog(DateInputDialog.Mode.SECONDS, optional);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), label));
        panel.add(m_dialogcomp);
        getComponentPanel().add(panel);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        m_dialogcomp.setDateAndMode(m_datemodel.getTimeInMillis(),
                                    DateInputDialog.getModeForStatus(m_datemodel.getSelectedFields()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        m_datemodel.setTimeInMillis(m_dialogcomp.getSelectedDate().getTime());
        m_datemodel.setSelectedFields(m_dialogcomp.getIntForStatus());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        //nothing todo here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_dialogcomp.setEnabled(enabled);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        //todo !?

    }
}
