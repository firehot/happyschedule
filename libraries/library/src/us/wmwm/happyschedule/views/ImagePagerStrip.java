package us.wmwm.happyschedule.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.support.v4.view.ViewPager;
import android.support.v7.internal.widget.LinearLayoutICS;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.StateSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGBuilder;
import com.larvalabs.svgandroid.SVGParser;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import us.wmwm.happyschedule.R;

/**
 * Created by gravener on 10/3/14.
 */
public class ImagePagerStrip extends RelativeLayout implements ViewPager.OnPageChangeListener {

    ViewPager viewPager;

    public static interface ImagePagerStripAdapter {

        int getCount();

        int getSVGResourceId(int position);

        CharSequence getPageTitle(int position);

        void onBack();
    }

    public void setViewPager(ViewPager viewPager) {
        this.viewPager = viewPager;
        viewPager.setOnPageChangeListener(this);
    }

    private static final int RIGHT = 1;
    private static final int LEFT = -1;

    int direction = 0;

    int positionOffsetPixels = -1;

    @Override
    public void onPageScrollStateChanged(int state) {
        if (ViewPager.SCROLL_STATE_IDLE == state) {
            direction = 0;
            positionOffsetPixels = -1;
        }
        if (ViewPager.SCROLL_STATE_SETTLING == state) {
            //System.out.println("SETTLING");
        }
        if (ViewPager.SCROLL_STATE_DRAGGING == state) {
            //System.out.println("DRAGGING");
        }

    }

    @Override
    public void onPageScrolled(int position, float positionOffset,
                               int positionOffsetPixels) {
        if(direction==RIGHT) {
            //System.out.println("RIGHT");
        }
        if(direction==LEFT) {
            //System.out.println("LEFT");
        }
//		System.out.println(String.format("onPageScrolled(%s,%s,%s)", position,
//				positionOffset, positionOffsetPixels));
        if (this.positionOffsetPixels == -1) {
            this.positionOffsetPixels = positionOffsetPixels;
        } else {
            if (direction == 0) {
                if (this.positionOffsetPixels < positionOffsetPixels) {
                    direction = LEFT;
                } else {
                    direction = RIGHT;
                }
            }
        }

        int nextPosition;
        if (LEFT == direction) {
            nextPosition = position + 1;
        } else if (RIGHT == direction) {
            nextPosition = position - 1;
        } else {
            return;
        }
//		System.out.println("\tnext position " + nextPosition);
        if (!(nextPosition < row.getChildCount())) {
            nextPosition = row.getChildCount() - 1;
        }
        if(nextPosition<0) {
            nextPosition = 0;
        }
        //System.out.println("current position" + position + " : " + " next position: " + nextPosition);
        updateTriangle(position,nextPosition,positionOffset);
    }

