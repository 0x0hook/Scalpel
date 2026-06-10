# Release

1. Update `version` in `build.gradle`.
2. Update `CHANGELOG.md`.
3. Build the extension:

```powershell
$env:GHIDRA_INSTALL_DIR="C:\Tools\ghidra_12.0.4_PUBLIC"
.\scripts\build.ps1
```

4. Create a GitHub release.
5. Attach the zip from `dist/`.
