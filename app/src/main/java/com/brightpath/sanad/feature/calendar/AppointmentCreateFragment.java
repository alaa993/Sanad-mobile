
package com.brightpath.sanad.feature.calendar;
import android.os.Bundle; import android.view.*; import android.widget.*;
import androidx.annotation.*; import androidx.fragment.app.Fragment; import androidx.lifecycle.ViewModelProvider;
import com.brightpath.sanad.R;
public class AppointmentCreateFragment extends Fragment {
  private CalendarViewModels.AppointmentsVM vm; private CalendarViewModels.SuggestedSlotsVM svm;
  @Nullable @Override public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s){ return i.inflate(R.layout.fragment_appointment_create, c, false); }
  @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s){
    super.onViewCreated(v,s);
    EditText etSpec = v.findViewById(R.id.etSpecialistId);
    EditText etDate = v.findViewById(R.id.etDate);
    Button btnSuggest = v.findViewById(R.id.btnSuggest);
    Button btnCreate = v.findViewById(R.id.btnCreate);
    EditText etStart = v.findViewById(R.id.etStart);
    EditText etEnd   = v.findViewById(R.id.etEnd);

    svm = new ViewModelProvider(this).get(CalendarViewModels.SuggestedSlotsVM.class);
    vm  = new ViewModelProvider(this).get(CalendarViewModels.AppointmentsVM.class);

    btnSuggest.setOnClickListener(x -> {
      int sid = Integer.parseInt(etSpec.getText().toString().trim());
      String date = etDate.getText().toString().trim();
      svm.load(sid, date);
      Toast.makeText(requireContext(), "تم جلب الفتحات المقترحة", Toast.LENGTH_SHORT).show();
    });

    btnCreate.setOnClickListener(x -> {
      int sid = Integer.parseInt(etSpec.getText().toString().trim());
      vm.create(sid, etStart.getText().toString().trim(), etEnd.getText().toString().trim(), null);
      Toast.makeText(requireContext(), "تم إرسال طلب الحجز", Toast.LENGTH_SHORT).show();
    });
  }
}
