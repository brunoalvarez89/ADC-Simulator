package com.UF.simulador;

import java.util.ArrayList;

import android.os.Handler;

public class Simulador extends Thread {
	private ArrayList<Canal> mCanales = new ArrayList<Canal>();
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
	
	// Constructor de clase
	Simulador(Handler mHandler, int mCantCanales, int mCantMuestras, double mFs, int mBits) {
		this.mHandler = mHandler;
		this.mCantCanales = mCantCanales;
		mMuestras = new short[mCantMuestras];
		for(int i=0; i<mCantCanales; i++) {
			Canal canal = new Canal(i, mFs, mBits, mCantMuestras);
			mCanales.add(canal);
		}
	}
		
	// Generación de muestras
	@Override
	public void run() {
		while(mRun) {			
			
			mMuestras = mCanales.get(mCanalActual).calcularMuestras();
			
			mHandler.obtainMessage(MENSAJES_SIMULADOR.MENSAJE_MUESTRA.getValue(), -1, mCanalActual, mMuestras).sendToTarget();
			
			candadoPausa();
		}
	}
	
	public void proximoCanal() {
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
	
}
