package com.UF.simulador;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.app.ActionBarActivity;

public class MainActivity extends ActionBarActivity {
	
	/*************
	 * DEBUGGING *
	 *************/
    private static final String TAG = "Simulador_MainActivity";
    private static final boolean D = true;
    
	/********************
	 * INTERFAZ GRÁFICA *
	 ********************/
	private Button mButtonConectar;
	private Button mButtonAgregarCanal;
	private Button mButtonRemoverCanal;
	private Spinner mSpinnerCanales;
	private Spinner mSpinnerSenales;
	private SeekBar mSeekBarF0;
	private SeekBar mSeekBarFs;
	private SeekBar mSeekBarOffset;
	private TextView mTextViewF0;
	private TextView mTextViewFs;
	private TextView mTextViewOffset;
	private TextView mTextViewDelay;
	protected TextView mTextViewEstado;
	
	/****************************
	 * PARÁMETROS DEL SIMULADOR *
	 ****************************/
	// Array de canales
	private ArrayList<Canal> mCanales = new ArrayList<Canal>();
	// Cantidad de canales
	private int mCantCanales = 3;
	// Canal actual
	private int mCanalActual = 0;
	// Frecuencia de la señal
	private double mF0 = 1;
	// Frecuencia de muestreo
	private double mFs = 500;
	// Período de muestreo
	private double mTs = 1/mFs;
	// Offset de la señal
	private double mOffset = 0;
	// Amplitud de la señal
	private double mAmplitud = 1;
	// Resolución (en bits) del ADC
	private int mBits = 12;
	// Instancia de la clase
	private Simulador mSimulador;
	// Contador de paquetes simulados
	private int mCantPaquetes = 0;
	// Keys para las señales a transmitir
	private final int SENAL_SENO = 1;
	private final int SENAL_SIERRA = 2;
	private final int SENAL_CUADRADA = 3;
	private final int SENAL_SECUENCIA = 4;
	
	/****************************
	 * VARIABLES DE LA CONEXIÓN *
	 ****************************/
	// Request Code para onResult de ElegirDispositivo
	protected int REQUEST_CODE_ELEGIR_DISPOSITIVO = 1;
	// MAC adress del dispositivo con el cual me conecto
	protected String mMAC = null;
	// Adaptador Bluetooth local
	private BluetoothAdapter mBluetoothAdapter;
	// Objeto para manejar la conexion
	private ConexionBT mConexionBT;
	// Keys recibidas por el Handler
	private static String mStringDispositivoRemoto;
	// Dispositivo al cual me conecto
	private BluetoothDevice  mDispositivoRemoto;	
	
	private double mDelayMax = 0.1;
	
	/**********************
	 * MENSAJE DE CONTROL *
	 **********************/
	// Cantidad de bytes de control
	private int mCantBytesMensajeControl = 3;
	// Mensaje de Control
	private byte[] mMensajeControl = new byte[mCantBytesMensajeControl];
	
	/*******************************
	 * MENSAJE DE CANT. DE CANALES *
	 *******************************/
	// Cantidad de Bytes del mensaje de canales. Es entero.
	private int mCantBytesMensajeCanales = 4;
	// Mensaje de cantidad de canales
	private byte[] mMensajeCanales = new byte[mCantBytesMensajeCanales];
	
	/****************************
	 * MENSAJE DE DATOS DEL ADC *
	 ****************************/
	// Cantidad de Bytes del mensaje de datos del ADC.
	private int mCantBytesMensajeADC;
	// Mensaje de datos del ADC
	private byte[] mMensajeADC;
	
	/****************************
	 * MENSAJE DE MUESTRAS *
	 ****************************/
	// Cantidad de muestras
	private int mCantMuestras;
	// Cantidad de bytes que voy a utilizar para cada muestra
	private int mCantBytesPorMuestra = 2;
	// Cantidad de bytes para indicar el canal (4 bytes -int- por defecto)
	private int mCantBytesCanal = 4;
	// Mensaje de muestras	
	private byte[] mMensajeMuestras;	
	
	
	/*************************************************************************************
	 * ENVÍO DE CANTIDAD DE CANALES  													 *
	 ************************************************************************************/
	private void enviarMensajeCanales() {
		// Paso cantidad de canales a Byte
		mMensajeCanales = intToByte(mCantCanales);
		// Envio
		mConexionBT.Escribir(mMensajeCanales);
	}
	
