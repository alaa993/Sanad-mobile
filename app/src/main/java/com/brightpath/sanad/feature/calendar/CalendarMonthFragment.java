
package com.brightpath.sanad.feature.calendar;
import android.os.Bundle; import android.view.*; import android.widget.*;
import androidx.annotation.*; import androidx.fragment.app.Fragment;
import com.brightpath.sanad.R;
public class CalendarMonthFragment extends Fragment {
  @Nullable @Override public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s){ return i.inflate(R.layout.fragment_calendar_month, c, false); }
  @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s){
    super.onViewCreated(v,s);
    Button openList = v.findViewById(R.id.btnOpenAppointments);
    openList.setOnClickListener(x -> {
      androidx.navigation.NavController nav = androidx.navigation.fragment.NavHostFragment.findNavController(this);
      nav.navigate(R.id.appointmentsListFragment);
    });
  }
}
