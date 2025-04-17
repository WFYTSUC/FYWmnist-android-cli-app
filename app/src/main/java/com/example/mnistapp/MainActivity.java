package com.example.mnistapp;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

// 导入 TFLite 相关库 (占位，稍后会用到具体类)
// import org.tensorflow.lite.Interpreter;
// import org.tensorflow.lite.support.image.TensorImage;
// import org.tensorflow.lite.support.label.Category;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Collections;
import java.util.Comparator;

// 1. 添加 DigitClassifier 的 import
import com.example.mnistapp.DigitClassifier;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private DrawingView drawingView;
    private Button clearButton;
    private Button detectButton;
    private TextView resultTextView;

    // TensorFlow Lite 相关变量 (占位)
    // private Interpreter tflite;
    // 2. 取消注释 digitClassifier 变量
    private DigitClassifier digitClassifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 设置布局文件

        // 获取 UI 元素引用
        drawingView = findViewById(R.id.drawing_view);
        clearButton = findViewById(R.id.clear_button);
        detectButton = findViewById(R.id.detect_button);
        resultTextView = findViewById(R.id.result_text_view);

        // 设置按钮点击监听器
        clearButton.setOnClickListener(v -> clearDrawing());
        detectButton.setOnClickListener(v -> detectDigit());

        // 3. 在 onCreate 中调用初始化方法
        setupDigitClassifier();

        Log.d(TAG, "Activity Created");
    }

    private void clearDrawing() {
        Log.d(TAG, "Clear button clicked");
        if (drawingView != null) {
            drawingView.clearCanvas();
        }
        resultTextView.setText("预测结果: -");
        Toast.makeText(this, "画布已清除", Toast.LENGTH_SHORT).show();
    }

    private void detectDigit() {
        Log.d(TAG, "Detect button clicked");
        if (drawingView == null) {
            Log.e(TAG, "Drawing view is null");
            return;
        }

        Bitmap drawingBitmap = drawingView.getDrawingBitmap();
        if (drawingBitmap == null) {
             Log.e(TAG, "Drawing bitmap is null");
             Toast.makeText(this, "无法获取绘图内容", Toast.LENGTH_SHORT).show();
             return;
        }

        // 4. 取消注释并适配模型调用逻辑
        if (digitClassifier != null) {
            // 注意：classify 是同步调用，如果耗时较长，应考虑放到后台线程
            float[] probabilities = digitClassifier.classify(drawingBitmap);

            if (probabilities != null) {
                // 调用新的 displayResult 方法处理 float 数组
                displayResult(probabilities);
            } else {
                 Log.e(TAG, "Classification returned null.");
                 Toast.makeText(this, "识别失败", Toast.LENGTH_SHORT).show();
                 resultTextView.setText("预测结果: 失败");
            }
        } else {
             Log.e(TAG, "Digit classifier not initialized");
             Toast.makeText(this, "分类器未初始化", Toast.LENGTH_SHORT).show();
        }

        // --- 移除临时的识别提示 ---
        // Toast.makeText(this, "识别功能待实现", Toast.LENGTH_SHORT).show();
        // resultTextView.setText("预测结果: ?");
        // --- 移除结束 ---
    }

    // 5. 取消注释并实现 setupDigitClassifier
    private void setupDigitClassifier() {
        try {
            digitClassifier = new DigitClassifier(this);
            digitClassifier.initialize(); // 加载模型
            Log.i(TAG, "Digit Classifier Initialized.");
            Toast.makeText(this, "模型加载成功", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize Digit Classifier.", e);
            Toast.makeText(this, "无法加载模型: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // 可以考虑在这里禁用识别按钮
            // detectButton.setEnabled(false);
        }
    }

    // 6. 实现新的 displayResult 方法 (处理 float[])
    private void displayResult(float[] probabilities) {
       if (probabilities == null || probabilities.length == 0) {
           resultTextView.setText("预测结果: 无效");
           return;
       }

       // 寻找概率最高的索引 (即识别出的数字)
       int maxIndex = -1;
       float maxProbability = -1.0f;
       for (int i = 0; i < probabilities.length; i++) {
           if (probabilities[i] > maxProbability) {
               maxProbability = probabilities[i];
               maxIndex = i;
           }
       }

       if (maxIndex != -1) {
            // 显示结果，格式化为： 数字 (概率%)
            String resultString = String.format(Locale.US, "预测结果: %d (%.1f%%)",
                                               maxIndex, maxProbability * 100.0f);
            resultTextView.setText(resultString);
            Log.d(TAG, "Displaying result: " + resultString);
       } else {
           resultTextView.setText("预测结果: 无法确定");
           Log.d(TAG, "Could not determine max probability.");
       }
    }

    @Override
    protected void onDestroy() {
        // 释放资源 (占位)
        // if (digitClassifier != null) {
        //     digitClassifier.close();
        // }
        Log.d(TAG, "Activity Destroyed");
        // 7. 取消注释 digitClassifier.close()
        if (digitClassifier != null) {
            digitClassifier.close();
        }
        super.onDestroy();
    }
}