package com.ufavaloro.android.simulador.bluetooth;

public enum BluetoothMessage {

	MENSAJE_DESCONECTADO(1),
	MENSAJE_ESCUCHANDO(2),
	MENSAJE_BUSCANDO(3),
	MENSAJE_CONECTADO(4),
	MENSAJE_LEER(5),
	MENSAJE_ESCRIBIR(6),
	MENSAJE_ESCRIBIR_INFO(7),
	MENSAJE_DISPOSITIVO_REMOTO(8),
	MENSAJE_CONEXION_PERDIDA(9);
	
	private final int value;
	
	private BluetoothMessage(int value){
		this.value=value;
	}
	 
	public int getValue(){return value;}
	
	public static BluetoothMessage values(int what) {
		switch(what){
		case 1: return MENSAJE_DESCONECTADO;
		case 2: return MENSAJE_ESCUCHANDO;
		case 3: return MENSAJE_BUSCANDO;
		case 4: return MENSAJE_CONECTADO;
		case 5: return MENSAJE_LEER;
		case 6: return MENSAJE_ESCRIBIR;
		case 7: return MENSAJE_ESCRIBIR_INFO;
		case 8: return MENSAJE_DISPOSITIVO_REMOTO;
		case 9: return MENSAJE_CONEXION_PERDIDA;
		default: return MENSAJE_CONEXION_PERDIDA;
		}	
	}
}
