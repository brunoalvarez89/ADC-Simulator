package com.ufavaloro.android.simulador.userinterface;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;

import com.UF.simulador.R;
import com.ufavaloro.android.simulador.adcsimulator.AdcChannel;
import com.ufavaloro.android.simulador.adcsimulator.AdcSimulator;
import com.ufavaloro.android.simulador.adcsimulator.AdcSimulatorMessage;
import com.ufavaloro.android.simulador.bluetooth.BluetoohService;
import com.ufavaloro.android.simulador.bluetooth.BluetoothMessage;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
	private Button mButtonAddChannel;
	private Button mButtonRemoveChannel;
	private Spinner mSpinnerChannels;
	private Spinner mSpinnerSignal;
	private SeekBar mSeekBarF0;
	private TextView mTextViewF0;
	protected TextView mTextViewStatus;
	protected TextView mTextViewChannels;
	protected TextView mTextViewFs;
	protected EditText mEditTextFs;
	protected TextView mTextViewTe;
	protected EditText mEditTextTe;
	protected TextView mTextViewResolution;
	protected EditText mEditTextResolution;
	protected EditText mEditTextSamplesQuantity;
	protected TextView mTextViewSamplesQuantity;
	private int mSelectedChannel = -1;
	private int mSelectedSignal;
	protected ArrayList<Integer> mChannelList = new ArrayList<Integer>();
	
