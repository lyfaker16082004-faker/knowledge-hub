import { useState } from "react";
import Icon from "../../../shared/components/ui/Icon";
import { errorMessage, mutate } from "../../../shared/api/http-client";
import { useRemote } from "../../../shared/hooks/useRemote";
import type { Account } from "../../auth/types";
import type { CommentItem, PostDetail } from "../types";
import CommentThread from "../components/CommentThread";
import {
  OwnerMenu,
  PostContent,
  PostEditor,
  TypeBadge,
  UserAvatar,
  authorName,
  sharePost,
  timeAgo,
  usePostActions,
} from "../components/PostUI";

/*
 * Trang chi tiết bài viết (#post/{id}) - Sprint 3.
 * Hiện nội dung Markdown, nút thích / lưu / chia sẻ và khu vực bình luận nhiều cấp.
 */

export default function PostDetailPage({
  id,
  user,
}: {
  id: number;
  user: Account | null;
}) {
  const detail = useRemote<PostDetail>(`/posts/${id}`);
  const comments = useRemote<CommentItem[]>(`/posts/${id}/comments`);
  const [editing, setEditing] = useState(false);
  const [override, setOverride] = useState<PostDetail | null>(null);
  const data = override || detail.data;
  return (
    <div className="cf-detail">
      <a className="cf-back" href="#community">
        <Icon name="chevronLeft" size={16} />
        Cộng đồng
      </a>
      {detail.loading ? (
        <div
          className="cf-card cf-skeleton tall"
          role="status"
          aria-label="Đang tải bài viết"
        >
          <span />
          <span />
          <span />
        </div>
      ) : detail.error ? (
        <div className="cf-panel cf-empty">
          <Icon name="message" size={32} />
          <h2>{detail.error}</h2>
          <p>Bài viết có thể đã bị xóa hoặc bạn không có quyền xem.</p>
          <a className="button primary" href="#community">
            Về Cộng đồng
          </a>
        </div>
      ) : (
        data && (
          <>
            <PostBody
              key={data.version}
              data={data}
              user={user}
              onEdit={() => setEditing(true)}
            />
            {comments.error ? (
              <div className="cf-panel cf-error" role="alert">
                <p>{comments.error}</p>
                <button
                  type="button"
                  className="button secondary compact"
                  onClick={comments.refresh}
                >
                  Thử lại
                </button>
              </div>
            ) : (
              comments.data && (
                <CommentThread
                  postId={id}
                  user={user}
                  initial={comments.data}
                  isQuestion={data.post.type === "QUESTION"}
                  onCount={() => undefined}
                />
              )
            )}
          </>
        )
      )}
      {editing && data && (
        <PostEditor
          initial={data}
          onClose={() => setEditing(false)}
          onSaved={(saved) => {
            setOverride(saved);
            setEditing(false);
          }}
        />
      )}
    </div>
  );
}

/** Phần thân bài viết: tác giả, tiêu đề, nội dung, thẻ và các nút tương tác. */
function PostBody({
  data,
  user,
  onEdit,
}: {
  data: PostDetail;
  user: Account | null;
  onEdit: () => void;
}) {
  const { post } = data;
  const actions = usePostActions(post, !!user);
  const [hint, setHint] = useState("");
  const [error, setError] = useState("");
  const name = authorName(post.author);
  /** Xoá bài viết (tác giả hoặc kiểm duyệt viên). */
  async function remove() {
    if (
      !confirm(
        "Xóa bài viết này? Bài viết sẽ không còn hiển thị với mọi người.",
      )
    )
      return;
    try {
      await mutate(`/posts/${post.id}`, "DELETE");
      location.hash = "community";
    } catch (e) {
      setError(errorMessage(e));
    }
  }
  return (
    <article className="cf-panel cf-post">
      <header className="cf-card-head">
        <a href={`#person/${post.author.id}`} className="cf-author">
          <UserAvatar
            name={name}
            url={post.author.avatarUrl}
            id={post.author.id}
            size="lg"
          />
          <span>
            <strong>{name}</strong>
            <small>
              {timeAgo(post.publishedAt)}
              {post.category && (
                <>
                  {" "}
                  ·{" "}
                  <a href={`#community?category=${post.categoryId}`}>
                    {post.category}
                  </a>
                </>
              )}{" "}
              · {post.views} lượt xem
            </small>
          </span>
        </a>
        <TypeBadge type={post.type} />
        {(post.mine || data.canModerate) && (
          <OwnerMenu
            onEdit={post.mine ? onEdit : undefined}
            onDelete={remove}
          />
        )}
      </header>
      <h1>{post.title}</h1>
      <PostContent text={data.content} />
      {post.tags.length > 0 && (
        <div className="cf-tags">
          {post.tags.map((tag) => (
            <a key={tag} href={`#community?tag=${encodeURIComponent(tag)}`}>
              #{tag}
            </a>
          ))}
        </div>
      )}
      <footer className="cf-actions">
        <button
          type="button"
          className={actions.liked ? "on like" : "like"}
          aria-pressed={actions.liked}
          onClick={actions.like}
        >
          <Icon name="heart" size={17} />
          {actions.likes}
          <span className="cf-hide-sm"> lượt thích</span>
        </button>
        <button
          type="button"
          className={actions.saved ? "on save" : "save"}
          aria-pressed={actions.saved}
          onClick={actions.bookmark}
        >
          <Icon name="bookmark" size={17} />
          {actions.saved ? "Đã lưu" : "Lưu bài"}
        </button>
        <button
          type="button"
          onClick={async () => {
            setHint(await sharePost(post.id, post.title));
            setTimeout(() => setHint(""), 2500);
          }}
        >
          <Icon name="share" size={17} />
          Chia sẻ
        </button>
        {(hint || actions.error || error) && (
          <span
            className={actions.error || error ? "cf-inline-error" : "cf-hint"}
            role="status"
          >
            {actions.error || error || hint}
          </span>
        )}
      </footer>
    </article>
  );
}
