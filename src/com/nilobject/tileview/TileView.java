package com.nilobject.tileview;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.GestureDetector.OnGestureListener;
import android.view.View;

public abstract class TileView extends View implements OnGestureListener {

	private int mVirtualWidth, mVirtualHeight;
	private int mOffsetX, mOffsetY;
	private int mTileSize;
	
	private GestureDetector mGestureDetector = new GestureDetector(this);
	private Handler mHandler = new Handler();
	
	private class Tile {
		Drawable drawable;
		int x;
		int y;
		Tile next;
		Tile previous;
	}
	
	public TileView(Context context) {
		super(context);
	}
	
	public TileView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public TileView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	private Tile mTileCacheHead;
	private int mTileCacheCount;
	final int TILE_CACHE_LIMIT = 32;
	
	public int getVirtualWidth() { return mVirtualWidth; }
	public void setVirtualWidth(int width) { mVirtualWidth = width; }
	
	public int getVirtualHeight() { return mVirtualHeight; }
	public void setVirtualHeight(int height) { mVirtualHeight = height; }
	
	public int getOffsetX() { return mOffsetX; }
	public void setOffsetX(int x) { mOffsetX = x; }
	
	public int getOffsetY() { return mOffsetY; }
	public void setOffsetY(int y) { mOffsetY = y; }
	
	public int getTileSize() { return mTileSize; }
	public void setTileSize(int size) { mTileSize = size; }
	
	abstract Drawable getTile(int x, int y);
	
	private Tile getTileAt(int x, int y) {
		Tile currentTile = mTileCacheHead;
		while (currentTile != null) {
			if (currentTile.x == x && currentTile.y == y) {
				// Move this tile to the front to avoid it being disposed of
				if (currentTile.next != null) currentTile.next.previous = currentTile.previous;
				if (currentTile.previous != null) currentTile.previous.next = currentTile.next;
				mTileCacheHead.previous = currentTile;
				currentTile.next = mTileCacheHead;
				mTileCacheHead = currentTile;
				return currentTile;
			}
			// Break out of the loop with currentTile set to the last tile
			// We do this simply to avoid needing to loop through the list again
			if (currentTile.next == null) break;
			currentTile = currentTile.next;
		}
		
		// Couldn't find a tile. First, remove the last element if we're past our threshold
		if (mTileCacheCount == TILE_CACHE_LIMIT) {
			currentTile.previous.next = null;
			currentTile.previous = null;
		} else {
			currentTile = new Tile();
			mTileCacheCount++;
		}
		currentTile.drawable = getTile(x, y);
		currentTile.x = x;
		currentTile.y = y;
		if (mTileCacheHead != null) mTileCacheHead.previous = currentTile;
		currentTile.next = mTileCacheHead;
		currentTile.previous = null;
		mTileCacheHead = currentTile;
		return currentTile;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		int currentX = mOffsetX;
		int currentY = mOffsetY;
		int x = currentX / mTileSize;
		int maxX = (currentX + canvas.getWidth() + mTileSize - 1) / mTileSize;
		for (; x < maxX; x++) {
			int y = currentY / mTileSize;
			int maxY = (currentY + canvas.getHeight() + mTileSize - 1) / mTileSize;
			for (; y < maxY; y++) {
				Tile t = getTileAt(x, y);
				int cx = x * mTileSize - currentX;
				int cy = y * mTileSize - currentY;
				t.drawable.setBounds(cx, cy, cx + mTileSize, cy + mTileSize);
				t.drawable.draw(canvas);
			}
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return mGestureDetector.onTouchEvent(event);
	}
	
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		mOffsetX += (int)distanceX;
		mOffsetY += (int)distanceY;
		mOffsetX = Math.min(mVirtualWidth, mOffsetX);
		mOffsetX = Math.max(0, mOffsetX);
		mOffsetY = Math.min(mVirtualHeight, mOffsetY);
		mOffsetY = Math.max(0, mOffsetY);
		invalidate();
		return false;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		if (flingTimer != null) {
			flingTimer.cancel();
			flingTimer = null;
		}
		currentFlingVelocityX = 0;
		currentFlingVelocityY = 0;
		return true;
	}

	private float currentFlingVelocityX, currentFlingVelocityY;
	private long lastFlingTime;
	private Timer flingTimer;
	private Runnable invalidateRunnable = new Runnable() {
		@Override
		public void run() {
			TileView.this.invalidate();
		}
	};
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		currentFlingVelocityX += velocityX;
		currentFlingVelocityY += velocityY;
		lastFlingTime = System.currentTimeMillis();
		if (flingTimer == null) {
			flingTimer = new Timer();
			flingTimer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					long currentFlingTime = System.currentTimeMillis();
					float elapsed = (currentFlingTime - lastFlingTime) / 1000f;
					mOffsetX -= (int)(currentFlingVelocityX * elapsed);
					mOffsetY -= (int)(currentFlingVelocityY * elapsed);
					mOffsetX = Math.min(mVirtualWidth, mOffsetX);
					mOffsetX = Math.max(0, mOffsetX);
					mOffsetY = Math.min(mVirtualHeight, mOffsetY);
					mOffsetY = Math.max(0, mOffsetY);
					lastFlingTime = currentFlingTime;
					
					currentFlingVelocityX -= currentFlingVelocityX * (elapsed * 1.2);
					currentFlingVelocityY -= currentFlingVelocityY * (elapsed * 1.2);
					
					if (Math.abs(currentFlingVelocityX) < 10 && Math.abs(currentFlingVelocityY) < 10) {
						flingTimer.cancel();
						flingTimer = null;
					}
					
					mHandler.removeCallbacks(invalidateRunnable);
					mHandler.post(invalidateRunnable);
				}
			}, 0, 30);
		}
		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}
}
