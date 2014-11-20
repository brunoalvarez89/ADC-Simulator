package com.UF.simulador;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import android.os.Handler;
import android.os.SystemClock;

public class AdcSimulator extends Thread {
	private ArrayList<AdcChannel> mCanales = new ArrayList<AdcChannel>();
	// Handler a MainActivity
	private Handler mHandler;
	// Valor de la muestra en short int
	private short[] mMuestras;
	// Lock de pausa
	private Object mPauseLock = new Object();
	// Flag de pausa
	private boolean mPaused = false;
	// Flag de run
	private boolean mRun;
	// Canal actual simulado
	private int mCanalActual = 0;
	// Cantidad total de canales
	private int mCantCanales;
	// Estoy on-line?
	private boolean mConnected = false;
	
	// Constructor de clase
	AdcSimulator(Handler mHandler, int mCantCanales, int mCantMuestras, double mFs, int mBits) {
		
		this.mHandler = mHandler;
		
		this.mCantCanales = mCantCanales;
		
		mMuestras = new short[mCantMuestras];
		
		for(int i=0; i<mCantCanales; i++) {
			AdcChannel canal = new AdcChannel(i, mFs, mBits, mCantMuestras);
			mCanales.add(canal);
		}
		
	}
		
	// Generación de muestras
	@Override
	public void run() {
		while(mRun) {			
			
			mMuestras = mCanales.get(mCanalActual).calcularMuestras();
						
			mHandler.obtainMessage(AdcSimulatorMessage.MENSAJE_MUESTRA.getValue(), -1, mCanalActual, mMuestras.clone()).sendToTarget();
			
			nextChannel();
			
			candadoPausa();
			
		}
	}
	
	public void nextChannel() {
		mCanalActual++;
		if(mCanalActual == mCantCanales) mCanalActual = 0;
	}
	
	public void setAmplitud(double amplitud, int canal) {
		if(canal >= mCantCanales) return;
		mCanales.get(canal).setAmplitud(amplitud);
	}
	
	public void setF0(double f0, int canal) {
		if(canal >= mCantCanales) return;
		mCanales.get(canal).setF0(f0);
	}
	
	public void setOffset(double offset, int canal) {
		if(canal >= mCantCanales) return;
		mCanales.get(canal).setOffset(offset);
	}
	
	public void setSenal(int tipo_senal, int canal) {
		if(canal >= mCantCanales) return;
		mCanales.get(canal).setSenal(tipo_senal);
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
		return mCanales.get(index);
	}
	
	public void setOnline(boolean mConnected) {
		this.mConnected = mConnected;
	}

	
	public void setInitialOffset(int offset, int channel) {
		if(channel >= mCantCanales) return;
		mCanales.get(channel).setInitialOffset(offset);
		
	}
}
