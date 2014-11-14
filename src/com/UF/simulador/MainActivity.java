package com.UF.simulador;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.app.ActionBarActivity;

public class MainActivity extends ActionBarActivity {
	

/*****************************************************************************************
* Inicio de atributos de clase				 										     *
*****************************************************************************************/
/*****************************************************************************************
* Debugging					 														     *
*****************************************************************************************/
    private static final String TAG = "Simulador_MainActivity";
    private static final boolean D = true;
    
/*****************************************************************************************
* Interfaz gráfica					 												     *
*****************************************************************************************/
	private Button mButtonConnect;
	private Spinner mSpinnerChannel;
	private Spinner mSpinnerSignal;
	private SeekBar mSeekBarF0;
	private SeekBar mSeekBarOffset;
	private TextView mTextViewF0;
	private TextView mTextViewOffset;
	protected TextView mTextViewStatus;
	private int mSelectedChannel;
	private int mSelectedSignal;
	
/*****************************************************************************************
* Parámetros del simulador	 														     *
*****************************************************************************************/
	// Cantidad de canales
	private int mTotalChannels = 2;
	// Frecuencia de la señal
	private double mF0 = 1;
	// Frecuencia de muestreo
	private double mFs = 500;
	// Período de muestreo
	private double mTs = 1/mFs;
	// Offset de la señal
	private double mOffset = 0;
	// Amplitud de la señal
	private double mAmplitude = 1;
	// Resolución (en bits) del ADC
	private int mBits = 12;
	// Instancia de la clase
	private AdcSimulator mAdcSimulator;
	// Contador de paquetes simulados
	private int mCantPaquetes = 0;
	private double mMaxVoltage = 2;
	private double mMinVoltage = -2;
	
	
/*****************************************************************************************
* Conexión Bluetooth		 														     *
*****************************************************************************************/
	// Request Code para onResult de ElegirDispositivo
	protected int REQUEST_CODE_ELEGIR_DISPOSITIVO = 1;
	// MAC adress del dispositivo con el cual me conecto
	protected String mMAC = null;
	// Adaptador Bluetooth local
	private BluetoothAdapter mBluetoothAdapter;
	// Objeto para manejar la conexion
	private BluetoohService mBluetoothConnection;
	// Keys recibidas por el Handler
	private static String mStringDispositivoRemoto;
	// Dispositivo al cual me conecto
	private BluetoothDevice  mDispositivoRemoto;	
	// Delay máximo admitido para generar un paquete de muestras
	private double mDelayMax = 0.1;

/*****************************************************************************************
* Paquetes Bluetooth		 														     *
*****************************************************************************************/
	/*************************************************************************************
	* Mensaje de control		 														 *
	*************************************************************************************/
	// Cantidad de bytes de control
	private int mCantBytesMensajeControl = 3;
	// Mensaje de Control
	private byte[] mMensajeControl = new byte[mCantBytesMensajeControl];
	
	/*************************************************************************************
	* Mensaje con la cantidad de canales 												 *
	*************************************************************************************/
	// Cantidad de Bytes del mensaje de canales. Es entero.
	private int mCantBytesMensajeCanales = 4;
	// Mensaje de cantidad de canales
	private byte[] mMensajeCanales = new byte[mCantBytesMensajeCanales];
	
	/*************************************************************************************
	* Mensaje con la información del ADC											     *
	*************************************************************************************/
	// Cantidad de Bytes del mensaje de datos del ADC.
	private int mAdcMessageTotalBytes;
	// Mensaje de datos del ADC
	private byte[] mAdcMessage;
	
	/*************************************************************************************
	* Mensaje con la cantidad de muestras 											     *
	*************************************************************************************/
	// Cantidad de muestras
	private int mTotalSamples;
	// Cantidad de bytes que voy a utilizar para cada muestra
	private int mCantBytesPorMuestra = 2;
	// Cantidad de bytes para indicar el canal (4 bytes -int- por defecto)
	private int mCantBytesCanal = 4;
	// Mensaje de muestras	
	private byte[] mSamplesMessage;	
	
	
/*****************************************************************************************
* Inicio de métodos de clase				 										     *
*****************************************************************************************/	
/*****************************************************************************************
* Paquetes Bluetooth				 												 	 *
*****************************************************************************************/
	private void setupControlMessage() {
		for(int i=0; i<mCantBytesMensajeControl; i++) {
		mMensajeControl[i] = '#';
		}
	}

