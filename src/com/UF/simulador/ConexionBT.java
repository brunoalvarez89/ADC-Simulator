package com.UF.simulador;

import java.io.IOException;
import java.util.UUID;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;

public class ConexionBT {
	
	Parcelable[] uuidExtra;
	
	/********************************
	 * INICIO DE ATRIBUTOS DE CLASE *
	 ********************************/
	// Debugging
    private static final String TAG = "Bluetooth";
    private static final boolean D = true;
    
	// UUID del dispositivo
	private static final UUID MI_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
	
	// Nombre del servicio Bluetooth que se mostrara en el BluetoothSocket
	private static final String NOMBRE_SERVICIO = "Bluetooth";
	
	// Estado de la conexion
	private int mEstado;
	public static final int ESTADO_DESCONECTADO = 0; // Desconectado
	public static final int ESTADO_ESCUCHANDO = 1; 	 // Escuchando conexiones entrantes (Servidor)
	public static final int ESTADO_BUSCANDO = 2;     // Inicializando una conexion saliente (Cliente)
	public static final int ESTADO_CONECTADO = 3;    // Conectado
	
	// Adaptador Bluetooth local (antena del dispositivo)
	private final BluetoothAdapter mBluetoothAdapter;
	
	// Handler de la conexión 
	private final Handler mHandler;
	
	// Threads
	private ThreadServidor mThreadServidor = null;
	private ThreadCliente mThreadCliente = null;
	private ThreadConexion mThreadConexion = null;

	/******************************
	 * INICIO DE MÉTODOS DE CLASE *
	 ******************************/
	// Constructor
	public ConexionBT(Handler mHandler) {
		if (D) Log.d(TAG, "Creando servicio Bluetooth...");
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mEstado = ESTADO_DESCONECTADO;
		this.mHandler = mHandler;
	}
	
	public void setRun() {
		mThreadConexion.setRunning(false);
	}
	
	public void pausarEscritura() {
		mThreadConexion.onPause();
	}
	
	// Getter de mEstado
	public int getEstado() {
		return mEstado;
	}
	
	// Setter de mEstado
	public void setEstado(int Estado) {
		this.mEstado = Estado;
	}

	// Metodo que para todos los Threads
	protected void stop() {
		// Mato Thread Servidor
		if (mThreadServidor != null) {
			mThreadServidor.cancel(); 
			boolean retry = true;
			while(retry) {
				try {
					mThreadServidor.join();
					retry = false;
				} catch (InterruptedException e) {}
			}
		}
		// Mato Thread Cliente
		if (mThreadCliente != null) {
			mThreadCliente.cancel(); 
			boolean retry = true;
			while(retry) {
				try {
					mThreadCliente.join();
					retry = false;
				} catch (InterruptedException e) {}
			}
		}
		// Mato Thread Conexion
		if (mThreadConexion != null) {
			mThreadConexion.cancel(); 
			boolean retry = true;
			while(retry) {
				try {
					mThreadConexion.join();
					retry = false;
				} catch (InterruptedException e) {}
			}
		}
		// Actualizo estado
		setEstado(ESTADO_DESCONECTADO);
		// Informo
		mHandler.obtainMessage(MENSAJES_CONEXION.MENSAJE_DESCONECTADO.getValue()).sendToTarget();

	}

	// Si el dispositivo se conecta como Servidor...
	public synchronized void soyServidor() {
		// Log
		if (D) Log.d(TAG, "Iniciando Servicio Bluetooth como servidor...");
		// Cancelo cualquier Thread de Establecer Conexion
		if (mThreadCliente != null) {mThreadCliente.cancel(); mThreadCliente = null;}
		// Cancelo cualquier Thread de Conexion Establecida
		if (mThreadConexion != null) {mThreadConexion.cancel(); mThreadConexion = null;}
		// Inicializo el Thread de Dialogar Conexion para escuchar en un BluetoothServerSocket
		if (mThreadServidor == null) {mThreadServidor = new ThreadServidor(); mThreadServidor.start();}
		// Actualizo estado
		setEstado(ESTADO_ESCUCHANDO);
		mHandler.obtainMessage(MENSAJES_CONEXION.MENSAJE_ESCUCHANDO.getValue()).sendToTarget();
	}

