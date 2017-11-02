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
 *   11.05.2017 (thor): created
 */
package org.knime.product.rcp;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.StatusLineLayoutData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

/**
 * Contribution for the status line that shows a warning that this is a nightly build.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
class NightlyBuildLabel extends ContributionItem {
    public NightlyBuildLabel() {
        super("knime.nightly.label");
    }

    @Override
    public void fill(final Composite parent) {
        Label sep = new Label(parent, SWT.SEPARATOR);
        Composite outer = new Composite(parent, SWT.NONE);

        GridLayout outerLayout = new GridLayout(1, false);
        outerLayout.marginHeight = 0;
        outerLayout.marginWidth = 0;
        outer.setLayout(outerLayout);

        Label label = new Label(outer, SWT.SHADOW_NONE);
        label.setText("    You are using a nightly build!    ");

        Font initialFont = label.getFont();
        FontData[] fontData = initialFont.getFontData();
        for (int i = 0; i < fontData.length; i++) {
            fontData[i].setHeight(13);
            fontData[i].setStyle(SWT.BOLD | SWT.ITALIC);
        }
        Font newFont = new Font(Display.getCurrent(), fontData);
        label.setFont(newFont);
        label.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
        outer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

        label.setToolTipText("If you encounter problems with this nightly build, please report them in our dedicated "
            + "forum section for nightly builds!");
        label.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

        // compute the size of the label to get the width hint for the contribution
        Point preferredSize = label.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        StatusLineLayoutData data = new StatusLineLayoutData();
        data.widthHint = preferredSize.x;
        data.heightHint = preferredSize.y;
        outer.setLayoutData(data);

        data = new StatusLineLayoutData();
        data.heightHint = preferredSize.y;
        sep.setLayoutData(data);
    }
}
