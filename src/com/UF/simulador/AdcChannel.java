package com.UF.simulador;

public class AdcChannel {
	// Amplitud de la Se�al
	private double mA = 1;
	// Frecuencia de la Se�al
	private double mF0 = 1;
	// Offset total de la Se�al (siempre tiene como base de continua mA)
	private double mOffset = 0;
	// Frecuencia de muestreo
	private double mFs;
	// Per�odo de muestreo
	private double mTs;
	// Resoluci�n
	private int mBits;
	// Escalones totales
	private double mEscalonesTotales;
	// Valor del escal�n
	private double mEscalon;
	// Valor de la muestra en short int
	private short[] mSamples;
	// Cantidad de muestras conjuntas a devolver
	private int mCantMuestras = 0;
	// Iterador
	private int n = 0;
	// Valor de la muestra actual
	private short mMuestraActual = 0;
	// Pendiente dientes de sierra
	private double mPendiente = 1;
	// Delay (en mS) que tarda el simulador en generar un array de mCantMuestras
	private long mDelay;
	// Se�al senoidal
	private final int SENAL_SENO = 1;
	// Se�al dientes de sierra
	private final int SENAL_SIERRA = 2;
	// Se�al cuadrada
	private final int SENAL_CUADRADA = 3;
	// Secuencia de n�meros creciente
	private final int SENAL_SECUENCIA = 4;
	// Se�al a transmitir
	private int TIPO_SENAL;
	//N�mero de canal
	private int mCanal;
	
	// Constructor de clase
	AdcChannel(int mCanal, double mFs, int mBits, int mCantMuestras) {
		this.mCanal = mCanal;
		this.mFs = mFs;
		mTs = 1/mFs;
		this.mBits = mBits;
		mEscalonesTotales = Math.pow(2, this.mBits);
		this.mCantMuestras = mCantMuestras;
		mSamples = new short[mCantMuestras];
		mDelay = ((long)(1000*mCantMuestras/mFs));
	}
	
	// C�lculo de muestras
	public short[] calcularMuestras() {
		
		for(int i=0; i<mCantMuestras; i++) {
			mSamples[i] = getSample();
		}
	
		/*
		try {
			Thread.sleep(mDelay);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		*/
		return mSamples;
	}
	
	// Selector de se�al
	private short getSample() {
		// Incremento
		n++;
		
		switch(TIPO_SENAL) {
		
			case SENAL_SENO:
				
				mMuestraActual = (short) ((mA*Math.sin(2*Math.PI*mF0*n*mTs) + mOffset) / mEscalon);
				
				break;
		
			case SENAL_SIERRA:
				
				if(n*mTs < 2*mA/mPendiente) {
					mMuestraActual = (short) ((-mF0*n*mTs + 2*mOffset) / mEscalon);
				}
						
				if(n*mTs >= 2*mA/mF0) n = 0;
				
				break;
					
			case SENAL_CUADRADA:
				
				double mT0 = 1 / mF0;
				
				if(n*mTs < mT0/2) {
					mMuestraActual = (short) (((2*mA) + mOffset - 1)  / mEscalon);
				}
				
				if(n*mTs > mT0/2) {
					mMuestraActual = (short) ((mOffset - 1)  / mEscalon);
				}
				
				if(n*mTs > mT0) {
					n = 0; 
				}
								
				break;
				
			case SENAL_SECUENCIA:
				
				if(n == mEscalonesTotales) { 
					n = 0;
				}
				
				mMuestraActual = (short) n;
				
				break;
		}
			
		return mMuestraActual;
		
	}
	
	public void setF0(double mF0) {
		this.mF0 = mF0;
	}
	
	public void setAmplitud(double mA) {
		this.mA = mA;
		mEscalon = 2 * mA / mEscalonesTotales;
	}

	public void setOffset(double offset) {
		mOffset = 1 + (offset*0.025);
	}
	
	public void setInitialOffset(double offset) {
		mOffset = offset;
	}
	
	public void setSenal(int TIPO_SENAL) {
		this.TIPO_SENAL = TIPO_SENAL;
		n = 0;
	}
	
	public int getCanal() {
		return mCanal;
	}
	
	public int getSignal() {
		return TIPO_SENAL;
	}
	
	public double getF0() {
		return mF0;
	}
	
	public double getOffset() {
		return mOffset;
	}
	
	public double getAmplitude() {
		return mA;
	}

}