	// Si el dispositivo se conecta como Cliente...
	public synchronized void soyCliente(BluetoothDevice Dispositivo) {
		// Log
		if (D) Log.d(TAG, "Iniciando Servicio BT como cliente...");	
		// Cancelo cualquier Thread de Establecer Conexion
		if (mEstado == ESTADO_BUSCANDO) { 
			if (mThreadCliente != null) {
				mThreadCliente.cancel(); 
				mThreadCliente = null;
			} 
		}
		// Cancelo cualquier Thread de Conexion Establecida
		if (mThreadConexion != null) {mThreadConexion.cancel(); mThreadConexion = null;}	
		// Inicializo el Thread de Establecer Conexion
		mThreadCliente = new ThreadCliente(Dispositivo);
		mThreadCliente.start();
		// Actualizo estado
		setEstado(ESTADO_BUSCANDO);
		// Informo
		mHandler.obtainMessage(MENSAJES_CONEXION.MENSAJE_BUSCANDO.getValue()).sendToTarget();

	}
	
	// Si los dispositivos ya se conectaron...
	public synchronized void Conexion(BluetoothSocket socket, BluetoothDevice dispositivo) {
		// Log
		if (D) Log.d(TAG, "Intentando conectar dispositivos...");
		// Envio el nombre del dispositivo remoto a la UI
		mHandler.obtainMessage(MENSAJES_CONEXION.MENSAJE_DISPOSITIVO_REMOTO.getValue(), -1, 
							   -1, dispositivo.getName()).sendToTarget();
		// Cierro cualquier Thread de Escuchar Conexion
		if (mThreadServidor != null) {mThreadServidor.cancel(); mThreadServidor = null;}
		// Cancelo cualquier Thread de Establecer Conexion
		// Comento esto porque se me cierra el socket cliente sino...
		// Queda el Thread Cliente encendido forevah
		//if (mThreadCliente != null) {mThreadCliente.cancel(); mThreadCliente = null;}
		// Cancelo cualquier Thread que este en una conexion
		if (mThreadConexion != null) {mThreadConexion.cancel(); mThreadConexion = null;}
		// Empiezo el Thread de Conexion Establecida
		mThreadConexion = new ThreadConexion(socket);
		mThreadConexion.start();
	    // Actualizo estado
		setEstado(ESTADO_CONECTADO);
		// Informo
		mHandler.obtainMessage(MENSAJES_CONEXION.MENSAJE_CONECTADO.getValue()).sendToTarget();
	}

	// Metodo de escritura sobre el Socket
	public synchronized void write(byte[] out) {
		//ThreadConexion temp;
		synchronized(this) {
			if (mEstado != ESTADO_CONECTADO) return;
			mThreadConexion.Escribir(out);
		}
		//temp.Escribir(out);
	}
	
	/*****************************************************
	 * THREAD SERVIDOR                                   *
	 * Se utiliza para crear un Bluetooth Server Socket. *
	 *****************************************************/
	private class ThreadServidor extends Thread {
		// Socket Bluetooth Servidor. Se utiliza para escuchar y aceptar conexiones entrantes.
		private final BluetoothServerSocket mmBluetoothServerSocket;
		
		// Constructor
		public ThreadServidor() {
			// Log
			if (D) Log.d(TAG, "Inicializando ThreadServidor()...");
			// Inicializo Socket a null 
			BluetoothServerSocket tmp = null;
			// Obtengo el Socket Servidor
			try {
				if (D) Log.d(TAG, "Intentando generar canal RFCOMM...");
				tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NOMBRE_SERVICIO, MI_UUID);
				if (D) Log.d(TAG, "Canal RFCOMM generado exitosamente.");
			} catch (IOException e) {
				if (D) Log.d(TAG, "Error al generar canal RFCOMM (" + e.getMessage() +").");
			}
			// Si no puede obtener un Socket Servidor, seguira siendo nulo.
			mmBluetoothServerSocket = tmp;
		}
	
