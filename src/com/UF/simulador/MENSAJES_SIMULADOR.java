package com.UF.simulador;

public enum MENSAJES_SIMULADOR {

	MENSAJE_MUESTRA(1),
	MENSAJE_CAMBIO_FS(2),
	MENSAJE_CAMBIO_SENAL(3);
	
	private final int value;
	
	private MENSAJES_SIMULADOR(int value){
		this.value=value;
	}
	 
	public int getValue(){return value;}
	
	public static MENSAJES_SIMULADOR values(int what) {
		switch(what){
		case 1: return MENSAJE_MUESTRA;
		case 2: return MENSAJE_CAMBIO_FS;
		case 3: return MENSAJE_CAMBIO_SENAL;
		default: return MENSAJE_MUESTRA;
		}	
	}
}
