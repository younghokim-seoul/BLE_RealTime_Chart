package com.cm.rxandroidble.ui.widget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

import com.cm.rxandroidble.util.L;
import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.renderer.LineChartRenderer;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

public class MyLineChartRenderer extends LineChartRenderer {

    private Paint mHighlightCirclePaint;
    private boolean isHeart;
    float[] pos;
    int[] colors;
    int[] range;

    private ViewPortHandler viewPortHandler;

    public MyLineChartRenderer(LineDataProvider chart, ChartAnimator animator, ViewPortHandler viewPortHandler) {
        super(chart, animator, viewPortHandler);
        mChart = chart;
        mCirclePaintInner = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaintInner.setStyle(Paint.Style.FILL);
        mCirclePaintInner.setColor(Color.WHITE);
        mHighlightCirclePaint = new Paint();
        this.viewPortHandler = viewPortHandler;
    }

    private float[] mLineBuffer = new float[4];

    @Override
    protected void drawLinear(Canvas c, ILineDataSet dataSet) {

        int entryCount = dataSet.getEntryCount();

        final boolean isDrawsteppedEnabled = dataSet.isDrawSteppedEnabled();
        final int pointsPerEntryPair = isDrawsteppedEnabled ? 4 : 2;

        Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

        float phaseY = mAnimator.getPhaseY();

        mRenderPaint.setStyle(Paint.Style.STROKE);

        Canvas canvas = null;

        if (dataSet.isDashedLineEnabled()) {
            canvas = mBitmapCanvas;
        } else {
            canvas = c;
        }

        mXBounds.set(mChart, dataSet);

        //if drawing filled is enabled

        if (dataSet.isDrawFilledEnabled() && entryCount > 0) {
            drawLinearFill(c, dataSet, trans, mXBounds);
        }


        L.i("more than 1 color");
        if (dataSet.getColors().size() > 1) {
            if (mLineBuffer.length <= pointsPerEntryPair * 2)
                mLineBuffer = new float[pointsPerEntryPair * 4];

            for (int j = mXBounds.min; j <= mXBounds.range + mXBounds.min; j++) {
                Entry e = dataSet.getEntryForIndex(j);
                if (e == null) continue;
                if (e.getY() == 0) continue;
                mLineBuffer[0] = e.getX();
                mLineBuffer[1] = e.getY() * phaseY;

                if (j < mXBounds.max) {
                    e = dataSet.getEntryForIndex(j + 1);

                    if (e == null) break;
                    if (e.getY() == 0) break;

                    if (isDrawsteppedEnabled) {
                        mLineBuffer[2] = e.getX();
                        mLineBuffer[3] = mLineBuffer[1];
                        mLineBuffer[4] = mLineBuffer[2];
                        mLineBuffer[5] = mLineBuffer[3];
                        mLineBuffer[6] = e.getX();
                        mLineBuffer[7] = e.getY() * phaseY;
                    } else {
                        mLineBuffer[2] = e.getX();
                        mLineBuffer[3] = e.getY() * phaseY;
                    }

                } else {
                    mLineBuffer[2] = mLineBuffer[0];
                    mLineBuffer[3] = mLineBuffer[1];
                }
                trans.pointValuesToPixel(mLineBuffer);

                if (!mViewPortHandler.isInBoundsRight(mLineBuffer[0]))
                    break;

                //make sure th lines dont't to shitty things outside
                //bounds

                if (!mViewPortHandler.isInBoundsLeft(mLineBuffer[2])
                        || (!mViewPortHandler.isInBoundsTop(mLineBuffer[1]) && !mViewPortHandler
                        .isInBoundsBottom(mLineBuffer[3])))
                    continue;

                mRenderPaint.setColor(dataSet.getColor(j));

                canvas.drawLines(mLineBuffer, 0, pointsPerEntryPair * 2, mRenderPaint);
            }
        } else {
            // only one color per dataset
            if (mLineBuffer.length < Math.max((entryCount) * pointsPerEntryPair, pointsPerEntryPair) * 2)
                mLineBuffer = new float[Math.max((entryCount) * pointsPerEntryPair, pointsPerEntryPair) * 4];

            Entry e1, e2;

            e1 = dataSet.getEntryForIndex(mXBounds.min);

            if (e1 != null) {
                int j = 0;
                for (int x = mXBounds.min; x <= mXBounds.range + mXBounds.min; x++) {
                    e1 = dataSet.getEntryForIndex(x == 0 ? 0 : (x - 1));
                    e2 = dataSet.getEntryForIndex(x);

                    if (e1.getY() == 0 || e2.getY() == 0) {
                        continue;
                    }

                    mLineBuffer[j++] = e1.getX();
                    mLineBuffer[j++] = e1.getY() + phaseY;

                    if (isDrawsteppedEnabled) {
                        mLineBuffer[j++] = e2.getX();
                        mLineBuffer[j++] = e1.getY() * phaseY;
                        mLineBuffer[j++] = e2.getX();
                        mLineBuffer[j++] = e1.getY() * phaseY;
                    }

                    mLineBuffer[j++] = e2.getX();
                    mLineBuffer[j++] = e2.getY() * phaseY;

                }

                if (j > 0) {
                    trans.pointValuesToPixel(mLineBuffer);

                    final int size = Math.max((mXBounds.range + 1) * pointsPerEntryPair, pointsPerEntryPair) * 2;

                    mRenderPaint.setColor(dataSet.getColor());

                    if (isHeart) {
                        LinearGradient liner = new LinearGradient(0, mViewPortHandler.getContentRect().top,
                                0, mViewPortHandler.getContentRect().bottom, colors, pos, Shader.TileMode.CLAMP);
                        liner.setLocalMatrix(viewPortHandler.getMatrixTouch());
                        mRenderPaint.setShader(liner);
                    }
                    canvas.drawLines(mLineBuffer, 0, size, mRenderPaint);

                }
            }
        }

        mRenderPaint.setPathEffect(null);
    }

