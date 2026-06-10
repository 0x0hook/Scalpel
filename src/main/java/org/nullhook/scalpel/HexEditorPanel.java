package org.nullhook.scalpel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressFormatException;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.MemoryAccessException;

final class HexEditorPanel extends JPanel implements HexPatchSink {
    private static final Color BG = new Color(0x0b0f12);
    private static final Color PANEL = new Color(0x11171b);
    private static final Color GRID = new Color(0x263139);
    private static final Color TEXT = new Color(0xd8f3e8);
    private static final Color MUTED = new Color(0x71838c);
    private static final Color ACCENT = new Color(0x49f2a4);
    private static final Color CYAN = new Color(0x65d8ff);
    private static final Color WARN = new Color(0xffc857);
    private static final Color BAD = new Color(0xff6b7a);

    private final HexTableModel model;
    private final JTable table;
    private final JTextField addressField;
    private final JTextField searchField;
    private final JComboBox<Integer> lengthBox;
    private final JCheckBox followCursor;
    private final JLabel title;
    private final JLabel status;

    private Program program;
    private Address lastCursor;
    private int searchCursor;

    HexEditorPanel() {
        super(new BorderLayout());
        model = new HexTableModel(this);
        table = new JTable(model);
        addressField = field(22);
        searchField = field(18);
        lengthBox = new JComboBox<>(new Integer[] {256, 1024, 4096, 16384, 65536});
        lengthBox.setSelectedItem(4096);
        followCursor = new JCheckBox("follow");
        title = new JLabel("$ scalpel");
        status = new JLabel("open a program");

        build();
    }

    void setProgram(Program program) {
        this.program = program;
        String name = program == null ? "no program" : program.getName();
        title.setText("$ scalpel --target " + name);
        status(program == null ? "open a program" : "ready", false);
        if (program == null) {
            model.load(null, null, 0);
            addressField.setText("");
            return;
        }
        Address start = program.getImageBase();
        lastCursor = start;
        load(start);
    }

    void follow(Address address) {
        lastCursor = address;
        if (address != null && followCursor.isSelected()) {
            load(address);
        }
    }

    void dispose() {
        program = null;
    }

    @Override
    public HexPatchResult patch(Address address, int value) {
        if (program == null || address == null) {
            return HexPatchResult.fail("no writable program");
        }
        int tx = program.startTransaction("Scalpel patch");
        boolean commit = false;
        try {
            program.getMemory().setByte(address, (byte) value);
            commit = true;
            status("patched " + address + " = " + String.format(Locale.ROOT, "%02X", value), false);
            return HexPatchResult.ok("patched");
        } catch (MemoryAccessException e) {
            status("patch failed: " + e.getMessage(), true);
            return HexPatchResult.fail(e.getMessage());
        } finally {
            program.endTransaction(tx, commit);
        }
    }

