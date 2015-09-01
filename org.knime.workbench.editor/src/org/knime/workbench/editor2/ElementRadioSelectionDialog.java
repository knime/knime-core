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
 *   Aug 28, 2015 (albrecht): created
 */
package org.knime.workbench.editor2;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 *
 * @author Christian Albrecht, KNIME.com AG, Zurich, Switzerland
 */
public class ElementRadioSelectionDialog extends Dialog {

    private String m_title;
    private String m_message = ""; //$NON-NLS-1$
    private RadioItem[] m_selectElements;
    private RadioItem m_initialSelectElement;
    private Button[] m_radioButtons;

    private RadioItem m_selectedElement;

    private int m_width = 60;
    private int m_height = 13;

    /**
     * @param parent
     */
    public ElementRadioSelectionDialog(final Shell parent) {
        super(parent);
        //setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
    }

    /**
     * Sets the radio button area size in unit of characters.
     * @param width  the width of the list.
     * @param height the height of the list.
     */
    public void setSize(final int width, final int height) {
        m_width = width;
        m_height = height;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(final String title) {
        m_title = title;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(final String message) {
        m_message = message;
    }

    /**
     * {@inheritDoc}
     */
    protected void setSelection(final RadioItem selection) {
        Assert.isNotNull(m_radioButtons);
        for (int i = 0; i < m_selectElements.length; i++) {
            if (m_selectElements[i].equals(selection)) {
                m_radioButtons[i].setSelection(true);
                m_selectedElement = m_selectElements[i];
                return;
            }
        }
    }

    @Override
    protected void configureShell(final Shell shell) {
        super.configureShell(shell);
        if (m_title != null) {
            shell.setText(m_title);
        }
    }

    /**
     * Creates the message area for this dialog.
     * <p>
     * This method is provided to allow subclasses to decide where the message
     * will appear on the screen.
     * </p>
     *
     * @param composite
     *            the parent composite
     * @return the message label
     */
    protected Label createMessageArea(final Composite composite) {
        Label label = new Label(composite, SWT.NONE);
        if (m_message != null) {
            label.setText(m_message);
        }
        label.setFont(composite.getFont());
        return label;
    }

    /**
     * {@inheritDoc}
     */
    public RadioItem getSelectedElement() {
        /*Assert.isNotNull(m_radioButtons);
        for (int i = 0; i < m_radioButtons.length; i++) {
            if (m_radioButtons[i].getSelection()) {
                return m_selectElements[i];
            }
        }
        return null;*/
        return m_selectedElement;
    }

    /**
     * Creates the radio buttons and labels.
     * @param parent the parent composite.
     * @return returns the composite containing the radio buttons
     */
    protected Composite createRadioList(final Composite parent) {
        Composite c = new Composite(parent, SWT.FILL);

        GridData data = new GridData();
        data.widthHint = convertWidthInCharsToPixels(m_width);
        data.heightHint = convertHeightInCharsToPixels(m_height);
        data.grabExcessVerticalSpace = true;
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.FILL;
        c.setLayoutData(data);
        GridLayout layout = new GridLayout(1, true);
        layout.marginTop = 10;
        c.setLayout(layout);
        c.setFont(parent.getFont());

        m_radioButtons = new Button[m_selectElements.length];
        for (int i = 0; i < m_selectElements.length; i++) {
            final RadioItem item = m_selectElements[i];
            Button radio = new Button(c, SWT.RADIO);
            radio.setText(item.getTitle());
            if (item.getTooltip() != null) {
                radio.setToolTipText(item.getTooltip());
            }
            radio.addSelectionListener(new SelectionAdapter() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    m_selectedElement = item;
                }
            });
            if (item.getDescription() != null) {
                Label description = new Label(c, SWT.WRAP);
                description.setLayoutData(new GridData(data.widthHint, convertHeightInCharsToPixels(2)));
                FontData[] fD = c.getFont().getFontData();
                fD[0].setHeight(Math.max(fD[0].getHeight()-2, 6));
                description.setFont(new Font(parent.getDisplay(), fD[0]));
                description.setText(item.getDescription());
                if (item.getTooltip() != null) {
                    description.setToolTipText(item.getTooltip());
                }
            }
            m_radioButtons[i] = radio;
        }
        return c;
    }

    /**
     * Sets the elements of the radio button list.
     * @param elements the elements of the list.
     */
    public void setElements(final RadioItem[] elements) {
        m_selectElements = elements;
    }

    /**
     * Sets the element initially selected.
     * @param element the element
     */
    public void setInitialSelectedElement(final RadioItem element) {
        m_initialSelectElement = element;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite contents = (Composite) super.createDialogArea(parent);

        if (m_selectElements == null) {
            m_selectElements = new RadioItem[0];
        }

        createMessageArea(contents);
        createRadioList(contents);

        setSelection(m_initialSelectElement);

        return contents;
    }

    /**
     * Container class for radio select items.
     */
    public static class RadioItem {

        private String m_title;
        private String m_tooltip;
        private String m_description;

        /** Creates radio item
         * @param title
         * @param tooltip
         * @param description
         *
         */
        public RadioItem(final String title, final String tooltip, final String description) {
            m_title = title;
            m_tooltip = tooltip;
            m_description = description;
        }

        /**
         * @return the title
         */
        public String getTitle() {
            return m_title;
        }

        /**
         * @param title the title to set
         */
        public void setTitle(final String title) {
            m_title = title;
        }

        /**
         * @return the tooltip
         */
        public String getTooltip() {
            return m_tooltip;
        }

        /**
         * @param tooltip the tooltip to set
         */
        public void setTooltip(final String tooltip) {
            m_tooltip = tooltip;
        }

        /**
         * @return the description
         */
        public String getDescription() {
            return m_description;
        }

        /**
         * @param description the description to set
         */
        public void setDescription(final String description) {
            m_description = description;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((m_description == null) ? 0 : m_description.hashCode());
            result = prime * result + ((m_title == null) ? 0 : m_title.hashCode());
            result = prime * result + ((m_tooltip == null) ? 0 : m_tooltip.hashCode());
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            RadioItem other = (RadioItem)obj;
            if (m_description == null) {
                if (other.m_description != null) {
                    return false;
                }
            } else if (!m_description.equals(other.m_description)) {
                return false;
            }
            if (m_title == null) {
                if (other.m_title != null) {
                    return false;
                }
            } else if (!m_title.equals(other.m_title)) {
                return false;
            }
            if (m_tooltip == null) {
                if (other.m_tooltip != null) {
                    return false;
                }
            } else if (!m_tooltip.equals(other.m_tooltip)) {
                return false;
            }
            return true;
        }


    }

}
