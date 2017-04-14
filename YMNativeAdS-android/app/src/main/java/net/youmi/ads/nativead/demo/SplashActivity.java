package net.youmi.ads.nativead.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import net.youmi.ads.base.log.DLog;
import net.youmi.ads.nativead.YoumiNativeAdHelper;
import net.youmi.ads.nativead.adrequest.OnYoumiNativeAdRequestListener;
import net.youmi.ads.nativead.adrequest.YoumiNativeAdModel;
import net.youmi.ads.nativead.adrequest.YoumiNativeAdResposeModel;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

/**
 * 延迟5秒钟打开demo主页，5秒钟内尝试加载原生开屏广告
 *
 * @author zhitao
 * @since 2017-04-14 10:32
 */
public class SplashActivity extends BaseActivity implements View.OnClickListener {
	
	private final static int MSG_START_MAIN_ACTIVITY = 1;
	
	/**
	 * 为了展示方便，这里设置大一点的时间
	 */
	private final static int DELAY_TIME = 5000;
	
	private ImageView mImageView;
	
	private TextView mTextView;
	
	private MyHandler mHandler;
	
	private boolean mIsDestroy;
	
	private YoumiNativeAdModel mYoumiNativeAdModel;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_splash);
		mTextView = (TextView) findViewById(R.id.tv_splash_msg);
		mImageView = (ImageView) findViewById(R.id.iv_splash_ad);
		mImageView.setOnClickListener(this);
		
		// 延迟一定时间后打开主界面
		mHandler = new MyHandler(this);
		mHandler.sendEmptyMessageDelayed(MSG_START_MAIN_ACTIVITY, DELAY_TIME);
		
		// 发起一个原生开屏广告位广告请求
		YoumiNativeAdHelper
				
				// 创建一个原生广告请求
				.newAdRequest(this)
				
				// （必须）指定appId
				.withAppId(BuildConfig.APPID)
				
				// （必须）指定请求广告位
				.withSlotId("7906")
				
				// （可选）指定请求广告数量，默认为1
				.withRequestCount(1)
				
				// （可选）其他可选配置，具体可以看代码注释
				//	.withAge("18")
				//	.withContentKeyword("游戏")
				//	.withContentTitle("好玩的游戏")
				//	.withGender("M")
				//	.withReqId("123456")
				//	.withUserAgent("username")
				
				// 发起同步请求
				// .request();
				
				// 发起异步请求
				.requestWithListener(new MyOnYoumiNativeAdRequesterListener(this));
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mHandler.removeMessages(MSG_START_MAIN_ACTIVITY);
		mIsDestroy = true;
		DLog.i("onDestroy");
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.iv_splash_ad:
			if (mYoumiNativeAdModel == null) {
				break;
			}
			// 点击了图片之后需要发送点击记录，进入详情页或者下载
			YoumiNativeAdHelper.newAdEffRequest(this)
			                   .withAppId(BuildConfig.APPID)
			                   .withYoumiNativeAdModel(mYoumiNativeAdModel)
			                   .asyncSendClickeff();
			Toast.makeText(
					this,
					String.format(Locale.getDefault(), "发送广告位 %d 的点击记录", mYoumiNativeAdModel.getSlotId()),
					Toast.LENGTH_SHORT
			).show();
			
			break;
		default:
			break;
		}
		
	}
	
	private static class MyOnYoumiNativeAdRequesterListener implements OnYoumiNativeAdRequestListener {
		
		private WeakReference<Activity> mReference;
		
		MyOnYoumiNativeAdRequesterListener(Activity context) {
			mReference = new WeakReference<>(context);
		}
		
		/**
		 * 请求结束（本回调执行在UI线程中）
		 * <p>
		 * 注意：这里只是请求结束，是否请求成功，返回数据是否正常还需要开发者针对返回结果做判断
		 *
		 * @param respModel 返回的请求结果
		 */
		@Override
		public void onRequestFinish(YoumiNativeAdResposeModel respModel) {
			DLog.i("请求成功");
			final SplashActivity activity = (SplashActivity) mReference.get();
			if (activity == null) {
				DLog.i("但是activity已经被回收，不展示");
				return;
			}
			// activity还没有被回收，检查是否已经被销毁，销毁了的话就不加载
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
				if (activity.mIsDestroy) {
					DLog.i("activity已经销毁，不再加载图片");
					return;
				}
			} else {
				if (activity.isDestroyed()) {
					DLog.i("activity已经销毁，不再加载图片");
					return;
				}
			}
			
			if (respModel == null || respModel.getCode() != 0) {
				return;
			}
			ArrayList<YoumiNativeAdModel> adModels = respModel.getAdModels();
			if (adModels == null || adModels.isEmpty()) {
				return;
			}
			
			// 因为只请求一个广告，所以这里就直接取0index
			final YoumiNativeAdModel adModel = adModels.get(0);
			ArrayList<YoumiNativeAdModel.YoumiNativeAdPicObject> pics = adModel.getAdPics();
			if (pics == null || pics.isEmpty()) {
				return;
			}
			
			for (YoumiNativeAdModel.YoumiNativeAdPicObject pic : pics) {
				if (pic == null) {
					continue;
				}
				activity.mTextView.setText("请求广告成功，开始加载图片");
				activity.mImageView.bringToFront();
				
				// 直接加载第一张图片
				// 实际使用时，可根据具体要求进行处理
				Glide.with(activity).load(pic.getUrl()).listener(new RequestListener<String, GlideDrawable>() {
					@Override
					public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean
							isFirstResource) {
						return false;
					}
					
					@Override
					public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target,
							boolean isFromMemoryCache, boolean isFirstResource) {
						activity.mYoumiNativeAdModel = adModel;
						// 发送曝光记录
						YoumiNativeAdHelper.newAdEffRequest(activity)
						                   .withAppId(BuildConfig.APPID)
						                   .withYoumiNativeAdModel(adModel)
						                   .asyncSendShowEff();
						Toast.makeText(
								activity,
								String.format(Locale.getDefault(), "发送广告位 %d 的曝光记录", adModel.getSlotId()),
								Toast.LENGTH_SHORT
						).show();
						return false;
						
					}
				}).into(activity.mImageView);
			}
		}
	}
	
	private static class MyHandler extends Handler {
		
		private WeakReference<Activity> mReference;
		
		MyHandler(Activity context) {
			mReference = new WeakReference<>(context);
		}
		
		@Override
		public void handleMessage(Message msg) {
			SplashActivity activity = (SplashActivity) mReference.get();
			if (activity == null) {
				return;
			}
			switch (msg.what) {
			case MSG_START_MAIN_ACTIVITY:
				activity.startActivity(new Intent(activity, MainActivity.class));
				activity.finish();
				break;
			default:
				break;
			}
		}
	}
	
}
