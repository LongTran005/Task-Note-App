package hcmute.edu.vn.ticktickandroid.Fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.ticktickandroid.R;
import hcmute.edu.vn.ticktickandroid.Service.MusicService;

public class MusicPickerFragment extends Fragment {

    public interface OnMusicSelectedListener {
        void onMusicSelected(int resId, String name);
        void onFileSelected(Uri uri, String name);
        void onPauseMusic();
        void onResumeMusic();
        void onStopMusic();
        void onBack();
    }

    private OnMusicSelectedListener listener;
    private RecyclerView rvMusic;
    private View btnBack;
    private View btnPickFile;
    private LinearLayout nowPlayingSection;
    private TextView tvNowPlayingName;
    private ImageButton btnNowPlayingPause;
    private ImageButton btnNowPlayingStop;

    private MusicAdapter adapter;
    private int currentPlayingResId = -1;
    private int currentState = MusicService.STATE_IDLE;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null && listener != null) {
                        // Persist permission
                        try {
                            requireContext().getContentResolver().takePersistableUriPermission(
                                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception ignored) {}

                        String name = getFileName(uri);
                        listener.onFileSelected(uri, name);
                    }
                }
            }
    );

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MusicService.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                currentState = intent.getIntExtra(MusicService.EXTRA_STATE, MusicService.STATE_IDLE);
                String musicName = intent.getStringExtra(MusicService.EXTRA_MUSIC_NAME);
                currentPlayingResId = intent.getIntExtra(MusicService.EXTRA_RES_ID, -1);
                updateNowPlayingUI(musicName);
                if (adapter != null) {
                    adapter.setPlayingResId(currentPlayingResId, currentState);
                }
            }
        }
    };

    public void setListener(OnMusicSelectedListener listener) {
        this.listener = listener;
    }

    public void updateState(int state, String musicName, int resId) {
        currentState = state;
        currentPlayingResId = resId;
        updateNowPlayingUI(musicName);
        if (adapter != null) {
            adapter.setPlayingResId(resId, state);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_music_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        rvMusic = view.findViewById(R.id.rv_music_list);
        btnBack = view.findViewById(R.id.btn_back);
        btnPickFile = view.findViewById(R.id.btn_pick_file);
        nowPlayingSection = view.findViewById(R.id.now_playing_section);
        tvNowPlayingName = view.findViewById(R.id.tv_now_playing_name);
        btnNowPlayingPause = view.findViewById(R.id.btn_now_playing_pause);
        btnNowPlayingStop = view.findViewById(R.id.btn_now_playing_stop);

        rvMusic.setLayoutManager(new LinearLayoutManager(getContext()));

        List<MusicItem> musicList = getRawMusicList();
        adapter = new MusicAdapter(musicList);
        rvMusic.setAdapter(adapter);

        btnBack.setOnClickListener(v -> {
            if (listener != null) listener.onBack();
        });

        btnPickFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/*");
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            filePickerLauncher.launch(intent);
        });

        btnNowPlayingPause.setOnClickListener(v -> {
            if (listener != null) {
                if (currentState == MusicService.STATE_PLAYING) {
                    listener.onPauseMusic();
                } else if (currentState == MusicService.STATE_PAUSED) {
                    listener.onResumeMusic();
                }
            }
        });

        btnNowPlayingStop.setOnClickListener(v -> {
            if (listener != null) listener.onStopMusic();
        });

        // Register broadcast receiver for state updates
        IntentFilter filter = new IntentFilter(MusicService.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(stateReceiver, filter);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            requireContext().unregisterReceiver(stateReceiver);
        } catch (Exception ignored) {}
    }

    private void updateNowPlayingUI(String musicName) {
        if (nowPlayingSection == null) return;

        if (currentState == MusicService.STATE_IDLE) {
            nowPlayingSection.setVisibility(View.GONE);
        } else {
            nowPlayingSection.setVisibility(View.VISIBLE);
            if (musicName != null) {
                tvNowPlayingName.setText(musicName);
            }
            // Update pause/play button icon
            if (currentState == MusicService.STATE_PLAYING) {
                btnNowPlayingPause.setImageResource(R.drawable.ic_pause);
                btnNowPlayingPause.setContentDescription("Tạm dừng");
            } else if (currentState == MusicService.STATE_PAUSED) {
                btnNowPlayingPause.setImageResource(R.drawable.ic_play);
                btnNowPlayingPause.setContentDescription("Tiếp tục");
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private List<MusicItem> getRawMusicList() {
        List<MusicItem> list = new ArrayList<>();
        Field[] fields = R.raw.class.getFields();
        for (Field field : fields) {
            try {
                int resId = field.getInt(null);
                String name = field.getName();
                // Skip non-audio files
                if (name.equals("tmp")) continue;
                list.add(new MusicItem(resId, name));
            } catch (Exception ignored) {}
        }
        return list;
    }

    static class MusicItem {
        int resId;
        String name;

        MusicItem(int resId, String name) {
            this.resId = resId;
            this.name = name;
        }
    }

    private class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {
        private final List<MusicItem> items;
        private int playingResId = -1;
        private int playState = MusicService.STATE_IDLE;

        MusicAdapter(List<MusicItem> items) {
            this.items = items;
        }

        void setPlayingResId(int resId, int state) {
            int oldPos = findPositionByResId(playingResId);
            playingResId = resId;
            playState = state;
            int newPos = findPositionByResId(resId);
            if (oldPos != -1) notifyItemChanged(oldPos);
            if (newPos != -1 && newPos != oldPos) notifyItemChanged(newPos);
        }

        private int findPositionByResId(int resId) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).resId == resId) return i;
            }
            return -1;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MusicItem item = items.get(position);
            // Format display name: replace underscores with spaces, capitalize
            String name = item.name.replace("_", " ");
            if (!name.isEmpty()) {
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
            }
            final String displayName = name;
            holder.tvName.setText(displayName);

            boolean isPlaying = (item.resId == playingResId && playState != MusicService.STATE_IDLE);

            if (isPlaying) {
                holder.itemView.setBackgroundResource(R.drawable.bg_now_playing);
                holder.ivPlayIndicator.setVisibility(View.VISIBLE);
                holder.ivPlayIndicator.setImageResource(
                        playState == MusicService.STATE_PLAYING ? R.drawable.ic_pause : R.drawable.ic_play);
                holder.tvSubtitle.setText(playState == MusicService.STATE_PLAYING ? "Đang phát" : "Tạm dừng");
                holder.tvSubtitle.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.musicPrimary));
            } else {
                holder.itemView.setBackgroundResource(R.drawable.bg_music_item);
                holder.ivPlayIndicator.setVisibility(View.GONE);
                holder.tvSubtitle.setText("Nhạc mặc định");
                holder.tvSubtitle.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.textSecondary));
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMusicSelected(item.resId, displayName);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            TextView tvSubtitle;
            ImageView ivPlayIndicator;
            ImageView ivMusicSource;
            ImageView ivMusicIcon;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_music_name);
                tvSubtitle = itemView.findViewById(R.id.tv_music_subtitle);
                ivPlayIndicator = itemView.findViewById(R.id.iv_play_indicator);
                ivMusicIcon = itemView.findViewById(R.id.iv_music_icon);
            }
        }
    }
}