    private void build() {
        setBackground(BG);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GRID));

        JToolBar top = new JToolBar();
        top.setFloatable(false);
        top.setBackground(BG);
        top.setBorder(new EmptyBorder(8, 10, 8, 10));

        title.setForeground(ACCENT);
        title.setFont(mono(Font.BOLD, 13));
        top.add(title);
        top.addSeparator(new Dimension(18, 1));
        top.add(label("addr"));
        top.add(addressField);
        top.add(button("go", this::go));
        top.add(button("cursor", e -> {
            if (lastCursor != null) {
                load(lastCursor);
            }
        }));
        top.addSeparator(new Dimension(10, 1));
        top.add(label("len"));
        styleCombo(lengthBox);
        top.add(lengthBox);
        top.add(button("reload", e -> reload()));
        styleCheck(followCursor);
        top.add(followCursor);
        top.addSeparator(new Dimension(10, 1));
        top.add(label("find"));
        top.add(searchField);
        top.add(button("next", this::findNext));
        top.add(button("hex", e -> copyHex()));
        top.add(button("ascii", e -> copyAscii()));

        add(top, BorderLayout.NORTH);

        table.setFont(mono(Font.PLAIN, 13));
        table.setRowHeight(24);
        table.setBackground(PANEL);
        table.setForeground(TEXT);
        table.setSelectionBackground(new Color(0x173a34));
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(GRID);
        table.setShowGrid(true);
        table.setFillsViewportHeight(true);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setDefaultRenderer(Object.class, new HexCellRenderer());
        table.setDefaultEditor(Object.class, new HexByteEditor(field(2)));
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke("control C"), "copyHex");
        table.getActionMap().put("copyHex", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyHex();
            }
        });

        JTableHeader header = table.getTableHeader();
        header.setFont(mono(Font.BOLD, 12));
        header.setBackground(new Color(0x0d1317));
        header.setForeground(CYAN);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GRID));
        sizeColumns();

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(BG);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, GRID));
        add(scroll, BorderLayout.CENTER);

        status.setFont(mono(Font.PLAIN, 12));
        status.setForeground(MUTED);
        status.setBorder(new EmptyBorder(7, 10, 7, 10));
        status.setBackground(BG);
        status.setOpaque(true);
        add(status, BorderLayout.SOUTH);
    }

    private void sizeColumns() {
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            TableColumn col = table.getColumnModel().getColumn(i);
            if (i == 0) {
                col.setPreferredWidth(145);
            } else if (i == HexTableModel.BYTES_PER_ROW + 1) {
                col.setPreferredWidth(150);
                col.setMinWidth(150);
            } else {
                col.setPreferredWidth(38);
                col.setMinWidth(38);
                col.setMaxWidth(46);
            }
        }
    }

    private void go(ActionEvent event) {
        if (program == null) {
            return;
        }
        try {
            Address address = program.getAddressFactory().getAddress(addressField.getText().trim());
            if (address == null) {
                throw new AddressFormatException("invalid address");
            }
            load(address);
        } catch (AddressFormatException e) {
            status("bad address", true);
        }
    }

    private void reload() {
        load(model.base());
    }

    private void load(Address address) {
        if (program == null || address == null) {
            return;
        }
        int len = (Integer) lengthBox.getSelectedItem();
        model.load(program, address, len);
        addressField.setText(address.toString());
        searchCursor = 0;
        status("mapped " + len + " bytes from " + address, false);
    }

    private void findNext(ActionEvent event) {
        HexPattern pattern = HexPattern.parse(searchField.getText());
        if (pattern.isEmpty()) {
            status("search accepts hex bytes, ?? wildcards, or quoted text", true);
            return;
        }
        int hit = model.find(pattern, searchCursor);
        if (hit < 0 && searchCursor > 0) {
            hit = model.find(pattern, 0);
        }
        if (hit < 0) {
            status("not found", true);
            return;
        }
        selectOffset(hit, pattern.length());
        ByteCell cell = model.cellAtOffset(hit);
        status("hit at " + (cell == null ? "unknown" : cell.address), false);
        searchCursor = hit + 1;
    }

    private void selectOffset(int offset, int length) {
        int row = offset / HexTableModel.BYTES_PER_ROW;
        int col = offset % HexTableModel.BYTES_PER_ROW + 1;
        int end = Math.min(offset + Math.max(1, length) - 1, model.byteCount() - 1);
        int endRow = end / HexTableModel.BYTES_PER_ROW;
        int endCol = end % HexTableModel.BYTES_PER_ROW + 1;
        table.changeSelection(row, col, false, false);
        table.changeSelection(endRow, endCol, false, true);
        table.scrollRectToVisible(table.getCellRect(row, col, true));
    }

    private void copyHex() {
        copy(model.selectedHex(table.getSelectedRows(), table.getSelectedColumns()), "copied hex");
    }

    private void copyAscii() {
        copy(model.selectedAscii(table.getSelectedRows(), table.getSelectedColumns()), "copied ascii");
    }

    private void copy(String text, String ok) {
        if (text == null || text.isBlank()) {
            status("nothing selected", true);
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        status(ok, false);
    }

    private JButton button(String text, java.awt.event.ActionListener action) {
        JButton button = new JButton(text);
        button.addActionListener(action);
        button.setFont(mono(Font.BOLD, 12));
        button.setBackground(new Color(0x13211e));
        button.setForeground(ACCENT);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x275247)),
            new EmptyBorder(4, 9, 4, 9)
        ));
        button.setFocusable(false);
        return button;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setFont(mono(Font.BOLD, 12));
        label.setBorder(new EmptyBorder(0, 6, 0, 4));
        return label;
    }

    private JTextField field(int cols) {
        JTextField field = new JTextField(cols);
        field.setFont(mono(Font.PLAIN, 13));
        field.setBackground(new Color(0x0f1519));
        field.setForeground(TEXT);
        field.setCaretColor(ACCENT);
        field.setSelectionColor(new Color(0x204842));
        field.setSelectedTextColor(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GRID),
            new EmptyBorder(4, 7, 4, 7)
        ));
        return field;
    }

    private void styleCombo(JComboBox<Integer> combo) {
        combo.setFont(mono(Font.PLAIN, 12));
        combo.setBackground(new Color(0x0f1519));
        combo.setForeground(TEXT);
        combo.setFocusable(false);
    }

    private void styleCheck(JCheckBox check) {
        check.setFont(mono(Font.BOLD, 12));
        check.setBackground(BG);
        check.setForeground(CYAN);
        check.setFocusable(false);
        check.setSelected(true);
    }

    private Font mono(int style, int size) {
        return new Font(Font.MONOSPACED, style, size);
    }

    private void status(String message, boolean error) {
        SwingUtilities.invokeLater(() -> {
            status.setText(message);
            status.setForeground(error ? BAD : MUTED);
        });
    }

    private final class HexCellRenderer extends DefaultTableCellRenderer {
        @Override
        protected void setValue(Object value) {
            setText(value == null ? "" : value.toString());
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focused, int row, int col) {
            java.awt.Component c = super.getTableCellRendererComponent(table, value, selected, focused, row, col);
            setHorizontalAlignment(col == HexTableModel.BYTES_PER_ROW + 1 ? LEFT : CENTER);
            if (!selected) {
                c.setBackground(col == 0 || col == HexTableModel.BYTES_PER_ROW + 1 ? BG : PANEL);
                c.setForeground(col == 0 ? CYAN : col == HexTableModel.BYTES_PER_ROW + 1 ? ACCENT : TEXT);
                int offset = model.offsetFor(row, col);
                ByteCell cell = model.cellAtOffset(offset);
                if (cell != null && !cell.readable) {
                    c.setForeground(WARN);
                }
            }
            setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            return c;
        }
    }
}
