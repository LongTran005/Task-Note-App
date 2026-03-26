package hcmute.edu.vn.ticktickandroid.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import hcmute.edu.vn.ticktickandroid.R;

public class CalendarMonthFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar_month, container, false);
        RecyclerView rvMonths = view.findViewById(R.id.rv_months);
        // TODO: Setup adapter and data for year grid
        return view;
    }
}
