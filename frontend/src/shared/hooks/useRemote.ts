import { useCallback, useEffect, useState } from 'react';
import { get, errorMessage } from '../api/http-client';

/**
 * Hook tải dữ liệu từ API (GET).
 *
 * Cách dùng:
 *   const docs = useRemote<DocumentItem[]>('/documents');
 *   docs.loading -> đang tải
 *   docs.error   -> câu báo lỗi (nếu có)
 *   docs.data    -> dữ liệu khi tải xong
 *   docs.refresh -> gọi để tải lại
 *
 * Truyền path = null nếu chưa muốn tải (ví dụ: chưa đăng nhập).
 * Khi path thay đổi, hook sẽ tự tải lại dữ liệu mới.
 */
export function useRemote<T>(path: string | null, dependency = '') {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [version, setVersion] = useState(0); // Tăng lên 1 mỗi khi muốn tải lại

  const refresh = useCallback(() => {
    setVersion((n) => n + 1);
  }, []);

  useEffect(() => {
    // Không có đường dẫn -> không tải gì cả
    if (path === null) {
      setData(null);
      setError('');
      setLoading(false);
      return;
    }

    // AbortController dùng để huỷ request cũ nếu người dùng chuyển trang giữa chừng
    const controller = new AbortController();
    setLoading(true);
    setError('');
    setData(null);

    get<T>(path, controller.signal)
      .then((result) => {
        setData(result);
      })
      .catch((e) => {
        if (!controller.signal.aborted) {
          setError(errorMessage(e));
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    // Khi component bị huỷ hoặc path đổi -> huỷ request đang chạy
    return () => controller.abort();
  }, [path, version, dependency]);

  return { data, error, loading, refresh };
}
