package com.example.mnistapp;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix; // Needed for resizing
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer; // Use ByteBuffer for input
import java.nio.ByteOrder; // Specify byte order
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class DigitClassifier {

    private static final String TAG = "DigitClassifier";

    private final Context context;
    private Interpreter tflite;

    // MNIST 模型的输入/输出细节
    private static final String MODEL_FILE_NAME = "mnist.tflite";
    private static final int INPUT_IMAGE_WIDTH = 28;
    private static final int INPUT_IMAGE_HEIGHT = 28;
    private static final int OUTPUT_CLASSES_COUNT = 10;
    private static final int BYTES_PER_CHANNEL = 1; // MNIST is grayscale, 1 byte per pixel
    private static final int PIXEL_SIZE = 1; // Grayscale
    private static final int BATCH_SIZE = 1;

    // Buffer for input image data
    private ByteBuffer inputBuffer = null;

    // Buffer for output probabilities
    private float[][] outputBuffer = null;


    public DigitClassifier(Context context) {
        this.context = context;
    }

    /**
     * Initializes the TensorFlow Lite interpreter.
     * @throws IOException If the model file fails to load.
     */
    public void initialize() throws IOException {
        try {
            Interpreter.Options options = new Interpreter.Options();
            MappedByteBuffer modelBuffer = loadModelFile(context.getAssets(), MODEL_FILE_NAME);
            tflite = new Interpreter(modelBuffer, options);

            // Allocate the input buffer (Change back to FLOAT32 size)
            int inputSize = BATCH_SIZE * INPUT_IMAGE_HEIGHT * INPUT_IMAGE_WIDTH * 4; // 4 bytes per float pixel
            inputBuffer = ByteBuffer.allocateDirect(inputSize);
            inputBuffer.order(ByteOrder.nativeOrder());

            // Allocate the output buffer
            outputBuffer = new float[BATCH_SIZE][OUTPUT_CLASSES_COUNT];

            Log.i(TAG, "TensorFlow Lite Interpreter Initialized.");

            // Log model input/output details
            int[] inputShape = tflite.getInputTensor(0).shape();
            org.tensorflow.lite.DataType inputType = tflite.getInputTensor(0).dataType(); // 获取输入类型
            int[] outputShape = tflite.getOutputTensor(0).shape();
            org.tensorflow.lite.DataType outputType = tflite.getOutputTensor(0).dataType();// 获取输出类型

            Log.i(TAG, "Model Input Shape: " + Arrays.toString(inputShape));
            Log.i(TAG, "Model Input DataType: " + inputType); // 打印输入类型
            Log.i(TAG, "Model Output Shape: " + Arrays.toString(outputShape));
            Log.i(TAG, "Model Output DataType: " + outputType); // 打印输出类型

        } catch (IOException e) {
            Log.e(TAG, "Error initializing TensorFlow Lite interpreter.", e);
            tflite = null;
            throw e;
        }
    }

    /**
     * Loads the model file from the assets folder.
     */
    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Classifies the input Bitmap.
     * @param bitmap The Bitmap image to classify.
     * @return An array of 10 floats representing the confidence for each digit (0-9), or null on failure.
     */
    public float[] classify(Bitmap bitmap) {
        if (tflite == null) {
            Log.e(TAG, "TensorFlow Lite interpreter is not initialized.");
            return null;
        }
        if (bitmap == null) {
            Log.e(TAG, "Input bitmap is null.");
            return null;
        }
        if (inputBuffer == null || outputBuffer == null) {
             Log.e(TAG, "Input or output buffer is not allocated.");
             return null;
        }

        // 1. Preprocess the image and load data into inputBuffer
        preprocessImageToBuffer(bitmap);

        // 2. Run inference
        try {
            tflite.run(inputBuffer, outputBuffer);
            Log.d(TAG, "Inference successful.");
            // Return the array of probabilities
            return outputBuffer[0];
        } catch (Exception e) {
            Log.e(TAG, "Error running inference.", e);
            return null;
        }
    }

    /**
     * Preprocesses the input Bitmap and puts the data into the input ByteBuffer.
     * Assumes the model expects UINT8 input [0-255] with digit as white/light on black background.
     * @param bitmap The original Bitmap (black stroke on white background).
     */
    private void preprocessImageToBuffer(Bitmap bitmap) {
        Bitmap resizedBitmap = getResizedBitmap(bitmap, INPUT_IMAGE_WIDTH, INPUT_IMAGE_HEIGHT);
        inputBuffer.rewind();
        int[] intValues = new int[INPUT_IMAGE_WIDTH * INPUT_IMAGE_HEIGHT];
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < INPUT_IMAGE_HEIGHT; ++i) {
            for (int j = 0; j < INPUT_IMAGE_WIDTH; ++j) {
                final int val = intValues[pixel++];
                int r = Color.red(val);
                int g = Color.green(val);
                int b = Color.blue(val);
                // Calculate luminance (standard formula)
                double luminance = (0.299 * r + 0.587 * g + 0.114 * b);

                // Invert colors: White background (high luminance) becomes 0, Black stroke (low luminance) becomes 1.
                // Normalize to [0, 1] float.
                float normalizedPixelValue = (float) ((255.0 - luminance) / 255.0); // Invert and normalize

                // --- Model expects FLOAT32 ---
                inputBuffer.putFloat(normalizedPixelValue); // Write float

                // --- Keep the UINT8 logic commented out ---
                // byte grayscaleByte = (byte) (255.0 - luminance);
                // inputBuffer.put(grayscaleByte);
            }
        }
        Log.d(TAG, "Image preprocessing to buffer (FLOAT32, inverted) successful."); // Update log message
        // Recycle the resized bitmap if it's no longer needed and wasn't the original
        if (resizedBitmap != bitmap && !resizedBitmap.isRecycled()) {
            // resizedBitmap.recycle();
        }
    }

    /**
     * Resizes a Bitmap to the specified dimensions.
     */
    private Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        // Recycle the original bitmap if it's not the same as the resized one
        if (resizedBitmap != bm && !bm.isRecycled()) {
             // bm.recycle(); // Be careful with recycling if the original might be needed elsewhere
        }
        return resizedBitmap;
    }

    /**
     * Closes the TensorFlow Lite interpreter.
     */
    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
            Log.i(TAG, "TensorFlow Lite Interpreter closed.");
        }
    }
} 