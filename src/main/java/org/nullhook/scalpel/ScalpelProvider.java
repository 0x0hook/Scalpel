package org.nullhook.scalpel;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

import docking.ComponentProvider;
import docking.WindowPosition;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;

final class ScalpelProvider extends ComponentProvider {
    private final JPanel root;
    private final HexEditorPanel panel;

    ScalpelProvider(PluginTool tool, String owner) {
        super(tool, "Scalpel", owner);
        panel = new HexEditorPanel();
        root = new JPanel(new BorderLayout());
        root.add(panel, BorderLayout.CENTER);
        setTitle("Scalpel");
        setDefaultWindowPosition(WindowPosition.RIGHT);
        addToTool();
    }

    @Override
    public JComponent getComponent() {
        return root;
    }

    void setProgram(Program program) {
        panel.setProgram(program);
    }

    void follow(Address address) {
        panel.follow(address);
    }

    void show() {
        setVisible(true);
        toFront();
    }

    public void dispose() {
        panel.dispose();
        removeFromTool();
    }
}
