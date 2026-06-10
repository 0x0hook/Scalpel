package org.nullhook.scalpel;

import java.util.Locale;

import javax.swing.table.AbstractTableModel;

import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressOverflowException;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.Memory;
import ghidra.program.model.mem.MemoryAccessException;

final class HexTableModel extends AbstractTableModel {
    static final int BYTES_PER_ROW = 16;

    private final HexPatchSink patchSink;
    private Program program;
    private Address base;
    private ByteCell[] cells = new ByteCell[0];

    HexTableModel(HexPatchSink patchSink) {
        this.patchSink = patchSink;
    }

    void load(Program program, Address base, int count) {
        this.program = program;
        this.base = base;
        if (program == null || base == null || count <= 0) {
            cells = new ByteCell[0];
            fireTableDataChanged();
            return;
        }

        Memory memory = program.getMemory();
        ByteCell[] next = new ByteCell[count];
        for (int i = 0; i < count; i++) {
            Address address = add(base, i);
            if (address == null || !memory.contains(address)) {
                next[i] = ByteCell.missing(address);
                continue;
            }
            try {
                next[i] = new ByteCell(address, memory.getByte(address), true);
            } catch (MemoryAccessException e) {
                next[i] = ByteCell.missing(address);
            }
        }
        cells = next;
        fireTableDataChanged();
    }

    Address base() {
        return base;
    }

    int byteCount() {
        return cells.length;
    }

    ByteCell cellAtOffset(int offset) {
        if (offset < 0 || offset >= cells.length) {
            return null;
        }
        return cells[offset];
    }

    int offsetFor(int row, int col) {
        int byteCol = col - 1;
        if (byteCol < 0 || byteCol >= BYTES_PER_ROW) {
            return -1;
        }
        int offset = row * BYTES_PER_ROW + byteCol;
        return offset < cells.length ? offset : -1;
    }

    int find(HexPattern pattern, int startOffset) {
        if (pattern.isEmpty()) {
            return -1;
        }
        int last = cells.length - pattern.length();
        for (int i = Math.max(0, startOffset); i <= last; i++) {
            boolean hit = true;
            for (int j = 0; j < pattern.length(); j++) {
                ByteCell cell = cells[i + j];
                if (!cell.readable || !pattern.matches(j, cell.value)) {
                    hit = false;
                    break;
                }
            }
            if (hit) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return (cells.length + BYTES_PER_ROW - 1) / BYTES_PER_ROW;
    }

    @Override
    public int getColumnCount() {
        return BYTES_PER_ROW + 2;
    }

    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return "offset";
        }
        if (column == BYTES_PER_ROW + 1) {
            return "ascii";
        }
        return String.format(Locale.ROOT, "%02X", column - 1);
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        int offset = offsetFor(row, col);
        ByteCell cell = cellAtOffset(offset);
        return program != null && cell != null && cell.readable;
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (col == 0) {
            return rowAddress(row);
        }
        if (col == BYTES_PER_ROW + 1) {
            return ascii(row);
        }
        ByteCell cell = cellAtOffset(offsetFor(row, col));
        if (cell == null) {
            return "";
        }
        return cell.readable ? String.format(Locale.ROOT, "%02X", cell.value) : "..";
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        ByteCell cell = cellAtOffset(offsetFor(row, col));
        if (cell == null || !cell.readable || value == null) {
            return;
        }
        int parsed = parseByte(value.toString());
        if (parsed < 0 || parsed == cell.value) {
            fireTableCellUpdated(row, col);
            return;
        }
        HexPatchResult result = patchSink.patch(cell.address, parsed);
        if (result.ok) {
            cells[offsetFor(row, col)] = new ByteCell(cell.address, parsed, true);
            fireTableRowsUpdated(row, row);
        } else {
            fireTableCellUpdated(row, col);
        }
    }

    String selectedHex(int[] rows, int[] cols) {
        StringBuilder out = new StringBuilder();
        for (int row : rows) {
            for (int col : cols) {
                int offset = offsetFor(row, col);
                ByteCell cell = cellAtOffset(offset);
                if (cell != null && cell.readable) {
                    if (out.length() > 0) {
                        out.append(' ');
                    }
                    out.append(String.format(Locale.ROOT, "%02X", cell.value));
                }
            }
        }
        return out.toString();
    }

    String selectedAscii(int[] rows, int[] cols) {
        StringBuilder out = new StringBuilder();
        for (int row : rows) {
            for (int col : cols) {
                int offset = offsetFor(row, col);
                ByteCell cell = cellAtOffset(offset);
                if (cell != null && cell.readable) {
                    int c = cell.value;
                    out.append(c >= 32 && c <= 126 ? (char) c : '.');
                }
            }
        }
        return out.toString();
    }

    private String rowAddress(int row) {
        Address address = add(base, (long) row * BYTES_PER_ROW);
        return address == null ? "" : address.toString();
    }

    private String ascii(int row) {
        StringBuilder out = new StringBuilder(BYTES_PER_ROW);
        int start = row * BYTES_PER_ROW;
        for (int i = 0; i < BYTES_PER_ROW; i++) {
            ByteCell cell = cellAtOffset(start + i);
            if (cell == null) {
                out.append(' ');
            } else if (!cell.readable) {
                out.append('.');
            } else {
                int c = cell.value;
                out.append(c >= 32 && c <= 126 ? (char) c : '.');
            }
        }
        return out.toString();
    }

    private static Address add(Address address, long offset) {
        if (address == null) {
            return null;
        }
        try {
            return address.addNoWrap(offset);
        } catch (AddressOverflowException e) {
            return null;
        }
    }

    private static int parseByte(String text) {
        String s = text.trim().replace("0x", "").replace("0X", "");
        if (s.length() == 1) {
            s = "0" + s;
        }
        if (s.length() != 2 || !s.matches("[0-9a-fA-F]{2}")) {
            return -1;
        }
        return Integer.parseInt(s, 16);
    }
}
