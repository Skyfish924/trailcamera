# TrailCamera
## Version 0.2

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

**Note: `model.dat` is not compatible with version 0.2, you will have to retrain. Apologies for the inconvenience.**

---

### Additions
- Added experimental hardware acceleration
- New neat little progress bar to indicate training progress
- Added a `bin` folder to the project root
### Changes
- Switched to `.png`
- Moved `ffmpeg.exe` to `bin`
- Reorganized some classes
### Bugfixes
- None, which means I'm such good coder there are none! or maybe I'm such a bad coder I just can't find any

---

## How to use
**This program is pretty easy to use, but just in case you're lost, here's an in-depth usage guide.**
### Uploading files
If you want to upload a video, add the mp4 to `training/`. If you want to upload an image, add it to either `dataset/car/` or `dataset/not_car/`.

### Extracting the frames
**Note: if you uploaded images, you can skip this step, as well as the next two.**

To extract the frames from the video, go to `Main`, change the `TRAINING-VIDEO-HERE.mp4` to the name of your video, then run the file. Only do this when `frames/` is empty. 

You can also change the FPS, however that will increase the time it takes to complete the next step.

### Labeling the frames
To label the frames as either containing a car or not containing a car, simply run `Labeler`.

### Adding the data
Same as the step above, just with `DatasetBuilder`.

### Training
To train the AI, confirm there are `.png` files in both `dataset/car/` and `dataset/not_car/`, then run `Trainer`.

### Testing
To test the AI's capabilities, change the `img.png` to be the name of the image you want to test it on.

### Using in-game
tba

