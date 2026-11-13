import {useRef,useState} from 'react';
import type {ChangeEvent,MouseEvent,ClipboardEvent} from 'react';
import {sanitizeDescriptionHtml} from './DocumentDescription';

function safeLink(value:string){try{const u=new URL(value);return ['http:','https:','mailto:'].includes(u.protocol)?u.href:null;}catch{return null;}}
function initialHtml(source:string){if(!source)return '';if(/<\/?(?:p|div|h[1-6]|ul|ol|li|blockquote|strong|b|em|i|u|s|a|img|br|span|pre|code|font)\b/i.test(source))return sanitizeDescriptionHtml(source);return '<p>'+source.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/\r?\n/g,'<br>')+'</p>';}
export default function SummernoteEditor({initialValue='',maxLength=10000}:{initialValue?:string|null;maxLength?:number}){
 const startValue=initialValue||'';const editor=useRef<HTMLDivElement>(null);const hiddenField=useRef<HTMLTextAreaElement>(null);const savedRange=useRef<Range|null>(null);
 const initial=useRef({__html:initialHtml(startValue)});const [value,setValue]=useState(initialValue||'');const [preview,setPreview]=useState(false);const [insertKind,setInsertKind]=useState<'link'|'image'|null>(null);const [url,setUrl]=useState('');const [label,setLabel]=useState('');const [error,setError]=useState('');
 function sync(){const node=editor.current;if(!node)return;const raw=node.innerHTML;const clean=sanitizeDescriptionHtml(raw);if(clean!==raw)node.innerHTML=clean;setValue(clean);hiddenField.current?.setCustomValidity(clean.length>maxLength?'Mô tả vượt quá '+maxLength+' ký tự được phép.':'');}
 function saveSelection(){const selection=window.getSelection();if(selection?.rangeCount)savedRange.current=selection.getRangeAt(0).cloneRange();} function rememberSelection(event:MouseEvent<HTMLElement>){event.preventDefault();saveSelection();editor.current?.focus();}
 function restoreSelection(){const selection=window.getSelection();if(savedRange.current&&selection){selection.removeAllRanges();selection.addRange(savedRange.current);}editor.current?.focus();}
 function command(name:string,arg?:string){restoreSelection();document.execCommand(name,false,arg);sync();}
 function openInsert(kind:'link'|'image'){const selection=window.getSelection();if(selection?.rangeCount)savedRange.current=selection.getRangeAt(0).cloneRange();setInsertKind(kind);setUrl('');setLabel('');setError('');}
 function insert(){const image=insertKind==='image';const valid=image?(()=>{try{const u=new URL(url);return u.protocol==='https:'?u.href:null;}catch{return null;}})():safeLink(url);if(!valid){setError(image?'Dùng URL ảnh công khai bắt đầu bằng https://.':'Nhập liên kết http://, https:// hoặc mailto:.');return;}
  restoreSelection();const selection=window.getSelection();const range=selection?.rangeCount?selection.getRangeAt(0):null;
  if(image){const img=document.createElement('img');img.src=valid;img.alt=label.trim()||'Ảnh minh họa';range?.deleteContents();range?.insertNode(img);range?.setStartAfter(img);range?.collapse(true);}
  else if(range&&!range.collapsed){document.execCommand('createLink',false,valid);}else{const anchor=document.createElement('a');anchor.href=valid;anchor.textContent=label.trim()||valid;range?.insertNode(anchor);range?.setStartAfter(anchor);range?.collapse(true);}
  if(selection&&range){selection.removeAllRanges();selection.addRange(range);}sync();setInsertKind(null);setUrl('');setLabel('');
 }
 function pastePlainText(event:ClipboardEvent<HTMLDivElement>){event.preventDefault();const text=event.clipboardData.getData('text/plain');document.execCommand('insertText',false,text);sync();}
 function sizeChange(event:ChangeEvent<HTMLSelectElement>){command('fontSize',event.target.value);}
 return <div className="summernote-editor"><div className="summernote-toolbar" role="toolbar" aria-label="Thanh công cụ soạn mô tả">
  <label className="summernote-select"><span>Kiểu chữ</span><select aria-label="Kiểu đoạn văn" defaultValue="p" onMouseDown={saveSelection} onChange={e=>command('formatBlock',e.target.value)}><option value="p">Đoạn văn</option><option value="h2">Tiêu đề lớn</option><option value="h3">Tiêu đề</option><option value="h4">Tiêu đề nhỏ</option><option value="blockquote">Trích dẫn</option></select></label>
  <span className="summernote-divider"/>
  <button type="button" title="In đậm" aria-label="In đậm" onMouseDown={rememberSelection} onClick={()=>command('bold')}><b>B</b></button><button type="button" title="In nghiêng" aria-label="In nghiêng" onMouseDown={rememberSelection} onClick={()=>command('italic')}><i>I</i></button><button type="button" title="Gạch chân" aria-label="Gạch chân" onMouseDown={rememberSelection} onClick={()=>command('underline')}><u>U</u></button><button type="button" title="Gạch ngang" aria-label="Gạch ngang" onMouseDown={rememberSelection} onClick={()=>command('strikeThrough')}><s>S</s></button>
  <label className="summernote-select summernote-size"><span>Cỡ chữ</span><select aria-label="Cỡ chữ" defaultValue="" onMouseDown={saveSelection} onChange={sizeChange}><option value="" disabled>Cỡ</option><option value="2">Nhỏ</option><option value="3">Vừa</option><option value="4">Lớn</option><option value="5">Rất lớn</option></select></label>
  <label className="summernote-color" title="Màu chữ">A<input type="color" aria-label="Màu chữ" defaultValue="#243247" onMouseDown={saveSelection} onChange={e=>command('foreColor',e.target.value)}/></label>
  <span className="summernote-divider"/>
  <button type="button" title="Danh sách chấm đầu dòng" aria-label="Danh sách chấm đầu dòng" onMouseDown={rememberSelection} onClick={()=>command('insertUnorderedList')}>• List</button><button type="button" title="Danh sách đánh số" aria-label="Danh sách đánh số" onMouseDown={rememberSelection} onClick={()=>command('insertOrderedList')}>1. List</button><button type="button" title="Căn trái" aria-label="Căn trái" onMouseDown={rememberSelection} onClick={()=>command('justifyLeft')}>≡</button><button type="button" title="Căn giữa" aria-label="Căn giữa" onMouseDown={rememberSelection} onClick={()=>command('justifyCenter')}>☰</button>
  <span className="summernote-divider"/><button type="button" title="Chèn liên kết" aria-label="Chèn liên kết" onMouseDown={e=>e.preventDefault()} onClick={()=>openInsert('link')}>↗ Link</button><button type="button" title="Chèn ảnh từ URL" aria-label="Chèn ảnh từ URL" onMouseDown={e=>e.preventDefault()} onClick={()=>openInsert('image')}>▧ Ảnh</button><button type="button" title="Xóa định dạng" aria-label="Xóa định dạng" onMouseDown={rememberSelection} onClick={()=>command('removeFormat')}>Tx</button>
  <span className="summernote-spacer"/><button type="button" className="summernote-preview-button" aria-pressed={preview} onClick={()=>setPreview(!preview)}>{preview?'Quay lại soạn':'Xem trước'}</button>
 </div>
 {insertKind&&<div className="summernote-insert"><label>{insertKind==='image'?'URL ảnh':'Địa chỉ liên kết'}<input autoFocus type="url" value={url} onChange={e=>setUrl(e.target.value)} placeholder={insertKind==='image'?'https://.../anh.jpg':'https://...'} onKeyDown={e=>{if(e.key==='Enter'){e.preventDefault();insert();}}}/></label><label>{insertKind==='image'?'Mô tả ảnh':'Nội dung liên kết'}<input value={label} onChange={e=>setLabel(e.target.value)} placeholder={insertKind==='image'?'Ví dụ: Sơ đồ bài học':'Chọn văn bản hoặc nhập tên liên kết'}/></label><button type="button" className="button primary" onClick={insert}>Chèn</button><button type="button" className="button secondary" onClick={()=>setInsertKind(null)}>Hủy</button>{error&&<span role="alert">{error}</span>}</div>}
 <textarea ref={hiddenField} className="summernote-value" name="description" aria-label="Mô tả chi tiết" maxLength={maxLength} value={value} onChange={()=>{}}/>
 <div ref={editor} className="summernote-canvas" contentEditable={!preview} suppressContentEditableWarning dangerouslySetInnerHTML={initial.current} onInput={sync} onPaste={pastePlainText} hidden={preview} role="textbox" aria-multiline="true" aria-label="Mô tả chi tiết"/>
 {preview&&<div className="summernote-preview" dangerouslySetInnerHTML={{__html:sanitizeDescriptionHtml(value)}}/>}
 <div className="summernote-footer"><span>Soạn trực tiếp như trong trình soạn thảo. Ảnh được chèn bằng URL HTTPS.</span><span>{value.length.toLocaleString('vi-VN')}/{maxLength.toLocaleString('vi-VN')}</span></div>
 </div>;
}





