package com.ufavaloro.android.simulador.adcsimulator;

public class CircularBuffer {
	
	private short[] mBuffer;
	private int mStoringIndex;
	
	public CircularBuffer(int bufferSize) {
		mBuffer = new short[bufferSize];
		mStoringIndex = 0;
	}
	
	public CircularBuffer(short[] buffer) {
		mBuffer = buffer;
	}
	
	public void writeRawSamples(short[] x) {
		// Almaceno
		for(int i=0; i<x.length; i++) {
			mBuffer[mStoringIndex] = x[i];
			
			// Incremento �ndices
			mStoringIndex++;
			
			// Si llego al m�ximo, pongo �ndices en cero
			if(mStoringIndex == mBuffer.length) { 
				mStoringIndex = 0;
			}
		}
	}
	
	public void writeRawSample(short sample) {
		mBuffer[mStoringIndex] = sample;
		
		// Incremento �ndices
		mStoringIndex++;
		
		// Si llego al m�ximo, pongo �ndices en cero
		if(mStoringIndex == mBuffer.length) { 
			mStoringIndex = 0;
		}
		
	}
	
	public int getStoringIndex() {
		return mStoringIndex;
	}

	public int size() {
		return mBuffer.length;
	}
	
	public short getSample(int index) {
		int length = mBuffer.length;
		// Inicializo un �ndice dummy
		int newIndex = 0;
		boolean flag = false;
		
		// Si el �ndice es negativo
		if(index < 0) {
			// Le sumo el largo del Buffer
			newIndex = index + length;
			if(newIndex < 0) flag = true;
		} else
		
		// Si el �ndice est� en el rango permitido
		if(index <= length - 1 && index >= 0) { 
			// No hago nada
			newIndex = index;
		} else
		
		// Si el �ndice es mayor al largo total
		if(index > length - 1) { 
			// Le resto el �ndice al largo total
			newIndex = index - length;
			if(newIndex > length - 1) flag = true;
		}
		
		// Devuelvo muestra
		if(flag == false) {
			return mBuffer[newIndex];
		} else {
			return getSample(newIndex);
		}
	}

	public void setBuffer(short[] buffer) {
		mBuffer = buffer;
	}
}

