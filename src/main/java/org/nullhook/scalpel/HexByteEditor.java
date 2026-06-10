package org.nullhook.scalpel;

import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;

final class HexByteEditor extends DefaultCellEditor {
    private final JTextField field;

    HexByteEditor(JTextField field) {
        super(field);
        this.field = field;
        setClickCountToStart(1);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int row, int column) {
        Component c = super.getTableCellEditorComponent(table, value, selected, row, column);
        field.selectAll();
        return c;
    }
}
