package com.ufavaloro.android.simulador.adc;

import java.util.ArrayList;
import android.os.Handler;

public class AdcSimulator extends Thread {
	// Channels of the ADC
	private ArrayList<AdcChannel> mChannels = new ArrayList<AdcChannel>();
	// Handler to Invoking Activity
	private Handler mHandler;
	// Valor de la muestra en short int
	private short[] mMuestras;
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
		mMuestras = new short[mCantMuestras];
		
		for(int i = 0; i < mCantCanales; i++) {
			AdcChannel canal = new AdcChannel(mFs, mBits, mCantMuestras);
			mChannels.add(canal);
		}
	}
		
	// Generación de muestras
	@Override
	public void run() {
		while(mRun) {			
			mMuestras = mChannels.get(mCurrentChannel).getSamples();			
			mHandler.obtainMessage(AdcSimulatorMessage.MENSAJE_MUESTRA.getValue(), -1, 
								   mCurrentChannel, mMuestras.clone()).sendToTarget();
			nextChannel();
			candadoPausa();
		}
	}
	
	public void nextChannel() {
		mCurrentChannel++;
		if(mCurrentChannel == mTotalChannels) mCurrentChannel = 0;
	}
	
	public void setAmplitud(double amplitud, int canal) {
		if(canal >= mTotalChannels) return;
		mChannels.get(canal).setAmplitude(amplitud);
	}
	
	public void setF0(double f0, int canal) {
		if(canal >= mTotalChannels) return;
		mChannels.get(canal).setF0(f0);
	}
	
	public void setOffset(double offset, int canal) {
		if(canal >= mTotalChannels) return;
		mChannels.get(canal).setOffset(offset);
	}
	
	public void setSenal(int tipo_senal, int canal) {
		if(canal >= mTotalChannels) return;
		mChannels.get(canal).setSignalType(tipo_senal);
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
	

	public void setInitialOffset(int offset, int channel) {
		if(channel >= mTotalChannels) return;
		mChannels.get(channel).setInitialOffset(offset);
		
	}
}
