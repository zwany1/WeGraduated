# ============================================================
# One-click frontend update (run on local Windows machine)
#   1. npm run build
#   2. package dist -> tar.gz (top level MUST contain dist/)
#   3. scp to server /tmp/
#   4. ssh server: clean stale dirs + run deploy/update.sh
#
# Usage (PowerShell):
#   powershell -ExecutionPolicy Bypass -File deploy\update-frontend.ps1
#
# You will be prompted for the root password for scp/ssh.
# ============================================================

param(
    [string]$ServerIP         = "123.56.158.11",
    [string]$SshUser          = "root",
    [string]$ProjectDir       = "D:\project\Graduated",
    [string]$DistTar          = "C:\Users\26518\AppData\Local\Temp\opencode\frontend-dist.tar.gz",
    [string]$ServerProjectDir = "/opt/WeGraduated"
)

$ErrorActionPreference = "Stop"
$frontend = Join-Path $ProjectDir "frontend"

Write-Host "=== [1/4] Build frontend ==="
Push-Location $frontend
npm run build
if ($LASTEXITCODE -ne 0) { throw "npm run build failed (exit $LASTEXITCODE)" }
Pop-Location

Write-Host "=== [2/4] Package dist ==="
tar -czf $DistTar -C $frontend dist
if ($LASTEXITCODE -ne 0) { throw "tar failed (exit $LASTEXITCODE)" }
$top = tar -tzf $DistTar | Select-Object -First 1
if ($top -ne "dist/") { throw "tar top level should be 'dist/' but got: '$top'" }

Write-Host "=== [3/4] Upload to server ==="
scp $DistTar "${SshUser}@${ServerIP}:/tmp/"

Write-Host "=== [4/4] Deploy on server ==="
ssh "${SshUser}@${ServerIP}" "cd $ServerProjectDir && rm -rf frontend/dist dist_stage/dist && bash deploy/update.sh"

Write-Host "=== Done. Hard-refresh browser (Ctrl+F5) ==="