/*****************************************************************************************
* Parámetros del simulador	 														     *
*****************************************************************************************/
	// Frecuencia de la señal
	private double mF0 = 1;
	// Frecuencia de muestreo
	private double mFs = 250;
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
	private double mPackageCount = 0;
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
	private BluetoohService mBluetoothService;
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
	private int mControlMessageByteCount = 3;
	// Mensaje de Control
	private byte[] mControlMessage = new byte[mControlMessageByteCount];
	
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
	private int mSamplesQuantity;
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
		for(int i=0; i<mControlMessageByteCount; i++) mControlMessage[i] = '#';
	}

	private void sendControlMessage() {
		mBluetoothService.write(mControlMessage);
	}
	
	private void sendChannelQtyMessage() {
		// Paso cantidad de canales a Byte
		mMensajeCanales = intToByte(mChannelList.size());
		// Envio
		mBluetoothService.write(mMensajeCanales);
	}
	
	private void sendAdcMessage() {
		// Contenido del paquete de LSB a MSB
		// 1) Vmax y Vmin de cada canal (2 doubles por canal)
		int voltageBlock = 2*mChannelList.size()*8;
		// 2) Amplitudes máximas y mínimas de cada canal (2 doubles por canal)
		int amplitudeBlock = 2*mChannelList.size()*8;
		// 3) Frecuencia de muestreo de cada canal (1 double por canal)
		int fsBlock = mChannelList.size()*8;
		// 4) Resolución de cada canal (1 entero por canal)
		int resolutionBlock = 4*mChannelList.size();
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
		for(int i=0; i<2*mChannelList.size(); i++) {
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
		for(int i = 0; i < 2*mChannelList.size(); i++) {
			amplitud = doubleToByte(Math.pow(-1, i));
			for(int j=i*8; j<(i+1)*8; j++) {
				mAdcMessage[j + start] = amplitud[j-(8*i)];
			}
		}
		
		// 3) Frecuencia de muestreo de cada canal
		start = end;
		end = start + fsBlock;
		
		byte[] fs = new byte[8];
		for(int i=0; i<mChannelList.size(); i++) {
			fs = doubleToByte(mFs);
			for(int j=i*8; j<(i+1)*8; j++) {
				mAdcMessage[j + start] = fs[j-(8*i)];
			}
		}
		
		// 4) Resolución de cada canal
		start = end;
		end = start + resolutionBlock;
		
		byte[] resolucion = new byte[4];
		for(int i=0; i<mChannelList.size(); i++) {
			resolucion = intToByte(mBits);
			for(int j=i*4; j<(i+1)*4; j++) {
				mAdcMessage[j + start] = resolucion[j-(4*i)];
			}
		}
		
		// 5) Cantidad de muestras por paquete
		start = end;
		end = start + sampleQtyBlock;
		
		byte[] muestrasPorPaquete = new byte[4];
		muestrasPorPaquete = intToByte(mSamplesQuantity);
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
		mBluetoothService.write(mAdcMessage);
	}
	
	private void sendSamples(short[] samples, int adcChannel) {
		
		// Coloco nro de canal en el buffer
		byte[] canal_byte = intToByte(adcChannel);
		mSamplesMessage[0] = canal_byte[0];
		mSamplesMessage[1] = canal_byte[1];
		mSamplesMessage[2] = canal_byte[2];
		mSamplesMessage[3] = canal_byte[3];
		
		// Paso las muestras de short a byte (2 bytes por muestra)
		for(int i = 0; i < mSamplesQuantity; i++) {
			for (int j = 0; j < 2; j++) {
				mSamplesMessage[4 + 2*i + j] = (byte)(samples[i] >> (j * 8));
			}
		}
		mBluetoothService.write(mSamplesMessage);
	}
	
	private void sendPackageCount(double packageNumber) {
		byte[] bytePackageNumber = doubleToByte(packageNumber);
		mBluetoothService.write(bytePackageNumber);
	}
	
/*****************************************************************************************
* Simulación de muestras									 				  			 *
*****************************************************************************************/
	// Handler que va y viene del Thread del Simulador
	@SuppressLint("HandlerLeak")
	private final Handler mAdcSimulatorHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
		AdcSimulatorMessage mensaje = AdcSimulatorMessage.values(msg.what);
			
		switch (mensaje) {
		
			case MENSAJE_MUESTRA:

				short[] samples = (short[]) msg.obj;
				int channel = msg.arg2;
				
				mPackageCount++;
				if(mBluetoothService != null) {
					sendControlMessage();
					sendSamples(samples, channel);
					sendPackageCount(mPackageCount);
				}

				break;
		
			default: 
				break;
			}
		}
	};
	
	private void setupAdcSimulator() {
		mSamplesMessage = new byte[mCantBytesCanal + mCantBytesPorMuestra*mSamplesQuantity];
		mAdcSimulator = new AdcSimulator(mAdcSimulatorHandler, mChannelList.size(), mSamplesQuantity, mFs, mBits);
		
		for(int i = 0; i < mChannelList.size(); i++) {
			mAdcSimulator.getChannel(i).setSignalType(mChannelList.get(i));
		}
	}

	private void startAdcSimulator() {
		mAdcSimulator.setRunning(true);
		mAdcSimulator.start();
	}
	
	private void stopAdcSimulator() {
		if (mAdcSimulator == null) return;
	
		boolean retry = true;		
		mAdcSimulator.setRunning(false);
		
		while(retry) {
			try {
				mAdcSimulator.join();
				retry = false;
			} catch (InterruptedException e) {}
		}
	}

	private double calculateSamplesQuantity() {
		if(mTs != 0 || mChannelList.size() != 0) {
			return (mDelayMax / (mTs * mChannelList.size()));
		} else {
			return 0;
		}
	}

