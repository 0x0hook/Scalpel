# Architecture

Scalpel is just simple

## Plugin

`ScalpelPlugin` listens for active-program and Listing-location changes, then forwards them to the provider

## Provider

`ScalpelProvider` owns the dockable Ghidra component and keeps a stable Swing root component for the docking framework

## UI

`HexEditorPanel` handles controls, rendering, search, clipboard export, and byte patching. It uses plain Swing so the extension has no runtime dependency beyond Ghidra

## Model

`HexTableModel` reads a bounded window of program memory into `ByteCell` values. Unreadable ranges are represented in the model and rendered as gaps instead of throwing through the UI

## Patching

Byte edits go through `Program.startTransaction` and `Program.endTransaction`. Failed writes roll back