	private void sendControlMessage() {
		mBluetoothConnection.write(mMensajeControl);
	}
	
	private void sendChannelQtyMessage() {
		// Paso cantidad de canales a Byte
		mMensajeCanales = intToByte(mTotalChannels);
		// Envio
		mBluetoothConnection.write(mMensajeCanales);
	}
	
	private void sendAdcMessage() {
		// Contenido del paquete de LSB a MSB
		// 1) Vmax y Vmin de cada canal (2 doubles por canal)
		int voltageBlock = 2*mTotalChannels*8;
		// 2) Amplitudes máximas y mínimas de cada canal (2 doubles por canal)
		int amplitudeBlock = 2*mTotalChannels*8;
		// 3) Frecuencia de muestreo de cada canal (1 double por canal)
		int fsBlock = mTotalChannels*8;
		// 4) Resolución de cada canal (1 entero por canal)
		int resolutionBlock = 4*mTotalChannels;
		// 5) Cantidad de muestras por paquete (1 entero)
		int sampleQtyBlock = 4;
		// 6) Cantidad de bytes por muestra (1 entero)
		int bytesPerSampleBlock = 4;
		
		// Total
		mAdcMessageTotalBytes = voltageBlock + 
							   amplitudeBlock +
							   fsBlock +
							   resolutionBlock +
							   sampleQtyBlock + 
							   bytesPerSampleBlock;
		
		// Genero paquete
		mAdcMessage = new byte[mAdcMessageTotalBytes];
		
		// 1) Vmax y Vmin de cada canal
		int start = 0;
		int end = voltageBlock;
		
		byte[] voltage = new byte[8];
		for(int i=0; i<2*mTotalChannels; i++) {
			double sign = Math.pow(-1, i);
			voltage = doubleToByte(sign*mMaxVoltage);
			for(int j=i*8; j<(i+1)*8; j++) {
				mAdcMessage[j] = voltage[j-(8*i)];
			}
		}
		
		// 2) Amax y Amin de cada canal
		start = voltageBlock;
		end = start + amplitudeBlock;
		
		byte[] amplitud = new byte[8];
		for(int i = 0; i < 2*mTotalChannels; i++) {
			amplitud = doubleToByte(Math.pow(-1, i));
			for(int j=i*8; j<(i+1)*8; j++) {
				mAdcMessage[j + start] = amplitud[j-(8*i)];
			}
		}
		
		// 3) Frecuencia de muestreo de cada canal
		start = end;
		end = start + fsBlock;
		
		byte[] fs = new byte[8];
		for(int i=0; i<mTotalChannels; i++) {
			fs = doubleToByte(mFs);
			for(int j=i*8; j<(i+1)*8; j++) {
				mAdcMessage[j + start] = fs[j-(8*i)];
			}
		}
		
		// 4) Resolución de cada canal
		start = end;
		end = start + resolutionBlock;
		
		byte[] resolucion = new byte[4];
		for(int i=0; i<mTotalChannels; i++) {
			resolucion = intToByte(mBits);
			for(int j=i*4; j<(i+1)*4; j++) {
				mAdcMessage[j + start] = resolucion[j-(4*i)];
			}
		}
		
		// 5) Cantidad de muestras por paquete
		start = end;
		end = start + sampleQtyBlock;
		
		byte[] muestrasPorPaquete = new byte[4];
		muestrasPorPaquete = intToByte(mTotalSamples);
		for(int i=start; i<end; i++) {
			mAdcMessage[i] = muestrasPorPaquete[i-start];
		}
	
		// 6) Cantidad de bytes por muestra
		start = end;
		end = start + bytesPerSampleBlock;
		
		byte[] bytesPorMuestra = new byte[4];
		bytesPorMuestra = intToByte(mCantBytesPorMuestra);
		for(int i=start; i<end; i++) {
			mAdcMessage[i] = bytesPorMuestra[i-start];
		}
		
		// Envío
		mBluetoothConnection.write(mAdcMessage);
	}
	
