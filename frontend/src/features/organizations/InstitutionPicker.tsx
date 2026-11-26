import { useEffect, useId, useState } from 'react';
import Icon from '../../shared/components/ui/Icon';
import { get, errorMessage } from '../../shared/api/http-client';
import { useRemote } from '../../shared/hooks/useRemote';

/** Một trường đại học / công ty / tổ chức khác. */
export interface Institution {
  id: number;
  name: string;
  type: 'UNIVERSITY' | 'COMPANY' | 'OTHER';
}

/** Một khoa (hoặc phòng ban) thuộc trường. */
export interface Faculty {
  id: number;
  institutionId: number;
  name: string;
}

type Props = {
  institutionId: string; // id trường đang chọn ('' = chưa chọn)
  facultyId: string; // id khoa đang chọn ('' = chưa chọn)
  emptyLabel?: string; // Chữ hiện ở lựa chọn trống của ô Trường
  onChange: (institution: string, faculty: string) => void;
};

/**
 * Hai ô chọn liền nhau: Trường / Tổ chức và Khoa / Chuyên ngành.
 * Danh sách khoa chỉ được tải sau khi đã chọn trường.
 * Loại "OTHER" (người đi làm / tự do) không có khoa nên ẩn ô Khoa.
 */
export default function InstitutionPicker({
  institutionId,
  facultyId,
  onChange,
  emptyLabel = 'Chưa chọn — xem toàn bộ thư viện',
}: Props) {
  const schools = useRemote<Institution[]>('/institutions');
  const [faculties, setFaculties] = useState<Faculty[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const id = useId();

  // Tìm trường đang được chọn trong danh sách
  let school: Institution | undefined = undefined;
  if (schools.data) {
    school = schools.data.find((s) => String(s.id) === institutionId);
  }
  const hasFaculties = school !== undefined && school.type !== 'OTHER';

  // Mỗi khi đổi trường -> tải lại danh sách khoa của trường đó
  useEffect(() => {
    setFaculties([]);
    setError('');

    if (!school || school.type === 'OTHER') {
      setLoading(false);
      return;
    }

    const controller = new AbortController();
    setLoading(true);
    get<Faculty[]>(`/institutions/${school.id}/faculties`, controller.signal)
      .then((list) => setFaculties(list))
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

    return () => controller.abort();
  }, [school?.id, school?.type]);

  return (
    <div className="organization-picker">
      {/* Ô chọn Trường / Tổ chức */}
      <div className="field">
        <label htmlFor={`${id}-institution`}>Trường / Tổ chức</label>
        <select
          id={`${id}-institution`}
          name="institutionId"
          value={institutionId}
          disabled={schools.loading || !!schools.error}
          onChange={(event) => onChange(event.target.value, '')}
        >
          <option value="">{emptyLabel}</option>
          {schools.data?.map((s) => (
            <option value={s.id} key={s.id}>
              {s.name}
              {s.type === 'OTHER' ? ' (người đi làm / tự do)' : ''}
            </option>
          ))}
        </select>

        {schools.loading && <small role="status">Đang tải danh sách trường…</small>}

        {schools.error && (
          <div className="inline-error" role="alert">
            {schools.error}
            <button className="text-button" type="button" onClick={schools.refresh}>
              <Icon name="refresh" size={16} />
              Thử lại
            </button>
          </div>
        )}
      </div>

      {/* Ô chọn Khoa: chỉ hiện khi trường có khoa */}
      {hasFaculties && (
        <div className="field">
          <label htmlFor={`${id}-faculty`}>Khoa / Chuyên ngành</label>
          <select
            id={`${id}-faculty`}
            name="facultyId"
            value={facultyId}
            disabled={loading || !!error}
            onChange={(event) => onChange(institutionId, event.target.value)}
          >
            <option value="">Không chọn khoa</option>
            {faculties.map((f) => (
              <option value={f.id} key={f.id}>
                {f.name}
              </option>
            ))}
          </select>

          {loading && <small role="status">Đang tải danh sách khoa…</small>}
          {error && (
            <small className="inline-error" role="alert">
              {error} Hãy chọn lại trường để thử lại.
            </small>
          )}
        </div>
      )}

      {school?.type === 'OTHER' && (
        <p className="organization-hint">Bạn sẽ xem toàn bộ thư viện, không cần chọn khoa.</p>
      )}
    </div>
  );
}
