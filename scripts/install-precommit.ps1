param()

Write-Host "Installing pre-commit tooling and hooks (if needed)..."

function Has-Command($name) {
    try { Get-Command $name -ErrorAction Stop | Out-Null; return $true } catch { return $false }
}

if (Has-Command pre-commit) {
    Write-Host "pre-commit already available on PATH"
} else {
    if (Has-Command pip) {
        Write-Host "Installing pre-commit via pip (user)..."
        pip install --user pre-commit
    } else {
        Write-Host "Could not find 'pre-commit' or 'pip' on PATH. Please install Python and pip, then run this script again or install pre-commit manually."
    }
}

Write-Host "Attempting to install pre-commit hooks for this repository..."
try {
    pre-commit install
    Write-Host "pre-commit hooks installed. To run checks manually: pre-commit run --all-files"
} catch {
    Write-Host "Failed to run 'pre-commit install'. If pre-commit was just installed, open a new shell or run 'pre-commit install' manually."
}
