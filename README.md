# MNIST 手写数字识别 Android 应用 (命令行构建)

这是一个简单的 Android 应用程序，允许用户手写数字 (0-9)，并使用 TensorFlow Lite 模型来识别该数字。本项目完全使用命令行工具进行构建和管理，没有依赖 Android Studio。

## 功能

*   提供一个画布供用户手写输入数字。
*   使用 TensorFlow Lite 的 MNIST 模型进行数字识别。
*   显示识别出的数字及其置信度。
*   提供清除画布的功能。

## 先决条件

*   **Android SDK:** 需要安装 Android SDK 命令行工具。可以通过 [Android 开发者官网](https://developer.android.com/studio#command-tools) 下载 `commandlinetools-*.zip`。
*   **SDK Manager:** 使用 SDK 包中的 `sdkmanager` 工具安装必要的平台、构建工具等 (例如 `platforms;android-34`, `build-tools;33.0.1`, `platform-tools`)。
*   **Java Development Kit (JDK):** 需要兼容的 JDK 版本 (本项目使用 Java 8)。
*   **环境变量:** 配置 `ANDROID_HOME` 指向你的 SDK 安装目录，并将 `$ANDROID_HOME/cmdline-tools/latest/bin`, `$ANDROID_HOME/platform-tools` 等添加到系统 `PATH`。
*   **Git:** 用于版本控制和从 GitHub 克隆 (如果需要)。
*   **Android 设备或模拟器:** 用于安装和运行应用，需要开启 USB 调试或确保模拟器能被 `adb` 检测到。

## 构建和运行 (命令行)

1.  **克隆仓库 (如果尚未克隆):**
    ```bash
    git clone https://github.com/WFYTSUC/FYWmnist-android-cli-app.git
    cd FYWmnist-android-cli-app
    ```

2.  **构建 Debug APK:**
    确保你在项目根目录下。运行 Gradle Wrapper 命令：
    ```bash
    ./gradlew assembleDebug
    ```
    首次构建会下载依赖，可能需要一些时间。成功后，APK 文件会生成在 `app/build/outputs/apk/debug/app-debug.apk`。

3.  **连接设备/模拟器:**
    确保你的 Android 设备已连接并开启 USB 调试，或者你的模拟器正在运行。通过以下命令确认设备已连接：
    ```bash
    adb devices
    ```
    你应该能看到列出的设备或模拟器 (例如 `emulator-5554 device`)。

4.  **安装 APK:**
    ```bash
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    ```
    (`-r` 选项表示替换安装，如果已存在旧版本)

5.  **启动应用:**
    ```bash
    adb shell am start -n com.example.mnistapp/.MainActivity
    ```

## 使用方法

1.  **绘制:** 在应用界面上方的白色方框内手写一个 0 到 9 之间的数字。请尽量将数字写在方框中央，线条清晰。
2.  **识别:** 点击界面下方的“识别”按钮。
3.  **查看结果:** 应用会在按钮下方的文本区域显示识别出的数字以及模型对其的置信度百分比 (例如 "预测结果: 3 (95.2%)")。
4.  **清除:** 点击“清除”按钮，画布将被清空，预测结果恢复为 "-”。

## 开发过程中的主要挑战与解决

在纯命令行环境下开发此应用遇到了一些特有的问题：

1.  **环境配置:** 手动配置 Android SDK 路径、环境变量 (`ANDROID_HOME`, `PATH`) 并使用 `sdkmanager` 下载所需组件比使用 IDE 更繁琐，容易遗漏。
2.  **Gradle 依赖与版本兼容性:**
    *   最初遇到了 `buildscript` 拼写错误和 `gradle.properties` 末尾空格导致的构建失败。
    *   **核心问题:** TensorFlow Lite 库的版本管理非常棘手。
        *   最初由于模型 (`mnist.tflite`) 由较新版本的 TensorFlow (`2.18.0`) 生成，包含了较新版本的操作 (`FULLY_CONNECTED` v12)，而 Android 应用中使用的 TFLite 运行时库版本 (`2.9.0`, `2.15.0`) 过旧，导致无法创建解释器而闪退。
        *   尝试更新 TFLite 库到最新稳定版 (`2.17.0`) 后解决了启动闪退，但需要将项目 `minSdk` 提高到 26 以满足新库的 API 要求。
        *   由于 TFLite 的 Support 库 (`tensorflow-lite-support`) 版本 (`0.4.4`) 与核心库 (`2.17.0`) 可能存在深层版本冲突或强制依赖旧核心库，最终**移除了 Support 库**，并手动实现了模型加载和图像预处理逻辑。
3.  **资源文件:**
    *   由于缺少默认的应用图标 (`ic_launcher`, `ic_launcher_round`) 及其 `mipmap` 目录，导致资源链接失败。通过移除 `AndroidManifest.xml` 中的图标引用解决。
    *   `backup_rules.xml` 和 `data_extraction_rules.xml` 文件不能为空，否则 XML 解析会失败。需要添加有效的 XML 根元素。
4.  **图像预处理:**
    *   需要手动实现 Bitmap 的缩放、灰度转换、颜色反转 (因为训练数据是白字黑底，而绘制是黑字白底) 和归一化。
    *   **输入类型匹配至关重要:** 最初假设模型需要 `UINT8` 输入，导致推理失败。通过日志检查模型期望的输入类型为 `FLOAT32`，并相应修改了预处理逻辑（将像素值转换为 `[0, 1]` 的 float）和输入缓冲区大小（每个像素 4 字节）后才成功。
    *   缓冲区大小计算错误 (`BufferOverflowException`)：分配缓冲区时需要根据写入的数据类型（`byte` vs `float`）计算正确的大小。
5.  **ADB 连接:** 偶尔 `adb` 无法检测到正在运行的模拟器，需要通过 `adb kill-server && adb start-server` 或重启模拟器来解决。
6.  **Git 推送:** 在特定网络环境下，直接推送到 GitHub 可能失败，需要配置 HTTP/HTTPS 代理。

## 模型信息

*   模型文件: `app/src/main/assets/mnist.tflite`
*   来源: 基于 TensorFlow 官网的 MNIST Codelab 生成，但需注意使用与 Android TFLite 运行时兼容的 TensorFlow 版本进行转换 (本项目最终使用 TFLite 2.17.0 运行时)。
*   输入: `[1, 28, 28, 1]` 的 `FLOAT32` 张量，像素值需要归一化到 `[0, 1]` 范围，并且是**白色/亮色数字在黑色/暗色背景上**。
*   输出: `[1, 10]` 的 `FLOAT32` 张量，代表数字 0-9 的概率。

## (可选) 未来改进

*   将模型推理操作放到后台线程，避免 UI 卡顿。
*   使用 `ViewModel` 和 `LiveData` 管理 UI 状态和模型交互。
*   添加正式的应用图标。
*   实现更健壮的错误处理。
*   尝试使用 TensorFlow Lite GPU Delegate 或 NNAPI Delegate 进行加速（需要重新添加相应依赖并修改 `DigitClassifier` 初始化逻辑）。
