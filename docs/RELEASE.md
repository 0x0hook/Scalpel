# Release

1. Update `version` in `build.gradle`
2. Update `CHANGELOG.md`
3. Build the extension:

```powershell
$env:GHIDRA_INSTALL_DIR="C:\path\to\ghidra_12.1_PUBLIC"
.\scripts\build.ps1
```

Linux:

```bash
export GHIDRA_INSTALL_DIR="$HOME/tools/ghidra_12.1_PUBLIC"
./scripts/build.sh
```

4. Create a GitHub release
5. Attach the zip from `dist/`
