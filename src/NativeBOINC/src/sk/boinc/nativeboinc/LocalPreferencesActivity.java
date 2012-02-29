/* 
 * NativeBOINC - Native BOINC Client with Manager
 * Copyright (C) 2011, Mateusz Szpakowski
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package sk.boinc.nativeboinc;

import sk.boinc.nativeboinc.clientconnection.ClientError;
import sk.boinc.nativeboinc.clientconnection.ClientPreferencesReceiver;
import sk.boinc.nativeboinc.clientconnection.VersionInfo;
import sk.boinc.nativeboinc.debug.Logging;
import sk.boinc.nativeboinc.nativeclient.NativeBoincUtils;
import sk.boinc.nativeboinc.util.ClientId;
import sk.boinc.nativeboinc.util.ProgressState;
import sk.boinc.nativeboinc.util.StandardDialogs;
import edu.berkeley.boinc.lite.GlobalPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TabHost;

/**
 * @author mat
 *
 */
public class LocalPreferencesActivity extends ServiceBoincActivity implements ClientPreferencesReceiver {
	private static final String TAG = "LocalPrefsActivity";
	
	private ClientId mConnectedClient = null;
	
	private int mGlobalPrefsFetchProgress = ProgressState.NOT_RUN;
	private boolean mGlobalPrefsSavingInProgress = false;
	
	private CheckBox mComputeOnBatteries;
	private CheckBox mComputeInUse;
	private CheckBox mUseGPUInUse;
	private EditText mComputeIdleFor;
	private EditText mComputeUsageLessThan;
	private EditText mBatteryLevelNL;
	private EditText mSwitchBetween;
	private EditText mUseAtMostCPUs;
	private EditText mUseAtMostCPUTime;
	private EditText mMaxDownloadRate;
	private EditText mMaxUploadRate;
	private EditText mTransferAtMost;
	private EditText mTransferPeriodDays;
	private EditText mConnectAboutEvery;
	private EditText mAdditionalWorkBuffer;
	private CheckBox mSkipVerifyImages;
	private EditText mUseAtMostDiskSpace;
	private EditText mLeaveAtLeastDiskFree;
	private EditText mUseAtMostTotalDisk;
	private EditText mCheckpointToDisk;
	private EditText mUseAtMostMemoryInUse;
	private EditText mUseAtMostMemoryInIdle;
	private CheckBox mLeaveApplications;
	
	private Button mApply;
	
	private static class SavedState {
		private final int globalPrefsFetchProgress;
		private final boolean globalPrefsSavingInProgress;
		
		public SavedState(LocalPreferencesActivity activity) {
			globalPrefsFetchProgress = activity.mGlobalPrefsFetchProgress;
			globalPrefsSavingInProgress = activity.mGlobalPrefsSavingInProgress;
		}
		
		public void restore(LocalPreferencesActivity activity) {
			activity.mGlobalPrefsFetchProgress = globalPrefsFetchProgress;
			activity.mGlobalPrefsSavingInProgress = globalPrefsSavingInProgress;
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		final SavedState savedState = (SavedState)getLastNonConfigurationInstance();
		if (savedState != null)
			savedState.restore(this);
		
		setUpService(true, true, false, false, false, false);
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.local_prefs);
		
		TabHost tabHost = (TabHost)findViewById(android.R.id.tabhost);
		
		tabHost.setup();
		
		Resources res = getResources();
		
		TabHost.TabSpec tabSpec1 = tabHost.newTabSpec("computeOptions");
		tabSpec1.setContent(R.id.localPrefComputeOptions);
		tabSpec1.setIndicator(getString(R.string.localPrefComputeOptions),
				res.getDrawable(R.drawable.ic_tab_compute));
		tabHost.addTab(tabSpec1);
		
		TabHost.TabSpec tabSpec2 = tabHost.newTabSpec("networkUsage");
		tabSpec2.setContent(R.id.localPrefNetworkOptions);
		tabSpec2.setIndicator(getString(R.string.localPrefNetworkUsage),
				res.getDrawable(R.drawable.ic_tab_network));
		tabHost.addTab(tabSpec2);
		