		// Thread.run()
		public void run() {
			// Socket Bluetooth. A traves de este socket se realizara la transferencia de informacion.
			BluetoothSocket mmBluetoothSocket = null;
			// Si no estoy conectado, escucho el ServerSocket
			while (mEstado != ESTADO_CONECTADO) {
				try {
					if (D) Log.d(TAG, "A la espera de conexiones entrantes...");
					mmBluetoothSocket = mmBluetoothServerSocket.accept();
					if (D) Log.d(TAG, "Conectado con " + mmBluetoothSocket.getRemoteDevice() + ".");
				} catch (IOException e) {
					if (D) Log.d(TAG, "Conexión entrante rechazada (" + e.getMessage() + ").");
					break;
				}
			// conexion aceptada	
				if (mmBluetoothSocket != null) {
                   synchronized (ConexionBT.this) {
                        switch (mEstado) {
                        case ESTADO_ESCUCHANDO:
                        case ESTADO_BUSCANDO:
                            // Todo normal. Inicializo la conexión.
            				//Conexion(mmBluetoothSocket, mmBluetoothSocket.getRemoteDevice());
                            break;
                        case ESTADO_DESCONECTADO:
                        case ESTADO_CONECTADO:
                            // No preparado o ya conectado. Cierro Socket.
                            try {
                            	mmBluetoothSocket.close();
                            } catch (IOException e) {}
                            break;
                        }
                    }
                }	
			}//while
			if (D) Log.d(TAG, "Sali del While...");
		}
		