    //cache for the circle bitmaps of all datasets

    private HashMap<IDataSet, DataSetImageCache> mImageCaches = new HashMap<>();
    private float[] mCirclesBuffer = new float[2];

    @Override
    protected void drawCircles(Canvas c) {
        mRenderPaint.setStyle(Paint.Style.FILL);
        float phaseY = mAnimator.getPhaseY();

        mCirclesBuffer[0] = 0;
        mCirclesBuffer[1] = 0;

        List<ILineDataSet> dataSets = mChart.getLineData().getDataSets();



        for (int i = 0; i < dataSets.size(); i++) {
            ILineDataSet dataSet = dataSets.get(i);

            if (!dataSet.isVisible() || !dataSet.isDrawCirclesEnabled() ||
                    dataSet.getEntryCount() == 0)
                continue;


            mCirclePaintInner.setColor(dataSet.getCircleHoleColor());

            Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

            mXBounds.set(mChart, dataSet);

            float circleRadius = dataSet.getCircleRadius();
            float circleHoleRadius = dataSet.getCircleHoleRadius();
            boolean drawCircleHole = dataSet.isDrawCircleHoleEnabled() &&
                    circleHoleRadius < circleRadius &&
                    circleHoleRadius > 0.f;
            boolean drawTransparentCircleHole = drawCircleHole &&
                    dataSet.getCircleHoleColor() == ColorTemplate.COLOR_NONE;



            DataSetImageCache imageCache;

            if (mImageCaches.containsKey(dataSet)) {
                imageCache = mImageCaches.get(dataSet);
            } else {
                imageCache = new DataSetImageCache();
                mImageCaches.put(dataSet, imageCache);
            }

            boolean changeRequired = imageCache.init(dataSet);


            //only fill the cache with new bitmaps if a change is required

            if (changeRequired) {
                imageCache.fill(dataSet, drawCircleHole, drawTransparentCircleHole);
            }

            int boundsRangeCount = mXBounds.range + mXBounds.min;

            for (int j = mXBounds.min; j <= boundsRangeCount; j++) {
                Entry e = dataSet.getEntryForIndex(j);

                if (e == null) break;
                if (e.getY() == 0) continue;
                mCirclesBuffer[0] = e.getX();
                mCirclesBuffer[1] = e.getY() * phaseY;

                trans.pointValuesToPixel(mCirclesBuffer);

                if (!mViewPortHandler.isInBoundsRight(mCirclesBuffer[0]))
                    break;

                if (!mViewPortHandler.isInBoundsLeft(mCirclesBuffer[0]) ||
                        !mViewPortHandler.isInBoundsY(mCirclesBuffer[1]))
                    continue;


                Bitmap circleBitmap = imageCache.getBitmap(j);
                Paint paint = new Paint();
                paint.setColor(Color.GREEN);
                if (circleBitmap != null) {
                    c.drawBitmap(circleBitmap, mCirclesBuffer[0] - circleRadius, mCirclesBuffer[1] - circleRadius, null);
                }
            }

        }

    }

