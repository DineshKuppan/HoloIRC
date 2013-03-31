/*
    LightIRC - an IRC client for Android

    Copyright 2013 Lalit Maganti

    This file is part of LightIRC.

    LightIRC is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    LightIRC is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with LightIRC. If not, see <http://www.gnu.org/licenses/>.
 */

package com.fusionx.lightirc.activity;

import com.fusionx.lightirc.R;
import com.fusionx.lightirc.activity.ServerSettingsActivity.BaseServerSettingFragment;
import com.fusionx.lightirc.adapters.LightPircBotXArrayAdapter;
import com.fusionx.lightirc.misc.IRCBinder;
import com.fusionx.lightirc.misc.LightPircBotX;
import com.fusionx.lightirc.services.IRCService;

import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceActivity;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

public class MainServerListActivity extends ListActivity {
	LightPircBotX[] serverList;
	
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_server);

		getListView().setLongClickable(true);
		registerForContextMenu(getListView());

		setListAdapter(new LightPircBotXArrayAdapter(this, getSetServerList()));
	}

	private LightPircBotX[] getSetServerList() {
		SharedPreferences settings = getSharedPreferences("main", 0);
		boolean firstRun = settings.getBoolean("firstrun", true);
		int noOfServers = settings.getInt("noOfServers", 0);
		LightPircBotX[] values;
		Editor e = settings.edit();

		if (firstRun || noOfServers == 0) {
			LightPircBotX freenode = new LightPircBotX();
			freenode.mURL = "irc.freenode.net";
			freenode.mNick = "LightIRCUser";
			freenode.setTitle("Freenode");
			freenode.mAutoJoinChannels = new String[] { "#testingircandroid" };
			values = new LightPircBotX[] { freenode };
			noOfServers = 1;
			for (String s : freenode.toHashMap().keySet()) {
				e.putString("server_0_" + s, freenode.toHashMap().get(s));
			}
			e.putString("server_0_autoJoin_channel", "#testingircandroid");
			e.putInt("server_0_autoJoin_no", 1);
		} else {
			values = new LightPircBotX[noOfServers];
			for (int i = 0; i < noOfServers; i++) {
				values[i].mURL = settings.getString("server_" + i + "_url", "");
				values[i].mUserName = settings.getString("server_" + i
						+ "_userName", "");
				values[i].mNick = settings.getString("server_" + i + "_nick",
						"");
				values[i].mServerPassword = settings.getString("server_" + i
						+ "_serverPassword", "");
				values[i].setTitle(settings.getString(
						"server_" + i + "_title", ""));

				String[] s = new String[settings.getInt("server_" + i
						+ "_autoJoin_no", 0)];
				for (int j = 0; j < s.length; j++) {
					s[j] = settings.getString("server_" + i
							+ "_autoJoin_channel_" + j, "");
				}
				values[i].mAutoJoinChannels = s;
			}
		}

		e.putBoolean("firstrun", true);
		e.putInt("noOfServers", noOfServers);
		e.commit();
		
		serverList = values;
		
		Intent service = new Intent(this, IRCService.class);
		startService(service);
		bindService(service, mConnection, 0);

		return values;
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			if(((IRCBinder) service).getService().mServerObjects.size() <= 0) {
				for (LightPircBotX s : serverList) {
					((IRCBinder) service).getService().mServerObjects.put(s.getTitle(), s);
				}
			}
			unbindService(mConnection);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
	};

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.context_server_long_press, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.edit:
			editServer(info.position);
			return true;
		case R.id.connect:
			connectToServer(info.position);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	protected void onListItemClick(final ListView l, final View v,
			final int position, final long id) {
		connectToServer(position);
	}

	private void connectToServer(int position) {
		LightPircBotX server = (LightPircBotX) getListView().getItemAtPosition(
				position);

		Intent intent = new Intent(MainServerListActivity.this,
				ServerChannelActivity.class);
		intent.putExtra("serverName", server.getTitle());
		startActivity(intent);
	}

	private void editServer(int position) {
		Intent intent = new Intent(MainServerListActivity.this,
				ServerSettingsActivity.class);
		intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
				BaseServerSettingFragment.class.getName());
		intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.main_server_list, menu);
		return true;
	}
}
