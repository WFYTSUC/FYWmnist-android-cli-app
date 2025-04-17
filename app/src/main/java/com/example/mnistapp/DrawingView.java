package com.example.mnistapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;

public class DrawingView extends View {

    private Path drawPath;
    private Paint drawPaint, canvasPaint;
    private Canvas drawCanvas;
    private Bitmap canvasBitmap;

    private static final float STROKE_WIDTH = 20f; // 画笔宽度

    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }

    private void setupDrawing() {
        drawPath = new Path();
        drawPaint = new Paint();

        drawPaint.setColor(Color.BLACK); // 画笔颜色设为黑色
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(STROKE_WIDTH);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        // 画布画笔 (用于绘制位图)
        canvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // View 大小确定后，创建 Bitmap 和 Canvas
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
        clearCanvas(); // 初始化为白色背景
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 将我们绘制的 Bitmap 绘制到 View 的 Canvas 上
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        // 绘制当前正在画的路径
        canvas.drawPath(drawPath, drawPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawPath.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                // 将完成的路径绘制到我们的 Bitmap 画布上
                drawCanvas.drawPath(drawPath, drawPaint);
                // 重置路径，准备下一次绘制
                drawPath.reset();
                break;
            default:
                return false;
        }

        // 触发 onDraw 重新绘制 View
        invalidate();
        return true;
    }

    /**
     * 清除画布
     */
    public void clearCanvas() {
        if (drawCanvas != null) {
            // 用白色填充画布
            drawCanvas.drawColor(Color.WHITE);
            // 重置路径
            drawPath.reset();
            // 触发重绘
            invalidate();
        }
    }

    /**
     * 获取绘制内容的 Bitmap
     * @return 包含绘制内容的 Bitmap
     */
    public Bitmap getDrawingBitmap() {
        // 创建一个只包含内容的副本，避免直接修改原始 Bitmap
        return Bitmap.createBitmap(canvasBitmap); 
    }
}
