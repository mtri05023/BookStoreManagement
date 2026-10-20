$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$buildPath = Join-Path $repoRoot ('build/check-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $buildPath -Force | Out-Null
$sources = @(Get-ChildItem (Join-Path $repoRoot 'src') -Recurse -Filter '*.java' |
    ForEach-Object { $_.FullName })
$testSource = Join-Path $repoRoot 'tests/RegressionChecks.java'
& javac -Xlint:all -encoding UTF-8 --release 17 -d $buildPath @sources $testSource
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
& java -cp $buildPath RegressionChecks $buildPath
if ($LASTEXITCODE -ne 0) { throw 'Regression checks failed.' }
