package screenstudio.gui;

/*
 * $Id: ComboBoxCellEditor.java 3738 2010-07-27 13:56:28Z bierhance $
 * 
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle, Santa Clara, California 95054, U.S.A. All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;

public class ComboBoxCellEditor extends DefaultCellEditor {

    /**
     * Creates a new ComboBoxCellEditor.
     * 
     * @param comboBox the comboBox that should be used as the cell editor.
     */
    public ComboBoxCellEditor(final JComboBox comboBox) {
        super(comboBox);

        comboBox.removeActionListener(this.delegate);

        this.delegate = new EditorDelegate() {
            @Override
            public void setValue(final Object value) {
                comboBox.setSelectedItem(value);
            }

            @Override
            public Object getCellEditorValue() {
                return comboBox.getSelectedItem();
            }

            @Override
            public boolean shouldSelectCell(final EventObject anEvent) {
                if (anEvent instanceof MouseEvent) {
                    final MouseEvent e = (MouseEvent) anEvent;
                    return e.getID() != MouseEvent.MOUSE_DRAGGED;
                }
                return true;
            }

            @Override
            public boolean stopCellEditing() {
                if (comboBox.isEditable()) {
                    // Commit edited value.
                    comboBox.actionPerformed(new ActionEvent(ComboBoxCellEditor.this, 0, ""));
                }
                return super.stopCellEditing();
            }

            @Override
            public void actionPerformed(final ActionEvent e) {
                ComboBoxCellEditor.this.stopCellEditing();
            }
        };
        comboBox.addActionListener(this.delegate);
    }
}
