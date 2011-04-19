package com.nilobject.tileview;

import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextPaint;

public class TileViewDemo extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new DemoTileView(this));
    }
    
    private class DemoTileView extends TileView {
		public DemoTileView(Context context) {
			super(context);
			setTileSize(256);
			setVirtualHeight(4096);
			setVirtualWidth(4096);
		}

		Random r = new Random();
		TextPaint tp;
		@Override
		Drawable getTile(int x, int y) {	
			Bitmap bmp = Bitmap.createBitmap(getTileSize(), getTileSize(), Bitmap.Config.ARGB_8888);
			if (tp == null) {
				tp = new TextPaint();
				tp.setARGB(255, 255, 255, 255);
			}
			Canvas c = new Canvas(bmp);
			c.drawRGB(r.nextInt(256), r.nextInt(256), r.nextInt(256));
			c.drawText(String.format("%1$d,%2$d", x, y), 0, getTileSize(), tp);
			return new BitmapDrawable(bmp);
		}
    }
}