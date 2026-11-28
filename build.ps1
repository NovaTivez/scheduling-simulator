param([switch]$Package)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
foreach ($tool in @('javac', 'jar')) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) { throw "Install JDK 17 or later and put $tool on PATH." }
}
# Compile into a fresh directory so removed or renamed classes cannot enter the JAR.
$classDir = Join-Path $PSScriptRoot ('build/app-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $classDir -Force | Out-Null
$sources = @(Get-ChildItem -LiteralPath src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)
& javac --release 17 -encoding UTF-8 -d $classDir $sources
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
$jarPath = Join-Path $PSScriptRoot 'build/SchedulingSimulator.jar'
& jar --create --file $jarPath --main-class ui.MainMenu -C $classDir .
if ($LASTEXITCODE -ne 0) { throw 'JAR creation failed.' }
Write-Host "Built $jarPath"
if ($Package) {
    $stage = Join-Path $PSScriptRoot ('build/package-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $stage -Force | Out-Null
    Copy-Item -LiteralPath src -Destination $stage -Recurse
    Copy-Item -LiteralPath tests -Destination $stage -Recurse
    Copy-Item -LiteralPath docs -Destination $stage -Recurse
    Copy-Item -LiteralPath $jarPath -Destination $stage
    Copy-Item -LiteralPath README.md,build.ps1,test.ps1 -Destination $stage
    Compress-Archive -Path (Join-Path $stage '*') -DestinationPath build/SourceCode.zip -Force
    Write-Host 'Packaged build/SourceCode.zip'
}
