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
 *   09.10.2015 (ohl): created
 */
package org.knime.workbench.editor2.directannotationedit;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;

/**
 *
 * @author ohl
 */
public class BorderStyleDialog extends Dialog {

    private final Color m_defColor;

    private final int m_defBorderSize;

    private int m_size;

    private RGB m_color;

    /**
     *
     */
    public BorderStyleDialog(final Shell parentShell, final Color defColor, final int defSize) {
        super(parentShell);
        if (defColor == null) {
            m_defColor = AnnotationEditPart.getAnnotationDefaultBorderColor();
        } else {
            m_defColor = defColor;
        }
        m_defBorderSize = defSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite overall = new Composite(parent, SWT.NONE);
        GridData fillBoth = new GridData(GridData.FILL_BOTH);
        overall.setLayoutData(fillBoth);
        overall.setLayout(new GridLayout(1, true));

        createHeader(overall);
        createColorSelector(overall);
        createSizeSelector(overall);

        return overall;
    }

    private void createColorSelector(final Composite parent) {
        Composite panel = new Composite(parent, SWT.FILL);
        GridData gData = new GridData(GridData.FILL_HORIZONTAL);
        panel.setLayoutData(gData);
        panel.setLayout(new GridLayout(2, false));

        Label msg = new Label(panel, SWT.LEFT);
        msg.setText("Border color (click to change):");
        msg.setLayoutData(gData);

        final ColorSelector sel = new ColorSelector(panel);
        sel.setColorValue(m_defColor.getRGB());
        sel.addListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                m_color = sel.getColorValue();
            }
        });
        m_color = m_defColor.getRGB();
    }

    private void createSizeSelector(final Composite parent) {
        Composite panel = new Composite(parent, SWT.FILL);
        GridData gData = new GridData(GridData.FILL_HORIZONTAL);
        panel.setLayoutData(gData);
        panel.setLayout(new GridLayout(2, false));

        Label msg = new Label(panel, SWT.LEFT);
        msg.setText("Border size (0 = no border):");
        msg.setLayoutData(gData);

        final Spinner spinner = new Spinner(panel, SWT.NONE);
        spinner.setDigits(3);
        spinner.setValues(m_defBorderSize, 0, 100, 0, 1, 5);
        spinner.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                m_size = spinner.getSelection();
            }
        });
        m_size = m_defBorderSize;
    }

    /**
     * Creates the header composite.
     *
     * @param parent the parent composite
     */
    protected void createHeader(final Composite parent) {
        Composite header = new Composite(parent, SWT.FILL);
        Color white = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
        header.setBackground(white);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        header.setLayoutData(gridData);
        header.setLayout(new GridLayout(2, false));
        Label exec = new Label(header, SWT.NONE);
        exec.setBackground(white);
        exec.setText("Annotation Border Style");
        FontData[] fd = parent.getFont().getFontData();
        for (FontData f : fd) {
            f.setStyle(SWT.BOLD);
            f.setHeight(f.getHeight() + 2);
        }
        exec.setFont(new Font(parent.getDisplay(), fd));
        exec.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        Label mkdirIcon = new Label(header, SWT.NONE);
        mkdirIcon.setBackground(white);
        Label txt = new Label(header, SWT.NONE);
        txt.setBackground(white);
        txt.setText("Please set the style of the border");
        txt.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
    }

    public RGB getColor() {
        return m_color;
    }

    public int getSize() {
        return m_size;
    }

}
