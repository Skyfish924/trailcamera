# TrailCamera
## Version Alpha 0.3

### Additions
- New Video Tester
### Changes
- Hardware Acceleration put on hold
- `Labeler` is now run inside of `Main`, after the frames are extracted
- Reorganized some classes v2
### Bugfixes
- None, again, which means I'm still such good coder, there still aren't any bugs! or maybe I'm still such a bad coder I still just can't find any

---

## How to use
**This program is pretty easy to use, but just in case you're lost, here's an in-depth usage guide.**
### Uploading files
If you want to upload a video, add the mp4 to `training/`. If you want to upload an image, add it to either `dataset/car/` or `dataset/not_car/`.

### Extracting/labeling the frames
**Note: if you uploaded images, you can skip this step, as well as the next one.**

To extract/label the frames from the video, go to `Main`, change the `TRAINING-VIDEO-HERE.mp4` to the name of your video, then run the file. 

You can also change the FPS, however that will increase the time it takes to complete the next step.

### Adding the data
To add the labeled frames to either `dataset/car` or `dataset/not_car`, simply run `DatasetBuilder`.

### Training
To train the AI, confirm there are `.png` files in both `dataset/car/` and `dataset/not_car/`, then run `Trainer`.

### Testing
To test the AI's capabilities, change the `img.png` to be the name of the image you want to test it on.

### Using in-game
tba

