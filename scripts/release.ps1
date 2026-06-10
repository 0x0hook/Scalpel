param(
    [string] $GhidraInstallDir = $env:GHIDRA_INSTALL_DIR
)

if (-not $GhidraInstallDir) {
    throw "Set GHIDRA_INSTALL_DIR or pass -GhidraInstallDir."
}

$env:GHIDRA_INSTALL_DIR = $GhidraInstallDir
gradle clean buildExtension
