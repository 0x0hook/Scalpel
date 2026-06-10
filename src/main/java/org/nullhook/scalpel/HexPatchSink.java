package org.nullhook.scalpel;

import ghidra.program.model.address.Address;

interface HexPatchSink {
    HexPatchResult patch(Address address, int value);
}
