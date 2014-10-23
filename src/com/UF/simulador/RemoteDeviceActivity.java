package com.UF.simulador;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class RemoteDeviceActivity extends Activity {
	
	// Atributos de clase
	private static final int REQUEST_ACTIVAR_BT = 1;
	private BluetoothAdapter mBluetoothAdapter;
	private ArrayAdapter<String> mArrayAdapterDispositivosNuevos;
	private ArrayAdapter<String> mArrayAdapterDispositivosViejos;
	
	// this.Activity onCreate()
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// Inflo
		setContentView(R.layout.activity_remote_device);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// Inicializo boton para buscar dispositivos
		Button mButtonBuscarDispositivos = (Button) findViewById(R.id.BotonBuscarDispositivos);
		mButtonBuscarDispositivos.setOnClickListener(new OnClickListener() {
			public void onClick(View v){
				BuscarDispositivos(v);
			}
		});	
		
		// Inicializo ListView para los nuevos dispositivos
		ListView mListViewDispositivosNuevos = (ListView) findViewById(R.id.ListViewDispositivosNuevos);
		mListViewDispositivosNuevos.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
				// Cancelamos el escaneo porque es costoso y nos vamos a conectar
				mBluetoothAdapter.cancelDiscovery();
				// Obtengo el texto del elemento seleccionado
				String info = ((TextView) v).getText().toString();
				// Obtengo MAC adress (ultimos 17 caracteres del objeto seleccionado)
				String MAC = info.substring(info.length()-17, info.length());
				// Creo el intent resultado con el MAC adress y el nombre del dispositivo
				Intent intent = new Intent();
				intent.putExtra("MAC", MAC);
				// Seteo el resultado y termino con la Activity
				setResult(Activity.RESULT_OK, intent);
				// Termino esta Activity y vuelvo a ChatBluetooth
				finish();
			}
		});
		
		// ListView de dispositivos previamente sincronizados
		ListView mListViewDispositivosViejos = (ListView) findViewById(R.id.ListViewDispositivosViejos);
		mListViewDispositivosViejos.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
				// Cancelamos el escaneo porque es costoso y nos vamos a conectar
				mBluetoothAdapter.cancelDiscovery();
				// Obtengo el texto del elemento seleccionado
				String info = ((TextView) v).getText().toString();
				// Obtengo MAC adress (ultimos 17 caracteres del objeto seleccionado)
				String MAC = info.substring(info.length()-17);
				// Creo el intent resultado con el MAC adress y el nombre del dispositivo
				Intent intent = new Intent();
				intent.putExtra("MAC", MAC);
				// Seteo el resultado y termino con la Activity
				setResult(Activity.RESULT_OK, intent);
				// Termino esta Activity y vuelvo a ChatBluetooth
				finish();
			}
		});
		mListViewDispositivosViejos.setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> arg0, View v, int pos, long id) {
            	// Cancelamos el escaneo porque es costoso y nos vamos a conectar
				mBluetoothAdapter.cancelDiscovery();
				// Obtengo el texto del elemento seleccionado
				String info = ((TextView) v).getText().toString();
				// Obtengo MAC adress (ultimos 17 caracteres del objeto seleccionado)
				String MAC = info.substring(info.length()-17);
                return true;
            }
        }); 
		
		// Inicializo Adapters
		mArrayAdapterDispositivosNuevos = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		mArrayAdapterDispositivosViejos = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
					
		// Asocio los Array Adapters a sus respectivas ListViews 
		mListViewDispositivosNuevos.setAdapter(mArrayAdapterDispositivosNuevos);
		mListViewDispositivosViejos.setAdapter(mArrayAdapterDispositivosViejos);
		
		// Me registro al Broadcast de dispositivo encontrado
		this.registerReceiver(ReceiverBluetooth, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		// Me registro al Broadcast de busqueda finalizada
		this.registerReceiver(ReceiverBluetooth, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
		
		// Inicializo el adaptador BT local
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		// Si esta apagado, fuerzo encender Bluetooth
		if(mBluetoothAdapter.isEnabled() == false) {
			Intent intentActivarBT  = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(intentActivarBT, REQUEST_ACTIVAR_BT);
		}
		
		// Obtengo una lista de los dispositivos ya sincronizados y los agrego a su Adapter
		Set<BluetoothDevice> DispositivosViejos = mBluetoothAdapter.getBondedDevices();
		
		// Si hay dispositivos sincronizados, los agrego al Adapter
		if (DispositivosViejos.size() > 0) {
			for(BluetoothDevice Device : DispositivosViejos ) {
				mArrayAdapterDispositivosViejos.add(Device.getName() + "\n" + Device.getAddress());
			}
		}
	}
	
	// this.Activity onActivityResult()
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ACTIVAR_BT) {
			if (resultCode == RESULT_OK) {
				// Obtengo una lista de los dispositivos ya sincronizados y los agrego a su Adapter
				Set<BluetoothDevice> DispositivosViejos = mBluetoothAdapter.getBondedDevices();
				// Si hay dispositivos sincronizados, los agrego al Adapter
				if (DispositivosViejos.size() > 0) {
					for(BluetoothDevice Device : DispositivosViejos ) {
						mArrayAdapterDispositivosViejos.add(Device.getName() + "\n" + Device.getAddress());
					}
				} 
				// Informo
				Toast.makeText(this, "Bluetooth activado.", Toast.LENGTH_SHORT).show();;
			}
			if (resultCode == RESULT_CANCELED) {
				// Creo intent
				Intent intent = new Intent();
				// Seteo el resultado negativo
				setResult(Activity.RESULT_CANCELED, intent);
				// Mato Activity
				finish();
			}
		}
	}
	
	// Metodo para buscar Dispositivos Bluetooth
	public void BuscarDispositivos(View v) {
		// Si ya estoy escaneando, cancelo
		if(mBluetoothAdapter.isDiscovering() == true) {
			mBluetoothAdapter.cancelDiscovery();
		}
		// Inicio el escaneo
		mBluetoothAdapter.startDiscovery();
		// Limpio ArrayAdapter para agregar los nuevos devices
		mArrayAdapterDispositivosNuevos.clear();
		// Registro mi Broadcast Receiver
		registerReceiver(ReceiverBluetooth, new IntentFilter(BluetoothDevice.ACTION_FOUND));
	}
	
	// Registro el Broadcast Receiver para BT y defino su metodo onReceive
	private final BroadcastReceiver ReceiverBluetooth = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			mArrayAdapterDispositivosNuevos.notifyDataSetChanged();
			String action = intent.getAction();
			// Cuando se encuentra un dispositivo
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Obtengo el objeto BluetoothDevice del Intent
				BluetoothDevice Dispositivo = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// Si no estaba previamente sincronizado, lo agrego
				if (Dispositivo.getBondState() != BluetoothDevice.BOND_BONDED) {
					mArrayAdapterDispositivosNuevos.add(Dispositivo.getName() + "\n" + Dispositivo.getAddress());
				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				if(mArrayAdapterDispositivosNuevos.getCount() == 0) {
					String NoDevices = "No se encontraron dispositivos.";
					mArrayAdapterDispositivosNuevos.add(NoDevices);
				}
			}	
		}
	};
	
	// this.Activity onDestroy()
	@Override 
	protected void onDestroy() {
		super.onDestroy();
		// Me aseguro de dejar de escanear
		if(mBluetoothAdapter != null) {
			mBluetoothAdapter.cancelDiscovery();
		}
		// Desregistro el Broadcast Receiver
		unregisterReceiver(ReceiverBluetooth);
	}
	
	// Sobrecargo el back navigation button
	public void onBackPressed() {
			// Mato la Activity
			finish();
		}

}
