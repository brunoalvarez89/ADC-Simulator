package com.ufavaloro.android.simulador.adc;

import java.util.ArrayList;
import android.os.Handler;

public class AdcSimulator extends Thread {
	// Channels of the ADC
	private ArrayList<AdcChannel> mChannels = new ArrayList<AdcChannel>();
	// Handler to Invoking Activity
	private Handler mHandler;
	// Valor de la muestra en short int
	private short[] mSamples;
	// Lock de pausa
	private Object mPauseLock = new Object();
	// Flag de pausa
	private boolean mPaused = false;
	// Flag de run
	private boolean mRun;
	// Current Channel
	private int mCurrentChannel = 0;
	// Total Channels
	private int mTotalChannels;

	// Constructor de clase
	public AdcSimulator(Handler mHandler, int mCantCanales, int mCantMuestras, double mFs, int mBits) {
		this.mHandler = mHandler;
		this.mTotalChannels = mCantCanales;
		mSamples = new short[mCantMuestras];
		
		for(int i = 0; i < mCantCanales; i++) {
			AdcChannel canal = new AdcChannel(mFs, mBits, mCantMuestras);
			mChannels.add(canal);
		}
	}
		
	// Generación de muestras
	@Override
	public void run() {
		while(mRun) {			
			mSamples = mChannels.get(mCurrentChannel).getSamples();			
			mHandler.obtainMessage(AdcSimulatorMessage.MENSAJE_MUESTRA.getValue(), -1, 
								   mCurrentChannel, mSamples.clone()).sendToTarget();
			nextChannel();
			candadoPausa();
		}
	}
	
	public void nextChannel() {
		mCurrentChannel++;
		if(mCurrentChannel == mTotalChannels) mCurrentChannel = 0;
	}
	
	private void candadoPausa() {
		
		synchronized(mPauseLock) {
			
			while(mPaused) {
				
				try {
					mPauseLock.wait();
				} catch (InterruptedException e) {}
			
			}
		
		}
	
	}
		
	public void onPause() {
		synchronized (mPauseLock) {
			mPaused = true;
		}
	}
		
	public void onResume() {
	    synchronized (mPauseLock) {
	        mPaused = false;
	        mPauseLock.notifyAll();
	    }
	}
		
	public void setRunning(boolean mRun) {
		this.mRun = mRun;
	}

	public boolean getPaused() {
		return mPaused;
	}
	
	public AdcChannel getChannel(int index) {
		return mChannels.get(index);
	}
	
}