/*****************************************************************************************
* Conexión Bluetooth		 														     *
*****************************************************************************************/
	private final Handler mBluetoothServiceHandler = new Handler() {
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
					
					disableUiElements();
					
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
					enableUiElements();
					mTextViewStatus.setText("Conexion perdida.");
					if(mAdcSimulator != null) stopAdcSimulator();
					if(mBluetoothService != null) stopBluetoothConnection();
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
		if(mBluetoothService != null) mBluetoothService.stop();
	}

	private void addConnection() {
		// Inicializo Adapter Bluetooth
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// Instancio conexión
		mBluetoothService = new BluetoohService(mBluetoothServiceHandler);
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
		
		// Inflo SpinnerCanales
		mSpinnerChannels = (Spinner) findViewById(R.id.spinnerCanales);
		setChannelSpinnerListener();
		populateChannelsSpinner();
		
		// Inflo SpinnerSenales
		mSpinnerSignal = (Spinner) findViewById(R.id.spinnerSenales);
		setSignalSpinnerListener();
		populateSignalSpinner();

		// Inflo SeekBarF0
		mTextViewF0 = (TextView) findViewById(R.id.textViewF0);
		mSeekBarF0 = (SeekBar) findViewById(R.id.seekBarF0);
		setSeekBarF0Listener();
		
		// Inflo EditTextSamplesQuantity
		mEditTextSamplesQuantity = (EditText) findViewById(R.id.editTextSamplesQuantity);
		mEditTextSamplesQuantity.setEnabled(false);
		
		// Inflo EditTextFs
		mEditTextFs = (EditText) findViewById(R.id.editTextFs);
		setEditTextFsListener();
		mTextViewFs = (TextView) findViewById(R.id.textViewFs);
		
		
		// Inflo EditTextResolution
		mEditTextResolution = (EditText) findViewById(R.id.editTextResolution);
		setEditTextResolutionListener();
		mTextViewResolution = (TextView) findViewById(R.id.textViewResolution);
		
		// Inflo EditTextTe
		mEditTextTe = (EditText) findViewById(R.id.editTextTe);
		setEditTextTeListener();
		mTextViewTe = (TextView) findViewById(R.id.textViewTe);

		// Inflo TextViewEstado
		mTextViewStatus = (TextView) findViewById(R.id.textViewEstado);
	
		// Inflo TextViewChannels
		mTextViewChannels = (TextView) findViewById(R.id.textViewChannels);	
		
		// Inflo ButtonConectar
		mButtonConnect = (Button) findViewById(R.id.buttonConectar);
		setButtonConnnect();
		
		// Inflo ButtonAddChannel
		mButtonAddChannel = (Button) findViewById(R.id.buttonAddChannel);
		setButtonAddChannelListener();
		
		// Inflo ButtonRemoveChannel
		mButtonRemoveChannel = (Button) findViewById(R.id.buttonRemoveChannel);
		setButtonRemoveChannelListener();
		
		mEditTextTe.setText("100");
		mEditTextResolution.setText("12");
		mEditTextFs.setText("500");
		mButtonAddChannel.performClick();
	
	}
	
	// mButtonConectar = Conectar
	private void setButtonConnnect() {
		mButtonConnect.setText("Conectar");
		mButtonConnect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mChannelList.size() == 0)  {
					Toast.makeText(getApplicationContext(), "Por favor, agregue al menos un canal.", Toast.LENGTH_LONG).show();
				} else {
					Intent intent = new Intent(getApplicationContext(), RemoteDeviceActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivityForResult(intent, REQUEST_CODE_ELEGIR_DISPOSITIVO);
				}
			}
		});
	}
	
	// mButtonConectar = Desconectar
	private void setButtonDisconnect() {
		mButtonConnect.setText("Desconectar");
		mButtonConnect.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {	
				mBluetoothService.stop();
				mAdcSimulator.onResume();
				setButtonConnnect();
			}
		});
	}
	
	private void setButtonAddChannelListener() {
		mButtonAddChannel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mChannelList.add(5);
				populateChannelsSpinner();
				mTextViewChannels.setText("Canales (" + mChannelList.size() + ")");
				mSamplesQuantity = (int) calculateSamplesQuantity();
				mEditTextSamplesQuantity.setText(String.valueOf(mSamplesQuantity));
			}
		});
	}
	
	private void setButtonRemoveChannelListener() {
		mButtonRemoveChannel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mChannelList.size() > 1) {
					mChannelList.remove(mChannelList.size()-1);
					populateChannelsSpinner();	
					mTextViewChannels.setText("Canales (" + mChannelList.size() + ")");
					mSamplesQuantity = (int) calculateSamplesQuantity();
					mEditTextSamplesQuantity.setText(String.valueOf(mSamplesQuantity));
				}
			}
		});
	}
	
	private void setEditTextFsListener() {
		mEditTextFs.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable text) {
				if(String.valueOf(text).isEmpty()) return;
				mFs = Double.parseDouble(String.valueOf(text));
				mTs = 1 / mFs;
				mSamplesQuantity = (int) calculateSamplesQuantity();
				mEditTextSamplesQuantity.setText(String.valueOf(mSamplesQuantity));
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
			
		});
	}
	
	private void setEditTextResolutionListener() {
		mEditTextResolution.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable text) {
				if(String.valueOf(text).isEmpty()) return;
				mBits = Integer.parseInt(String.valueOf(text));
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
			
		});
	}
	
	private void setEditTextTeListener() {
		mEditTextTe.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable text) {
				if(String.valueOf(text).isEmpty()) return;
				mDelayMax = Double.parseDouble(String.valueOf(text)) / 1000;
				mSamplesQuantity = (int) calculateSamplesQuantity();
				mEditTextSamplesQuantity.setText(String.valueOf(mSamplesQuantity));
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
			
		});
	}
	
	private void populateChannelsSpinner() {
		ArrayList<String> channels = new ArrayList<String>();
		for (int i = 0; i < mChannelList.size(); i++) channels.add(Integer.toString(i+1));
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, channels);
	    mSpinnerChannels.setAdapter(arrayAdapter); 
	}
	
	private void populateSignalSpinner() {
		ArrayList<String> signals = new ArrayList<String>();
		signals.add("Senoidal");
		signals.add("Dientes de Sierra");
	    signals.add("Cuadrada");
	    signals.add("Secuencia");
	    signals.add("ECG");
	    signals.add("Presión");
	    
	    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, signals);
	    mSpinnerSignal.setAdapter(arrayAdapter);
	}
	
	private void setChannelSpinnerListener() {
		mSpinnerChannels.setOnItemSelectedListener(new OnItemSelectedListener() {
			
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {			
				mSelectedChannel = arg2;
				
				if(mAdcSimulator != null) {
					AdcChannel selectedChannel = mAdcSimulator.getChannel(mSelectedChannel);

					int signalCode = selectedChannel.getSignalCode();
					
					switch(signalCode) {
						
						// Sine
						case 1:
							mSeekBarF0.setEnabled(true);
							mTextViewF0.setText("Frecuencia de la Señal");
							break;
						
						// Sawtooth
						case 2:
							mSeekBarF0.setEnabled(true);
							mTextViewF0.setText("Frecuencia de la Señal");
							break;
						
						// Square
						case 3:
							mSeekBarF0.setEnabled(true);
							mTextViewF0.setText("Frecuencia de la Señal");
							break;
						
						// Sequence
						case 4:
							mSeekBarF0.setEnabled(false);
							mTextViewF0.setText("-");
							break;
						
						// EKG
						case 5:
							mSeekBarF0.setEnabled(true);
							mTextViewF0.setText("Frecuencia Cardíaca");
							break;
							
						// Pressure
						case 6:
							mSeekBarF0.setEnabled(false);
							mTextViewF0.setText("-");
							break;
					}
					
					mSpinnerSignal.setSelection(signalCode - 1, true);

					int f0 = (int) selectedChannel.getF0();
					mSeekBarF0.setProgress(f0);
					
				} else {
					int signal = mChannelList.get(mSelectedChannel);
					mSpinnerSignal.setSelection(signal-1, true);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
			
		});

	}
	
	private void setSignalSpinnerListener() {
		mSpinnerSignal.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				mSelectedSignal = arg2 + 1;
				if(mSelectedChannel < mChannelList.size() && mSelectedChannel != -1) { 
					mChannelList.set(mSelectedChannel, mSelectedSignal);
				}
				
				switch(mSelectedSignal) {
				
					// Sine
					case 1:
						mSeekBarF0.setMax(20);
						mSeekBarF0.setEnabled(true);
						mTextViewF0.setText("Frecuencia de la Señal");
						break;
					
					// Sawtooth
					case 2:
						mSeekBarF0.setMax(20);
						mSeekBarF0.setEnabled(true);
						mTextViewF0.setText("Frecuencia de la Señal");
						break;
					
					// Square
					case 3:
						mSeekBarF0.setMax(20);
						mSeekBarF0.setEnabled(true);
						mTextViewF0.setText("Frecuencia de la Señal");
						break;
					
					// Sequence
					case 4:
						mSeekBarF0.setEnabled(false);
						mTextViewF0.setText("-");
						break;
					
					// EKG
					case 5:
						mSeekBarF0.setMax(100);
						mSeekBarF0.setEnabled(true);
						mTextViewF0.setText("Frecuencia Cardíaca");
						break;
						
					// Pressure
					case 6:
						mSeekBarF0.setEnabled(false);
						mTextViewF0.setText("-");
						break;
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
                	int signalCode = selectedChannel.getSignalCode();
                	
                	switch(signalCode) {
					
						// Sine
						case 1:
							mSeekBarF0.setMax(20);
							selectedChannel.setF0((double)progress);
		                	mTextViewF0.setText("Frecuencia de la Señal (Hz): " + selectedChannel.getF0());
		                	break;
						
						// Sawtooth
						case 2:
							mSeekBarF0.setMax(20);
							selectedChannel.setF0((double)progress);
		                	mTextViewF0.setText("Frecuencia de la Señal (Hz): " + selectedChannel.getF0());
							break;
						
						// Square
						case 3:
							mSeekBarF0.setMax(20);
							selectedChannel.setF0((double)progress);
		                	mTextViewF0.setText("Frecuencia de la Señal (Hz): " + selectedChannel.getF0());
							break;
						
						// Sequence
						case 4:
							break;
						
						// EKG
						case 5:
							mSeekBarF0.setMax(240);
							// CF(mDelay) = [mDelay * (EkgSignal.length() / SamplesQuantity)]^-1
							// mDelay(CF)= [CF * (EkgSignal.length/SamplesQuantity)]^-1
							double CF = ((double) progress+1) / 60;
							int signalLength = selectedChannel.getEkgSignal().length;
							double delay = (1 / (CF * signalLength / mSamplesQuantity))*1000;
							mTextViewF0.setText("Frecuencia Cardíaca: " + (double)(CF*60) + " LPM");
							selectedChannel.setDelay(delay);
							break;
							
						// Pressure
						case 6:
							break;
                	}
                	
                }
				
            }
           
			public void onStartTrackingTouch(SeekBar seekBar) {}
            
			public void onStopTrackingTouch(SeekBar seekBar) {}
        
		});
	}
	
	private void disableUiElements() {
		mButtonAddChannel.setEnabled(false);
		mButtonRemoveChannel.setEnabled(false);
		mSpinnerSignal.setEnabled(false);
		mTextViewFs.setEnabled(false);
		mEditTextFs.setEnabled(false);
		mTextViewTe.setEnabled(false);
		mEditTextTe.setEnabled(false);
		mTextViewResolution.setEnabled(false);
		mEditTextResolution.setEnabled(false);
	}
	
	private void enableUiElements() {
		mButtonAddChannel.setEnabled(true);
		mButtonRemoveChannel.setEnabled(true);
		mSpinnerSignal.setEnabled(true);
		mTextViewFs.setEnabled(true);
		mEditTextFs.setEnabled(true);
		mTextViewTe.setEnabled(true);
		mEditTextTe.setEnabled(true);
		mTextViewResolution.setEnabled(true);
		mEditTextResolution.setEnabled(true);
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
		if(mBluetoothService == null) {
			setupUI();
			addConnection();
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
					mBluetoothService.soyCliente(mDispositivoRemoto);
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
