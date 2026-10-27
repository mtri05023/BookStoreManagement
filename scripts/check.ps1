$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$buildPath = Join-Path $repoRoot ('build/check-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $buildPath -Force | Out-Null
$sources = @(Get-ChildItem (Join-Path $repoRoot 'src') -Recurse -Filter '*.java' |
    ForEach-Object { $_.FullName })
$testSources = @(Get-ChildItem (Join-Path $repoRoot 'tests') -Filter '*.java' |
    ForEach-Object { $_.FullName })
& javac -Xlint:all -encoding UTF-8 --release 17 -d $buildPath @sources @testSources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
& java -cp $buildPath RegressionChecks $buildPath
if ($LASTEXITCODE -ne 0) { throw 'Regression checks failed.' }
& java -cp $buildPath LeaderChecks $buildPath
if ($LASTEXITCODE -ne 0) { throw 'Leader checks failed.' }