    @Override
    public void onPageSelected(int sel) {
        for(int i = 0; i < row.getChildCount(); i++) {
            View view = row.getChildAt(i);
            view.setSelected(i==sel);
            if(i==sel) {
                Paint p = indicator.getPaint();
                float width = p.measureText(adapter.getPageTitle(sel).toString());
                int mesWidth = row.getChildAt(0).getMeasuredWidth();
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)indicator.getLayoutParams();
                if(width > mesWidth) {
                    float newWidth = width + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,4,getResources().getDisplayMetrics());
                    float diff = newWidth - mesWidth;
                    lp.leftMargin-=(diff/2);
                    lp.width = (int) newWidth;
                    indicator.setLayoutParams(lp);
                } else {
                    lp.width = mesWidth;
                    indicator.setLayoutParams(lp);
                }
                System.out.println(width + " vs " + view.getMeasuredWidth());
                indicator.setText(adapter.getPageTitle(sel));
            }
        }
    }

    private void updateTriangle(int position, int nextPosition, float positionOffset) {
        System.out.println("position " + position + "  positionOffset " + positionOffset);
        View cur = row.getChildAt(position);
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)indicator.getLayoutParams();

        int offset = position * cur.getMeasuredWidth() + (int)(cur.getMeasuredWidth() * positionOffset);
        lp.leftMargin = offset;
        indicator.setLayoutParams(lp);

    }

    LinearLayout row;

    TextView indicator;

    private ImagePagerStripAdapter adapter;

    public void setAdapter(ImagePagerStripAdapter adapter) {
        this.adapter = adapter;
        notifyDataSetChanged();
    }

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    public static int generateViewId() {
        for (;;) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    public ImagePagerStrip(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(Color.WHITE);
    }

    private void notifyDataSetChanged() {
        removeAllViews();
        row = new LinearLayoutICS(getContext(),null) {
            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                super.onLayout(changed, l, t, r, b);
                //System.out.println("onLayout("+changed+","+l+","+t+","+r+","+b+")");
                if(changed) {
                    if(getChildCount()>0) {
                        View view = getChildAt(0);
                        int width = view.getMeasuredWidth();
                        indicator.getLayoutParams().width=width;
                        indicator.setLayoutParams(indicator.getLayoutParams());
                        System.out.println("width: " + width);
                    }
                }
            }
        };
        row.setOrientation(LinearLayout.HORIZONTAL);
        indicator = new TextView(getContext());

        Resources res = getResources();
        int indicheight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,11.5f,res.getDisplayMetrics());
        indicator.setTextSize(TypedValue.COMPLEX_UNIT_DIP,9);
        indicator.setGravity(Gravity.CENTER);
        indicator.setTextColor(Color.WHITE);
        indicator.setHorizontallyScrolling(true);
        indicator.setSingleLine(true);
        indicator.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        indicator.setIncludeFontPadding(false);
        RelativeLayout.LayoutParams indicLP = new LayoutParams(27,indicheight);


        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,51,res.getDisplayMetrics());
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,12,res.getDisplayMetrics());
        RelativeLayout.LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,height);
        row.setId(ImagePagerStrip.generateViewId());
        addView(row, lp);
        indicLP.addRule(RelativeLayout.ALIGN_BOTTOM,row.getId());
        int color = res.getColor(R.color.get_schedule_11);
        indicator.setBackgroundColor(color);
        addView(indicator,indicLP);
        for(int i =0; i < adapter.getCount(); i++) {
            ImageView image = new ImageView(getContext());
            SVGBuilder b = new SVGBuilder().readFromResource(res, adapter.getSVGResourceId(i));
            b.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));


            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB) {
                image.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
            //image.setColorFilter(Color.rgb(163, 163, 163), PorterDuff.Mode.DST_IN);
            StateListDrawable sld = new StateListDrawable();
            Drawable d = b.build().getDrawable();
            sld.addState(new int[]{android.R.attr.state_pressed}, d);
            sld.addState(new int[]{android.R.attr.state_selected}, d);
            b = new SVGBuilder().readFromResource(res,adapter.getSVGResourceId(i));
            b.setColorFilter(new PorterDuffColorFilter(Color.rgb(163, 163, 163), PorterDuff.Mode.SRC_ATOP));
            sld.addState(StateSet.WILD_CARD, b.build().getDrawable());

            StateListDrawable bsld = new StateListDrawable();
            bsld.addState(new int[] {android.R.attr.state_pressed}, new ColorDrawable(Color.argb(80,Color.red(color),Color.green(color),Color.blue(color))));
            bsld.addState(StateSet.WILD_CARD, new ColorDrawable(Color.TRANSPARENT));
            image.setBackgroundDrawable(bsld);
            image.setPadding(padding, padding, padding, padding);
            image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            image.setImageDrawable(sld);
            image.setTag(i);
            image.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Integer tag = (Integer)v.getTag();
                    if(tag.equals(viewPager.getCurrentItem())) {
                        adapter.onBack();
                    } else {
                        viewPager.setCurrentItem(tag);
                    }
                }
            });
            LinearLayoutICS.LayoutParams lp2 = new LinearLayoutICS.LayoutParams(0,height);
            lp2.weight = 1;
            row.addView(image, lp2);
        }

        View view = new View(getContext());
        view.setBackgroundColor(Color.rgb(199,200,205));
        RelativeLayout.LayoutParams lp2 = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,2);
        lp2.addRule(RelativeLayout.BELOW,row.getId());
        addView(view,lp2);
    }

}