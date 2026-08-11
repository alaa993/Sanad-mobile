
package com.brightpath.sanad.feature.reports;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.brightpath.sanad.R;

public class ReportsFragment extends Fragment {
  private ReportsRepository repo;
  private TextView tvUsers, tvSessions, tvPaid, tvRevenue, tvRating, tvTopSpecialists, tvTopOrganizations, tvConversion, tvRetention;
  private EditText etFrom, etTo;

  @Nullable @Override public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
    return i.inflate(R.layout.fragment_reports, c, false);
  }

  @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
    super.onViewCreated(v, s);
    tvUsers = v.findViewById(R.id.tvUsers);
    tvSessions = v.findViewById(R.id.tvSessions);
    tvPaid = v.findViewById(R.id.tvPaid);
    tvRevenue = v.findViewById(R.id.tvRevenue);
    tvRating = v.findViewById(R.id.tvRating);
    tvTopSpecialists = v.findViewById(R.id.tvTopSpecialists);
    tvTopOrganizations = v.findViewById(R.id.tvTopOrganizations);
    tvConversion = v.findViewById(R.id.tvConversion);
    tvRetention = v.findViewById(R.id.tvRetention);
    etFrom = v.findViewById(R.id.etFrom);
    etTo = v.findViewById(R.id.etTo);
    repo = new ReportsRepository(requireContext());
    v.findViewById(R.id.btnRefresh).setOnClickListener(x -> load());
    MaterialButton btnExport = v.findViewById(R.id.btnExportCsv);
    if (btnExport != null) {
      btnExport.setOnClickListener(x -> exportCsv());
    }
    load();
  }

  private String fromValue() {
    return etFrom.getText() == null ? "" : etFrom.getText().toString();
  }

  private String toValue() {
    return etTo.getText() == null ? "" : etTo.getText().toString();
  }

  private void load() {
    String from = fromValue();
    String to = toValue();
    repo.overview(from, to, new ReportsRepository.Cb<ReportsApi.OverviewResponse>() {
      public void ok(ReportsApi.OverviewResponse d) {
        if (!isAdded()) return;
        tvUsers.setText("0");
        tvSessions.setText("0");
        tvPaid.setText("0");
        tvRevenue.setText("0");
        tvRating.setText("—");
        if (d.cards != null) {
          for (ReportsApi.OverviewResponse.Card c : d.cards) {
            if (c == null || c.key == null) continue;
            switch (c.key) {
              case "new_users": tvUsers.setText(String.valueOf(c.value)); break;
              case "sessions_total": tvSessions.setText(String.valueOf(c.value)); break;
              case "sessions_paid": tvPaid.setText(String.valueOf(c.value)); break;
              case "revenue": tvRevenue.setText(String.valueOf(c.value)); break;
              case "avg_rating": tvRating.setText(c.value == null ? "—" : String.valueOf(c.value)); break;
            }
          }
        }
      }
      public void err(Throwable e) {}
    });
    repo.topSpec(from, to, topCb(tvTopSpecialists));
    repo.topOrg(from, to, topCb(tvTopOrganizations));
    repo.conversion(from, to, new ReportsRepository.Cb<ReportsApi.FunnelResponse>() {
      public void ok(ReportsApi.FunnelResponse d) {
        if (!isAdded() || tvConversion == null) return;
        if (d.data == null || d.data.isEmpty()) {
          tvConversion.setText("—");
          return;
        }
        StringBuilder sb = new StringBuilder();
        for (ReportsApi.FunnelResponse.Stage row : d.data) {
          if (row == null) continue;
          if (sb.length() > 0) sb.append("\n");
          sb.append(stageLabel(row.stage)).append(": ").append(row.value);
        }
        tvConversion.setText(sb.toString());
      }
      public void err(Throwable e) {
        if (isAdded() && tvConversion != null) tvConversion.setText("—");
      }
    });
    repo.retention(from, to, new ReportsRepository.Cb<ReportsApi.CohortsResponse>() {
      public void ok(ReportsApi.CohortsResponse d) {
        if (!isAdded() || tvRetention == null) return;
        if (d.data == null || d.data.isEmpty()) {
          tvRetention.setText("—");
          return;
        }
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(5, d.data.size());
        for (int i = 0; i < limit; i++) {
          ReportsApi.CohortsResponse.Row row = d.data.get(i);
          if (row == null) continue;
          if (sb.length() > 0) sb.append("\n");
          sb.append(row.week != null ? row.week : "—")
              .append(" — ")
              .append(row.retained)
              .append("/")
              .append(row.users);
        }
        tvRetention.setText(sb.toString());
      }
      public void err(Throwable e) {
        if (isAdded() && tvRetention != null) tvRetention.setText("—");
      }
    });
  }

  private ReportsRepository.Cb<ReportsApi.TopResponse> topCb(final TextView target) {
    return new ReportsRepository.Cb<ReportsApi.TopResponse>() {
      public void ok(ReportsApi.TopResponse d) {
        if (!isAdded() || target == null) return;
        if (d.data == null || d.data.isEmpty()) {
          target.setText("—");
          return;
        }
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(5, d.data.size());
        for (int i = 0; i < limit; i++) {
          ReportsApi.TopResponse.Top row = d.data.get(i);
          if (row == null) continue;
          if (sb.length() > 0) sb.append("\n");
          sb.append(row.name != null ? row.name : "—").append(" (").append((int) row.sessions).append(")");
        }
        target.setText(sb.toString());
      }
      public void err(Throwable e) {
        if (isAdded() && target != null) target.setText("—");
      }
    };
  }

  private String stageLabel(String stage) {
    if (stage == null) return "—";
    switch (stage) {
      case "signup": return getString(R.string.reports_stage_signup);
      case "first_session": return getString(R.string.reports_stage_first_session);
      case "paid": return getString(R.string.reports_stage_paid);
      default: return stage;
    }
  }

  private void exportCsv() {
    repo.exportCsv(fromValue(), toValue(), new ReportsRepository.Cb<String>() {
      public void ok(String csv) {
        if (!isAdded()) return;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, csv);
        startActivity(Intent.createChooser(share, getString(R.string.reports_export_csv)));
        Toast.makeText(requireContext(), R.string.reports_export_ready, Toast.LENGTH_SHORT).show();
      }
      public void err(Throwable e) {
        if (isAdded()) Toast.makeText(requireContext(), R.string.reports_export_failed, Toast.LENGTH_SHORT).show();
      }
    });
  }
}
