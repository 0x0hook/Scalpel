package org.nullhook.scalpel;

import ghidra.program.model.address.Address;

final class ByteCell {
    final Address address;
    final int value;
    final boolean readable;

    ByteCell(Address address, int value, boolean readable) {
        this.address = address;
        this.value = value & 0xff;
        this.readable = readable;
    }

    static ByteCell missing(Address address) {
        return new ByteCell(address, 0, false);
    }
}