		TabHost.TabSpec tabSpec3 = tabHost.newTabSpec("diskUsage");
		tabSpec3.setContent(R.id.localPrefDiskOptions);
		tabSpec3.setIndicator(getString(R.string.localPrefDiskUsage),
				res.getDrawable(R.drawable.ic_tab_disk));
		tabHost.addTab(tabSpec3);
		
		TabHost.TabSpec tabSpec4 = tabHost.newTabSpec("diskUsage");
		tabSpec4.setContent(R.id.localPrefMemoryOptions);
		tabSpec4.setIndicator(getString(R.string.localPrefMemoryUsage),
				res.getDrawable(R.drawable.ic_tab_memory));
		tabHost.addTab(tabSpec4);
		
		mComputeOnBatteries = (CheckBox)findViewById(R.id.localPrefComputeOnBatteries);
		mComputeInUse = (CheckBox)findViewById(R.id.localPrefComputeInUse);
		mUseGPUInUse = (CheckBox)findViewById(R.id.localPrefUseGPUInUse);
		mComputeIdleFor = (EditText)findViewById(R.id.localPrefComputeIdleFor);
		mComputeUsageLessThan = (EditText)findViewById(R.id.localPrefComputeUsageLessThan);
		mBatteryLevelNL = (EditText)findViewById(R.id.localPrefBatteryNL);
		mSwitchBetween = (EditText)findViewById(R.id.localPrefSwitchBetween);
		mUseAtMostCPUs = (EditText)findViewById(R.id.localPrefUseAtMostCPUs);
		mUseAtMostCPUTime = (EditText)findViewById(R.id.localPrefUseAtMostCPUTime);
		mMaxDownloadRate = (EditText)findViewById(R.id.localPrefMaxDownloadRate);
		mMaxUploadRate = (EditText)findViewById(R.id.localPrefMaxUploadRate);
		mTransferAtMost = (EditText)findViewById(R.id.localPrefTransferAtMost);
		mTransferPeriodDays = (EditText)findViewById(R.id.localPrefTransferPeriodDays);
		mConnectAboutEvery = (EditText)findViewById(R.id.localPrefConnectAboutEvery);
		mAdditionalWorkBuffer = (EditText)findViewById(R.id.localPrefAdditWorkBuffer);
		mSkipVerifyImages = (CheckBox)findViewById(R.id.localPrefSkipImageVerify);
		mUseAtMostDiskSpace = (EditText)findViewById(R.id.localPrefUseAtMostDiskSpace);
		mLeaveAtLeastDiskFree = (EditText)findViewById(R.id.localPrefLeaveAtLeastDiskFree);
		mUseAtMostTotalDisk = (EditText)findViewById(R.id.localPrefUseAtMostTotalDisk);
		mCheckpointToDisk = (EditText)findViewById(R.id.localPrefCheckpointToDisk);
		mUseAtMostMemoryInUse = (EditText)findViewById(R.id.localPrefUseAtMostMemoryInUse);
		mUseAtMostMemoryInIdle = (EditText)findViewById(R.id.localPrefUseAtMostMemoryInIdle);
		mLeaveApplications = (CheckBox)findViewById(R.id.localPrefLeaveApplications);
		
