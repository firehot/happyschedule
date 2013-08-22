package us.wmwm.happyschedule.service;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import us.wmwm.happyschedule.Alarms;
import us.wmwm.happyschedule.ThreadHelper;
import us.wmwm.happyschedule.activity.AlarmActivity;
import us.wmwm.happyschedule.model.Alarm;
import us.wmwm.happyschedule.util.Streams;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;

import com.squareup.okhttp.OkHttpClient;

public class HappyScheduleService extends Service {

	NotificationManager notifs;
	AlarmManager alarmManager;
	
	@Override
	public void onCreate() {
		super.onCreate();
		notifs = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		PendingIntent pi = null;
		Intent i = new Intent(this, HappyScheduleService.class);
		i.setData(Uri.parse("http://wmwm.us?type=lines"));
		pi = PendingIntent.getService(this, 0, i, 0);
		alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis()+10000, 43200000,pi);
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent!=null) {
			Uri data = intent.getData();
			if(data!=null) {
				String type = data.getQueryParameter("type");
				if("alarm".equals(type)) {
					String id = data.getQueryParameter("id");
					Alarm alarm = Alarms.getAlarm(this,id);
					String action = data.getQueryParameter("action");
					if("dismiss".equals(action)) {
						notifs.cancel(id.hashCode());
						if(alarm!=null) {
							Intent i = AlarmActivity.from(this, alarm.getStationToStation(), alarm.getTime(), alarm.getType());
							PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
							alarmManager.cancel(pi);
							Alarms.removeAlarm(this, alarm);
						}
						System.out.println("notif cancel: " + id);
						
					}
				}
				if("lines".equals(type)) {
					ThreadHelper.getScheduler().submit(new Runnable() {
						@Override
						public void run() {
							OkHttpClient client = new OkHttpClient();
							HttpURLConnection conn = null;
							InputStream in = null;
							FileOutputStream fos = null;
							try {
								conn = client.open(new URL("http://ryangravener.com/njrails/config.json"));
								String txt = Streams.readFully(in = conn.getInputStream());
								if(txt!=null) {
									fos = openFileOutput("config.json", Context.MODE_PRIVATE);
									fos.write(txt.getBytes());									
								}
							} catch (Exception e) {
								
							} finally {
								conn.disconnect();
								if(in!=null) {
									try {
										in.close();
									} catch (Exception ex) {}
								}
								if(fos!=null) {
									try {
										fos.close();
									} catch (Exception e) {
										
									}
								}
							}
						}
					});
				}
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

}
