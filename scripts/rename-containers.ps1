<#
Rename running Docker Compose containers for services `trade-store` and `trade-expiry` to a hyphenated pattern:
  trade-store-1, trade-store-2, ...
  trade-expiry-1, trade-expiry-2, ...

Usage:
  # Dry run (shows what would be renamed)
  pwsh .\scripts\rename-containers.ps1 -DryRun

  # Perform renames
  pwsh .\scripts\rename-containers.ps1

Notes:
- This script finds containers created by Docker Compose by filtering the label
  `com.docker.compose.service=<service>` which Compose adds to containers.
- If a target name (e.g. `trade-store-1`) already exists, the script aborts to
  avoid collisions.
- After renaming, Docker Compose will no longer automatically refer to the
  renamed container names. You can still manage services with `docker compose`
  (but Compose will show its own logical names). Use the renamed names for
  ad-hoc debugging or external tooling.
#>
param(
    [switch]$DryRun
)

$services = @('trade-store','trade-expiry')

# Helper: returns true if a container name exists (any state)
function Test-ContainerNameExists([string]$name) {
    $all = docker ps -a --format "{{.Names}}" 2>$null
    foreach ($n in $all) { if ($n -eq $name) { return $true } }
    return $false
}

foreach ($service in $services) {
    Write-Host "Processing service: $service"

    # Get Compose-created containers for the service (may include project prefix)
    $cmd = "docker ps --filter label=com.docker.compose.service=$service --format '{{.Names}}'"
    $out = Invoke-Expression $cmd 2>$null
    if (-not $out) {
        Write-Host "  No running containers found for service '$service' (label filter). Skipping.`n"
        continue
    }

    $containers = $out -split "\r?\n" | Where-Object { $_ -ne '' } | Sort-Object

    $i = 1
    foreach ($c in $containers) {
        $target = "$service-$i"

        if ($c -eq $target) {
            Write-Host "  Container '$c' already named as desired. Skipping."
            $i++
            continue
        }

        if (Test-ContainerNameExists $target) {
            Write-Host "  ERROR: target container name '$target' already exists. Aborting to avoid collision."
            exit 1
        }

        if ($DryRun) {
            Write-Host "  Would rename: $c -> $target"
        }
        else {
            Write-Host "  Renaming: $c -> $target"
            docker rename $c $target
            if ($LASTEXITCODE -ne 0) {
                Write-Host "    ERROR: failed to rename $c -> $target" -ForegroundColor Red
                exit $LASTEXITCODE
            }
        }

        $i++
    }

    Write-Host ""
}

Write-Host "Done."