	private void sendSamples(short[] samples, int adcChannel) {
		
		// Coloco nro de canal en el buffer
		byte[] canal_byte = intToByte(adcChannel);
		mSamplesMessage[0] = canal_byte[0];
		mSamplesMessage[1] = canal_byte[1];
		mSamplesMessage[2] = canal_byte[2];
		mSamplesMessage[3] = canal_byte[3];
		
		// Paso las muestras de short a byte (2 bytes por muestra)
		for(int i = 0; i < mTotalSamples; i++) {
			
			for (int j = 0; j < 2; j++) {
				
				mSamplesMessage[4 + 2*i + j] = (byte)(samples[i] >> (j * 8));
			
			}
			
		}
		
		mBluetoothConnection.write(mSamplesMessage);
	}
	
	
/*****************************************************************************************
* Simulación de muestras									 				  			 *
*****************************************************************************************/
	// Handler que va y viene del Thread del Simulador
	@SuppressLint("HandlerLeak")
	private final Handler mHandlerSimulador = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
		AdcSimulatorMessage mensaje = AdcSimulatorMessage.values(msg.what);
			
		switch (mensaje) {
		
			case MENSAJE_MUESTRA:

				short[] samples = (short[]) msg.obj;
				int channel = msg.arg2;
				
				mCantPaquetes++;
				if(mBluetoothConnection != null) {
					sendControlMessage();
					sendSamples(samples, channel);
				}
				
				
								
				break;
		
			default: 
				break;
			}
		}
	};

	private void setupAdcSimulator() {

		//mTotalSamples = (int) (mDelayMax / (mTs * mTotalChannels));
		mTotalSamples = 4;
		mSamplesMessage = new byte[mCantBytesCanal + mCantBytesPorMuestra*mTotalSamples];
		mAdcSimulator = new AdcSimulator(mHandlerSimulador, mTotalChannels, mTotalSamples, mFs, mBits);
		
		for(int i = 0; i < mTotalChannels; i++) {
			
			mAdcSimulator.setAmplitud(1, i);
			mAdcSimulator.setF0(1, i);
			mAdcSimulator.setInitialOffset(1, i);
			mAdcSimulator.setSenal(i+1, i);
			
		}
		
	}

	private void startAdcSimulator() {
		
		mAdcSimulator.setOnline(true);
		mAdcSimulator.setRunning(true);
		mAdcSimulator.start();
	
	}
	
	private void stopAdcSimulator() {
		
		if (mAdcSimulator != null) { 
			
			boolean retry = true;
			
			mAdcSimulator.setRunning(false);
			
			while(retry) {
				
				try {
					
					mAdcSimulator.join();
					retry = false;
				
				} catch (InterruptedException e) {}
			
			}
			
		}
		
	}

	
