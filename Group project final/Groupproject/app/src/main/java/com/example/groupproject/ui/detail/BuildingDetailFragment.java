package com.example.groupproject.ui.detail;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.groupproject.R;
import com.example.groupproject.model.Comment;
import com.example.groupproject.model.CommentStore;

import java.util.List;

public class BuildingDetailFragment extends Fragment {

    private String currentEmoji = "😊";
    private String buildingId   = "";
    private String defaultEmoji = "🏢";
    private TextView tvMood;
    private LinearLayout commentContainer;
    private LinearLayout layoutComment;
    private TextView tvLockHint;

    public BuildingDetailFragment() {
        super(R.layout.fragment_building_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle    = view.findViewById(R.id.tvTitle);
        tvMood              = view.findViewById(R.id.tvMood);
        TextView tvIntro    = view.findViewById(R.id.tvIntro);
        tvLockHint          = view.findViewById(R.id.tvLockHint);
        layoutComment       = view.findViewById(R.id.layoutComment);
        EditText etComment  = view.findViewById(R.id.etComment);
        Button btnSubmit    = view.findViewById(R.id.btnSubmit);
        commentContainer    = view.findViewById(R.id.commentContainer);
        Button btnTestUnlock = view.findViewById(R.id.btnTestUnlock);

        Button btnEmoji1 = view.findViewById(R.id.btnEmoji1);
        Button btnEmoji2 = view.findViewById(R.id.btnEmoji2);
        Button btnEmoji3 = view.findViewById(R.id.btnEmoji3);
        Button btnEmoji4 = view.findViewById(R.id.btnEmoji4);

        Bundle args = getArguments();
        if (args != null) {
            buildingId   = args.getString("buildingId", "unknown");
            defaultEmoji = args.getString("buildingEmoji", "🏢");
            tvTitle.setText(args.getString("buildingName", "Unknown"));
            tvIntro.setText(args.getString("buildingIntro", ""));
        }

        refreshMoodEmoji();
        applyLockState();

        // 测试解锁按钮
        btnTestUnlock.setOnClickListener(v -> {
            CommentStore.getInstance().unlockBuilding(buildingId);
            applyLockState();
        });

        // emoji 选择
        View.OnClickListener emojiListener = v -> {
            Button btn = (Button) v;
            currentEmoji = btn.getText().toString();
            btnEmoji1.setAlpha(0.4f);
            btnEmoji2.setAlpha(0.4f);
            btnEmoji3.setAlpha(0.4f);
            btnEmoji4.setAlpha(0.4f);
            btn.setAlpha(1.0f);
        };
        btnEmoji1.setOnClickListener(emojiListener);
        btnEmoji2.setOnClickListener(emojiListener);
        btnEmoji3.setOnClickListener(emojiListener);
        btnEmoji4.setOnClickListener(emojiListener);

        // 提交评论
        btnSubmit.setOnClickListener(v -> {
            String text = etComment.getText().toString().trim();
            if (!text.isEmpty()) {
                CommentStore.getInstance().addComment(
                        new Comment(buildingId, currentEmoji, text));
                etComment.setText("");
                refreshMoodEmoji();
                refreshComments();
            }
        });

        refreshComments();
    }

    private void applyLockState() {
        boolean unlocked = CommentStore.getInstance().isUnlocked(buildingId);
        if (unlocked) {
            tvLockHint.setVisibility(View.GONE);
            layoutComment.setVisibility(View.VISIBLE);
        } else {
            tvLockHint.setVisibility(View.VISIBLE);
            layoutComment.setVisibility(View.GONE);
        }
    }

    private void refreshMoodEmoji() {
        String dominant = CommentStore.getInstance().getDominantEmoji(buildingId);
        tvMood.setText(dominant != null ? dominant : defaultEmoji);
    }

    private void refreshComments() {
        commentContainer.removeAllViews();
        List<Comment> comments =
                CommentStore.getInstance().getCommentsForBuilding(buildingId);

        for (int i = 0; i < comments.size(); i++) {
            Comment c = comments.get(i);
            final int index = i;

            // 每条评论用一个横向 LinearLayout 包裹
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            // 评论内容
            TextView tv = new TextView(requireContext());
            tv.setText(c.getEmoji() + "  " + c.getText());
            tv.setTextSize(15f);
            LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tv.setLayoutParams(tvParams);

            // 删除按钮
            Button btnDelete = new Button(requireContext());
            btnDelete.setText("❌");
            btnDelete.setTextSize(12f);
            btnDelete.setPadding(8, 0, 8, 0);
            btnDelete.setBackgroundColor(0x00000000); // 透明背景
            btnDelete.setOnClickListener(v -> {
                CommentStore.getInstance().removeComment(buildingId, index);
                refreshMoodEmoji();
                refreshComments();
            });

            row.addView(tv);
            row.addView(btnDelete);
            commentContainer.addView(row);
        }
    }
}
