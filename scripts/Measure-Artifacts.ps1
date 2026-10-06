param([string]$OutputPath='.artifacts/artifact-sizes.json')
$ErrorActionPreference='Stop'
$outputParent=Split-Path -Parent $OutputPath
if($outputParent){New-Item -ItemType Directory -Path $outputParent -Force | Out-Null}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$paths=@('app/build/outputs/apk/debug/app-debug.apk','app/build/outputs/apk/release/app-release-unsigned.apk','app/build/outputs/bundle/release/app-release.aab')
$report=foreach($path in $paths){
    $file=Get-Item -LiteralPath $path
    $zip=[System.IO.Compression.ZipFile]::OpenRead($file.FullName)
    try{
        $entries=@($zip.Entries | ForEach-Object {
            $name=$_.FullName
            $group=if($name -match '(^|/)lib/'){ 'native' }elseif($name -match '\.dex$'){ 'dex' }elseif($name -match '(^|/)res/|resources\.(arsc|pb)$'){ 'resources' }elseif($name -match '(^|/)assets/'){ 'assets' }else{ 'metadata' }
            [pscustomobject]@{Path=$name;Group=$group;CompressedBytes=$_.CompressedLength;UncompressedBytes=$_.Length}
        })
        [pscustomobject]@{
            Artifact=$path;Bytes=$file.Length;SHA256=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash
            Groups=@($entries | Group-Object Group | ForEach-Object { [pscustomobject]@{Name=$_.Name;CompressedBytes=($_.Group | Measure-Object CompressedBytes -Sum).Sum;UncompressedBytes=($_.Group | Measure-Object UncompressedBytes -Sum).Sum} })
            LargestEntries=@($entries | Sort-Object CompressedBytes -Descending | Select-Object -First 12)
            NativeLibraries=@($entries | Where-Object Group -eq 'native')
        }
    }finally{$zip.Dispose()}
}
$report | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $OutputPath
$report | Select-Object Artifact,Bytes,SHA256
