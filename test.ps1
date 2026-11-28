param([switch]$Ui)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
& "$PSScriptRoot/build.ps1"
$testDir = Join-Path $PSScriptRoot ('build/tests-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testDir -Force | Out-Null
$sources = @(Get-ChildItem -LiteralPath src,tests -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)
& javac --release 17 -encoding UTF-8 -d $testDir $sources
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
foreach ($test in @('scheduler.cpu.CpuChecks', 'scheduler.disk.DiskChecks', 'checks.PreservationChecks')) {
    & java '-Djava.awt.headless=true' -cp $testDir $test
    if ($LASTEXITCODE -ne 0) { throw "Failed: $test" }
}
if ($Ui) {
    # Put the actual packaged JAR first so these checks exercise its production classes.
    & java -cp "build/SchedulingSimulator.jar;$testDir" checks.UiChecks
    if ($LASTEXITCODE -ne 0) { throw 'GUI integration tests failed.' }
    # Launch the manifest entry point exactly as a user does.
    $runtimeHome = & java -cp $testDir checks.RuntimeHome
    if ($LASTEXITCODE -ne 0) { throw 'Cannot resolve the Java runtime.' }
    $javaApp = Join-Path $runtimeHome 'bin/javaw.exe'
    $jarPath = Join-Path $PSScriptRoot 'build/SchedulingSimulator.jar'
    $smoke = Start-Process -FilePath $javaApp -ArgumentList ('-jar "{0}"' -f $jarPath) -WindowStyle Hidden -PassThru -RedirectStandardError (Join-Path $PSScriptRoot 'build/jar-smoke-error.log')
    # Retain the native handle so Windows PowerShell can read ExitCode after a fast exit.
    $null = $smoke.Handle
    try {
        $deadline = (Get-Date).AddSeconds(10)
        do {
            Start-Sleep -Milliseconds 100
            $smoke.Refresh()
        } while (-not $smoke.HasExited -and $smoke.MainWindowHandle -eq 0 -and (Get-Date) -lt $deadline)
        if ($smoke.HasExited -or $smoke.MainWindowHandle -eq 0) { throw 'Executable JAR did not open its main window.' }
        if (-not $smoke.CloseMainWindow()) { throw 'Could not close the executable JAR main window.' }
        if (-not $smoke.WaitForExit(5000)) { throw 'Executable JAR did not exit after closing its main window.' }
        if ($smoke.ExitCode -ne 0) { throw "Executable JAR exit code was '$($smoke.ExitCode)'." }
        if ((Get-Item -LiteralPath build/jar-smoke-error.log).Length -gt 0) { throw 'Executable JAR logged an error.' }
        Write-Host 'PASS executable JAR: manifest launch, main window, clean exit'
    } finally {
        if (-not $smoke.HasExited) { Stop-Process -Id $smoke.Id }
    }
}
Write-Host 'All requested checks passed.'