	/*************************************************************************************
	 * ENVÍO DE DATOS DEL ADC 															 *
	 ************************************************************************************/
	// Método para enviar la información del ADC
	private void enviarMensajeADC() {
		// Contenido del paquete de LSB a MSB
		// 1) Vmax y Vmin de cada canal (2 doubles por canal)
		int bloqueVoltajes = 2*mCantCanales*8;
		// 2) Frecuencia de muestreo de cada canal (1 double por canal)
		int bloqueFs = mCantCanales*8;
		// 3) Resolución de cada canal (1 entero por canal)
		int bloqueResolucion = 4*mCantCanales;
		// 4) Cantidad de muestras por paquete (1 entero)
		int bloqueMuestras = 4;
		// 5) Cantidad de bytes por muestra (1 entero)
		int bloqueBytesPorMuestra = 4;
		
		// Total
		mCantBytesMensajeADC = bloqueVoltajes + 
							   bloqueFs +
							   bloqueResolucion +
							   bloqueMuestras + 
							   bloqueBytesPorMuestra;
		
		// Genero paquete
		mMensajeADC = new byte[mCantBytesMensajeADC];
		
		// Lleno el paquete
		// 1) Vmax y Vmin de cada canal
		int inicio = 0;
		int fin = bloqueVoltajes;
		
		byte[] voltaje = new byte[8];
		for(int i=0; i<2*mCantCanales; i++) {
			voltaje = doubleToByte(Math.pow(-1, i));
			for(int j=i*8; j<(i+1)*8; j++) {
				mMensajeADC[j] = voltaje[j-(8*i)];
			}
		}
		
		// 2) Frecuencia de muestreo de cada canal
		inicio = fin;
		fin = inicio + bloqueFs;
		
		byte[] fs = new byte[8];
		for(int i=0; i<mCantCanales; i++) {
			fs = doubleToByte(mFs);
			for(int j=i*8; j<(i+1)*8; j++) {
				mMensajeADC[j + inicio] = fs[j-(8*i)];
			}
		}
		
		// 3) Resolución de cada canal
		inicio = fin;
		fin = inicio + bloqueResolucion;
		
		byte[] resolucion = new byte[4];
		for(int i=0; i<mCantCanales; i++) {
			resolucion = intToByte(mBits);
			for(int j=i*4; j<(i+1)*4; j++) {
				mMensajeADC[j + inicio] = resolucion[j-(4*i)];
			}
		}
		
		// 4) Cantidad de muestras por paquete
		inicio = fin;
		fin = inicio + bloqueMuestras;
		
		byte[] muestrasPorPaquete = new byte[4];
		muestrasPorPaquete = intToByte(mCantMuestras);
		for(int i=inicio; i<fin; i++) {
			mMensajeADC[i] = muestrasPorPaquete[i-inicio];
		}
	
		// 5) Cantidad de bytes por muestra
		inicio = fin;
		fin = inicio + bloqueBytesPorMuestra;
		
		byte[] bytesPorMuestra = new byte[4];
		bytesPorMuestra = intToByte(mCantBytesPorMuestra);
		for(int i=inicio; i<fin; i++) {
			mMensajeADC[i] = bytesPorMuestra[i-inicio];
		}
		
		// Envío
		mConexionBT.Escribir(mMensajeADC);
	}
	

	/**********************************************************************************************
	 * ENVÍO DE MENSAJE DE CONTROL																  *
	 **********************************************************************************************/
	private void enviarMensajeControl() {
		mConexionBT.Escribir(mMensajeControl);
	}
	
	/**********************************************************************************************
	 * ENVÍO DE MUESTRAS																		  *
	 **********************************************************************************************/
	private void enviarMuestras(short[] muestras, int canal) {
		// Coloco nro de canal en el buffer
		byte[] canal_byte = intToByte(canal);
		mMensajeMuestras[0] = canal_byte[0];
		mMensajeMuestras[1] = canal_byte[1];
		mMensajeMuestras[2] = canal_byte[2];
		mMensajeMuestras[3] = canal_byte[3];
		// Paso las muestras de short a byte (2 bytes por muestra)
		for(int i=0; i<mCantMuestras; i++) {
			for (int j = 0; j < 2; j++) {
				mMensajeMuestras[4 + 2*i + j] = (byte)(muestras[i] >> (j * 8));
			}
		}
		mConexionBT.Escribir(mMensajeMuestras);
	}
	
	
	/**********************************************************************************************
	 * HANDLER DEL THREAD DE SIMULACIÓN DE ADC 									 				  *
	 **********************************************************************************************/
	// Handler que va y viene del Thread del Simulador
	@SuppressLint("HandlerLeak")
	private final Handler mHandlerSimulador = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
		MENSAJES_SIMULADOR mensaje = MENSAJES_SIMULADOR.values(msg.what);
			
