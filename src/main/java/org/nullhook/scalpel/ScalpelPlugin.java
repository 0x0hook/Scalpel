package org.nullhook.scalpel;

import docking.ActionContext;
import docking.action.DockingAction;
import docking.action.MenuData;
import ghidra.app.plugin.PluginCategoryNames;
import ghidra.app.plugin.ProgramPlugin;
import ghidra.framework.main.UtilityPluginPackage;
import ghidra.framework.plugintool.PluginInfo;
import ghidra.framework.plugintool.PluginTool;
import ghidra.framework.plugintool.util.PluginStatus;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;
import ghidra.program.util.ProgramLocation;

@PluginInfo(
    status = PluginStatus.RELEASED,
    packageName = UtilityPluginPackage.NAME,
    category = PluginCategoryNames.ANALYSIS,
    shortDescription = "Dark hex editor",
    description = "Dark-mode hex editor for malware triage and reverse engineering."
)
public class ScalpelPlugin extends ProgramPlugin {
    private final ScalpelProvider provider;

    public ScalpelPlugin(PluginTool tool) {
        super(tool);
        provider = new ScalpelProvider(tool, getName());
        installActions();
    }

    @Override
    protected void dispose() {
        provider.dispose();
        super.dispose();
    }

    @Override
    protected void programActivated(Program program) {
        provider.setProgram(program);
    }

    @Override
    protected void programDeactivated(Program program) {
        provider.setProgram(null);
    }

    @Override
    protected void locationChanged(ProgramLocation location) {
        Address address = location == null ? null : location.getAddress();
        provider.follow(address);
    }

    void showProvider() {
        provider.show();
    }

    private void installActions() {
        DockingAction show = new DockingAction("Show Scalpel", getName()) {
            @Override
            public void actionPerformed(ActionContext context) {
                showProvider();
            }
        };
        show.setMenuBarData(new MenuData(new String[] {"Window", "Scalpel"}));
        tool.addAction(show);
    }
}
