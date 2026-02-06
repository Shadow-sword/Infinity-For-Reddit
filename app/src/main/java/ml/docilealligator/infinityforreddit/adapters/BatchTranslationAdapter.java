package ml.docilealligator.infinityforreddit.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.comment.Comment;
import ml.docilealligator.infinityforreddit.customtheme.CustomThemeWrapper;
import ml.docilealligator.infinityforreddit.customviews.CommentIndentationView;
import ml.docilealligator.infinityforreddit.translation.BatchTranslationResult;

/**
 * 批量翻译结果展示 Adapter
 * 包含帖子头部和评论列表两种 ViewType
 */
public class BatchTranslationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_POST_HEADER = 0;
    private static final int VIEW_TYPE_COMMENT = 1;

    private final BatchTranslationResult mResult;
    private final ArrayList<Comment> mComments;
    private final int[] mCommentColors;
    private final int mCommentColor;
    private final int mSecondaryTextColor;

    public BatchTranslationAdapter(BatchTranslationResult result, ArrayList<Comment> comments,
                                    CustomThemeWrapper customThemeWrapper) {
        mResult = result;
        mComments = filterValidComments(comments);
        mCommentColors = new int[]{
                customThemeWrapper.getCommentVerticalBarColor1(),
                customThemeWrapper.getCommentVerticalBarColor2(),
                customThemeWrapper.getCommentVerticalBarColor3(),
                customThemeWrapper.getCommentVerticalBarColor4(),
                customThemeWrapper.getCommentVerticalBarColor5(),
                customThemeWrapper.getCommentVerticalBarColor6(),
                customThemeWrapper.getCommentVerticalBarColor7()
        };
        mCommentColor = customThemeWrapper.getCommentColor();
        mSecondaryTextColor = customThemeWrapper.getSecondaryTextColor();
    }

    private ArrayList<Comment> filterValidComments(ArrayList<Comment> comments) {
        ArrayList<Comment> filtered = new ArrayList<>();
        if (comments == null) return filtered;
        for (Comment comment : comments) {
            if (comment.getPlaceholderType() == Comment.NOT_PLACEHOLDER) {
                filtered.add(comment);
            }
        }
        return filtered;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_POST_HEADER : VIEW_TYPE_COMMENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_POST_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_batch_translation_post_header, parent, false);
            return new PostHeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_batch_translation_comment, parent, false);
            return new CommentViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof PostHeaderViewHolder) {
            bindPostHeader((PostHeaderViewHolder) holder);
        } else if (holder instanceof CommentViewHolder) {
            bindComment((CommentViewHolder) holder, position - 1);
        }
    }

    private void bindPostHeader(PostHeaderViewHolder holder) {
        holder.originalTitleTextView.setText(mResult.getOriginalTitle());
        holder.translatedTitleTextView.setText(mResult.getTranslatedTitle());

        // 设置颜色
        holder.originalTitleLabel.setTextColor(mSecondaryTextColor);
        holder.translatedTitleLabel.setTextColor(mSecondaryTextColor);
        holder.originalTitleTextView.setTextColor(mCommentColor);
        holder.translatedTitleTextView.setTextColor(mCommentColor);
        holder.commentsSectionLabel.setTextColor(mCommentColor);

        String originalBody = mResult.getOriginalBody();
        if (originalBody != null && !originalBody.isEmpty()) {
            holder.originalBodyLabel.setVisibility(View.VISIBLE);
            holder.originalBodyTextView.setVisibility(View.VISIBLE);
            holder.originalBodyTextView.setText(originalBody);
            holder.originalBodyLabel.setTextColor(mSecondaryTextColor);
            holder.originalBodyTextView.setTextColor(mCommentColor);

            String translatedBody = mResult.getTranslatedBody();
            if (translatedBody != null && !translatedBody.isEmpty()) {
                holder.translatedBodyLabel.setVisibility(View.VISIBLE);
                holder.translatedBodyTextView.setVisibility(View.VISIBLE);
                holder.translatedBodyTextView.setText(translatedBody);
                holder.translatedBodyLabel.setTextColor(mSecondaryTextColor);
                holder.translatedBodyTextView.setTextColor(mCommentColor);
            }
        }

        // 如果没有评论，隐藏分区标题
        if (mComments.isEmpty()) {
            holder.commentsSectionLabel.setVisibility(View.GONE);
        }
    }

    private void bindComment(CommentViewHolder holder, int commentPosition) {
        if (commentPosition < 0 || commentPosition >= mComments.size()) return;

        Comment comment = mComments.get(commentPosition);

        // 设置缩进
        holder.commentIndentationView.setLevelAndColors(comment.getDepth(), mCommentColors);

        // 设置作者
        holder.authorTextView.setText("u/" + comment.getAuthor());
        holder.authorTextView.setTextColor(mSecondaryTextColor);

        // 设置原文
        String originalText = comment.getCommentRawText();
        holder.originalTextView.setText(originalText != null ? originalText : "");
        holder.originalTextView.setTextColor(mCommentColor);
        holder.originalLabel.setTextColor(mSecondaryTextColor);

        // 设置译文
        HashMap<String, String> translatedComments = mResult.getTranslatedComments();
        String translated = translatedComments != null ? translatedComments.get(comment.getId()) : null;
        holder.translatedTextView.setText(translated != null ? translated : "");
        holder.translatedTextView.setTextColor(mCommentColor);
        holder.translatedLabel.setTextColor(mSecondaryTextColor);
    }

    @Override
    public int getItemCount() {
        return 1 + mComments.size();
    }

    static class PostHeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView originalTitleLabel;
        final TextView originalTitleTextView;
        final TextView translatedTitleLabel;
        final TextView translatedTitleTextView;
        final TextView originalBodyLabel;
        final TextView originalBodyTextView;
        final TextView translatedBodyLabel;
        final TextView translatedBodyTextView;
        final TextView commentsSectionLabel;

        PostHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            originalTitleLabel = itemView.findViewById(R.id.original_title_label);
            originalTitleTextView = itemView.findViewById(R.id.original_title_text_view);
            translatedTitleLabel = itemView.findViewById(R.id.translated_title_label);
            translatedTitleTextView = itemView.findViewById(R.id.translated_title_text_view);
            originalBodyLabel = itemView.findViewById(R.id.original_body_label);
            originalBodyTextView = itemView.findViewById(R.id.original_body_text_view);
            translatedBodyLabel = itemView.findViewById(R.id.translated_body_label);
            translatedBodyTextView = itemView.findViewById(R.id.translated_body_text_view);
            commentsSectionLabel = itemView.findViewById(R.id.comments_section_label);
        }
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        final CommentIndentationView commentIndentationView;
        final TextView authorTextView;
        final TextView originalLabel;
        final TextView originalTextView;
        final TextView translatedLabel;
        final TextView translatedTextView;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            commentIndentationView = itemView.findViewById(R.id.comment_indentation_view);
            authorTextView = itemView.findViewById(R.id.author_text_view);
            originalLabel = itemView.findViewById(R.id.original_label);
            originalTextView = itemView.findViewById(R.id.original_text_view);
            translatedLabel = itemView.findViewById(R.id.translated_label);
            translatedTextView = itemView.findViewById(R.id.translated_text_view);
        }
    }
}
