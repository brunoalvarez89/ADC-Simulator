package com.ufavaloro.android.simulador.adcsimulator;

public class AdcChannel {
	/**
	 * Channel Parameters
	 */
	// Sampling Period
	private double mTs;
	// Resolution in Bits
	private int mBits;
	// Total Resolution Steps
	private double mTotalSteps;
	// Resolution Step
	private double mStep;
	// Samples Array
	private short[] mSamples;
	// Samples to generate
	private int mTotalSamples = 0;
	// Iterator
	private int n = 0;
	// Value of the current Sample
	private short mCurrentSample = 0;
	// Delay (in mS) to generate mSamples (it depends on the Sampling Frequency and the
	// amount of Samples to generate
	private long mDelay;
	private double mSampling = 1;
	
	/**
	 * Signal Parameters
	 */
	// Signal Amplitude
	private double mA = 1;
	// Signal Frequency
	private double mF0 = 1;
	// Signal Offset (siempre tiene como base de continua mA)
	private double mOffset = 0;
	// Sawtooth slope
	private double mSawtoothSlope = 1;
	
	/**
	 * Signal Codes
	 */
	// Sine
	private final int SIGNAL_SINE = 1;
	
	// Sawtooth
	private final int SIGNAL_SAWTOOTH = 2;
	
	// Square
	private final int SIGNAL_SQUARE = 3;
	
	// Incrementing Sequence
	private final int SIGNAL_SEQUENCE = 4;
	
