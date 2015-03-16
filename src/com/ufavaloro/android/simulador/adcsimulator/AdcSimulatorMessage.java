package com.ufavaloro.android.simulador.adcsimulator;

public enum AdcSimulatorMessage {

	MENSAJE_MUESTRA(1),
	MENSAJE_CAMBIO_FS(2),
	MENSAJE_CAMBIO_SENAL(3);
	
	private final int value;
	
	private AdcSimulatorMessage(int value){
		this.value=value;
	}
	 
	public int getValue(){return value;}
	
	public static AdcSimulatorMessage values(int what) {
		switch(what){
		case 1: return MENSAJE_MUESTRA;
		case 2: return MENSAJE_CAMBIO_FS;
		case 3: return MENSAJE_CAMBIO_SENAL;
		default: return MENSAJE_MUESTRA;
		}	
	}
}