		switch (mensaje) {
		
			case MENSAJE_MUESTRA:
				// Pauseo la generación de muestras
				mSimulador.onPause();
				// Obtengo canal
				int canal = msg.arg2;
				// Levanto el array con las muestras generadas
				short[] muestras = (short[]) msg.obj;
				// Envío mensaje de control
				// Envío muestras
				//if(mCantPaquetes < 100) { 
				if(mConexionBT != null) {
					enviarMensajeControl();
					enviarMuestras(muestras, canal);
					SystemClock.sleep(1);
					mSimulador.proximoCanal();
				}
				//}
				// Resumo la generación de muestras con el próximo canal
				// Log
				//for(int i=0; i<mBufferedOutput.length; i++) {
					//if (D) Log.d(TAG, "Paquete nº "+ mCantPaquetes + ": " + mBufferedOutput[i]);
				//}
				mCantPaquetes++;
				mSimulador.onResume();
				break;
		
			default: 
				break;
			}
		}
	};
	
	
	/**********************************************************************************************
	 * HANDLER DEL THREAD DE CONEXIÓN BLUETOOTH													  *
	 **********************************************************************************************/
	@SuppressLint("HandlerLeak")
	private final Handler mHandlerConexionBT = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			MENSAJES_CONEXION mensaje = MENSAJES_CONEXION.values(msg.what);
			
			switch (mensaje) {
			
				// Estoy buscando al dispositivo
				case MENSAJE_BUSCANDO: 
					mTextViewEstado.setText("Conectando...");
					break;
			
				// Me conecte
				case MENSAJE_CONECTADO: 
					setupSimulador();
					setupMensajeControl();
					SystemClock.sleep(1);
					enviarMensajeControl();
					enviarMensajeCanales();
					//SystemClock.sleep(1000);
					enviarMensajeControl();
					//SystemClock.sleep(100);
					enviarMensajeADC();
					//SystemClock.sleep(1000);
					
					mTextViewEstado.setText("Conectado con " + mStringDispositivoRemoto + ".");
					setButtonDesconectar();
					mSimulador.setRunning(true);
					mSimulador.start();
					break;
				
				// Con quien me conecte?	
				case MENSAJE_DISPOSITIVO_REMOTO:
					mStringDispositivoRemoto = msg.obj.toString();
					break;
			
				// Perdi la conexion
				case MENSAJE_CONEXION_PERDIDA:
					mTextViewEstado.setText("Conexion perdida.");
					if(mSimulador != null) stopThreadSimulador();
					if(mConexionBT != null) stopThreadConexion();
					setButtonConectar();
					break;
					
				case MENSAJE_LEER:
					// Paso a ByteBuffer el array recibido
					Byte caracterControl = (Byte) msg.obj;
					
					if(checkCantCanalesOK(caracterControl) == true) {
						enviarMensajeControl();
						enviarMensajeADC();
					}
					
					if(checkInfoAdcOK(caracterControl) == true) { 
						//mConexionBT.setRun();
						SystemClock.sleep(200);
						mSimulador.setRunning(true);
						mSimulador.start();
					}	
					break;
			
				default:
					break;
			}//switch
		}
	};//mHandlerConexion
	
	// Configuro simulador
	public void setupSimulador() {
		// Empiezo a simular
		// Simulador(Handler mHandler, int mCantCanales, int mCantMuestras, double mFs, int mBits)
		mCantMuestras = (int) (mDelayMax / (mTs * mCantCanales));
		mMensajeMuestras = new byte[mCantBytesCanal + mCantBytesPorMuestra*mCantMuestras];
		mSimulador = new Simulador(mHandlerSimulador, mCantCanales, mCantMuestras, mFs, mBits);
		for(int i=0; i<mCantCanales; i++) {
			mSimulador.setAmplitud(1, i);
			mSimulador.setF0(1, i);
			mSimulador.setOffset(1, i);
			
			int tipo_senal;
			if(i <= 5) tipo_senal = 1;
				else tipo_senal = 1;
			mSimulador.setSenal(tipo_senal, i);
		}
	}
	
	private boolean checkCantCanalesOK(Byte caracterControl) {
		if(caracterControl == '&') {
			Toast.makeText(getApplicationContext(), "Canales configurados.", Toast.LENGTH_LONG).show();
			return true;
		} else return false;
	}
	
	private boolean checkInfoAdcOK(Byte caracterControl) {
		if(caracterControl == '$') { 
			Toast.makeText(getApplicationContext(), "Visualización configurada.", Toast.LENGTH_LONG).show();
			return true;
		} else return false;
	}
	
	// Método para pasar de double a byte
	private byte[] doubleToByte(double mDouble) {
		byte[] output = new byte[8];
		ByteBuffer.wrap(output).putDouble(mDouble);
		return output;
	}
	
	// Método para pasar de int a byte
	private byte[] intToByte(int mInt) {
		return  ByteBuffer.allocate(4).putInt(mInt).array();
	}
	
	private void setupMensajeControl() {
		for(int i=0; i<mCantBytesMensajeControl; i++) {
		mMensajeControl[i] = '#';
		}
	}
	
	/**********************************************************************************************
	 * THIS.ACTIVITY ON CREATE																	  *
	 **********************************************************************************************/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// No titulo. Fullscreen. No apagar pantalla.
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
	    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
	      					 WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	    // Inflo layout
	    setContentView(R.layout.layout_simulador_sensor);
	    // Fijo orientación vertical
	    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
		
	/**********************************************************************************************
	 * THIS.ACTIVITY ON START 																	  *
	 **********************************************************************************************/
	@Override
	public synchronized void onStart() {
		super.onStart();
		// Si es la primera vez que ejecuto la Activity, configuro el simulador
		if(mConexionBT == null) {
			setupUI();
			setupConexion();
		}
	}
		
	/**********************************************************************************************
	 * THIS.ACTIVITY ON DESTROY																  *
	 **********************************************************************************************/
	@Override
	public synchronized void onDestroy() {
		super.onDestroy();
		stopThreadConexion();
		stopThreadSimulador();
	}
		
	public void stopThreadConexion() {
		if (mConexionBT != null) mConexionBT.stop();
	}
		
	public void stopThreadSimulador() {
		if (mSimulador != null) { 
			boolean retry = true;
			mSimulador.setRunning(false);
			while(retry) {
				try {
					mSimulador.join();
					retry = false;
				} catch (InterruptedException e) {}
			}
		}
	}
		
	// Metodo que evita que la Activity se destruya al apretar atras
	public void onBackPressed() {
	    moveTaskToBack(true);
	}
	
	// Configuro conexión BT
	private void setupConexion() {
		// Inicializo Adapter Bluetooth
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// Instancio conexión
		mConexionBT = new ConexionBT(mHandlerConexionBT);
	}
	// Configuro UI
	private void setupUI() {
		// Inflo ButtonConectar
		mButtonConectar = (Button) findViewById(R.id.buttonConectar);
		// Inflo ButtonAgregarCanal
		mButtonAgregarCanal = (Button) findViewById(R.id.buttonAgregarCanal);
		// Inflo ButtonRemoverCanal
		mButtonRemoverCanal = (Button) findViewById(R.id.buttonRemoverCanal);
		// Inflo SpinnerCanales
		mSpinnerCanales = (Spinner) findViewById(R.id.spinnerCanales);
		// Inflo SpinnerSenales
		mSpinnerSenales = (Spinner) findViewById(R.id.spinnerSenales);
		// Inflo TextViewEstado
		mTextViewEstado = (TextView) findViewById(R.id.textViewEstado);
		// Inflo TextViewF0
		mTextViewF0 = (TextView) findViewById(R.id.textViewF0);
		// Inflo TextViewFs
		mTextViewFs = (TextView) findViewById(R.id.textViewFs);
		// Inflo TextViewBits
		mTextViewOffset = (TextView) findViewById(R.id.textViewOffset);
		// Inflo TextViewDelay
		mTextViewDelay = (TextView) findViewById(R.id.textViewDelay);
		// Inflo SeekBarF0
		mSeekBarF0 = (SeekBar) findViewById(R.id.seekBarF0);
		// Inflo SeekBarFs
		mSeekBarFs = (SeekBar) findViewById(R.id.seekBarFs);
		// Inflo SeekBarBits
		mSeekBarOffset = (SeekBar) findViewById(R.id.seekBarOffset);

		// Seteo botón de conexión
		setButtonConectar();
		
		// Seteo Listener buttonAgregarCanal
		mButtonAgregarCanal.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) { 
				if(mSimulador != null) { 
					//agregarCanal();
				}
			}
		});
		// Seteo Listener buttonRemoverCanal
		mButtonRemoverCanal.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) { 
				if(mSimulador != null) {
					//removerCanal() 
				}
			}
		});
		
		// Seteo Listener SeekBarF0
		mSeekBarF0.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {	
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                if(mSimulador != null) { 
                	//mSimulador.setF0((double)progress);
                	//mTextViewF0.setText("Frecuencia de la Señal (Hz): " + mSimulador.getF0());
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
		// Seteo Listener SeekBarFs
		mSeekBarFs.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mSimulador != null) { 
                	//mSimulador.setFs((double)progress+100);
                	//mTextViewFs.setText("Frecuencia de Muestreo (Hz): " + mSimulador.getFs());
                	//mTextViewDelay.setText("" + mSimulador.getDelay());
                }
            }
			public void onStartTrackingTouch(SeekBar seekBar) {}
			public void onStopTrackingTouch(SeekBar seekBar) {}
        });
		// Seteo Listener SeekBarOffset
		mSeekBarOffset.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                if(mSimulador != null) { 
                	//mSimulador.setOffset((progress - 99)*0.05);
                	//mTextViewOffset.setText("Offset: " + (mSimulador.getOffset()  
                	//						           -  mSimulador.getAmplitud()));
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
	}
	
	// mButtonConectar = Conectar
	private void setButtonConectar() {
		mButtonConectar.setText("Conectar");
		mButtonConectar.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(), ActivityElegirDispositivo.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivityForResult(intent, REQUEST_CODE_ELEGIR_DISPOSITIVO);
			}
		});
	}
	
	// mButtonConectar = Desconectar
	private void setButtonDesconectar() {
		mButtonConectar.setText("Desconectar");
		mButtonConectar.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mConexionBT.stop();
				setButtonConectar();
			}
		});
	}
	
	// this.Activity onActivityResult()
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Si vengo de seleccion de dispositivo a conectar
		if (requestCode == REQUEST_CODE_ELEGIR_DISPOSITIVO) {
		// Si la seleccion de dispositivo fue exitosa
			if(resultCode == RESULT_OK) {
				// Obtengo MAC Adress del dispositivo a conectar
				mMAC = data.getStringExtra("MAC");
				// Chequeo que el MAC adress sea valido (el formato es predefinido)
				if (BluetoothAdapter.checkBluetoothAddress(mMAC) == true) {
					// Obtengo objeto de tipo BluetoothDevice remoto
					mDispositivoRemoto = mBluetoothAdapter.getRemoteDevice(mMAC);
					// Intento conectarme con DispositivoRemoto
					mConexionBT.soyCliente(mDispositivoRemoto);
					// Inicializo TextViews y SeekBars
					//mSeekBarF0.setProgress((int)mF0);
					//mSeekBarFs.setProgress((int)mFs);
					//mSeekBarOffset.setProgress(mSeekBarOffset.getMax()/2 - 1);
				} else {
					// Informo que el MAC adress esta mal
					Toast.makeText(this, "MAC adress inválido.", Toast.LENGTH_LONG).show();
				}
			}
			// Si la seleccion de dispositivo no fue exitosa
			if(resultCode == RESULT_CANCELED) {
				// Termino con la Activity
				Toast.makeText(this, "Por favor, seleccione un dispositivo.", Toast.LENGTH_LONG).show();;
			}
        }	
    }
	
	// Basura que necesitan las progessbar para funcionar
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
	public void onStartTrackingTouch(SeekBar seekBar) {}
	public void onStopTrackingTouch(SeekBar seekBar) {}
}