	// Human EKG Heartbeat
	// Already sampled and digitalized. 0-5v, 12 Bits, Fs ~ 500 Hz, 543 samples, 55 bpm 
	private final int SIGNAL_EKG = 5;
	private CircularBuffer mEkgBuffer;
	private final short[] mEkgSignal = {381, 384, 387, 390, 393, 396, 399, 403, 407, 411, 
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
							  402, 401, 399, 398, 396, 395, 393, 391, 389, 387, 
							  385, 383, 381};
	// Dog Aortic Pressure Wave (not digitalized, aprox. 50-120 mmHg, Fs: 250 Hz)
	private final int SIGNAL_PRESSURE = 6;
	private final double[] mPressureSignal = {70.25506592, 70.37869263, 69.38970947, 
											  69.88421631, 69.51333618, 69.88421631, 69.63696289, 
											  68.27713013, 68.89523315, 68.15350342, 68.02987671, 
											  67.41177368, 67.16455078, 67.53540039, 66.67007446, 
											  65.80471802, 66.29919434, 65.43383789, 64.81573486, 
											  65.06298828, 65.18661499, 64.93936157, 64.81573486,
											  65.18661499, 64.56851196, 64.07400513, 63.95040894, 
											  63.82678223, 64.69213867, 63.95040894, 63.57952881, 
											  63.82678223, 63.45590210, 63.33230591, 64.19763184, 
											  63.33230591, 63.20867920, 64.19763184, 63.33230591, 
											  63.08505249, 63.08505249, 62.83779907, 62.09606934, 
											  62.71420288, 61.60159302, 61.47796631, 61.23074341, 
											  60.73626709, 60.73626709, 59.50003052, 59.87091064, 
											  59.62365723, 59.50003052, 59.25280762, 58.38745117, 
											  59.00555420, 58.63470459, 59.12918091, 58.51107788, 
											  58.26382446, 57.76934814, 58.26382446, 58.51107788, 
											  58.63470459, 59.25280762, 59.12918091, 60.85986328, 
											  63.82678223, 69.76058960, 78.29043579, 86.20217896, 
											  91.14700317, 97.32806396, 102.2729187, 106.4760132, 
											  109.8137817, 111.4208679, 113.2751770, 115.1294861, 
											  116.8601990, 117.2310486, 118.3436279, 118.5908813, 
											  118.9617310, 119.2089844, 119.5798340, 119.8270874, 
											  119.7034607, 119.2089844, 118.5908813, 117.7255249, 
											  116.9837952, 116.1184692, 115.6239624, 115.2531128, 
											  114.1405334, 113.5224304, 112.2862244, 110.0610352, 
											  108.8248291, 106.8468933, 106.3523865, 104.8689575, 
											  103.6327515, 101.7784119, 99.67687988, 96.83358765,
											  92.87771606, 89.41632080, 90.03442383, 92.50683594, 
											  92.75408936, 91.88873291, 91.14700317, 91.27062988, 
											  91.39425659, 92.13598633, 92.13598633, 92.38323975,
			 								  92.50683594, 92.25961304, 93.00134277, 93.24856567, 
			 								  93.61944580, 94.23754883, 94.97927856, 95.22650146, 
			 								  95.35012817, 95.84460449, 95.84460449, 96.33911133, 
			 								  96.21548462, 96.46270752, 96.09185791, 95.84460449,
			 								  95.47375488, 94.97927856, 93.74307251, 94.23754883,
			 								  93.61944580, 92.63046265, 92.25961304, 91.76513672, 
			 								  91.51788330, 90.77615356, 89.91079712, 89.41632080,
			 								  88.42736816, 88.42736816, 87.43838501, 86.82028198,
			 								  86.20217896, 85.21319580, 84.84234619, 84.47149658,
			 								  84.22424316, 83.35888672, 82.49356079, 82.74078369, 
			 								  81.75183105, 81.62820435, 81.38095093, 80.63922119, 
			 								  80.51562500, 80.39199829, 80.02111816, 79.52664185, 
			 								  79.89752197, 78.78491211, 79.03216553, 78.78491211,
			 								  78.41406250, 78.29043579, 77.17785645, 77.17785645, 
			 								  76.31250000, 76.31250000, 76.18887329, 74.82904053, 
			 								  75.57077026, 74.82904053, 74.95266724, 74.33456421, 
			 								  73.59283447, 73.59283447, 73.09835815, 73.09835815, 
			 								  72.97473145, 72.72747803, 72.48025513, 72.97473145,
			 								  72.35662842, 72.72747803, 71.98574829, 72.23300171, 
			 								  72.10937500, 71.86215210, 71.61489868, 70.87316895,
			 								  71.49127197, 70.62594604, 70.74954224, 69.51333618,
			 								  69.76058960, 69.51333618, 68.77160645, 68.27713013, 
			 								  67.53540039, 67.41177368, 67.28817749, 66.67007446,
			 								  66.29919434, 66.17556763, 66.42282104, 66.05194092,
			 								  65.80471802, 66.17556763, 66.42282104, 65.92834473,
			 								  66.42282104, 65.92834473, 66.42282104};
	private final double mPressureMax = 119.8270874;
	private final double mPressureMin = 57.76934814;
	// Current Signal
	private int SIGNAL_TYPE;
	
	/**
	 * Constructor.
	 * 
	 * @param fs - Sampling Frequency.
	 * @param bits - Resolution.
	 * @param totalSamples - Amount of samples to generate.
	 */
	AdcChannel(double fs, int bits, int totalSamples, int totalChannels) {
		mTs = 1/fs;
		mBits = bits;
		mTotalSteps = Math.pow(2, mBits);
		mTotalSamples = totalSamples;
		mSamples = new short[totalSamples];
		mDelay = ((long)(1000*totalSamples*mTs/totalChannels));
	}
	
	/**
	 * Populates the mSamples array and waits mDelay to return it.
	 * @return The array of generated samples.
	 */
	public synchronized short[] getSamples() {	
		for(int i = 0; i < mTotalSamples; i++) mSamples[i] = generateSample();
		try { Thread.sleep(mDelay); } catch (InterruptedException e1) {}
		return mSamples;
	}
	