/*****************************************************************************************
* Conexión Bluetooth		 														     *
*****************************************************************************************/
	private final Handler mHandlerConexionBT = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			BluetoothMessage mensaje = BluetoothMessage.values(msg.what);
			
			switch (mensaje) {
			
				// Estoy buscando al dispositivo
				case MENSAJE_BUSCANDO: 
					mTextViewStatus.setText("Conectando...");
					break;
			
				// Me conecte
				case MENSAJE_CONECTADO: 
					
					setupAdcSimulator();
					setupControlMessage();
					
					sendControlMessage();
					sendChannelQtyMessage();
					
					sendControlMessage();
					sendAdcMessage();
					
					mTextViewStatus.setText("Conectado con " + mStringDispositivoRemoto + ".");
					
					setButtonDisconnect();
					
					startAdcSimulator();
					
					break;
				
				// Con quien me conecte?	
				case MENSAJE_DISPOSITIVO_REMOTO:
					mStringDispositivoRemoto = msg.obj.toString();
					break;
			
				// Perdi la conexion
				case MENSAJE_CONEXION_PERDIDA:
					mTextViewStatus.setText("Conexion perdida.");
					if(mAdcSimulator != null) stopAdcSimulator();
					if(mBluetoothConnection != null) stopBluetoothConnection();
					setButtonConnnect();
					break;
					
				case MENSAJE_LEER:
					// Paso a ByteBuffer el array recibido
					Byte caracterControl = (Byte) msg.obj;
					
					if(checkChannelQtyMessage(caracterControl) == true) {
						sendControlMessage();
						sendAdcMessage();
					}
					
					if(checkAdcMessage(caracterControl) == true) { 
						//mConexionBT.setRun();
						SystemClock.sleep(200);
						mAdcSimulator.setRunning(true);
						mAdcSimulator.start();
					}	
					break;
			
				default:
					break;
			}//switch
		}
	};//mHandlerConexion
	
	private boolean checkChannelQtyMessage(Byte caracterControl) {
		if(caracterControl == '&') {
			Toast.makeText(getApplicationContext(), "Canales configurados.", Toast.LENGTH_LONG).show();
			return true;
		} else return false;
	}
	
	private boolean checkAdcMessage(Byte caracterControl) {
		if(caracterControl == '$') { 
			Toast.makeText(getApplicationContext(), "Visualización configurada.", Toast.LENGTH_LONG).show();
			return true;
		} else return false;
	}

	public void stopBluetoothConnection() {
		if (mBluetoothConnection != null) mBluetoothConnection.stop();
	}

	private void setupConexion() {
		
		// Inicializo Adapter Bluetooth
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// Instancio conexión
		mBluetoothConnection = new BluetoohService(mHandlerConexionBT);
		
	}	
	

/*****************************************************************************************
* Conversión de tipos de datos														     *
*****************************************************************************************/
	private byte[] doubleToByte(double mDouble) {
		byte[] output = new byte[8];
		ByteBuffer.wrap(output).putDouble(mDouble);
		return output;
	}
	
	private byte[] intToByte(int mInt) {
		return  ByteBuffer.allocate(4).putInt(mInt).array();
	}
	
	
