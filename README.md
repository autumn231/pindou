# 拼豆

一个纯本地、零联网的 Android 拼豆图纸生成 APP。

## 特性

- 图片转拼豆图纸（Lab + CIEDE2000 色差匹配）
- 主体提取（TFLite U2Net 自动 + OpenCV GrabCut 手动）
- 多品牌调色板（Artkal C/S、MARD）
- Floyd-Steinberg 抖动（可选）
- 用料清单导出

## 技术栈

- Kotlin + Jetpack Compose
- OpenCV 4.10 Android SDK（抠图兜底）
- TensorFlow Lite + U2Net lite INT8（自动抠图）
- 最低 Android 8.0 (API 26)，目标 Android 13 (API 33)

## 构建

GitHub Actions 自动编译并发布到 Release。本地编译需先执行：

```bash
# 1. 下载 OpenCV SDK
wget https://github.com/opencv/opencv/releases/download/4.10.0/opencv-4.10.0-android-sdk.zip
unzip opencv-4.10.0-android-sdk.zip
cp -r OpenCV-android-sdk/sdk/java/src opencv/src/main/java/
mkdir -p opencv/src/main/jniLibs
cp -r OpenCV-android-sdk/sdk/native/libs/arm64-v8a opencv/src/main/jniLibs/
cp -r OpenCV-android-sdk/sdk/native/libs/armeabi-v7a opencv/src/main/jniLibs/

# 2. 下载模型
mkdir -p app/src/main/assets/models
wget -O app/src/main/assets/models/u2net_lite_int8.tflite \
  https://github.com/ailia-ai/ailia-models-tflite/raw/main/background_removal/u2net/u2netp_full_integer_quant.tflite

# 3. 拉取色卡
pip install pillow
python scripts/fetch_palettes.py
python scripts/generate_icon.py

# 4. 编译
gradle assembleRelease
```

## License

MIT