    public class DataSetImageCache {
        private Path mCirclePathBuffer = new Path();

        private Bitmap[] circleBitmaps;

        protected boolean init(ILineDataSet set) {
            int size = set.getCircleColorCount();

            boolean changeRequired = false;

            if (circleBitmaps == null) {
                circleBitmaps = new Bitmap[size];
                changeRequired = true;
            } else if (circleBitmaps.length != size) {
                circleBitmaps = new Bitmap[size];
                changeRequired = true;
            }
            return changeRequired;
        }

        protected void fill(ILineDataSet set, boolean drawCircleHole, boolean drawTransparentCircleHole) {
            int colorCount = set.getCircleColorCount();
            float circleRadius = set.getCircleRadius();
            float circleHoleRadius = set.getCircleHoleRadius();


            Timber.i("::::::colorCount " + colorCount);


            for (int i = 0; i < colorCount; i++) {
                Bitmap.Config conf = Bitmap.Config.ARGB_4444;
                Bitmap circleBitmap = Bitmap.createBitmap((int) (circleRadius * 2.1), (int) (circleRadius * 2.1), conf);

                Canvas canvas = new Canvas(circleBitmap);
                circleBitmaps[i] = circleBitmap;
                mRenderPaint.setColor(set.getCircleColor(i));

                if (drawTransparentCircleHole) {
                    mCirclePathBuffer.reset();

                    mCirclePathBuffer.addCircle(
                            circleRadius,
                            circleRadius,
                            circleRadius,
                            Path.Direction.CW);

                    //Cut hole in path
                    mCirclePathBuffer.addCircle(
                            circleRadius,
                            circleRadius,
                            circleHoleRadius,
                            Path.Direction.CCW);

                    canvas.drawPath(mCirclePathBuffer, mRenderPaint);

                } else {

                    Paint paint = new Paint();
                    paint.setColor(Color.BLACK);

                    canvas.drawCircle(
                            circleRadius,
                            circleRadius,
                            circleRadius,
                            paint);

                    if (drawCircleHole) {

                        canvas.drawCircle(
                                circleRadius,
                                circleRadius,
                                circleHoleRadius,
                                paint);
                    }
                }

            }
        }

        protected Bitmap getBitmap(int index) {
            return circleBitmaps[index % circleBitmaps.length];
        }
    }

