# App Icon Setup Guide

## Required Icon Sizes for Umbrella Icon

Place your converted umbrella icons in these directories with these exact sizes:

### Standard Icons (ic_launcher.png)
- `app/src/main/res/mipmap-mdpi/ic_launcher.png` → 48×48 px
- `app/src/main/res/mipmap-hdpi/ic_launcher.png` → 72×72 px
- `app/src/main/res/mipmap-xhdpi/ic_launcher.png` → 96×96 px
- `app/src/main/res/mipmap-xxhdpi/ic_launcher.png` → 144×144 px
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` → 192×192 px

### Round Icons (ic_launcher_round.png) - Same sizes
- `app/src/main/res/mipmap-mdpi/ic_launcher_round.png` → 48×48 px
- `app/src/main/res/mipmap-hdpi/ic_launcher_round.png` → 72×72 px
- `app/src/main/res/mipmap-xhdpi/ic_launcher_round.png` → 96×96 px
- `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png` → 144×144 px
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png` → 192×192 px

## Steps:
1. Convert Umbrella.jpg to PNG format
2. Create versions at each size listed above
3. Replace the existing .webp files in each mipmap directory
4. Clean and rebuild your project

## Tools for Icon Generation:
- Android Studio's built-in Image Asset Studio (File → New → Image Asset)
- Online tools like https://icon.kitchen/
- Image editing software like GIMP, Photoshop, or Canva
