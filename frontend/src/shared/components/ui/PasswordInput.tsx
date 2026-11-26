import { useId, useState } from 'react';
import type { InputHTMLAttributes } from 'react';
import Icon from './Icon';

type Props = InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  hint?: string; // Dòng gợi ý nhỏ bên dưới ô nhập
};

/** Ô nhập mật khẩu có nút "con mắt" để ẩn / hiện mật khẩu. */
export default function PasswordInput({ label, hint, ...props }: Props) {
  const [shown, setShown] = useState(false);
  const id = useId(); // id riêng để nối <label> với <input>
  const hintId = hint ? `${id}-hint` : undefined;

  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <div className="password-wrap">
        <input {...props} id={id} type={shown ? 'text' : 'password'} aria-describedby={hintId} />
        <button
          type="button"
          className="eye-button"
          onClick={() => setShown(!shown)}
          aria-label={shown ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
          aria-pressed={shown}
        >
          <Icon name="eye" />
        </button>
      </div>
      {hint && <small id={hintId}>{hint}</small>}
    </div>
  );
}