		public void cancel() {
			try {mmBluetoothServerSocket.close();} catch (IOException e) {}
		}
	}//ThreadServidor
	
	
	/**********************************************
	 * THREAD CLIENTE                             *
	 * Se utiliza para crear un Bluetooth Socket. *
	 **********************************************/
	private class ThreadCliente extends Thread {
		private BluetoothSocket mmBluetoothSocket;
		private final BluetoothDevice mmBluetoothDevice;
			
		// Constructor
		public ThreadCliente(BluetoothDevice dispositivo) {
			// Log
			if (D) Log.d(TAG, "Inicializando ThreadCliente()...");
			BluetoothSocket tmp = null;
			mmBluetoothDevice = dispositivo;
			
			Method m = null;
			try {
				m = mmBluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
			} catch (NoSuchMethodException e) {
				if (D) Log.d(TAG, "Error en la generación del BluetoothSocket (" + e.getMessage() + ").");
				e.printStackTrace();
			}
			
			try {
				tmp = (BluetoothSocket) m.invoke(mmBluetoothDevice, Integer.valueOf(3));
				if (D) Log.d(TAG, "BluetoothSocket generado exitosamente.");
			} catch (IllegalAccessException e) {
				if (D) Log.d(TAG, "Error en la generación del BluetoothSocket (" + e.getMessage() + ").");
				e.printStackTrace();
				} 
			  catch (IllegalArgumentException e) {
				if (D) Log.d(TAG, "Error en la generación del BluetoothSocket (" + e.getMessage() + ").");
				e.printStackTrace();
				} 
			  catch (InvocationTargetException e) {
				if (D) Log.d(TAG, "Error en la generación del BluetoothSocket (" + e.getMessage() + ").");
			    e.printStackTrace();
			}
			// Obtengo el BluetoothSocket para conectarme con el BluetoothDevice seleccionado
			
			/*
			try {
				tmp = mmBluetoothDevice.createInsecureRfcommSocketToServiceRecord(MI_UUID);
			} catch (IOException e) {}
			
			if (D) Log.d(TAG, "Socket cliente creado: " + tmp);
			*/
			
			mmBluetoothSocket = tmp;
		}
			
		@Override
		public void run() {
			// Cancelo escucha de dispositivos
			mBluetoothAdapter.cancelDiscovery();
			
			try {				
				if (D) Log.d(TAG, "Ejecutando BluetoothSocket.connect()...");
				mmBluetoothSocket.connect();
				if (D) Log.d(TAG, "Conectado a " + mmBluetoothDevice.getName() + " exitosamente." );
			} catch(IOException e1) {
				if (D) Log.d(TAG, "mmBluetoothSocket.connect() falló (" + e1.getMessage() +"), cerrando Socket...");
				try {
					mmBluetoothSocket.close();
				} catch (IOException e2) {
					if (D) Log.d(TAG, "mmBluetoothSocket.close() falló (" + e2.getMessage() +").");
				}
			}
			
			// Empiezo el Thread de conexion establecida
			Conexion(mmBluetoothSocket, mmBluetoothDevice);
		}
		
		
		public void cancel() {
			try {
				mmBluetoothSocket.close();
			} catch (IOException e) {
				// no se pudo cerrar socket
			}
		}
	}//ThreadCliente
	
	/*****************************************************
	 * THREAD DE CONEXION ESTABLECIDA                    *
	 * Permite leer y escribir información en el Socket. *
	 *****************************************************/ 
	private class ThreadConexion extends Thread {
		private final BluetoothSocket mmBluetoothSocket;
		private final InputStream mmInputStream;
		private final OutputStream mmOutputStream;
		// Candado que traba el thread
		private Object mPauseLock = new Object();
		// Flag de pausa
		private boolean mPaused = false;
		// Flag de run
		private boolean mRun = true;
		
		// Constructor de clase
		public ThreadConexion(BluetoothSocket Socket) {
			// Log
			if (D) Log.d(TAG, "Inicializando ThreadConexion()...");
			
			mmBluetoothSocket = Socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			
			// Obtengo InputStream y OutputStream del BluetoothSocket
			try {
				if (D) Log.d(TAG, "Generando InputSTream() y OutputStream()...");
				tmpIn = mmBluetoothSocket.getInputStream();
				tmpOut = mmBluetoothSocket.getOutputStream();
				if (D) Log.d(TAG, "InputSTream() y OutputStream() generados exitosamente.");
			} catch (IOException e) {				
				if (D) Log.d(TAG, "Error en la creación de InputSTream() y OutputStream() (" + e.getMessage() +").");
			}
			
			mmInputStream = tmpIn;
			mmOutputStream = tmpOut;
		}
		
		// Thread.run()
		public void run() {
			byte[] mmInputBuffer = new byte[1];
			int mmBytes;
			
			/************************************************************************************* 
			 * BUCLE DE ESCUCHA!																 *
			 * read(buffer) devuelve -1 si End Of Stream                                         *
			 * read(buffer) devuelve error si no puede leer el primer byte o si hay desconexion. *
			 *************************************************************************************/
			if (D) Log.d(TAG, "Iniciando bucle de escucha...");
			while(mRun) {
				
				try {
					
					// Candado de pausa
					// Deja el Thread en espera utilizando wait() hasta que mPaused == false
					synchronized(mPauseLock) {
						while(mPaused) {
							try {
								mPauseLock.wait();
							} catch (InterruptedException e) {}
						}
					}
					
					mmBytes = mmInputStream.read(mmInputBuffer);
					// Envio los Bytes recibidos a la UI mediante el Handler
					mHandler.obtainMessage(MENSAJES_CONEXION.MENSAJE_LEER.getValue(), mmBytes, -1, mmInputBuffer[0]).sendToTarget();
					
				}// Desconexión! 
				 catch (IOException e) { 
					if (D) Log.d(TAG, "Conexión perdida (" + e.getMessage() +").");
					// Actualizo estado
					setEstado(ESTADO_DESCONECTADO);
					// Informo
					mHandler.obtainMessage(MENSAJES_CONEXION.MENSAJE_CONEXION_PERDIDA.getValue()).sendToTarget();
					break; 
				}
				
			}//while
		}
		
		// Metodo de escritura sobre el Socket
		public void Escribir(byte[] buffer) {
			try {
				mmOutputStream.write(buffer);
				//mmOutputStream.flush();
			} catch (IOException e) {}
		}
		
		// Metodo para cerrar el Socket
		public void cancel() {
			mRun = false;
			try {
				mmBluetoothSocket.close();
			} catch (IOException e) {}
		}
		
		// Pauseo el Thread
		public void onPause() {
			synchronized (mPauseLock) {
				mPaused = true;
			}
		}
		 
		// Resumo el Thread
		public void onResume() {
		    synchronized (mPauseLock) {
		        mPaused = false;
		        mPauseLock.notifyAll();
		    }
		}
		
		// Setter de mRun
		public void setRunning(boolean mRun) {
			this.mRun = mRun;	
		}
		
	}//ThreadConexionEstablecida
	
}//ConexionBT
