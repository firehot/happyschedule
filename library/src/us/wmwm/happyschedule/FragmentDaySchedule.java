package us.wmwm.happyschedule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

import us.wmwm.happyschedule.views.ScheduleView;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

public class FragmentDaySchedule extends Fragment {

	ExpandableListView list;

	Future<?> loadScheduleFuture;

	Station from;
	Station to;
	Date day;
	Handler handler = new Handler();

	BaseExpandableListAdapter adapter;

	public interface OnDateChange {
		void onDateChange(Calendar cal);
	}

	OnDateChange onDateChange;

	public void setOnDateChange(OnDateChange onDateChange) {
		this.onDateChange = onDateChange;
	}

	private static final String TAG = FragmentDaySchedule.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_schedule_day, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_go_to_next_train) {
			moveToNextTrain();
		}
		return super.onOptionsItemSelected(item);
	}

	private void moveToNextTrain() {
		Calendar now = Calendar.getInstance();
		int usablePosition = 0;
		for (int i = 0; i < adapter.getGroupCount(); i++) {
			StationToStation s = (StationToStation) adapter.getGroup(i);
			if (s.departTime.after(now)) {
				usablePosition = i;
				break;
			}
		}
		if (usablePosition > 1) {
			usablePosition--;
		}
		list.setSelectionFromTop(usablePosition, 0);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (DateUtils.isToday(day.getTime())) {
			menu.removeItem(R.id.menu_go_to_today);
		} else {
			menu.removeItem(R.id.menu_go_to_next_train);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_day_schedule, container,
				false);
		list = (ExpandableListView) view.findViewById(R.id.list2);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Bundle b = getArguments();
		from = (Station) b.getSerializable("from");
		to = (Station) b.getSerializable("to");
		day = (Date) b.getSerializable("date");
		adapter = new BaseExpandableListAdapter() {

			@Override
			public int getGroupCount() {
				return getCount();
			}

			// @Override
			public View getView(int position, View convertView, ViewGroup parent) {
				System.out.println("get view");
				ScheduleView view = (ScheduleView) convertView;
				if (view == null) {
					view = new ScheduleView(parent.getContext());
				}
				view.setData(getItem(position));
				return view;
			}

			@Override
			public View getGroupView(int groupPosition, boolean isExpanded,
					View convertView, ViewGroup parent) {
				return getView(groupPosition, convertView, parent);
			}

			@Override
			public Object getChild(int groupPosition, int childPosition) {
				// TODO Auto-generated method stub
				return null;
			}

			int TYPE_FARE = 0, TYPE_CONTROLS = 2, TYPE_TRIPS = 1;

			@Override
			public int getChildType(int groupPosition, int childPosition) {
				return childPosition;
			}

			@Override
			public int getChildTypeCount() {
				return 3;
			}

			@Override
			public int getChildrenCount(int groupPosition) {
				return 3;
			}

			@Override
			public View getChildView(int groupPosition, int childPosition,
					boolean isLastChild, View convertView, ViewGroup parent) {
				TextView tv = new TextView(parent.getContext());
				if (childPosition == TYPE_CONTROLS) {
					return new ScheduleControlsView(parent.getContext());
				} else if (childPosition == TYPE_FARE) {
					tv.setText("Fare");
				} else if (childPosition == TYPE_TRIPS) {
					tv.setText("Trips");
				}
				return tv;
			}

			@Override
			public StationToStation getGroup(int groupPosition) {
				return getItem(groupPosition);
			}

			// @Override
			// public long getItemId(int position) {
			// // TODO Auto-generated method stub
			// return 0;
			// }

			@Override
			public long getGroupId(int groupPosition) {
				// TODO Auto-generated method stub
				return 0;
			}

			// @Override
			public StationToStation getItem(int position) {
				return o.get(position);
			}

			// @Override
			public int getCount() {
				if (o == null) {
					return 0;
				}
				System.out.println(o.size());
				return o.size();
			}

			@Override
			public boolean areAllItemsEnabled() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public long getCombinedChildId(long groupId, long childId) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public long getCombinedGroupId(long groupId) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public boolean hasStableIds() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean isChildSelectable(int groupPosition,
					int childPosition) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public long getChildId(int groupPosition, int childPosition) {
				// TODO Auto-generated method stub
				return 0;
			}
		};
		list.setAdapter(adapter);
		loadSchedule();
	}

	List<StationToStation> o = new ArrayList<StationToStation>();

	private void loadSchedule() {
		if (loadScheduleFuture != null) {
			loadScheduleFuture.cancel(true);
		}
		Runnable load = new Runnable() {
			@Override
			public void run() {
				Calendar date = Calendar.getInstance();
				date.setTime(day);
				Calendar tomorrow = Calendar.getInstance();
				tomorrow.setTime(day);
				tomorrow.add(Calendar.DAY_OF_YEAR, 1);
				Schedule schedule = null;
				try {
					schedule = ScheduleDao.get().getSchedule(from.id, to.id,
							date.getTime(), tomorrow.getTime());
					o.clear();
					final Calendar limit = Calendar.getInstance();
					limit.setTime(schedule.end);
					Calendar start = Calendar.getInstance();
					start.setTime(schedule.start);
					final boolean isToday = DateUtils.isToday(start
							.getTimeInMillis());
					if (isToday) {
						limit.set(Calendar.HOUR_OF_DAY, 9);
						limit.set(Calendar.MINUTE, 0);
					} else {
						limit.add(Calendar.DAY_OF_YEAR, 0);
						limit.set(Calendar.HOUR_OF_DAY, 0);
						limit.set(Calendar.MINUTE, 0);
						limit.set(Calendar.SECOND, 0);
						limit.set(Calendar.MILLISECOND, 0);
					}

					final Calendar priorLimit = Calendar.getInstance();
					priorLimit.setTime(day);
					// priorLimit.add(Calendar.HOUR_OF_DAY, -2);

					schedule.inOrderTraversal(new ScheduleTraverser() {

						@Override
						public void populateItem(int index,
								StationToStation stationToStation, int total) {
							if (!isToday) {
								if (!stationToStation.departTime.before(limit)) {
									// System.out.println(stationToStation.departTime.getTime());
									o.add(stationToStation);
								}
							} else {
								if (!stationToStation.departTime
										.before(priorLimit)
										&& !stationToStation.departTime
												.after(limit))
									o.add(stationToStation);

							}

						}
					});
					getActivity().runOnUiThread(populateAdpter);
					Log.i(TAG, "SUCCESSFUL SCHEDULE");
				} catch (Exception e) {
					Log.e(TAG, "UNSUCCESSFUL SCHEDULE", e);
				}

			}
		};
		loadScheduleFuture = ThreadHelper.getScheduler().submit(load);
	}

	Runnable populateAdpter = new Runnable() {
		@Override
		public void run() {
			Activity activity = getActivity();
			if (activity == null) {
				return;
			}
			adapter.notifyDataSetChanged();
			if (DateUtils.isToday(day.getTime())) {
				moveToNextTrain();
			}
		}
	};

	@Override
	public void onDestroy() {
		handler.removeCallbacks(populateAdpter);
		if (loadScheduleFuture != null) {
			loadScheduleFuture.cancel(true);
		}
		super.onDestroy();

	}

	public static FragmentDaySchedule newInstance(Station from, Station to,
			Date date) {
		FragmentDaySchedule f = new FragmentDaySchedule();
		Bundle b = new Bundle();
		b.putSerializable("from", from);
		b.putSerializable("to", to);
		b.putSerializable("date", date);
		f.setArguments(b);
		return f;
	}

}