	/**
	 * Generates a sample of the CURRENT_SIGNAL.
	 * @return The generated sample.
	 */
	// Selector de señal
	private synchronized short generateSample() {
		n++;
		
		switch(SIGNAL_TYPE) {
		
			case SIGNAL_SINE:
				mCurrentSample = (short) ((mA*Math.sin(2*Math.PI*mF0*n*mTs) + mOffset) / mStep);
				break;
		
			case SIGNAL_SAWTOOTH:
				if(n*mTs < 2*mA/mSawtoothSlope) mCurrentSample = (short) ((-mF0*n*mTs + 2*mOffset) / mStep);
				if(n*mTs >= 2*mA/mF0) n = 0;
				break;
					
			case SIGNAL_SQUARE:
				double mT0 = 1 / mF0;
				if(n*mTs < mT0/2) mCurrentSample = (short) (((2*mA) + mOffset - 1)  / mStep);
				if(n*mTs > mT0/2) mCurrentSample = (short) ((mOffset - 1)  / mStep);
				if(n*mTs > mT0) n = 0; 				
				break;
				
			case SIGNAL_SEQUENCE:
				if(n == mTotalSteps) n = 0;
				mCurrentSample = (short) n;
				break;
				
			case SIGNAL_EKG:
				if(n == mEkgSignal.length/4) n = 0;
				mCurrentSample = mEkgSignal[n*4];
				//mCurrentSample = mEkgBuffer.getSample((int) (n*mSampling*4));
				break;
				
			case SIGNAL_PRESSURE:
				if(n == mPressureSignal.length) n = 0;
				mCurrentSample = (short) ((mPressureSignal[n] - mPressureMin) / ((mPressureMax-mPressureMin)*mStep));
		}
			
		return mCurrentSample;
		
	}
	
	/**
	 * Sets the Signal Frequency.
	 * @param f0 - Signal Frequency.
	 */
	public void setF0(double f0) {
		mF0 = f0;
		
		if(SIGNAL_TYPE == SIGNAL_EKG) {
			mSampling = f0;
		}
	}
	
	/**
	 * Sets the Signal Amplitude.
	 * @param amplitude - Signal Amplitude.
	 */
	public void setAmplitude(double amplitude) {
		mA = amplitude;
		mStep = 2 * amplitude / mTotalSteps;
	}

	/**
	 * Sets the Signal Offset.
	 * @param offset - Signal Offset.
	 */
	public void setOffset(double offset) {
		mOffset = 1 + (offset*0.025);
	}
	
	/**
	 * Sets the initial offset.
	 * @param offset
	 */
	public void setInitialOffset(double offset) {
		mOffset = offset;
	}
	
	/**
	 * Sets the Signal Type.
	 * @param signalCode - 1 = Sine, 2 = Sawtooth, 3 = Square, 4 = Incrementing Sequence, 5 = EKG, 6 = Pressure.
	 */
	public void setSignalType(int signalCode) {
		
		SIGNAL_TYPE = signalCode;
		n = 0;
		
		switch(signalCode) {
			case SIGNAL_SINE:
				setInitialOffset(1);
				setF0(1);
				setAmplitude(1);
				break;
		
			case SIGNAL_SAWTOOTH:
				setInitialOffset(1);
				setF0(1);
				setAmplitude(1);
				break;
					
			case SIGNAL_SQUARE:	
				setInitialOffset(1);
				setF0(1);
				setAmplitude(1);
				break;
				
			case SIGNAL_SEQUENCE:
				break;
				
			case SIGNAL_EKG:
				mEkgBuffer = new CircularBuffer(mEkgSignal);
				break;
				
			case SIGNAL_PRESSURE:
				mStep = 1 / mTotalSteps;
				break;
		}
	}
	
	/**
	 * Sets the delay time between each batch of samples.
	 * @param delay (in seconds).
	 */
	public void setDelay(double delay) {
		mDelay = (long) delay*4;
	}
	
	/**
	 * Gets the Signal Code.
	 */
	public int getSignalCode() {
		return SIGNAL_TYPE;
	}
	
	/**
	 * Gets the Signal Frequency.
	 */
	public double getF0() {
		return mF0;
	}
	
	/**
	 * Gets the Signal Offset.
	 */
	public double getOffset() {
		return mOffset;
	}
	
	/**
	 * Gets the Signal Amplitude.
	 */
	public double getAmplitude() {
		return mA;
	}

	/**
	 * Gets the Ekg Signal.
	 */
	public short[] getEkgSignal() {
		return mEkgSignal;
	}

}// AdcChannel
