package com.imooc.game.pintu.view;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.imooc.game.pintu.R;
import com.imooc.game.utils.ImagePiece;
import com.imooc.game.utils.ImageSplitterUtil;

public class GamePintuLayout extends RelativeLayout implements OnClickListener{

	private int mColumn = 3;
	/**
	 * inner padding
	 */
	private int mPadding;
	/**
	 * inner margin
	 */
	private int mMargin = 3;

	private ImageView[] mGamePintuItems;

	private int mItemWidth;

	/**
	 * image
	 */
	private Bitmap mBitmap;

	private List<ImagePiece> mItemBitmaps;

	private boolean once;

	/**
	 * width of pad
	 */
	private int mWidth;

	private boolean isGameSuccess;
	private boolean isGameOver;

	public interface GamePintuListener{
		void nextLevel(int nextLevel);

		void timechanged(int currentTime);

		void gameover();
	}

	public GamePintuListener mListener;

	/**
	 * 
	 * 
	 * @param mListener
	 */
	public void setOnGamePintuListener(GamePintuListener mListener){
		this.mListener = mListener;
	}

	private int mLevel = 1;
	private static final int TIME_CHANGED = 0x110;
	private static final int NEXT_LEVEL = 0x111;

	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg){
			switch (msg.what){
			case TIME_CHANGED:
				if (isGameSuccess || isGameOver || isPause)
					return;
				if (mListener != null){
					mListener.timechanged(mTime);
				}
				if (mTime == 0){
					isGameOver = true;
					mListener.gameover();
					return;
				}
				mTime--;
				mHandler.sendEmptyMessageDelayed(TIME_CHANGED, 1000);

				break;
			case NEXT_LEVEL:
				mLevel = mLevel + 1;
				if (mListener != null){
					mListener.nextLevel(mLevel);
				} else{
					nextLevel();
				}
				break;

			}
		};
	};

	private boolean isTimeEnabled = false;
	private int mTime;

	/**
	 * set timer
	 * 
	 * @param isTimeEnabled
	 */
	public void setTimeEnabled(boolean isTimeEnabled){
		this.isTimeEnabled = isTimeEnabled;
	}

	public GamePintuLayout(Context context){
		this(context, null);
	}

	public GamePintuLayout(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public GamePintuLayout(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		init();
	}

	private void init(){//transfer to dp
		mMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				3, getResources().getDisplayMetrics());
		mPadding = min(getPaddingLeft(), getPaddingRight(), getPaddingTop(),
				getPaddingBottom());
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		// min of width and height
		mWidth = Math.min(getMeasuredHeight(), getMeasuredWidth());

		if (!once){
			// cut and sort
			initBitmap();
			// set properties of ImageView(Item)
			initItem();
			// check time
			checkTimeEnable();

			once = true;
		}
		// square
		setMeasuredDimension(mWidth, mWidth);

	}

	private void checkTimeEnable(){
		if (isTimeEnabled){
			// set timer
			countTimeBaseLevel();
			mHandler.sendEmptyMessage(TIME_CHANGED);
		}

	}

	private void countTimeBaseLevel(){
		mTime = (int) Math.pow(2, mLevel) * 60;
	}

	/**
	 *cut and sort
	 */
	private void initBitmap(){
		if (mBitmap == null){
			mBitmap = BitmapFactory.decodeResource(getResources(),
					R.drawable.image1);
		}
		mItemBitmaps = ImageSplitterUtil.splitImage(mBitmap, mColumn);

		// disorder
		Collections.sort(mItemBitmaps, new Comparator<ImagePiece>(){
			@Override
			public int compare(ImagePiece a, ImagePiece b){
				return Math.random() > 0.5 ? 1 : -1;
			}
		});

	}

	/**
	 * set width and height of ImageView(Item)
	 */
	private void initItem(){
		mItemWidth = (mWidth - mPadding * 2 - mMargin * (mColumn - 1))
				/ mColumn;
		mGamePintuItems = new ImageView[mColumn * mColumn];
		// create Item，set rule
		for (int i = 0; i < mGamePintuItems.length; i++){
			ImageView item = new ImageView(getContext());
			item.setOnClickListener(this);
			item.setImageBitmap(mItemBitmaps.get(i).getBitmap());

			mGamePintuItems[i] = item;
			item.setId(i + 1);

			// save tag of Item(index)
			item.setTag(i + "_" + mItemBitmaps.get(i).getIndex());

			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
					mItemWidth, mItemWidth);

			//set horizontal distance between column
			// not the last col
			if ((i + 1) % mColumn != 0){
				lp.rightMargin = mMargin;
			}
			// not the first col
			if (i % mColumn != 0){
				lp.addRule(RelativeLayout.RIGHT_OF,
						mGamePintuItems[i - 1].getId());
			}
			// not the first row, add top margin
			if ((i + 1) > mColumn){
				lp.topMargin = mMargin;
				lp.addRule(RelativeLayout.BELOW,
						mGamePintuItems[i - mColumn].getId());
			}
			addView(item, lp);
		}

	}

	public void restart(){
		isGameOver = false;
		mColumn--;
		nextLevel();
	}
	
	private boolean isPause ; 
	
	public void pause(){
		isPause = true ; 
		mHandler.removeMessages(TIME_CHANGED);
	}
	
	public void resume(){
		if(isPause){
			isPause = false ;
			mHandler.sendEmptyMessage(TIME_CHANGED);
		}
	}

	public void nextLevel(){
		this.removeAllViews();
		mAnimLayout = null;
		mColumn++;
		isGameSuccess = false;
		checkTimeEnable();
		initBitmap();
		initItem();
	}

	
	private int min(int... params){
		int min = params[0];

		for (int param : params){
			if (param < min)
				min = param;
		}
		return min;
	}

	//first piece and second one
	private ImageView mFirst;
	private ImageView mSecond;

	@Override
	public void onClick(View v){
		if (isAniming)
			return;

		// double click the same item, cancel click
		if (mFirst == v){
			mFirst.setColorFilter(null);
			mFirst = null;
			return;
		}
		if (mFirst == null){
			mFirst = (ImageView) v;
			//transparent
			mFirst.setColorFilter(Color.parseColor("#55FF0000"));
		} else{
			mSecond = (ImageView) v;
			// exchange Item
			exchangeView();
		}

	}

	
	private RelativeLayout mAnimLayout;
	private boolean isAniming;

	/**
	 * exchange Item
	 */
	private void exchangeView(){
		mFirst.setColorFilter(null);
		
		setUpAnimLayout();

		ImageView first = new ImageView(getContext());
		final Bitmap firstBitmap = mItemBitmaps.get(
				getImageIdByTag((String) mFirst.getTag())).getBitmap();
		first.setImageBitmap(firstBitmap);
		LayoutParams lp = new LayoutParams(mItemWidth, mItemWidth);
		lp.leftMargin = mFirst.getLeft() - mPadding;
		lp.topMargin = mFirst.getTop() - mPadding;
		first.setLayoutParams(lp);
		mAnimLayout.addView(first);

		ImageView second = new ImageView(getContext());
		final Bitmap secondBitmap = mItemBitmaps.get(
				getImageIdByTag((String) mSecond.getTag())).getBitmap();
		second.setImageBitmap(secondBitmap);
		LayoutParams lp2 = new LayoutParams(mItemWidth, mItemWidth);
		lp2.leftMargin = mSecond.getLeft() - mPadding;
		lp2.topMargin = mSecond.getTop() - mPadding;
		second.setLayoutParams(lp2);
		mAnimLayout.addView(second);

		// 设置动画
		TranslateAnimation anim = new TranslateAnimation(0, mSecond.getLeft()
				- mFirst.getLeft(), 0, mSecond.getTop() - mFirst.getTop());
		anim.setDuration(300);
		anim.setFillAfter(true);
		first.startAnimation(anim);

		TranslateAnimation animSecond = new TranslateAnimation(0,
				-mSecond.getLeft() + mFirst.getLeft(), 0, -mSecond.getTop()
						+ mFirst.getTop());
		animSecond.setDuration(300);
		animSecond.setFillAfter(true);
		second.startAnimation(animSecond);

		
		anim.setAnimationListener(new AnimationListener(){
			@Override
			public void onAnimationStart(Animation animation){
				mFirst.setVisibility(View.INVISIBLE);
				mSecond.setVisibility(View.INVISIBLE);

				isAniming = true;
			}

			@Override
			public void onAnimationRepeat(Animation animation){
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animation animation){

				String firstTag = (String) mFirst.getTag();
				String secondTag = (String) mSecond.getTag();

				mFirst.setImageBitmap(secondBitmap);
				mSecond.setImageBitmap(firstBitmap);

				mFirst.setTag(secondTag);
				mSecond.setTag(firstTag);

				mFirst.setVisibility(View.VISIBLE);
				mSecond.setVisibility(View.VISIBLE);

				mFirst = mSecond = null;
				mAnimLayout.removeAllViews();
				// check whether success
				checkSuccess();
				isAniming = false;
			}
		});

	}

	/**
	 * check whether success
	 */
	private void checkSuccess(){
		boolean isSuccess = true;

		for (int i = 0; i < mGamePintuItems.length; i++){
			ImageView imageView = mGamePintuItems[i];
			if (getImageIndexByTag((String) imageView.getTag()) != i){
				isSuccess = false;
			}
		}

		if (isSuccess){
			isGameSuccess = true;
			mHandler.removeMessages(TIME_CHANGED);

			Toast.makeText(getContext(), "Success ， level up !!!",
					Toast.LENGTH_LONG).show();
			mHandler.sendEmptyMessage(NEXT_LEVEL);
		}

	}

	/**
	 * get ID from tag
	 * 
	 * @param tag
	 * @return
	 */
	public int getImageIdByTag(String tag){
		String[] split = tag.split("_");
		return Integer.parseInt(split[0]);
	}

	public int getImageIndexByTag(String tag){
		String[] split = tag.split("_");
		return Integer.parseInt(split[1]);
	}

	
	private void setUpAnimLayout(){
		if (mAnimLayout == null){
			mAnimLayout = new RelativeLayout(getContext());
			addView(mAnimLayout);
		}
	}

}