    @Override
    public void drawHighlighted(Canvas c, Highlight[] indices) {
        super.drawHighlighted(c, indices);

        L.i(":::::drawHighlighted");
        float phaseY = mAnimator.getPhaseY();
        ILineDataSet lineData = mChart.getLineData().getDataSetByIndex(0);
        Transformer trans = mChart.getTransformer(lineData.getAxisDependency());
        mCirclesBuffer[0] = 0;
        mCirclesBuffer[1] = 0;

        for (Highlight high : indices) {
            Entry e = lineData.getEntryForXValue(high.getX(), high.getY());
            mCirclesBuffer[0] = e.getX();
            mCirclesBuffer[1] = e.getY() * phaseY;
            trans.pointValuesToPixel(mCirclesBuffer);
            mHighlightCirclePaint.setColor(lineData.getHighLightColor());

            if (isHeart) {
                if (e.getY() <= range[0]) {
                    mHighlightCirclePaint.setColor(colors[0]);
                } else if (e.getY() < range[0] && e.getY() >= range[1]) {
                    mHighlightCirclePaint.setColor(colors[2]);
                } else if (e.getY() >= range[2] && e.getY() < range[1]) {
                    mHighlightCirclePaint.setColor(colors[4]);
                } else {
                    mHighlightCirclePaint.setColor(colors[6]);
                }
            }
            c.drawCircle(mCirclesBuffer[0], mCirclesBuffer[1], 10, mHighlightCirclePaint);
            mHighlightCirclePaint.setColor(Color.WHITE);
            c.drawCircle(mCirclesBuffer[0], mCirclesBuffer[1], 5, mHighlightCirclePaint);
        }
    }

    @Override
    protected void drawHorizontalBezier(ILineDataSet dataSet) {

        float phaseY = mAnimator.getPhaseY();

        Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

        mXBounds.set(mChart, dataSet);

        cubicPath.reset();

        if (mXBounds.range >= 1) {

            Entry prev = dataSet.getEntryForIndex(mXBounds.min);
            Entry cur = prev;

            //let the spline start
            cubicPath.moveTo(cur.getX(), cur.getY() * phaseY);

            for (int j = mXBounds.min + 1; j <= mXBounds.range + mXBounds.min; j++) {
                prev = cur;
                cur = dataSet.getEntryForIndex(j);

                final float cpx = (prev.getX())
                        + (cur.getX() - prev.getX()) / 2.0f;

                cubicPath.cubicTo(
                        cpx, prev.getY() * phaseY,
                        cpx, cur.getY() * phaseY,
                        cur.getX(), cur.getY() * phaseY);

            }
        }

        // if filled is enabled , close the path

        if (dataSet.isDrawFilledEnabled()) {
            cubicFillPath.reset();
            cubicFillPath.addPath(cubicPath);
            drawCubicFill(mBitmapCanvas, dataSet, cubicFillPath, trans, mXBounds);
        }

        mRenderPaint.setColor(dataSet.getColor());
        mRenderPaint.setStyle(Paint.Style.STROKE);

        trans.pathValueToPixel(cubicPath);
        if (isHeart) {
            LinearGradient liner = new LinearGradient(0, mViewPortHandler.getContentRect().top, 0, mViewPortHandler.getContentRect().bottom, colors, pos, Shader.TileMode.CLAMP);
            liner.setLocalMatrix(viewPortHandler.getMatrixTouch());
            mRenderPaint.setShader(liner);
        }

        mBitmapCanvas.drawPath(cubicPath, mRenderPaint);
        mRenderPaint.setPathEffect(null);
    }

    public void setHeartLine(boolean isHeart, int medium, int larger, int limit, int[] colors) {
        this.isHeart = isHeart;
        range = new int[3];
        range[0] = limit;
        range[1] = larger;
        range[2] = medium;
        float[] pos = new float[4];
        float Ymax = ((LineChart) mChart).getAxisLeft().getAxisMaximum();
        float Ymin = ((LineChart) mChart).getAxisLeft().getAxisMinimum();


        L.i(":::yMin " + Ymin + " Ymax " + Ymax);
        pos[0] = (Ymax - limit) / (Ymax - Ymin);
        pos[1] = (limit - larger) / (Ymax - Ymin) + pos[0];
        pos[2] = (larger - medium) / (Ymax - Ymin) + pos[1];
        pos[3] = 1f;

        this.pos = new float[pos.length * 2];
        this.colors = new int[colors.length * 2];
        int index = 0;
        for (int i = 0; i < pos.length; i++) {
            this.colors[index] = colors[i];
            this.colors[index + 1] = colors[i];

            if (i == 0) {
                this.pos[index] = 0f;
                this.pos[index + 1] = pos[i];
            } else {
                this.pos[index] = pos[i - 1];
                this.pos[index + 1] = pos[i];
            }

            index += 2;
        }
    }
}
