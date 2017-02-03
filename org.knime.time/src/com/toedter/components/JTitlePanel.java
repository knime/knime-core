/*
 *  Copyright (C) 2004 Kai Toedter
 *  kai@toedter.com
 *  www.toedter.com
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package com.toedter.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;


/**
 * A simple JPanel with a border and a title
 *
 * @author Kai Toedter
 * @version $LastChangedRevision$
 * @version $LastChangedDate$
 */
public class JTitlePanel extends JPanel {
	private static final long serialVersionUID = 9104873267039717087L;
	protected JPanel northPanel;
    protected JLabel label;
    
    /**
     * Constructs a titled panel.
     *
     * @param title the title
     * @param content the JComponent that contains the content
     * @param outerBorder the outer border
     */
    public JTitlePanel(String title, Icon icon, JComponent content, Border outerBorder) {
        setLayout(new BorderLayout());

        label = new JLabel(title, icon, JLabel.LEADING);
        label.setForeground(Color.WHITE);

        GradientPanel titlePanel = new GradientPanel(Color.BLACK);
        titlePanel.setLayout(new BorderLayout());
        titlePanel.add(label, BorderLayout.WEST);
        int borderOffset = 2;
        if(icon == null) {
        	borderOffset += 1;
        }
        titlePanel.setBorder(BorderFactory.createEmptyBorder(borderOffset, 4, borderOffset, 1));
        add(titlePanel, BorderLayout.NORTH);
        
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BorderLayout());
        northPanel.add(content,BorderLayout.NORTH);
        northPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(northPanel, BorderLayout.CENTER);

        if (outerBorder == null) {
            setBorder(BorderFactory.createLineBorder(Color.GRAY));
        } else {
            setBorder(BorderFactory.createCompoundBorder(outerBorder,
                    BorderFactory.createLineBorder(Color.GRAY)));
        }
    }
  
    public void setTitle(String label, Icon icon) {
    	this.label.setText(label);
    	this.label.setIcon(icon);
    }
    
    private static class GradientPanel extends JPanel {
		private static final long serialVersionUID = -6385751027379193053L;

		private GradientPanel(Color background) {
            setBackground(background);
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (isOpaque()) {
                // Color controlColor = UIManager.getColor("control");
                Color controlColor = new Color(165, 201, 215);
                // Color controlColor = new Color(99, 153, 255);
                int width = getWidth();
                int height = getHeight();

                Graphics2D g2 = (Graphics2D) g;
                Paint oldPaint = g2.getPaint();
                g2.setPaint(new GradientPaint(0, 0, getBackground(), width, 0,
                        controlColor));
                g2.fillRect(0, 0, width, height);
                g2.setPaint(oldPaint);
            }
        }
    }
}
