package com.ufavaloro.android.simulador.adc;

public class AdcChannel {
	// Amplitud de la Señal
	private double mA = 1;
	// Frecuencia de la Señal
	private double mF0 = 1;
	// Offset total de la Señal (siempre tiene como base de continua mA)
	private double mOffset = 0;
	// Frecuencia de muestreo
	private double mFs;
	// Período de muestreo
	private double mTs;
	// Resolución
	private int mBits;
	// Escalones totales
	private double mEscalonesTotales;
	// Valor del escalón
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
	// Señal senoidal
	private final int SENAL_SENO = 1;
	// Señal dientes de sierra
	private final int SENAL_SIERRA = 2;
	// Señal cuadrada
	private final int SENAL_CUADRADA = 3;
	// Secuencia de números creciente
	private final int SENAL_SECUENCIA = 4;
	// ECG (0-5v, 16 Bits, 1 KHZ)
	private final int SENAL_ECG = 5;
	private final int[] ECG ={381, 384, 387, 390, 393, 396, 399, 403, 407, 411, 
			   				  415, 420, 424, 429, 435, 441, 447, 454, 461, 469, 
							  477, 485, 495, 504, 515, 526, 537, 549, 562, 575, 
							  588, 602, 616, 630, 645, 660, 675, 690, 705, 720, 
							  734, 749, 763, 776, 789, 801, 813, 823, 833, 842, 
							  849, 856, 861, 865, 867, 868, 868, 866, 863, 859, 
							  853, 846, 837, 827, 816, 803, 790, 775, 760, 744, 
							  728, 710, 693, 675, 657, 639, 622, 604, 588, 571,
							  556, 541, 527, 514, 502, 491, 481, 473, 465, 459, 
							  454, 450, 447, 445, 444, 443, 443, 443, 444, 445, 
							  445, 446, 445, 444, 442, 439, 435, 430, 423, 414, 
							  404, 392, 378, 362, 345, 326, 306, 284, 261, 238,
							  215, 191, 168, 146, 126, 107, 92, 80, 72, 68, 
							  70, 78, 93, 115, 145, 183, 231, 288, 354, 431, 
							  518, 614, 721, 838, 965, 1101, 1245, 1396, 1555, 1720, 
							  1889, 2062, 2236, 2412, 2586, 2759, 2927, 3089, 3244, 3391,
							  3527, 3651, 3762, 3859, 3940, 4005, 4053, 4083, 4095, 4088, 
							  4063, 4020, 3960, 3882, 3789, 3680, 3558, 3423, 3276, 3120, 
							  2956, 2785, 2610, 2431, 2251, 2071, 1893, 1718, 1547, 1382, 
							  1224, 1074, 932, 800, 677, 565, 463, 372, 292, 222, 
							  162, 113, 73, 43, 21, 6, 0, 0, 5, 16, 
							  32, 51, 73, 98, 125, 152, 181, 210, 238, 266, 
							  292, 318, 342, 364, 384, 402, 418, 432, 443, 453, 
							  461, 467, 471, 473, 474, 473, 472, 469, 466, 461, 
							  456, 451, 446, 440, 435, 429, 424, 419, 414, 410, 
							  406, 403, 400, 398, 396, 395, 394, 394, 394, 395, 
							  396, 397, 399, 401, 403, 405, 408, 410, 413, 415, 
							  418, 420, 423, 425, 427, 429, 431, 433, 434, 436, 
							  437, 438, 439, 440, 441, 441, 442, 442, 443, 443, 
							  443, 444, 444, 444, 445, 445, 446, 446, 447, 448, 
							  449, 449, 450, 452, 453, 454, 456, 457, 459, 461, 
							  463, 465, 467, 469, 471, 473, 476, 478, 481, 483, 
							  486, 488, 491, 493, 496, 499, 501, 504, 507, 509, 
							  512, 515, 518, 521, 524, 527, 530, 533, 537, 540, 
							  544, 548, 551, 555, 559, 564, 568, 572, 577, 582, 
							  587, 591, 596, 602, 607, 612, 617, 623, 628, 634, 
							  639, 645, 651, 656, 662, 668, 674, 680, 686, 693, 
							  699, 706, 712, 719, 726, 733, 740, 748, 755, 763, 
							  771, 779, 787, 796, 805, 813, 822, 832, 841, 850, 
							  860, 869, 879, 889, 899, 909, 919, 930, 940, 951, 
							  961, 972, 982, 993, 1004, 1015, 1026, 1036, 1047, 1058, 
							  1070, 1081, 1092, 1103, 1114, 1125, 1136, 1147, 1157, 1168, 
							  1178, 1189, 1199, 1209, 1219, 1228, 1237, 1246, 1254, 1263, 
							  1270, 1277, 1284, 1291, 1296, 1302, 1306, 1310, 1314, 1316, 
							  1318, 1320, 1320, 1320, 1319, 1317, 1315, 1311, 1307, 1302, 
							  1296, 1290, 1282, 1274, 1264, 1255, 1244, 1232, 1220, 1207, 
							  1193, 1179, 1164, 1148, 1132, 1115, 1098, 1080, 1063, 1044, 
							  1026, 1007, 988, 968, 949, 930, 910, 891, 871, 852,
							  833, 814, 795, 777, 759, 741, 724, 707, 690, 674, 
							  658, 643, 628, 614, 601, 587, 575, 563, 551, 540, 
							  530, 520, 511, 502, 494, 486, 479, 472, 466, 460, 
							  455, 450, 445, 441, 437, 433, 430, 427, 424, 422, 
							  419, 417, 415, 413, 412, 410, 408, 407, 405, 404, 
							  402, 401, 399, 398, 396, 395, 393, 391, 389, 387, 385, 383, 381};
	// Señal a transmitir
	private int TIPO_SENAL;
	//Número de canal
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
	
	// Cálculo de muestras
	public short[] calcularMuestras() {
		
		for(int i=0; i<mCantMuestras; i++) {
			mSamples[i] = getSample();
		}
	
		
		try {
			Thread.sleep(mDelay);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		return mSamples;
	}
	
	// Selector de señal
	private short getSample() {
		// Incremento
		n++;
		
		switch(TIPO_SENAL) {
		
			case SENAL_SENO:
				mMuestraActual = (short) ((mA*Math.sin(2*Math.PI*mF0*n*mTs) + mOffset) / mEscalon);
				break;
		
			case SENAL_SIERRA:
				if(n*mTs < 2*mA/mPendiente) mMuestraActual = (short) ((-mF0*n*mTs + 2*mOffset) / mEscalon);
				if(n*mTs >= 2*mA/mF0) n = 0;
				break;
					
			case SENAL_CUADRADA:
				double mT0 = 1 / mF0;
				if(n*mTs < mT0/2) mMuestraActual = (short) (((2*mA) + mOffset - 1)  / mEscalon);
				if(n*mTs > mT0/2) mMuestraActual = (short) ((mOffset - 1)  / mEscalon);
				if(n*mTs > mT0) n = 0; 				
				break;
				
			case SENAL_SECUENCIA:
				if(n == mEscalonesTotales) n = 0;
				mMuestraActual = (short) n;
				break;
				
			case SENAL_ECG:
				if(n == ECG.length) n = 0;
				mMuestraActual = (short) ECG[n];
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
	
	public void setSignal(int TIPO_SENAL) {
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
