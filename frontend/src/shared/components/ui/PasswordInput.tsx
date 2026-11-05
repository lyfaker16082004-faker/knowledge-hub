import { useId, useState } from 'react';
import type { InputHTMLAttributes } from 'react';
import Icon from './Icon';
export default function PasswordInput({ label, hint, ...props }: InputHTMLAttributes<HTMLInputElement> & { label: string; hint?: string }) {
 const [shown, setShown] = useState(false); const id = useId();
 return <div className="field"><label htmlFor={id}>{label}</label><div className="password-wrap"><input {...props} id={id} type={shown ? 'text' : 'password'} aria-describedby={hint ? `${id}-hint` : undefined}/><button type="button" className="eye-button" onClick={() => setShown(!shown)} aria-label={shown ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} aria-pressed={shown}><Icon name="eye"/></button></div>{hint && <small id={`${id}-hint`}>{hint}</small>}</div>;
}