/*****************************************************************************************
* Interfaz gráfica																	     *
*****************************************************************************************/
	private void setupUI() {
		
		// Inflo ButtonConectar
		mButtonConnect = (Button) findViewById(R.id.buttonConectar);
		
		// Inflo SpinnerCanales
		mSpinnerChannel = (Spinner) findViewById(R.id.spinnerCanales);
		setChannelSpinnerListener();
		populateChannelSpinner();
		
		// Inflo SpinnerSenales
		mSpinnerSignal = (Spinner) findViewById(R.id.spinnerSenales);
		setSignalSpinnerListener();
		populateSignalSpinner();
	
		// Inflo TextViewEstado
		mTextViewStatus = (TextView) findViewById(R.id.textViewEstado);
		
		// Inflo TextViewF0
		mTextViewF0 = (TextView) findViewById(R.id.textViewF0);
		
		// Inflo TextViewBits
		mTextViewOffset = (TextView) findViewById(R.id.textViewOffset);
		
		// Inflo SeekBarF0
		mSeekBarF0 = (SeekBar) findViewById(R.id.seekBarF0);
		
		// Inflo SeekBarOffset
		mSeekBarOffset = (SeekBar) findViewById(R.id.seekBarOffset);

		// Seteo botón de conexión
		setButtonConnnect();
		
		// Seteo Listener SeekBarF0
		setSeekBarF0Listener();
           
		// Seteo Listener SeekBarOffset
		setSeekBarOffsetListener();
		
	}
	
	// mButtonConectar = Conectar
	private void setButtonConnnect() {
		mButtonConnect.setText("Conectar");
		mButtonConnect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(), RemoteDeviceActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivityForResult(intent, REQUEST_CODE_ELEGIR_DISPOSITIVO);
			}
		});
	}
	
	// mButtonConectar = Desconectar
	private void setButtonDisconnect() {
		
		mButtonConnect.setText("Desconectar");
		
		mButtonConnect.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				mBluetoothConnection.stop();
				mAdcSimulator.setOnline(false);
				mAdcSimulator.onResume();
				setButtonConnnect();
			
			}
		
		});
	
	}
	
	private void populateChannelSpinner() {
		
		ArrayList<String> channels = new ArrayList<String>();
	    
		for (int i = 0; i < mTotalChannels; i++) {
	        channels.add(Integer.toString(i+1));
	    }
	   
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, channels);

	    mSpinnerChannel.setAdapter(arrayAdapter);
	    
	}
	
	private void populateSignalSpinner() {
		
		ArrayList<String> signals = new ArrayList<String>();
		
		signals.add("Senoidal");
		signals.add("Dientes de Sierra");
	    signals.add("Cuadrada");
	    
	    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, signals);
	    
	    mSpinnerSignal.setAdapter(arrayAdapter);
	}
	
	private void setChannelSpinnerListener() {
		
		mSpinnerChannel.setOnItemSelectedListener(new OnItemSelectedListener() {
			
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				
				mSelectedChannel = arg2;
				
				if(mAdcSimulator != null) {
					
					AdcChannel selectedChannel = mAdcSimulator.getChannel(mSelectedChannel);
					
					int offset = (int) (selectedChannel.getOffset() + 99);
					mSeekBarOffset.setProgress(offset);
					
					int f0 = (int) selectedChannel.getF0();
					mSeekBarF0.setProgress(f0);
					
					int signal = selectedChannel.getSignal();
					mSpinnerSignal.setSelection(signal - 1, true);
				
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
			
		});
		
	}
	
	private void setSignalSpinnerListener() {
		
		mSpinnerSignal.setOnItemSelectedListener(new OnItemSelectedListener() {
			
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

				mSelectedSignal = arg2 + 1;
				
				if(mAdcSimulator != null) {
					AdcChannel selectedChannel = mAdcSimulator.getChannel(mSelectedChannel);
					selectedChannel.setSenal(mSelectedSignal);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
			
		});
	
	}

	private void setSeekBarF0Listener() {
		
		mSeekBarF0.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {	
            
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                
				if(mAdcSimulator != null) { 
                	AdcChannel selectedChannel = mAdcSimulator.getChannel(mSelectedChannel);
                	selectedChannel.setF0((double)progress);
                	mTextViewF0.setText("Frecuencia de la Señal (Hz): " + selectedChannel.getF0());
                }
				
            }
           
			public void onStartTrackingTouch(SeekBar seekBar) {}
            
			public void onStopTrackingTouch(SeekBar seekBar) {}
        
		});

	}
	
	private void setSeekBarOffsetListener() {
		
		mSeekBarOffset.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                
				if(mAdcSimulator != null) {
					
					AdcChannel selectedChannel = mAdcSimulator.getChannel(mSelectedChannel);
                	selectedChannel.setOffset((progress - 99));
                	
                	// Actualizo Label de Zoom X
    				DecimalFormat df = new DecimalFormat();
    				df.setMaximumFractionDigits(1);
    				
    				double newOffset = selectedChannel.getOffset() - selectedChannel.getAmplitude();
    				df.format(newOffset);
    				
                	mTextViewOffset.setText("Offset: " + df.format(newOffset));
                
				}
				
            }
            
			public void onStartTrackingTouch(SeekBar seekBar) {}
            
			public void onStopTrackingTouch(SeekBar seekBar) {}
       
		});
	
	}


/*****************************************************************************************
* Ciclo de vida de la activity														     *
*****************************************************************************************/
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
	    
	    //Intent intent = new Intent(getApplicationContext(), AdcChannelsActivity.class);
		//intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		//startActivityForResult(intent, REQUEST_CODE_ELEGIR_DISPOSITIVO);
	}
		
	@Override
	public synchronized void onStart() {
		super.onStart();
		// Si es la primera vez que ejecuto la Activity, configuro el simulador
		if(mBluetoothConnection == null) {
			setupUI();
			setupConexion();
		}
	}
		
	@Override
	public synchronized void onDestroy() {
		super.onDestroy();
		stopBluetoothConnection();
		stopAdcSimulator();
	}

	public synchronized void onBackPressed() {
	    moveTaskToBack(true);
	}
	
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
					mBluetoothConnection.soyCliente(mDispositivoRemoto);
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

	
/*****************************************************************************************
* Otros métodos																		     *
*****************************************************************************************/	
	// Basura que necesitan las progessbar para funcionar
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
	public void onStartTrackingTouch(SeekBar seekBar) {}
	public void onStopTrackingTouch(SeekBar seekBar) {}
}