		mApply = (Button)findViewById(R.id.localPrefApply);
		mApply.setEnabled(false);
		mApply.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				doApplyPreferences();
			}
		});
		
		Button applyDefault = (Button)findViewById(R.id.localPrefDefault);
		applyDefault.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mConnectionManager.setGlobalPrefsOverride(NativeBoincUtils.INITIAL_BOINC_CONFIG);
			}
		});
		
		Button cancel = (Button)findViewById(R.id.localPrefCancel);
		cancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		TextWatcher textWatcher = new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				setApplyButtonState();
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
			}
		};
		
		// hide keyboard
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		mComputeIdleFor.addTextChangedListener(textWatcher);
		mComputeUsageLessThan.addTextChangedListener(textWatcher);
		mBatteryLevelNL.addTextChangedListener(textWatcher);
		
		mSwitchBetween.addTextChangedListener(textWatcher);
		mUseAtMostCPUs.addTextChangedListener(textWatcher);
		mUseAtMostCPUTime.addTextChangedListener(textWatcher);
		
		mMaxDownloadRate.addTextChangedListener(textWatcher);
		mMaxUploadRate.addTextChangedListener(textWatcher);
		mTransferAtMost.addTextChangedListener(textWatcher);
		mTransferPeriodDays.addTextChangedListener(textWatcher);
		mConnectAboutEvery.addTextChangedListener(textWatcher);
		mAdditionalWorkBuffer.addTextChangedListener(textWatcher);
		
		mUseAtMostDiskSpace.addTextChangedListener(textWatcher);
		mUseAtMostTotalDisk.addTextChangedListener(textWatcher);
		mLeaveAtLeastDiskFree.addTextChangedListener(textWatcher);
		mCheckpointToDisk.addTextChangedListener(textWatcher);
		
		mUseAtMostMemoryInIdle.addTextChangedListener(textWatcher);
		mUseAtMostMemoryInUse.addTextChangedListener(textWatcher);
	}
	
	private void updateActivityState() {
		// 
		setProgressBarIndeterminateVisibility(mConnectionManager.isWorking());
		
		if (mGlobalPrefsFetchProgress == ProgressState.IN_PROGRESS || mGlobalPrefsSavingInProgress) {
			ClientError error = mConnectionManager.getPendingClientError();
			
			if (error != null) {
				clientError(error.errorNum, error.message);
				return;
			} else if (mConnectedClient == null) {
				clientDisconnected(); // if disconnected
				return;
			}
		}
		
		if (mGlobalPrefsFetchProgress != ProgressState.FINISHED) {
			if (mGlobalPrefsFetchProgress == ProgressState.NOT_RUN) {
				if (Logging.DEBUG) Log.d(TAG, "get project config from client");
				mConnectionManager.getGlobalPrefsWorking();
				mGlobalPrefsFetchProgress = ProgressState.IN_PROGRESS;
			} else if (mGlobalPrefsFetchProgress == ProgressState.IN_PROGRESS) {
				GlobalPreferences globalPrefs = mConnectionManager.getPendingGlobalPrefsWorking();

				if (globalPrefs != null) {	// if finished
					mGlobalPrefsFetchProgress = ProgressState.FINISHED;
					updatePreferences(globalPrefs);
				}
			}
			// if failed
		}
		
		if (mGlobalPrefsSavingInProgress) {
			if (!mConnectionManager.isGlobalPrefsBeingOverriden()) {
				// its finally overriden
				onGlobalPreferencesChanged();
			}
		}
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new SavedState(this);
	}
	
	@Override
	protected void onConnectionManagerConnected() {
		mConnectedClient = mConnectionManager.getClientId();
		
		updateActivityState();
	}
	
	@Override
	protected void onConnectionManagerDisconnected() {
		mConnectedClient = null;
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (mConnectionManager != null) {
			mConnectedClient = mConnectionManager.getClientId();
			
			updateActivityState();
		}
	}
	
	
	private void setApplyButtonState() {
		try {
			
			double idle_time_to_run = Double.parseDouble(mComputeIdleFor.getText().toString());
			double suspend_cpu_usage = Double.parseDouble(mComputeUsageLessThan.getText().toString());
			
			double cpu_scheduling_period_minutes = Double.parseDouble(
					mSwitchBetween.getText().toString());
			double max_ncpus_pct = Double.parseDouble(mUseAtMostCPUs.getText().toString());
			double cpu_usage_limit = Double.parseDouble(mUseAtMostCPUTime.getText().toString());
			double battery_level_nl = Double.parseDouble(mBatteryLevelNL.getText().toString());
			
			double max_bytes_sec_down = Double.parseDouble(mMaxDownloadRate.getText().toString());
			double max_bytes_sec_up = Double.parseDouble(mMaxUploadRate.getText().toString());
			double daily_xfer_limit_mb = Double.parseDouble(mTransferAtMost.getText().toString());
			double daily_xfer_period_days = Integer.parseInt(mTransferPeriodDays.getText().toString());
			double work_buf_min_days = Double.parseDouble(mConnectAboutEvery.getText().toString());
			double work_buf_addit_days = Double.parseDouble(mAdditionalWorkBuffer.getText().toString());
			
			double disk_max_used_gb = Double.parseDouble(mUseAtMostDiskSpace.getText().toString());
			double disk_max_used_pct = Double.parseDouble(mUseAtMostTotalDisk.getText().toString());
			double disk_min_free_gb = Double.parseDouble(mLeaveAtLeastDiskFree.getText().toString());
			double disk_interval = Double.parseDouble(mCheckpointToDisk.getText().toString());
			
			double ram_max_used_busy_frac = Double.parseDouble(
					mUseAtMostMemoryInUse.getText().toString());
			double ram_max_used_idle_frac = Double.parseDouble(
					mUseAtMostMemoryInIdle.getText().toString());
			
			if (idle_time_to_run < 0.0 || suspend_cpu_usage > 100.0 || suspend_cpu_usage < 0.0 ||
					battery_level_nl < 0.0 || battery_level_nl > 100.0 ||
					cpu_scheduling_period_minutes < 0.0 || max_ncpus_pct > 100.0 || max_ncpus_pct < 0.0 ||
					cpu_usage_limit > 100.0 || cpu_usage_limit < 0.0 ||
					max_bytes_sec_down < 0.0 || max_bytes_sec_up < 0.0 || daily_xfer_limit_mb < 0.0 ||
					daily_xfer_period_days < 0 || work_buf_min_days < 0.0 || work_buf_addit_days < 0.0 || 
					disk_max_used_gb < 0.0 || disk_max_used_pct > 100.0 || disk_max_used_pct < 0.0 ||
					disk_min_free_gb < 0.0 || disk_interval < 0.0 ||
					ram_max_used_busy_frac > 100.0 || ram_max_used_busy_frac < 0.0 ||
					ram_max_used_idle_frac > 100.0 || ram_max_used_idle_frac < 0.0) {
				/* if invalids values */
				if (Logging.DEBUG) Log.d(TAG, "Set up 'apply' as disabled");
				mApply.setEnabled(false);
			} else {
				if (Logging.DEBUG) Log.d(TAG, "Set up 'apply' as enabled");
				mApply.setEnabled(true);
			}
		} catch(NumberFormatException ex) {
			if (Logging.DEBUG) Log.d(TAG, "Cant parse numbers");
			mApply.setEnabled(false);
			return;
		}
	}
	
	private void updatePreferences(GlobalPreferences globalPrefs) {
		mComputeInUse.setChecked(globalPrefs.run_if_user_active);
		mComputeOnBatteries.setChecked(globalPrefs.run_on_batteries);
		mUseGPUInUse.setChecked(globalPrefs.run_gpu_if_user_active);
		
		mComputeIdleFor.setText(Double.toString(globalPrefs.idle_time_to_run));
		mComputeUsageLessThan.setText(Double.toString(globalPrefs.suspend_cpu_usage));
		mBatteryLevelNL.setText(Double.toString(globalPrefs.run_if_battery_nl_than));
		
		mSwitchBetween.setText(Double.toString(globalPrefs.cpu_scheduling_period_minutes));
		mUseAtMostCPUs.setText(Double.toString(globalPrefs.max_ncpus_pct));
		mUseAtMostCPUTime.setText(Double.toString(globalPrefs.cpu_usage_limit));
		
		mMaxDownloadRate.setText(Double.toString(globalPrefs.max_bytes_sec_down));
		mMaxUploadRate.setText(Double.toString(globalPrefs.max_bytes_sec_up));
		mTransferAtMost.setText(Double.toString(globalPrefs.daily_xfer_limit_mb));
		mTransferPeriodDays.setText(Integer.toString(globalPrefs.daily_xfer_period_days));
		mConnectAboutEvery.setText(Double.toString(globalPrefs.work_buf_min_days));
		mAdditionalWorkBuffer.setText(Double.toString(globalPrefs.work_buf_additional_days));
		mSkipVerifyImages.setChecked(globalPrefs.dont_verify_images);
		
		mUseAtMostDiskSpace.setText(Double.toString(globalPrefs.disk_max_used_gb));
		mUseAtMostTotalDisk.setText(Double.toString(globalPrefs.disk_max_used_pct));
		mLeaveAtLeastDiskFree.setText(Double.toString(globalPrefs.disk_min_free_gb));
		mCheckpointToDisk.setText(Double.toString(globalPrefs.disk_interval));
		
		mUseAtMostMemoryInIdle.setText(Double.toString(globalPrefs.ram_max_used_idle_frac));
		mUseAtMostMemoryInUse.setText(Double.toString(globalPrefs.ram_max_used_busy_frac));
		mLeaveApplications.setChecked(globalPrefs.leave_apps_in_memory);
	}

	private void doApplyPreferences() {
		GlobalPreferences globalPrefs = new GlobalPreferences();
		
		try {
			globalPrefs.run_if_user_active = mComputeInUse.isChecked();
			globalPrefs.run_on_batteries = mComputeOnBatteries.isChecked();
			globalPrefs.run_gpu_if_user_active = mUseGPUInUse.isChecked();
			
			globalPrefs.idle_time_to_run = Double.parseDouble(mComputeIdleFor.getText().toString());
			globalPrefs.suspend_cpu_usage = Double.parseDouble(
					mComputeUsageLessThan.getText().toString());
			globalPrefs.run_if_battery_nl_than = Double.parseDouble(
					mBatteryLevelNL.getText().toString());
			
			globalPrefs.cpu_scheduling_period_minutes = Double.parseDouble(
					mSwitchBetween.getText().toString());
			globalPrefs.max_ncpus_pct = Double.parseDouble(mUseAtMostCPUs.getText().toString());
			globalPrefs.cpu_usage_limit = Double.parseDouble(mUseAtMostCPUTime.getText().toString());
			
			globalPrefs.max_bytes_sec_down = Double.parseDouble(mMaxDownloadRate.getText().toString());
			globalPrefs.max_bytes_sec_up = Double.parseDouble(mMaxUploadRate.getText().toString());
			globalPrefs.daily_xfer_limit_mb = Double.parseDouble(mTransferAtMost.getText().toString());
			globalPrefs.daily_xfer_period_days = Integer.parseInt(
					mTransferPeriodDays.getText().toString());
			globalPrefs.work_buf_min_days = Double.parseDouble(mConnectAboutEvery.getText().toString());
			globalPrefs.work_buf_additional_days = Double.parseDouble(
					mAdditionalWorkBuffer.getText().toString());
			globalPrefs.dont_verify_images = mSkipVerifyImages.isChecked();
			
			globalPrefs.disk_max_used_gb = Double.parseDouble(mUseAtMostDiskSpace.getText().toString());
			globalPrefs.disk_max_used_pct = Double.parseDouble(mUseAtMostTotalDisk.getText().toString());
			globalPrefs.disk_min_free_gb = Double.parseDouble(mLeaveAtLeastDiskFree.getText().toString());
			globalPrefs.disk_interval = Double.parseDouble(mCheckpointToDisk.getText().toString());
			
			globalPrefs.ram_max_used_busy_frac = Double.parseDouble(
					mUseAtMostMemoryInUse.getText().toString());
			globalPrefs.ram_max_used_idle_frac = Double.parseDouble(
					mUseAtMostMemoryInIdle.getText().toString());
			globalPrefs.leave_apps_in_memory = mLeaveApplications.isChecked();
		} catch(NumberFormatException ex) {
			return;	// do nothing
		}
		mConnectionManager.setGlobalPrefsOverrideStruct(globalPrefs);
	}
	
	@Override
	public void clientConnectionProgress(int progress) {
		// do nothing
	}

	@Override
	public void clientConnected(VersionInfo clientVersion) {
		mConnectedClient = mConnectionManager.getClientId();
	}

	@Override
	public void clientDisconnected() {
		mGlobalPrefsFetchProgress = ProgressState.FAILED;
		StandardDialogs.tryShowDisconnectedErrorDialog(this, mConnectionManager, null, mConnectedClient);
	}
	
	@Override
	public void currentGlobalPreferences(GlobalPreferences globalPrefs) {
		mGlobalPrefsFetchProgress = ProgressState.FINISHED;
		
		updatePreferences(globalPrefs);
	}
	
	@Override
	public void onGlobalPreferencesChanged() {
		mGlobalPrefsSavingInProgress = false;
		// finish
		finish();
	}

	@Override
	public boolean clientError(int errorNum, String errorMessage) {
		mGlobalPrefsFetchProgress = ProgressState.FAILED;
		mGlobalPrefsSavingInProgress = false;
		
		StandardDialogs.showClientErrorDialog(this, errorNum, errorMessage);
		return true;
	}

	@Override
	public void onClientIsWorking(boolean isWorking) {
		setProgressBarIndeterminateVisibility(isWorking);
	}
}
