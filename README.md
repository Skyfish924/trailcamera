# TrailCamera
## Version 2.0

---

**Warning: support for `.jpg` has been removed in this update. Make sure all images use `.png`, including in `dataset/` and `frames/`.** 

To quickly do so, open the terminal, and run the commands below. **Do not rename the images, as that may cause them to stop working.**

```powershell
Get-ChildItem training -Filter *.jpg -Recurse | ForEach-Object {
    $output = Join-Path $_.DirectoryName ($_.BaseName + ".png")
    .\bin\ffmpeg.exe -i $_.FullName $output
}
```

After confirming all images have been converted:

```powershell
Get-ChildItem training -Filter *.jpg -Recurse | Remove-Item
```

---

### Additions
- Added experimental hardware acceleration
- Added a `bin` folder in `root`
### Changes
- Switched to `.png`
- Moved `ffmpeg.exe` to `bin`
- Optimized some classes
### Bugfixes
- None, which means I'm such good coder there are none! or maybe I'm such a bad coder I just can't find any