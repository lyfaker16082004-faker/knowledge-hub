import { useRemote } from '../../../shared/hooks/useRemote';
import Icon from '../../../shared/components/ui/Icon';
import type { DocumentItem } from '../types';
import { DocumentGrid } from './LibraryUI';

const RELATED_LIMIT = 4;

/**
 * Mục "Tài liệu liên quan" ở cuối trang chi tiết.
 * Backend chấm điểm theo môn học, nhãn, chủ đề và khoa (GET /api/documents/{id}/related).
 * Không có tài liệu nào liên quan thì ẩn hẳn mục này.
 */
export default function RelatedDocuments({ document }: { document: DocumentItem }) {
  const related = useRemote<DocumentItem[]>(`/documents/${document.id}/related?limit=${RELATED_LIMIT}`);

  if (related.error) return null;
  if (!related.loading && !related.data?.length) return null;

  return (
    <section className="dd-related" aria-labelledby="dd-related-title">
      <div className="dd-related-head">
        <div>
          <span className="dd-panel-label">Có thể bạn quan tâm</span>
          <h2 id="dd-related-title">Tài liệu liên quan</h2>
        </div>
        <a className="dd-related-more" href={`#documents?category=${document.categoryId}`}>
          Xem thêm về {document.category}<Icon name="arrow" size={15} />
        </a>
      </div>

      {related.loading
        ? <RelatedSkeleton />
        : <DocumentGrid documents={related.data || []} />}
    </section>
  );
}

/** Khung xám nhấp nháy trong lúc chờ dữ liệu. */
function RelatedSkeleton() {
  const placeholders = Array.from({ length: RELATED_LIMIT }, (_, index) => index);
  return (
    <div className="document-grid" aria-hidden="true">
      {placeholders.map(index => (
        <div key={index} className="dd-skeleton">
          <span className="dd-skeleton-cover" />
          <span className="dd-skeleton-line" />
          <span className="dd-skeleton-line short" />
        </div>
      ))}
    </div>
  );
}
